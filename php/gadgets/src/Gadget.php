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

final class GadgetId extends GadgetViewID {
	private $uri;
	private $moduleId;
	
	public function GadgetId($uri, $moduleId)
	{
		$this->uri = $uri;
		$this->moduleId = $moduleId;
	}
	
	public function getURI()
	{
		return $this->uri;
	}
	
	public function getModuleId()
	{
		return $this->moduleId;
	}
	
	public function getKey()
	{
		return $this->getURI();
	}
}

class Gadget extends GadgetSpec {
	public $baseSpec;
	private $jsLibraries;
	private $substitutions;
	private $userPrefValues;
	private $currentMessageBundle = array();
	
	public function __construct($id, GadgetSpec $baseSpec, $prefs)
	{
		$this->id = $id;
		$this->baseSpec = $baseSpec;
		$this->substitutions = new Substitutions();
		$this->userPrefValues = $prefs;
		$this->jsLibraries = array();
	}
	
	public function getAuthor()
	{
		return $this->substitutions->substitute($this->baseSpec->getAuthor());
	}
	
	public function getAuthorEmail()
	{
		return $this->substitutions->substitute($this->baseSpec->getAuthorEmail());
	}
	
	public function getBaseSpec()
	{
		return $this->baseSpec;
	}
	
	public function getContentData($view = false)
	{
		return $this->substitutions->substitute($this->baseSpec->getContentData($view));
	}
	
	public function getContentHref()
	{
		return $this->substitutions->substitute($this->baseSpec->getContentHref());
	}
	
	public function getContentType()
	{
		return $this->baseSpec->getContentType();
	}
	
	public function getCurrentMessageBundle()
	{
		return $this->currentMessageBundle;
	}
	
	public function getDescription()
	{
		return $this->substitutions->substitute($this->baseSpec->getDescription());
	}
	
	public function getDirectoryTitle()
	{
		return $this->substitutions->substitute($this->baseSpec->getDirectoryTitle());
	}
	
	public function getIcons()
	{
		return $this->baseSpec->getIcons();
	}
	
	public function getId()
	{
		return $this->id;
	}
	
	public function getJsLibraries()
	{
		return $this->jsLibraries;
	}
	
	public function addJsLibrary($library)
	{
		$this->jsLibraries[] = $library;
	}
	
	public function getLocaleSpecs()
	{
		return $this->baseSpec->getLocaleSpecs();
	}
	
	public function getFeatureParams($gadget, $feature)
	{
		//FIXME not working atm
		$spec = $gadget->getRequires();
		$spec = isset($spec[$feature->getName()]) ? $spec[$feature->getName()] : null;
		if ($spec == null) {
			return array();
		} else {
			return $spec->getParams();
		}
	}
	
	public function getPreloads()
	{
		$ret = array();
		foreach ( $this->baseSpec->getPreloads() as $preload ) {
			$ret[] = $this->substitutions->substitute($preload);
		}
		return $ret;
	}
	
	public function getRequires()
	{
		return $this->baseSpec->getRequires();
	}
	
	public function getScreenshot()
	{
		return $this->substitutions->substitute($this->baseSpec->getScreenshot());
	}
	
	public function getSubstitutions()
	{
		return $this->substitutions;
	}
	
	public function getThumbnail()
	{
		return $this->substitutions->substitute($this->baseSpec->getThumbnail());
	}
	
	public function getTitle()
	{
		return $this->substitutions->substitute($this->baseSpec->getTitle());
	}
	
	public function getTitleURI()
	{
		$ret = null;
		if (($uri = $this->baseSpec->getTitleURI()) != null) {
			$ret = $this->substitutions->substitute($uri);
		}
		return $ret;
	}
	
	public function getUserPrefs()
	{
		return $this->baseSpec->getUserPrefs();
	}
	
	public function getUserPrefValues()
	{
		return $this->userPrefValues;
	}
	
	public function setCurrentMessageBundle($messageBundle)
	{
		$this->currentMessageBundle = $messageBundle;
	}
}