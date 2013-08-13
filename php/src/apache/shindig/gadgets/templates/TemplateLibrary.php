<?php
namespace apache\shindig\gadgets\templates;
use apache\shindig\common\XmlError;
use apache\shindig\common\Config;
use apache\shindig\common\File;
use apache\shindig\gadgets\GadgetContext;

/*
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
  /**
   *
   * @var array
   */
  private $osmlTags = array('os:Name', 'os:PeopleSelector', 'os:Badge');
  /**
   *
   * @var array
   */
  private $templates = array();
  /**
   *
   * @var boolean
   */
  private $osmlLoaded = false;
  /**
   *
   * @var GadgetContext
   */
  private $gadgetContext;

  /**
   *
   * @param GadgetContext $gadgetContext
   */
  public function __construct(GadgetContext $gadgetContext) {
    $this->gadgetContext = $gadgetContext;
  }

  public function parseTemplate($tag, $caller) {
    $template = $this->getTemplate($tag);
    if ($template->dom) {
      $templateDomCopy = new \DOMDocument(null, 'utf-8');
      $templateDomCopy->preserveWhiteSpace = true;
      $templateDomCopy->formatOutput = false;
      $templateDomCopy->strictErrorChecking = false;
      $templateDomCopy->recover = false;
      $templateDomCopy->resolveExternals = false;
      // If this template pulls in any new style and/or javascript, add those to the document
      if ($style = $template->getStyle()) {
        $styleNode = $templateDomCopy->createElement('style');
        $styleNode->appendChild($templateDomCopy->createTextNode($style));
        $templateDomCopy->appendChild($styleNode);
      }
      if ($script = $template->getScript()) {
        $scriptNode = $templateDomCopy->createElement('script');
        $scriptNode->setAttribute('type', 'text/javascript');
        $scriptNode->appendChild($templateDomCopy->createCDATASection($script));
        $templateDomCopy->appendChild($scriptNode);
      }
      // Copy the DOM structure since parseNode() modifies the DOM structure directly
      $removeNodes = array();
      foreach ($template->dom->childNodes as $node) {
        $newNode = $templateDomCopy->importNode($node, true);
        $newNode = $templateDomCopy->appendChild($newNode);
        // Parse the template's DOM using our current data context (which includes the My context for templates)
        if (($removeNode = $caller->parseNode($newNode)) !== false) {
          $removeNodes[] = $removeNode;
        }
      }
      foreach ($removeNodes as $removeNode) {
        $removeNode->parentNode->removeChild($removeNode);
      }
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
  public function addTemplateByNode(\DOMElement &$node, $scripts = false, $styles = false) {
    $tag = $node->getAttribute('tag');
    $template = new TemplateLibraryEntry($node);
    if ($scripts) {
      foreach ($scripts as $script) {
        $template->addScript($script);
      }
    }
    if ($styles) {
      foreach ($styles as $style) {
        $template->addstyle($style);
      }
    }
    $this->templates[$tag] = $template;
  }

  /**
   *
   * @param DOMElement $node
   * @param TemplateLibraryContent $globalScript
   * @param TemplateLibraryContent $globalStyle
   */
  private function addTemplateDef(\DOMElement &$node, $globalScript, $globalStyle) {
    $tag = $node->getAttribute('tag');
    if (empty($tag)) {
      throw new ExpressionException("Missing tag attribute on TemplateDef element");
    }
    $templateNodes = array();
    foreach ($node->childNodes as $childNode) {
      if (isset($childNode->tagName)) {
        switch ($childNode->tagName) {
          case 'Template':
            $templateNodes[] = $childNode;
            break;
          case 'JavaScript':
            $globalScript[] = new TemplateLibraryContent($childNode->nodeValue);
            break;
          case 'Style':
            $globalStyle[] = new TemplateLibraryContent($childNode->nodeValue);
            break;
        }
      }
    }
    // Initialize the templates after scanning the entire structure so that all scripts and styles will be included with each template
    foreach ($templateNodes as $templateNode) {
      $templateNode->setAttribute('tag', $tag);
      $this->addTemplateByNode($templateNode, $globalScript, $globalStyle);
    }
  }

  /**
   * Add a template library set, for details see:
   * http://opensocial-resources.googlecode.com/svn/spec/0.9/OpenSocial-Templating.xml#rfc.section.13
   *
   * @param string $library
   */
  public function addTemplateLibrary($library) {
    libxml_use_internal_errors(true);
    $doc = new \DOMDocument(null, 'utf-8');
    $doc->preserveWhiteSpace = true;
    $doc->formatOutput = false;
    $doc->strictErrorChecking = false;
    $doc->recover = false;
    $doc->resolveExternals = false;
    if (! $doc->loadXML($library)) {
      throw new ExpressionException("Error parsing template library:\n" . XmlError::getErrors($library));
    }
    // Theoretically this could support multiple <Templates> root nodes, which isn't quite spec, but owell
    foreach ($doc->childNodes as $rootNode) {
      $templateNodes = array();
      $globalScript = array();
      $globalStyle = array();
      if (isset($rootNode->tagName) && $rootNode->tagName == 'Templates') {
        foreach ($rootNode->childNodes as $childNode) {
          if (isset($childNode->tagName)) {
            switch ($childNode->tagName) {
              case 'TemplateDef':
                $this->addTemplateDef($childNode, $globalScript, $globalStyle);
                break;
              case 'Template':
                $templateNodes[] = $childNode;
                break;
              case 'JavaScript':
                $globalScript[] = new TemplateLibraryContent($childNode->nodeValue);
                break;
              case 'Style':
                $globalStyle[] = new TemplateLibraryContent($childNode->nodeValue);
                break;
            }
          }
        }
      }
      // Initialize the templates after scanning the entire structure so that all scripts and styles will be included with each template
      foreach ($templateNodes as $templateNode) {
        $this->addTemplateByNode($templateNode, $globalScript, $globalStyle);
      }
    }
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

  /*
   *
   */
  private function loadOsmlLibrary() {
    $container = $this->gadgetContext->getContainer();
    $containerConfig = $this->gadgetContext->getContainerConfig();
    $gadgetsFeatures = $containerConfig->getConfig($container, 'gadgets.features');
    if (! isset($gadgetsFeatures['osml'])) {
      throw new ExpressionException("Missing OSML configuration key in config/config.js");
    } elseif (! isset($gadgetsFeatures['osml']['library'])) {
      throw new ExpressionException("Missing OSML.Library configuration key in config/config.js");
    }
    $osmlLibrary = Config::get('container_path') . str_replace('config/', '', $gadgetsFeatures['osml']['library']);
    if (! File::exists($osmlLibrary)) {
      throw new ExpressionException("Missing OSML Library ($osmlLibrary)");
    }
    $this->addTemplateLibrary(file_get_contents($osmlLibrary));
    $this->osmlLoaded = true;
  }
}
