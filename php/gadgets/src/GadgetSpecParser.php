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

class GadgetSpecParser {
	//public function parse(GadgetId $id, String $xml)
	public function parse($xml)
	{
		$id = 1;
		if (empty($xml)) {
			throw new SpecParserException("Empty XML document");
		}
		//TODO add libxml_get_errors() functionality so we can have a bit more understandable errors..
		if (($doc = simplexml_load_string($xml, 'SimpleXMLElement', LIBXML_NOCDATA)) == false) {
			throw new SpecParserException("Invalid XML document");
		}
		if (count($doc->ModulePrefs) != 1) {
			throw new SpecParserException("Missing or duplicated <ModulePrefs>");
		}
		$spec = new ParsedGadgetSpec();
		$spec->id = $id;
		// process ModulePref attributes
		$this->processModulePrefs($id, $spec, $doc->ModulePrefs);
		// process UserPrefs, if any
		foreach ( $doc->UserPref as $pref ) {
			$this->processUserPref($spec, $pref);
		}
		foreach ( $doc->Content as $content ) {
			$this->processContent($spec, $content);
		}
		//FIXME : should we add an else { throw new SpecParserException("Missing <Content> block"); } here ? Java version doesn't but it seems like we should ?
		foreach ( $doc->ModulePrefs->Require as $feature ) {
			$this->processFeature($spec, $feature, true);
		}
		foreach ( $doc->ModulePrefs->Optional as $feature ) {
			$this->processFeature($spec, $feature, false);
		}
		//TODO java version has a todo here for parsing icons
		return $spec;
	}
	
	private function processModulePrefs($id, &$spec, $ModulePrefs)
	{
		$attributes = $ModulePrefs->attributes();
		if (empty($attributes['title'])) {
			throw new SpecParserException("Missing or empty \"title\" attribute.");
		}
		// Get ModulePrefs attributes
		// (trim is used here since it not only cleans up the text, but also auto-casts the SimpleXMLElement to a string)
		$spec->title = trim($attributes['title']);
		$spec->author = isset($attributes['author']) ? trim($attributes['author']) : '';
		$spec->authorEmail = isset($attributes['author_email']) ? trim($attributes['author_email']) : '';
		$spec->description = isset($attributes['description']) ? trim($attributes['description']) : '';
		$spec->directoryTitle = isset($attributes['directory_title']) ? trim($attributes['directory_title']) : '';
		$spec->screenshot = isset($attributes['screenshot']) ? trim($attributes['screenshot']) : '';
		$spec->thumbnail = isset($attributes['thumbnail']) ? trim($attributes['thumbnail']) : '';
		$spec->titleUrl = isset($attributes['title_url']) ? trim($attributes['title_url']) : '';
		foreach ( $ModulePrefs->Locale as $locale ) {
			$spec->localeSpecs[] = $this->processLocale($locale);
		}
	
	}
	
	private function processLocale($locale)
	{
		$attributes = $locale->attributes();
		$messageAttr = isset($attributes['messages']) ? trim($attributes['messages']) : '';
		$languageAttr = isset($attributes['lang']) ? trim($attributes['lang']) : 'all';
		$countryAttr = isset($attributes['country']) ? trim($attributes['country']) : 'all';
		$rtlAttr = isset($attributes['language_direction']) ? trim($attributes['language_direction']) : '';
		$rightToLeft = $rtlAttr == 'rtl';
		$locale = new ParsedMessageBundle();
		$locale->rightToLeft = $rightToLeft;
		//FIXME java seems to use a baseurl here, probably for the http:// part but i'm not sure yet. Should verify behavior later to see if i got it right
		$locale->url = $messageAttr;
		$locale->locale = new Locale($languageAttr, $countryAttr);
		return $locale;
	}
	
	private function processUserPref(&$spec, $pref)
	{
		$attributes = $pref->attributes();
		$preference = new ParsedUserPref();
		if (empty($attributes['name'])) {
			throw new SpecParserException("All UserPrefs must have name attributes.");
		}
		$preference->name = trim($attributes['name']);
		$preference->displayName = isset($attributes['display_name']) ? trim($attributes['display_name']) : '';
		// if its set -and- in our valid 'enum' of types, use it, otherwise assume STRING, to try and emulate java's enum behavior
		$preference->dataType = isset($attributes['datatype']) && in_array(strtoupper($attributes['datatype']), $preference->DataTypes) ? strtoupper($attributes['datatype']) : 'STRING';
		$preference->defaultValue = isset($attributes['default_value']) ? trim($attributes['default_value']) : '';
		if (isset($pref->EnumValue)) {
			foreach ( $pref->EnumValue as $enum ) {
				$attr = $enum->attributes();
				// java based shindig doesn't throw an exception here, but it -is- invalid and should trigger a parse error
				if (empty($attr['value'])) {
					throw new SpecParserException("EnumValue must have a value field.");
				}
				$valueText = trim($attr['value']);
				$displayText = ! empty($attr['display_value']) ? trim($attr['display_value']) : $valueText;
				$preference->enumValues[$valueText] = $displayText;
			}
		}
		$spec->userPrefs[] = $preference;
	}
	
