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

class GadgetUrlRenderer extends GadgetRenderer {

  /**
   * Renders an 'URL' type view (where the iframe is redirected to the specified url)
   * This is more a legacy iGoogle support feature then something that should be actually
   * used. Proxied content is the socially aware (and higher performance) version of this
   * See GadgetHrefRenderer for it's implementation.
   *
   * @param Gadget $gadget
   * @param Array $view
   */
  public function renderGadget(Gadget $gadget, $view) {
    // Preserve existing query string parameters.
    $redirURI = $view['href'];
    $queryStr = strpos($redirURI, '?') !== false ? substr($redirURI, strpos($redirURI, '?')) : '';
    $query = $queryStr;
    $query .= $this->getPrefsQueryString($gadget->gadgetSpec->userPrefs);
    $features = array();
    $forcedLibs = Config::get('focedJsLibs');
    if ($forcedLibs == null) {
      $features = $gadget->features;
    } else {
      $features = explode(':', $forcedLibs);
    }
    $query .= $this->appendLibsToQuery($features);
    // code bugs out with me because of the invalid url syntax since we dont have a URI class to fix it for us
    // this works around that
    if (substr($query, 0, 1) == '&') {
      $query = '?' . substr($query, 1);
    }
    $redirURI .= $query;
    header('Location: ' . $redirURI);
  }

  /**
   * Returns the requested libs (from getjsUrl) with the libs_param_name prepended
   * ie: in libs=core:caja:etc.js format
   *
   * @param string $libs the libraries
   * @param Gadget $gadget
   * @return string the libs=... string to append to the redirection url
   */
  private function appendLibsToQuery($features) {
    $ret = "&";
    $ret .= Config::get('libs_param_name');
    $ret .= "=";
    $ret .= str_replace('?', '&', $this->getJsUrl($features));
    return $ret;
  }

  /**
   * Returns the user preferences in &up_<name>=<val> format
   *
   * @param array $libs array of features this gadget requires
   * @param Gadget $gadget
   * @return string the up_<name>=<val> string to use in the redirection url
   */
  private function getPrefsQueryString($prefs) {
    $ret = '';
    foreach ($prefs as $pref) {
      $ret .= '&';
      $ret .= Config::get('userpref_param_prefix');
      $ret .= urlencode($pref['name']);
      $ret .= '=';
      $ret .= urlencode($pref['value']);
    }
    return $ret;
  }
}
