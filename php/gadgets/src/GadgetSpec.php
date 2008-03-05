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

define('DEFAULT_VIEW', 'default');

// Locale class doesn't exist in php, so to allow the code base to be closer to the java one, were faking one
class Locale {
	public $language;
	public $country;
	
	public function __construct($language, $country)
	{
		$this->language = $language;
		$this->country = $country;
	}
	public function equals($obj)
	{
		if (!($obj instanceof Locale)) {
			return false;
		}
		return ($obj->language == $this->language && $obj->country == $this->country);
	}
	
	public function getLanguage()
	{
		return $this->language;
	}
	
	public function getCountry()
	{
		return $this->country;
	}
	
}

abstract class Icon {
	abstract public function getURI();
	abstract public function getMode();
	abstract public function getType();
}

abstract class LocaleSpec {
	abstract public function getLocale();
	abstract public function getURI();
	abstract public function isRightToLeft();
}

abstract class FeatureSpec {
	abstract public function getName();
	abstract public function getParams();
	abstract public function isOptional();
}

abstract class UserPref {
	// enums are not suported in php, so we store our allowed values, and programaticly check for consitency later on
	public $DataTypes = array('STRING', 'HIDDEN', 'BOOL', 'ENUM', 'LIST', 'NUMBER');
	public $dataType;
	abstract public function getName();
	abstract public function getDisplayName();
	abstract public function getDefaultValue();
	abstract public function isRequired();
	abstract public function getDataType();
	abstract public function getEnumValues();
}

abstract class GadgetSpec {
	// As in UserPref, no enums so fake it
	public $contentTypes    = array('HTML', 'URL');
	abstract public function getAuthor();
	abstract public function getAuthorEmail();
	abstract public function getContentData($view = false);
	abstract public function getContentHref();
	abstract public function getContentType();
	abstract public function getDescription();
	abstract public function getDirectoryTitle();
	abstract public function getIcons();
	abstract public function getLocaleSpecs();
	abstract public function getPreloads();
	abstract public function getRequires();
	abstract public function getScreenshot();
	abstract public function getThumbnail();
	abstract public function getTitle();
	abstract public function getTitleURI();
	abstract public function getUserPrefs();
}