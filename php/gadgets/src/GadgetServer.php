<?
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */

/*
 * This isn't a multi threaded java envirioment, so we do things a bit more straightforward with context blocks and workflows,
 * which means departing from how the shinding java implementation works but it saves a lot 'dead' code here
 */

class GadgetServer {
	private $registry;
	private $blacklist;
	private $gc;
	private $gadgetId;
	private $userPrefs;
	private $renderingContext;
	private $locale;
	private $httpFetcher;
	
	public function processGadget($gadgetId, $userPrefs, $locale, $rctx, $httpFetcher, $registry)
	{
		global $config;
		$this->gadgetId = $gadgetId;
		$this->userPrefs = $userPrefs;
		$this->renderingContext = $rctx;
		$this->locale = $locale;
		$this->registry = $registry;
		$this->httpFetcher = $httpFetcher;
		$this->gc = new GadgetContext($httpFetcher, $locale, $rctx, $registry);
		$this->blacklist = new $config['blacklist_class']();
		$gadget = $this->specLoad();
		$this->featuresLoad($gadget);
		return $gadget;
	}
	
	private function specLoad()
	{
		if ($this->blacklist != null && $this->blacklist->isBlacklisted($this->gadgetId->getURI())) {
			throw new GadgetException("Gadget is blacklisted");
		}
		$request = new RemoteContentRequest($this->gadgetId->getURI());
		$xml = $this->httpFetcher->fetch($request);
		if ($xml->getHttpCode() != '200') {
			throw new GadgetException("Failed to retrieve gadget content");
		}
		$specParser = new GadgetSpecParser();
		$gadget = $specParser->parse($xml->getResponseContent(), $this->gadgetId, $this->userPrefs);
		return $gadget;
	}
		
	private function getBundle($localeSpec, $context)
	{
		if ($localeSpec != null) {
			$uri = $localeSpec->getURI();
			if ($uri != null) {
				$fetcher = $context->getHttpFetcher();
				$response = $fetcher->fetch(new RemoteContentRequest($uri));
				$parser = new MessageBundleParser();
				$bundle = $parser->parse($response->getResponseContent());
				return $bundle;				
			}
		}
		return null;
	}
	
	private function localeSpec($gadget, $locale)
	{
		$localeSpecs = $gadget->getLocaleSpecs();
		foreach ( $localeSpecs as $locSpec ) {
			//fix me
			if ($locSpec->getLocale()->equals($locale)) {
				return $locSpec;
			}
		}
		return null;
	}
	
	private function getLocaleSpec($gadget)
	{
		$locale = $this->gc->getLocale();
		// en-US
		$localeSpec = $this->localeSpec($gadget, $locale);
		if ($localeSpec == null) {
			// en-all
			$localeSpec = $this->localeSpec($gadget, new Locale($locale->getLanguage(), "all"));
		}
		if ($localeSpec == null) {
			// all-all
			$localeSpec = $this->localeSpec($gadget, new Locale("all", "all"));
		}
		return $localeSpec;
	}
	
	private function featuresLoad($gadget)
	{
		//NOTE i've been a bit liberal here with folding code into this function, while it did get a bit long, the many include()'s are slowing us down
		// Should really clean this up a bit in the future though
		$localeSpec = $this->getLocaleSpec($gadget);
		
		// get the message bundle for this gadget
		$bundle = $this->getBundle($localeSpec, $this->gc);
		
		//FIXME this is a half-assed solution between following the refactoring and maintaining some of the old code, fixing this up later
		$gadget->setMessageBundle($bundle);
		
		// perform substitutions
		$substitutor = $gadget->getSubstitutions();
		
		// Module ID
		$substitutor->addSubstitution('MODULE', "ID", $gadget->getId()->getModuleId());
		
		// Messages (multi-language)
		if ($bundle) {
			$gadget->getSubstitutions()->addSubstitutions('MSG', $bundle->getMessages());
		}
		
		// Bidi support
		$rtl = false;
		if ($localeSpec != null) {
			$rtl = $localeSpec->isRightToLeft();
		}
		$substitutor->addSubstitution('BIDI', "START_EDGE", $rtl ? "right" : "left");
		$substitutor->addSubstitution('BIDI', "END_EDGE", $rtl ? "left" : "right");
		$substitutor->addSubstitution('BIDI', "DIR", $rtl ? "rtl" : "ltr");
		$substitutor->addSubstitution('BIDI', "REVERSE_DIR", $rtl ? "ltr" : "rtl");
		
		// userPref's
		$upValues = $gadget->getUserPrefValues();
		foreach ( $gadget->getUserPrefs() as $pref ) {
			$name = $pref->getName();
			$value = $upValues->getPref($name);
			if ($value == null) {
				$value = $pref->getDefaultValue();
			}
			if ($value == null) {
				$value = "";
			}
			$substitutor->addSubstitution('USER_PREF', $name, $value);
		}
		
		// Process required / desired features
		$requires = $gadget->getRequires();
		$needed = array();
		$optionalNames = array();
		foreach ( $requires as $key => $entry ) {
			$needed[] = $key;
			if ($entry->isOptional()) {
				$optionalNames[] = $key;
			}
		}
		$resultsFound = array();
		$resultsMissing = array();
		$missingOptional = array();
		$missingRequired = array();
		$this->registry->getIncludedFeatures($needed, $resultsFound, $resultsMissing);
		foreach ( $resultsMissing as $missingResult ) {
			if (in_array($missingResult, $optionalNames)) {
				$missingOptional[$missingResult] = $missingResult;
			} else {
				$missingRequired[$missingResult] = $missingResult;
			}
		}
		if (count($missingRequired)) {
			throw new GadgetException("Unsupported feature(s): " . implode(', ', $missingRequired));
		}
		// create features
		$features = array();
		foreach ( $resultsFound as $entry ) {
			$features[$entry] = $this->registry->getEntry($entry)->getFeature()->create();
		}
		// prepare them
		foreach ( $features as $key => $feature ) {
			$params = $gadget->getFeatureParams($gadget, $this->registry->getEntry($key));
			$feature->prepare($gadget, $this->gc, $params);
		}
		// and process them
		foreach ( $features as $key => $feature ) {
			$params = $gadget->getFeatureParams($gadget, $this->registry->getEntry($key));
			$feature->process($gadget, $this->gc, $params);
		}
	}
}



