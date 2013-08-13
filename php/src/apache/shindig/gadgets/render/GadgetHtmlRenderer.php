<?php
namespace apache\shindig\gadgets\render;
use apache\shindig\common\OpenSocialVersion;
use apache\shindig\common\Config;
use apache\shindig\gadgets\Gadget;

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
 * Renders a Gadget's Content type="html" view, inlining the content, feature javascript and javascript initialization
 * into the gadget's content. Most of the logic is performed with in the shared GadgetBaseRender class
 *
 */
class GadgetHtmlRenderer extends GadgetBaseRenderer {

  /**
   *
   * @param Gadget $gadget
   * @param array $view
   */
  public function renderGadget(Gadget $gadget, $view) {
    $this->setGadget($gadget);
    // Was a privacy policy header configured? if so set it
    if (Config::get('P3P') != '') {
      header("P3P: " . Config::get('P3P'));
    }
    $content = '';

    // Set no doctype if quirks mode is requestet because of quirks or doctype attribute
    if ((isset($view['quirks']) && $view['quirks']) || $gadget->useQuirksMode()) {
    } else {
      // Override & insert DocType if Gadget is written for OpenSocial 2.0 or greater,
      // if quirksmode is not set
      $version20 = new OpenSocialVersion('2.0.0');
      if ($gadget->getDoctype()) {
        $content .= "<!DOCTYPE " . $gadget->getDoctype() . "\n";
      } else if ($gadget->getSpecificationVersion()->isEqualOrGreaterThan($version20)) {
        $content .= "<!DOCTYPE HTML>\n";
      } else { // prior to 2.0 the php version always set this doc type, when no quirks attribute was specified
        $content .= "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n";
      }
    }

    // Rewriting the gadget's content using the libxml library does impose some restrictions to the validity of the input html, so
    // for the time being (until either gadgets are all fixed, or we find a more tolerant html parsing lib), we try to avoid it when we can
    $domRewrite = false;
    if (isset($gadget->gadgetSpec->rewrite) || Config::get('rewrite_by_default')) {
      $domRewrite = true;
    } elseif ((strpos($view['content'], 'text/os-data') !== false || strpos($view['content'], 'text/os-template') !== false) && ($gadget->gadgetSpec->templatesDisableAutoProcessing == false)) {
      $domRewrite = true;
    }
    if (!$domRewrite) {
      // Manually generate the html document using basic string concatinations instead of using our DOM based functions
      $content .= "<html>\n<head>\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>\n";
      $content .= '<style>'.Config::get('gadget_css')."</style>\n";

      $scripts = $this->getJavaScripts();
      foreach ($scripts as $script) {
        if ($script['type'] == 'inline') {
          $content .= "<script type=\"text/javascript\">{$script['content']}</script>\n";
        } else {
          $content .= "<script type=\"text/javascript\" src=\"{$script['content']}\"></script>\n";
        }
      }

      $content .= "</head>\n<body>\n";
      $content .= $gadget->substitutions->substitute($view['content']);
      $content .= '<script type="text/javascript">'.$this->getBodyScript()."</script>\n";
      $content .= "\n</body>\n</html>\n";
    } else {
      // Use the (libxml2 based) DOM rewriter
      $content .= "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/></head><body>\n";
      // Append the content for the selected view
      $content .= $gadget->substitutions->substitute($view['content']);
      $content .= "\n</body>\n</html>";
      $content = $this->parseTemplates($content);
      $content = $this->rewriteContent($content);
      $content = $this->addTemplates($content);
    }
    echo $content;
  }
}
