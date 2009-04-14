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

class EmptyClass {
}

/**
 * Renders a Gadget's Content type="html" view, inlining the content, feature javascript and javascript initialization
 * into the gadget's content
 *
 */
class GadgetHtmlRenderer extends GadgetRenderer {
  public $gadget;

  public function renderGadget(Gadget $gadget, $view) {
    $this->gadget = $gadget;
    // Was a privacy policy header configured? if so set it
    if (Config::get('P3P') != '') {
      header("P3P: " . Config::get('P3P'));
    }
    $content = '';
    // Set doctype if quirks = false or empty in the view
    if (! empty($view['quirks']) || ! $view['quirks']) {
      $content .= "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n";
    }
    $content .= "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/></head><body>\n";
    // Append the content for the selected view
    $content .= $gadget->substitutions->substitute($view['content']);
    $content .= "\n</body>\n</html>";
    // Rewrite the content, this will rewrite resource links to proxied versions (if requested), sanitize if configured, and
    // add the various javascript tags to the document
    $rewriter = new GadgetRewriter($this->context);
    $rewriter->addObserver('head', $this, 'addHeadTags');
    $rewriter->addObserver('body', $this, 'addBodyTags');
    $content = $rewriter->rewrite($content, $gadget);
    echo $content;
  }

  /**
   * Append the runOnLoadHandlers script to the gadget's document body
   *
   * @param DOMElement $node
   * @param DOMDocument $doc
   */
  public function addBodyTags(DOMElement &$node, DOMDocument $doc) {
    $script = "gadgets.util.runOnLoadHandlers();";
    if ($this instanceof GadgetHrefRenderer) {
      $script .= " gadgets.window.adjustHeight();";
    }
    $scriptNode = $doc->createElement('script');
    $scriptNode->nodeValue = $script;
    $node->appendChild($scriptNode);
  }

  /**
   * Adds the various bits of javascript to the gadget's document head element
   *
   * @param DOMElement $node
   * @param DOMDocument $doc
   */
  public function addHeadTags(DOMElement &$node, DOMDocument $doc) {

    // Inject our configured gadget document style
    $styleNode = $doc->createElement('style');
    $styleNode->setAttribute('type', 'text/css');
    $styleNode->nodeValue = Config::get('gadget_css');
    $node->appendChild($styleNode);

    // Inject the OpenSocial feature javascripts
    $forcedJsLibs = $this->getForcedJsLibs();
    if (! empty($forcedJsLibs)) {
      // if some of the feature libraries are externalized (through a browser cachable <script src="/gadgets/js/opensocial-0.9:settitle.js"> type url)
      // we inject the tag and don't inline those libs (and their dependencies)
      $forcedJsLibs = explode(':', $forcedJsLibs);
      $scriptNode = $doc->createElement('script');
      $scriptNode->setAttribute('src', Config::get('default_js_prefix') . $this->getJsUrl($forcedJsLibs, $this->gadget) . "&container=" . $this->context->getContainer());
      $node->appendChild($scriptNode);
      $registry = $this->context->getRegistry();
      $missing = array();
      $registry->resolveFeatures($forcedJsLibs, $forcedJsLibs, $missing);
    }

    $script = '';
    foreach ($this->gadget->features as $feature) {
      if (! is_array($forcedJsLibs) || (is_array($forcedJsLibs) && ! in_array($feature, $forcedJsLibs))) {
        $script .= $this->context->getRegistry()->getFeatureContent($feature, $this->context, true);
      }
    }

    // Add the JavaScript initialization strings for the configuration, localization and preloads
    $script .= "\n";
    $script .= $this->appendJsConfig($this->gadget, count($forcedJsLibs));
    $script .= $this->appendMessages($this->gadget);
    $script .= $this->appendPreloads($this->gadget);

    $scriptNode = $doc->createElement('script');
    $scriptNode->setAttribute('type', 'text/javascript');
    $scriptNode->nodeValue = str_replace('&', '&amp;', $script);
    $node->appendChild($scriptNode);
  }

  /**
   * Retrieve the forced javascript libraries (if any), using either the &libs= from the query
   * or if that's empty, from the config
   *
   * @return unknown
   */
  private function getForcedJsLibs() {
    $forcedJsLibs = $this->context->getForcedJsLibs();
    // allow the &libs=.. param to override our forced js libs configuration value
    if (empty($forcedJsLibs)) {
      $forcedJsLibs = Config::get('focedJsLibs');
    }
    return $forcedJsLibs;
  }

  /**
   * Appends the javascript features configuration string
   *
   * @param Gadget $gadget
   * @param unknown_type $hasForcedLibs
   * @return string
   */
  private function appendJsConfig(Gadget $gadget, $hasForcedLibs) {
    $container = $this->context->getContainer();
    $containerConfig = $this->context->getContainerConfig();
    //TODO some day we should parse the forcedLibs too, and include their config selectivly as well for now we just include everything if forced libs is set.
    if ($hasForcedLibs) {
      $gadgetConfig = $containerConfig->getConfig($container, 'gadgets.features');
    } else {
      $gadgetConfig = array();
      $featureConfig = $containerConfig->getConfig($container, 'gadgets.features');
      foreach ($gadget->getJsLibraries() as $library) {
        $feature = $library->getFeatureName();
        if (! isset($gadgetConfig[$feature]) && ! empty($featureConfig[$feature])) {
          $gadgetConfig[$feature] = $featureConfig[$feature];
        }
      }
    }
    // Add gadgets.util support. This is calculated dynamically based on request inputs.
    // See java/org/apache/shindig/gadgets/render/RenderingContentRewriter.java for reference.
    $requires = array();
    foreach ($gadget->features as $feature) {
      $requires[$feature] = new EmptyClass();
    }
    $gadgetConfig['core.util'] = $requires;
    return "gadgets.config.init(" . json_encode($gadgetConfig) . ");\n";
  }

  /**
   * Injects the relevant translation message bundle into the javascript api
   *
   * @param Gadget $gadget
   * @return string
   */
  private function appendMessages(Gadget $gadget) {
    $msgs = '';
    if (! empty($gadget->gadgetSpec->locales)) {
      $msgs = json_encode($gadget->gadgetSpec->locales);
    }
    return "gadgets.Prefs.setMessages_($msgs);\n";
  }

  /**
   * Injects the preloaded content into the javascript api
   *
   * @param Gadget $gadget
   * @return string
   */
  private function appendPreloads(Gadget $gadget) {
    return "gadgets.io.preloaded_ = " . (count($gadget->gadgetSpec->preloads) ? json_encode($gadget->gadgetSpec->preloads) : "{}") . ";\n";
  }
}