	private function processContent(&$spec, $content)
	{
		$attributes = $content->attributes();
		if (empty($attributes['type'])) {
			throw new SpecParserException("No content type specified!");
		}
		$type = trim($attributes['type']);
		if ($type == 'url') {
			if (empty($attributes['href'])) {
				throw new SpecParserException("Malformed <Content> href value");
			}
			$url = trim($attributes['href']);
			$spec->contentType = 'URL';
			$spec->contentHref = $url;
		} else {
			$spec->contentType = 'HTML';
			$html = (string)$content; // no trim here since empty lines can have structural meaning, so typecast to string instead
			$view = isset($attributes['view']) ? trim($attributes['view']) : '';
			$views = explode(',', $view);
			foreach ( $views as $view ) {
				$spec->addContent($view, $html);
			}
		}
	}
	
	private function processFeature(&$spec, $feature, $required)
	{
		$featureSpec = new ParsedFeatureSpec();
		$attributes = $feature->attributes();
		if (empty($attributes['feature'])) {
			throw new SpecParserException("Feature not specified in <" . (required ? "Required" : "Optional") . "> tag");
		}
		$featureSpec->name = trim($attributes['feature']);
		$featureSpec->optional = ! $required;
		foreach ( $feature->Param as $param ) {
			$attr = $param->attributes();
			if (empty($attr['name'])) {
				throw new SpecParserException("Missing name attribute in <Param>.");
			}
			$name = trim($attr['name']);
			$value = trim($param);
			$featureSpec->params[$name] = $value;
		}
		$spec->requires[$featureSpec->name] = $featureSpec;
	}
}

class ParsedIcon extends Icon {
	public $mode;
	public $url;
	public $type;
	
	public function getMode()
	{
		return $this->mode;
	}
	
	public function getURI()
	{
		return $this->url;
	}
	
	public function getType()
	{
		return $this->type;
	}
}

class ParsedFeatureSpec extends FeatureSpec {
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

class ParsedUserPref extends UserPref {
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

class ParsedMessageBundle extends LocaleSpec {
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

class ParsedGadgetSpec extends GadgetSpec {
	public $id;
	public $author;
	public $authorEmail;
	public $description;
	public $directoryTitle;
	public $contentType;
	public $contentHref;
	public $contentData = array();
	public $icons = array();
	public $localeSpecs = array();
	public $preloads = array();
	public $requires = array();
	public $screenshot;
	public $thumbnail;
	public $title;
	public $titleUrl;
	public $userPrefs = array();
	
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
	
	public function getAuthor()
	{
		return $this->author;
	}
	
	public function getAuthorEmail()
	{
		return $this->authorEmail;
	}
	
	public function getContentData($view = false)
	{
		if ($this->contentType != 'HTML') {
			throw new SpecParserException("getContentData() requires contentType HTML");
		}
		if (empty($view) || !$view) {
			$view = DEFAULT_VIEW;
		}
		return isset($this->contentData[$view]) ? trim($this->contentData[$view]) : '';
	}
	
	public function getContentHref()
	{
		return $this->getContentType() == 'URL' ? $this->contentHref : null;
	}
	
	public function getContentType()
	{
		return $this->contentType;
	}
	
	public function getDescription()
	{
		return $this->description;
	}
	
	public function getDirectoryTitle()
	{
		return $this->directoryTitle;
	}
	
	public function getIcons()
	{
		return $this->icons;
	}
	
	public function getLocaleSpecs()
	{
		return $this->localeSpecs;
	}
	
	public function getPreloads()
	{
		return $this->preloads;
	}
	
	public function getRequires()
	{
		return $this->requires;
	}
	
	public function getScreenshot()
	{
		return $this->screenshot;
	}
	
	public function getThumbnail()
	{
		return $this->thumbnail;
	}
	
	public function getTitle()
	{
		return $this->title;
	}
	
	public function getTitleURI()
	{
		return $this->titleUrl;
	}
	
	public function getUserPrefs()
	{
		return $this->userPrefs;
	}
}

