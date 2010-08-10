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

require 'src/common/HttpServlet.php';
require 'src/gadgets/GadgetFeatureRegistry.php';
require 'src/gadgets/JsFeatureLoader.php';
require 'src/gadgets/JsLibrary.php';
require 'src/gadgets/GadgetFeatureFactory.php';
require 'src/gadgets/GadgetFeature.php';
require 'src/gadgets/JsLibraryFeatureFactory.php';

/**
 * This event handler deals with the /js/core:caja:etc.js request which content type=url gadgets can use
 * to retrieve our features javascript code, or used to make the most frequently used part of the feature
 * library external, and hence cachable by the browser
 */
class JsServlet extends HttpServlet {

  public function doGet() {
    $this->noHeaders = true;
    if (isset($_SERVER['HTTP_IF_MODIFIED_SINCE'])) {
      header("HTTP/1.1 304 Not Modified");
      header('Content-Length: 0');
      ob_end_clean();
      die();
    }
    $uri = strtolower($_SERVER["REQUEST_URI"]);
    $uri = substr($uri, strrpos($uri, '/') + 1);
    // remove any params that would confuse our parser
    if (strpos($uri, '?')) {
      $uri = substr($uri, 0, strpos($uri, '?'));
    }
    if (strpos($uri, '.js') !== false) {
      $uri = substr($uri, 0, strlen($uri) - 3);
    }
    $needed = array();
    if (strpos($uri, ':')) {
      $needed = explode(':', $uri);
    } else {
      $needed[] = $uri;
    }
    $found = array();
    $missing = array();
    $registry = new GadgetFeatureRegistry(Config::get('features_path'));
    if ($registry->getIncludedFeatures($needed, $found, $missing)) {
      $containerParam = isset($_GET["c"]) ? $_GET["c"] : '';
      $context = $containerParam == '1' ? 'CONTAINER' : 'GADGET';
      $jsData = '';
      $done = array();
      do {
        foreach ($found as $entry) {
          if (! in_array($entry, $done)) {
            $feat = $registry->getEntry($entry);
            $feature = $feat->getFeature();
            if ($feature instanceof JsLibraryFeatureFactory) {
              $jsLib = $feature;
              foreach ($jsLib->getLibraries($context) as $lib) {
                if ($lib->getType() != 'URL') {
                  $jsData .= $lib->getContent() . "\n";
                }
              }
            }
            $done[] = $entry;
          }
        }
      } while (count($done) != count($found));
      if (! strlen($jsData)) {
        header("HTTP/1.0 404 Not Found", true);
        die();
      }
      $this->setCachingHeaders();
      header("Content-Type: text/javascript");
      echo $jsData;
    } else {
      header("HTTP/1.0 404 Not Found", true);
    }
    die();
  }

  private function setCachingHeaders() {
    header("Expires: Tue, 01 Jan 2030 00:00:01 GMT");
    // IE seems to need this (10 years should be enough).
    header("Cache-Control: public,max-age=315360000");
    // Firefox requires this for certain cases.
    header("Last-Modified: " . gmdate('D, d M Y H:i:s', time()));
  }
}
