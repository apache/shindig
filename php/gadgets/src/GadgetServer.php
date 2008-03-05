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
	private $options;
	private $locale;
	private $httpFetcher;
	
	public function processGadget($gadgetId, $userPrefs, $locale, $rctx, $options, $httpFetcher, $registry)
	{
		global $config;
		$this->gadgetId = $gadgetId;
		$this->userPrefs = $userPrefs;
		$this->renderingContext = $rctx;
		$this->locale = $locale;
		$this->registry = $registry;
		$this->options = $options;
		$this->httpFetcher = $httpFetcher;
		$this->gc = new GadgetContext($httpFetcher, $locale, $rctx, $options, $registry);
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
		$request = new remoteContentRequest($this->gadgetId->getURI());
		list($xml) = $this->httpFetcher->fetch($request, $this->options);
		if ($xml->getHttpCode() != '200') {
			throw new GadgetException("Failed to retrieve gadget content");
		}
		$specParser = new GadgetSpecParser();
		$spec = $specParser->parse($xml->getResponseContent());
		$gadget = new Gadget($this->gadgetId, $spec, $this->userPrefs);
		return $gadget;
	}
	
	private function featuresLoad($gadget)
	{
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



