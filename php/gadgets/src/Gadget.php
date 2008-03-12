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
 * In Java terms this would be the Gadget, GadgetView and GadgetSpec all rolled into one
 * We combined it into one class since it makes more sense in PHP and provides a nice 
 * speedup too.
 */

class GadgetException extends Exception {}

class Gadget {
	private $jsLibraries;
	private $substitutions;
	private $userPrefValues;
	private $messageBundle = array();
	// As in UserPref, no enums so fake it
	public $contentTypes    = array('HTML', 'URL');
	public $id;
	public $author;
	public $authorEmail;
	public $description;
	public $directoryTitle;
	public $contentType;
	public $contentHref;
	public $contentData = array();
	public $localeSpecs = array();
	public $preloads = array();
	public $requires = array();
	public $screenshot;
	public $thumbnail;
	public $title;
	public $titleUrl = null;
	public $userPrefs = array();
	
	public function __construct($id = false, $prefs = false)
	{
		if ($id) $this->id = $id;
		if ($prefs) $this->userPrefValues = $prefs;
		$this->substitutions = new Substitutions();
		$this->jsLibraries = array();
	}
	
	public function setId($id)
	{
		$this->id = $id;
	}
	
	public function setPrefs($prefs)
	{
		$this->userPrefValues = $prefs;
	}
	
	public function getAuthor()
	{
		return $this->substitutions->substitute($this->author);
	}
	
	public function getAuthorEmail()
	{
		return $this->substitutions->substitute($this->authorEmail);
	}
		
	public function getContentData($view = false)
	{
		if ($this->contentType != 'HTML') {
			throw new SpecParserException("getContentData() requires contentType HTML");
		}
		if (empty($view) || !$view) {
			$view = DEFAULT_VIEW;
		}
		return $this->substitutions->substitute(isset($this->contentData[$view]) ? trim($this->contentData[$view]) : '');
	}
	
	public function getContentHref()
	{
		return $this->substitutions->substitute($this->getContentType() == 'URL' ? $this->contentHref : null);
	}
	
	public function getMessageBundle()
	{
		return $this->messageBundle;
	}
	
	public function getDescription()
	{
		return $this->substitutions->substitute($this->description);
	}
	
	public function getDirectoryTitle()
	{
		return $this->substitutions->substitute($this->directoryTitle);
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
		return $this->localeSpecs;
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
		foreach ( $this->preloads as $preload ) {
			$ret[] = $this->substitutions->substitute($preload);
		}
		return $ret;
	}
	
	public function getRequires()
	{
		return $this->requires;
	}
	
	public function getScreenshot()
	{
		return $this->substitutions->substitute($this->screenshot);
	}
	
	public function getSubstitutions()
	{
		return $this->substitutions;
	}
	
	public function getThumbnail()
	{
		return $this->substitutions->substitute($this->thumbnail);
	}
	
	public function getTitle()
	{
		return $this->substitutions->substitute($this->title);
	}
	
	public function getTitleURI()
	{
		$ret = null;
		if (!empty($this->titleURI)) {
			$ret = $this->substitutions->substitute($this->titleURI);
		}
		return $ret;
	}
	
	public function getUserPrefs()
	{
		return $this->userPrefs;
	}
	
	public function getUserPrefValues()
	{
		return $this->userPrefValues;
	}
	
	public function setMessageBundle($messageBundle)
	{
		$this->messageBundle = $messageBundle;
	}
	
	/* gadget Spec functions */
	public function addContent($view, $data)
	{
		if (empty($view)) {
			$view = DEFAULT_VIEW;
		}
		if (! isset($this->contentData[$view])) {
			$this->contentData[$view] = '';
		}
		$this->contentData[$view] .= $data;
	}
				
	public function getContentType()
	{
		return $this->contentType;
	}
}

class LocaleSpec {
	public $url;
	public $locale;
	public $rightToLeft;
	
	public function getURI()
	{
		return $this->url;
	}
	
	public function getLocale()
	{
		return $this->locale;
	}
	
	public function isRightToLeft()
	{
		return $this->rightToLeft;
	}
}

class FeatureSpec {
	public $name;
	public $params = array();
	public $optional;
	
	public function getName()
	{
		return $this->name;
	}
	
	public function getParams()
	{
		return $this->params;
	}
	
	public function isOptional()
	{
		return $this->optional;
	}
}

class UserPref {
	public $DataTypes = array('STRING', 'HIDDEN', 'BOOL', 'ENUM', 'LIST', 'NUMBER');
	public $dataType;
	public $name;
	public $displayName;
	public $defaultValue;
	public $required;
	public $enumValues;
	public $contentType;
	
	public function getName()
	{
		return $this->name;
	}
	
	public function getDisplayName()
	{
		return $this->displayName;
	}
	
	public function getDefaultValue()
	{
		return $this->defaultValue;
	}
	
	public function isRequired()
	{
		return $this->required;
	}
	
	public function getDataType()
	{
		return $this->dataType;
	}
	
	public function getEnumValues()
	{
		return $this->enumValues;
	}
}
