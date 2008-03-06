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

class MessageBundleSubstituter extends GadgetFeatureFactory {
	public function create()
	{
		return new MessageBundleSubstituterFeature();
	}
}

class MessageBundleSubstituterFeature extends GadgetFeature {
	private $parser;
	private $bundle = null;
	
	public function __construct()
	{
		$this->parser = new MessageBundleParser();
	}
	
	private function getLocaleSpec($spec, $locale)
	{
		$localeSpecs = $spec->getLocaleSpecs();
		foreach ( $localeSpecs as $locSpec ) {
			//fix me
			if ($locSpec->getLocale()->equals($locale)) {
				return $locSpec;
			}
		}
		return null;
	}
	
	public function prepare($gadget, $context, $params)
	{
		$locale = $context->getLocale();
		// en-US
		$localeData = $this->getLocaleSpec($gadget, $locale);
		if ($localeData == null) {
			// en-all
			$localeData = $this->getLocaleSpec($gadget, new Locale($locale->getLanguage(), "all"));
		}
		if ($localeData == null) {
			// all-all
			$localeData = $this->getLocaleSpec($gadget, new Locale("all", "all"));
		}
		if ($localeData != null) {
			$uri = $localeData->getURI();
			if ($uri != null) {
				// We definitely need a bundle, now we need to fetch it.
				// Doing it a little different then the java version since our fetcher and cache are intergrated
				$fetcher = $context->getHttpFetcher();
				$response = $fetcher->fetch(new remoteContentRequest($uri));
				//TODO caching the parsed bundle instead of just the xml data would be a lot more efficient and speedy
				$this->bundle = $this->parser->parse($response->getResponseContent());				
			}
		}
	}
	
	public function process($gadget, $context, $params)
	{
		$js = '';
		$moduleId = $gadget->getId()->getModuleId();
		$locale = $context->getLocale();
		$setLangFmt = "gadgets.prefs_.setLanguage(%d, \"%s\");";
		$setCountryFmt = "gadgets.prefs_.setCountry(%d, \"%s\");";
		$js .= sprintf($setLangFmt, $moduleId, $locale->getLanguage());
		$js .= sprintf($setCountryFmt, $moduleId, $locale->getCountry());
		if ($this->bundle != null) {
			$gadget->setCurrentMessageBundle($this->bundle);
			$gadget->getSubstitutions()->addSubstitutions('MSG', $this->bundle->getMessages());
			$rc = $context->getRenderingContext();
			if ($rc == 'GADGET') {
				$msgs = $this->bundle->getMessages();
				$json = array();
				foreach ( $msgs as $key => $value ) {
					$json[$key] = $value;
				}
				$setMsgFmt = "gadgets.prefs_.setMsg(%d, %s);";
				$js .= sprintf($setMsgFmt, $moduleId, json_encode($json));
			}
		}
		$gadget->addJsLibrary(JsLibrary::create('INLINE', $js));
	}
}