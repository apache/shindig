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

class BidiSubstituter extends GadgetFeatureFactory {
	private $feature;
	
	public function __construct()
	{
		$this->feature = new BidiSubstituterFeature();
	}
	
	public function create()
	{
		return $this->feature;
	}
}

class BidiSubstituterFeature extends GadgetFeature {
	private function getLocaleSpec($spec, $locale)
	{
		$localeSpecs = $spec->getLocaleSpecs();
		foreach ( $localeSpecs as $locSpec ) {
			//FIXME equals, localeSpec, much unknown here still..
			if ($locSpec->getLocale()->equals($locale)) {
				return $locSpec;
			}
		}
		return null;
	}
	
	public function prepare($spec, $context, $params)
	{
		// Nothing here.
	}
	
	public function process($gadget, $context, $params)
	{
		$subst = $gadget->getSubstitutions();
		$locale = $context->getLocale();
		// Find an appropriate locale for the ltr flag.
		$locSpec = $this->getLocaleSpec($gadget, $locale);
		if ($locSpec == null) {
			$locSpec = $this->getLocaleSpec($gadget, new Locale($locale->getLanguage(), "all"));
		}
		if ($locSpec == null) {
			$locSpec = $this->getLocaleSpec($gadget, new Locale("all", "all"));
		}
		$rtl = false;
		if ($locSpec != null) {
			$rtl = $locSpec->isRightToLeft();
		}
		$subst->addSubstitution('BIDI', "START_EDGE", $rtl ? "right" : "left");
		$subst->addSubstitution('BIDI', "END_EDGE", $rtl ? "left" : "right");
		$subst->addSubstitution('BIDI', "DIR", $rtl ? "rtl" : "ltr");
		$subst->addSubstitution('BIDI', "REVERSE_DIR", $rtl ? "ltr" : "rtl");
	}
}