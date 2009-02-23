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

  public function renderGadget(Gadget $gadget, $view) {
    // Was a privacy policy header configured? if so set it
    if (Config::get('P3P') != '') {
      header("P3P: " . Config::get('P3P'));
    }

    $content = '';
    // Set doctype if quirks = false or empty in the view
    if (! empty($view['quirks']) || ! $view['quirks']) {
      $content .= "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n";
    }
    $content .= "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><style type=\"text/css\">" . Config::get('gadget_css') . "</style></head><body>\n";

    // Inject the OpenSocial feature javascripts
    $forcedJsLibs = $this->getForcedJsLibs();
    if (! empty($forcedJsLibs)) {
      // if some of the feature libraries are externalized (through a browser cachable <script src="/gadgets/js/opensocial-0.9:settitle.js"> type url)
      // we inject the tag and don't inline those libs (and their dependencies)
      $forcedJsLibs = explode(':', $forcedJsLibs);
      $content .= sprintf("<script src=\"%s\"></script>\n", Config::get('default_js_prefix') . $this->getJsUrl($forcedJsLibs, $gadget) . "&container=" . $this->context->getContainer()) . "\n";
      $registry = $this->context->getRegistry();
      $missing = array();
      $registry->resolveFeatures($forcedJsLibs, $forcedJsLibs, $missing);
    }
    $content .= "<script>\n";
    foreach ($gadget->features as $feature) {
      if (! is_array($forcedJsLibs) || (is_array($forcedJsLibs) && ! in_array($feature, $forcedJsLibs))) {
        $content .= $this->context->getRegistry()->getFeatureContent($feature, $this->context, true);
      }
    }

    // Add the JavaScript initialization strings for the configuration, localization and preloads
    $content .= "\n";
    $content .= $this->appendJsConfig($gadget, count($forcedJsLibs));
    $content .= $this->appendMessages($gadget);
    $content .= $this->appendPreloads($gadget);
    $content .= "</script>";

    // Append the content for the selected view
    $viewContent = $gadget->substitutions->substitute($view['content']);

    // Rewrite the content, the GadgetRewriter will check for the Content-Rewrtie and Caja feature, if
    // sanitazation is required, plus look for any template exapanding that needs to be done
    $rewriter = new GadgetRewriter($this->context);
    $content .= $rewriter->rewrite($viewContent, $gadget);

    // And add our runOnLoadHandlers() call
    $content .= "\n<script>gadgets.util.runOnLoadHandlers();</script></body>\n</html>";

    echo $content;
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
