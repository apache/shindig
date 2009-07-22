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

/**
 * Class that manages & loads template libraries (either inline, osml or external)
 * See http://opensocial-resources.googlecode.com/svn/spec/0.9/OpenSocial-Templating.xml#rfc.section.13 for details
 * Template libraries can be loaded from the gadget spec using:
 * <Require feature="opensocial-templates">
 *   <Param name="requireLibrary">http://www.example.com/templates.xml</Param>
 * </Require>
 */
class TemplateLibrary {
  private $osmlTags = array('os:Name', 'os:PeopleSelector', 'os:badge');
  private $templates = array();
  private $osmlLoaded = false;

  public function parseTemplate($tag, $caller) {
    $template = $this->getTemplate($tag);
    $ret = '';
    if ($template->dom instanceof DOMElement) {
      $templateDomCopy = new DOMDocument(null, 'utf-8');
      // If this template pulls in any new style and/or javascript, add those to the document
      if ($style = $template->getStyle()) {
        $styleNode = $templateDomCopy->createElement('style');
        $styleNode->appendChild($templateDomCopy->createTextNode($style));
        $templateDomCopy->appendChild($styleNode);
      }
      if ($script = $template->getScript()) {
        $scriptNode = $templateDomCopy->createElement('script');
        $scriptNode->setAttribute('type', 'text/javascript');
        $scriptNode->appendChild($templateDomCopy->createTextNode($script));
        $templateDomCopy->appendChild($scriptNode);
      }
      // Copy the DOM structure since parseNode() modifies the DOM structure directly
      foreach ($template->dom->childNodes as $node) {
        $importedNode = $templateDomCopy->importNode($node, true);
        $templateDomCopy->appendChild($importedNode);
      }
      // Parse the template's DOM using our current data context (which includes the My context for templates)
      $caller->parseNode($templateDomCopy);
      return $templateDomCopy;
    }
    return false;
  }

  /**
   * Add template by DOMElement node, this function is primary called from
   * the GadgetBaseRenderer class when it comes accross a script block with
   * type=os-template & tag="some:name"
   *
   * @param DOMElement $node
   */
  public function addTemplateByNode(DOMElement &$node) {
    $tag = $node->getAttribute('tag');
    $this->templates[$tag] = new TemplateLibraryEntry($node);
  }

  /**
   * Add an external template library by URL
   *
   * @param string $libraryUrl (ie: 'http://www.example.com/templates.xml')
   */
  public function addTemplateLibrary($libraryUrl) {// add library by external URL and addTemplate for each entry contained in it
}

  /**
   * Check to see if a template with name $tag exists
   *
   * @param string $tag
   * @return boolean
   */
  public function hasTemplate($tag) {
    if (in_array($tag, $this->osmlTags)) {
      if (! $this->osmlLoaded) {
        $this->loadOsmlLibrary();
      }
      return true;
    }
    return isset($this->templates[$tag]);
  }

  public function getTemplate($tag) {
    if (! $this->hasTemplate($tag)) {
      throw new ExpressionException("Invalid template tag");
    }
    return $this->templates[$tag];
  }

  private function loadOsmlLibrary() {
    $this->osmlLoaded = true;
    // preload osml lib, see container config key for location (gadget context->container config->osml->library
  /*
    "osml": {
     // OSML library resource.  Can be set to null or the empty string to disable OSML
     // for a container.
     "library": "config/OSML_library.xml"
   }
   */
  }
}

/**
 * Misc class that holds the template information, an inline template
 * will only contain a text blob (stored as parsed $dom node), however
 * external and OSML library templates can also container script and
 * style blocks
 *
 */
class TemplateLibraryEntry {
  public $dom;
  public $style = array();
  public $script = array();

  public function __construct($dom = false) {
    $this->dom = $dom;
  }

  /**
   * Adds a javascript blob to this template
   *
   * @param unknown_type $script
   */
  public function addScript($script) {
    $this->script[] = new TemplateLibraryContent($script);
  }

  /**
   * Adds a style blob to this template
   *
   * @param unknown_type $style
   */
  public function addStyle($style) {
    $this->style[] = new TemplateLibraryContent($style);
  }

  /**
   * Returns the (combined, in inclusion  order) script text blob, or
   * false if there's no javascript for this template
   *
   * @return javascript string or false
   */
  public function getScript() {
    $ret = '';
    foreach ($this->script as $script) {
      if (! $script->included) {
        $ret .= $script->content . "\n";
      }
    }
    return ! empty($ret) ? $ret : false;
  }

  /**
   * Returns the (combined, in inclusion  order) stylesheet text blob, or
   * false if there's no style sheet associated with this template
   *
   * @return javascript string or false
   */
  public function getStyle() {
    $ret = '';
    foreach ($this->style as $style) {
      if (! $style->included) {
        $ret .= $style->content . "\n";
      }
    }
    return ! empty($ret) ? $ret : false;
  }
}

/**
 * Scripts can be global per library set, so we assign the global script to each actual template
 * and on calling it, the TemplateLibraryEntry will check to see if the content was already output
 */
class TemplateLibraryContent {
  public $content;
  public $included;

  public function __construct($content) {
    $this->content = $content;
    $this->included = false;
  }
}
