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

class UserPrefSubstituter extends GadgetFeatureFactory {
	private $feature;
	public function __construct()
	{
		$this->feature = new UserPrefSubstituterFeature();
	}
	
	public function create()
	{
		return $this->feature;
	}
}

class UserPrefSubstituterFeature extends GadgetFeature {
	
	public function prepare($gadget, $context, $params)
	{
	}
	
	public function process($gadget, $context, $params)
	{
		$substitutions = $gadget->getSubstitutions();
		$upValues = $gadget->getUserPrefValues();
		$json = array();
		foreach ( $gadget->getUserPrefs() as $pref ) {
			$name = $pref->getName();
			$value = $upValues->getPref($name);
			if ($value == null) {
				$value = $pref->getDefaultValue();
			}
			if ($value == null) {
				$value = "";
			}
			$substitutions->addSubstitution('USER_PREF', $name, $value);
			
			if ($context->getRenderingContext() == 'GADGET') {
				$json[$name] = $value;
			}
		}
		if (count($json)) {
			$setPrefFmt = "gadgets.prefs_.setPref(%d, %s);";
			$moduleId = $gadget->getId()->getModuleId();
			$setPrefStr = sprintf($setPrefFmt, $moduleId, json_encode($json));
			$gadget->addJsLibrary(JsLibrary::create('INLINE', $setPrefStr));
		}
	}
}
