<?php
namespace apache\shindig\gadgets\render;
use apache\shindig\gadgets\SigningFetcherFactory;
use apache\shindig\common\ShindigRsaSha1SignatureMethod;
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

class GadgetUrlRenderer extends GadgetRenderer {

  /**
   * Renders an 'URL' type view (where the iframe is redirected to the specified url)
   * This is more a legacy iGoogle support feature than something that should be actually
   * used. Proxied content is the socially aware (and higher performance) version of this
   * See GadgetHrefRenderer for it's implementation.
   *
   * @param Gadget $gadget
   * @param Array $view
   */
  public function renderGadget(Gadget $gadget, $view) {
    $redirURI = $this->getSubstitutedUrl($gadget, $view);
    header('Location: ' . $redirURI);
  }

  /**
   * retrieves url of content tag and substitutes it
   *
   * @param Gadget $gadget
   * @param string $view
   * @return string
   */
  public function getSubstitutedUrl(Gadget $gadget, $view) {
    // Preserve existing query string parameters.
    $redirURI = $view['href'];
    $query = $this->getPrefsQueryString($gadget->gadgetSpec->userPrefs);

    // deal with features
    $registry = $this->context->getRegistry();
    // since the URL mode doesn't actually have the gadget XML body, it can't inline
    // the javascript content anyway - thus could us just ignore the 'forcedJsLibs' part.
    $sortedFeatures = array();
    $registry->sortFeatures($gadget->features, $sortedFeatures);

    $query .= $this->appendLibsToQuery($sortedFeatures);
    $query .= '&lang=' . urlencode(isset($_GET['lang']) ? $_GET['lang'] : 'en');
    $query .= '&country=' . urlencode(isset($_GET['country']) ? $_GET['country'] : 'US');

    $redirURI = $gadget->substitutions->substituteUri(null, $redirURI);
    if (strpos($redirURI, '?') !== false) {
      $redirURI = $redirURI . $query;
    } elseif (substr($query, 0, 1) == '&') {
      $redirURI = $redirURI . '?' . substr($query, 1);
    } else {
      $redirURI = $redirURI . '?' . $query;
    }

    $authz = $this->getAuthz($view);

    if ($authz === 'signed') {
      $gadgetSigner = Config::get('security_token_signer');
      $gadgetSigner = new $gadgetSigner();
      $token = $gadget->gadgetContext->extractAndValidateToken($gadgetSigner);

      $signingFetcherFactory = new SigningFetcherFactory(Config::get("private_key_file"));

      $redirURI .= '&xoauth_signature_publickey=' . urlencode($signingFetcherFactory->getKeyName());
      $redirURI .= '&xoauth_public_key=' . urlencode($signingFetcherFactory->getKeyName());

      if ($this->getSignOwner($view)) {
        $redirURI .= '&opensocial_owner_id=' . urlencode($token->getOwnerId());
      }
      if ($this->getSignViewer($view)) {
        $redirURI .= '&opensocial_viewer_id=' . urlencode($token->getViewerId());
      }

      $redirURI .= '&opensocial_app_url=' . urlencode($token->getAppUrl());
      $redirURI .= '&opensocial_app_id=' . urlencode($token->getAppId());
      $redirURI .= '&opensocial_instance_id=' . urlencode($token->getModuleId());

      $consumer = new \OAuthConsumer(NULL, NULL, NULL);
      $signatureMethod = new ShindigRsaSha1SignatureMethod($signingFetcherFactory->getPrivateKey(), null);
      $req_req = \OAuthRequest::from_consumer_and_token($consumer, NULL, 'GET', $redirURI);
      $req_req->sign_request($signatureMethod, $consumer, NULL);
      $redirURI = $req_req->to_url();


    }

    return $redirURI;
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

  /**
   * Returns the authz attribute of the view, can be 'none', 'signed' or 'oauth'
   *
   * @param array $view
   * @return string authz attribute
   */
  private function getAuthz($view) {
    return ! empty($view['authz']) ? strtolower($view['authz']) : 'none';
  }


  /**
   * Returns the signOwner attribute of the view (true or false, default is true)
   *
   * @param array $view
   * @return string signOwner attribute
   */
  private function getSignOwner($view) {
    return ! empty($view['signOwner']) && strcasecmp($view['signOwner'], 'false') == 0 ? false : true;
  }

  /**
   * Returns the signViewer attribute of the view (true or false, default is true)
   *
   * @param array $view
   * @return string signViewer attribute
   */
  private function getSignViewer($view) {
    return ! empty($view['signViewer']) && strcasecmp($view['signViewer'], 'false') == 0 ? false : true;
  }
}

