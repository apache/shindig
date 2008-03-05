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

class ModuleSubstituter extends GadgetFeatureFactory {
	private $feature;
	public function __construct()
	{
		$this->feature = new ModuleSubstituterFeature();
	}
	public function create()
	{
		return $this->feature;
	}
}

class ModuleSubstituterFeature extends GadgetFeature {
	
	public function prepare($gadget, $context, $params)
	{
		//TODO Auto-generated method stub
	}
	
	public function process($gadget, $context, $params)
	{
		$gadget->getSubstitutions()->addSubstitution('MODULE', "ID", $gadget->getId()->getModuleId());
		if ($context->getRenderingContext() == 'GADGET') {
			$format = "gadgets.prefs_.setDefaultModuleId(%d);";
			$fmtStr = sprintf($format, $gadget->getId()->getModuleId());
			$gadget->addJsLibrary(JsLibrary::create('INLINE', $fmtStr));
		}
	}
}
 