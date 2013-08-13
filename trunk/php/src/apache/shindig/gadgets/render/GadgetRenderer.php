<?php
namespace apache\shindig\gadgets\render;
use apache\shindig\common\Cache;
use apache\shindig\common\Config;
use apache\shindig\gadgets\GadgetContext;
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
 * base class that all the rendering methods inherit from
 *
 */
abstract class GadgetRenderer {
  /**
   * @var GadgetContext
   */
  protected $context;

  /**
   *
   * @param GadgetContext $context
   */
  public function __construct(GadgetContext $context) {
    $this->context = $context;
  }

  /**
   * generates the library string (core:caja:etc.js) including a checksum of all the
   * javascript content (?v=<md5 of js>) for cache busting
   *
   * @param array $features
   * @return string the list of libraries in core:caja:etc.js?v=checksum> format
   */
  protected function getJsUrl($features) {
    if (! is_array($features) || ! count($features)) {
      return 'null';
    }
    $registry = $this->context->getRegistry();
    // Given the JsServlet automatically expends the js library, we just need
    // to include the "leaf" nodes of the features.
    $ret = $features;
    foreach ($features as $feature) {
      $depFeatures = $registry->features[$feature]['deps'];
      $ret = array_diff($ret, $depFeatures);
    }
    $ret = implode(':', $ret);
    $cache = Cache::createCache(Config::get('feature_cache'), 'FeatureCache');
    if (($md5 = $cache->get(md5('getJsUrlMD5'))) === false) {
      $features = $registry->features;

      // Build a version string from the md5() checksum of all included javascript
      // to ensure the client always has the right version
      $inlineJs = '';
      foreach ($features as $feature => $content) {
        $inlineJs .= $registry->getFeatureContent($feature, $this->context, true);
      }
      $md5 = md5($inlineJs);
      $cache->set(md5('getJsUrlMD5'), $md5);
    }
    $ret .= ".js?v=" . $md5;
    return $ret;
  }

  /**
   * @param Gadget $gadget
   * @param array $view
   */
  abstract function renderGadget(Gadget $gadget, $view);
}
