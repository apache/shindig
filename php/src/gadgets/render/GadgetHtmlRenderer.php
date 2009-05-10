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

require 'GadgetBaseRenderer.php';

/**
 * Renders a Gadget's Content type="html" view, inlining the content, feature javascript and javascript initialization
 * into the gadget's content. Most of the logic is performed with in the shared GadgetBaseRender class
 *
 */
class GadgetHtmlRenderer extends GadgetBaseRenderer {

  public function renderGadget(Gadget $gadget, $view) {
    $this->setGadget($gadget);
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
    $content = $this->parseTemplates($content);
    $content = $this->rewriteContent($content);
    $content = $this->addTemplates($content);
    echo $content;
  }
}
