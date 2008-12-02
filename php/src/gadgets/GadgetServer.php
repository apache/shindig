<?php
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

class GadgetServer {

  public function processGadget($context) {
    $gadget = $this->specLoad($context);
    $this->featuresLoad($gadget, $context);
    return $gadget;
  }

  private function specLoad($context) {
    if ($context->getBlacklist() != null && $context->getBlacklist()->isBlacklisted($context->getUrl())) {
      throw new GadgetException("Gadget is blacklisted");
    }
    $request = new RemoteContentRequest($context->getUrl());
    $xml = $context->getHttpFetcher()->fetch($request, $context);
    if ($xml->getHttpCode() != '200') {
      throw new GadgetException("Failed to retrieve gadget content");
    }
    $specParser = new GadgetSpecParser();
    $gadget = $specParser->parse($xml->getResponseContent(), $context);
    return $gadget;
  }

  private function getBundle($context, $gadget) {
    $locale = $context->getLocale();
    $localeSpec = $this->localeSpec($gadget, $locale); // en-US
    $language_allSpec = $this->localeSpec($gadget, new Locale($locale->getLanguage(), "all")); // en-all
    $all_allSpec = $this->localeSpec($gadget, new Locale("all", "all")); // all-all
    $messagesArray = $this->getMessagesArrayForSpecs($context, array($localeSpec, $language_allSpec, $all_allSpec));
    if (count($messagesArray) == 0) {
      return null;
    }
    for ($i = 1; $i < count($messagesArray); ++ $i) {
      $diff = array_diff_key($messagesArray[$i], $messagesArray[0]);
      foreach ($diff as $diffKey => $diffValue) {
        $messagesArray[0][$diffKey] = $diffValue;
      }
    }
    return new MessageBundle($messagesArray[0]);
  }

  private function getMessagesArrayForSpecs($context, Array $specs) {
    $requestArray = array();
    $contextArray = array();
    $messagesArray = array();
    foreach ($specs as $spec) {
      if ($spec == null) continue;
      $uri = $spec->getURI();
      if ($uri == null) continue;
      $requestArray[] = new RemoteContentRequest($uri);
      $contextArray[] = $context;
    }
    if (count($requestArray) == 0) return array();
    $fetcher = $context->getHttpFetcher();
    $responseArray = $fetcher->multiFetch($requestArray, $contextArray);
    $parser = new MessageBundleParser();
    foreach ($responseArray as $response) {
      $messagesArray[] = $parser->parse($response->getResponseContent());
    }
    return $messagesArray;
  }

  private function localeSpec($gadget, $locale) {
    $localeSpecs = $gadget->getLocaleSpecs();
    foreach ($localeSpecs as $locSpec) {
      //fix me
      if ($locSpec->getLocale()->equals($locale)) {
        return $locSpec;
      }
    }
    return null;
  }

  private function featuresLoad(Gadget $gadget, $context) {
    //NOTE i've been a bit liberal here with folding code into this function, while it did get a bit long, the many include()'s are slowing us down
  	// get the message bundle for this gadget
    $bundle = $this->getBundle($context, $gadget);
    //FIXME this is a half-assed solution between following the refactoring and maintaining some of the old code, fixing this up later
    $gadget->setMessageBundle($bundle);
    // perform substitutions
    $substitutor = $gadget->getSubstitutions();
    // Module ID
    $substitutor->addSubstitution('MODULE', "ID", $gadget->getId()->getModuleId());
    if ($bundle) {
      $gadget->getSubstitutions()->addSubstitutions('MSG', $bundle->getMessages());
    }    
    // Bidi support
    $rtl = false;
    $locale = $context->getLocale();
    $localeSpec = $this->localeSpec($gadget, $locale); // en-US
    if ($localeSpec != null) {
      $rtl = $localeSpec->isRightToLeft();
    }
    $substitutor->addSubstitution('BIDI', "START_EDGE", $rtl ? "right" : "left");
    $substitutor->addSubstitution('BIDI', "END_EDGE", $rtl ? "left" : "right");
    $substitutor->addSubstitution('BIDI', "DIR", $rtl ? "rtl" : "ltr");
    $substitutor->addSubstitution('BIDI', "REVERSE_DIR", $rtl ? "ltr" : "rtl");    
    // userPref's
    $upValues = $gadget->getUserPrefValues();
    foreach ($gadget->getUserPrefs() as $pref) {
      $name = $pref->getName();
      $value = $upValues->getPref($name);
      if ($value == null) {
        $value = $pref->getDefaultValue();
      }
      if ($value == null) {
        $value = "";
      }
      $substitutor->addSubstitution('UP', $name, $value);
    }
    $this->substitutePreloads($gadget, $substitutor);
    // Process required / desired features
    $requires = $gadget->getRequires();
    $needed = array();
    $optionalNames = array();
    foreach ($requires as $key => $entry) {
      $needed[] = $key;
      if ($entry->isOptional()) {
        $optionalNames[] = $key;
      }
    }
    $resultsFound = array();
    $resultsMissing = array();
    $missingOptional = array();
    $missingRequired = array();
    $context->getRegistry()->getIncludedFeatures($needed, $resultsFound, $resultsMissing);
    foreach ($resultsMissing as $missingResult) {
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
    foreach ($resultsFound as $entry) {
      $features[$entry] = $context->getRegistry()->getEntry($entry)->getFeature()->create();
    }
    // prepare them
    foreach ($features as $key => $feature) {
      $params = $gadget->getFeatureParams($gadget, $context->getRegistry()->getEntry($key));
      $feature->prepare($gadget, $context, $params);
    }
    // and process them
    foreach ($features as $key => $feature) {
      $params = $gadget->getFeatureParams($gadget, $context->getRegistry()->getEntry($key));
      $feature->process($gadget, $context, $params);
    }
  }

  private function substitutePreloads(Gadget $gadget, $substituter) {
    $preloads = array();
    foreach ($gadget->preloads as $preload) {
      $preloads[] = $preload->substitute($substituter);
    }
    $gadget->preloads = $preloads;
  }
}



