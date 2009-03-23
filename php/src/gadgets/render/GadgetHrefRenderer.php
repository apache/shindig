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

class GadgetHrefRenderer extends GadgetRenderer {

  public function renderGadget(Gadget $gadget, $view) {
    /* TODO
     * We should really re-add OAuth fetching support some day, uses these view atributes:
     * $view['oauthServiceName'], $view['oauthTokenName'], $view['oauthRequestToken'], $view['oauthRequestTokenSecret'];
    */

    $gadgetSigner = Config::get('security_token_signer');
    $gadgetSigner = new $gadgetSigner();
    $token = $gadget->gadgetContext->extractAndValidateToken($gadgetSigner);

    $authz = $this->getAuthz($view);
    $refreshInterval = $this->getRefreshInterval($view);
    $href = $this->buildHref($view, $token);

    $signingFetcherFactory = false;
    $request = new RemoteContentRequest($href);
    $request->setToken($token);
    if ($authz != 'NONE') {
      $signingFetcherFactory = new SigningFetcherFactory(Config::get("private_key_file"));
      $request->setAuthType($authz);
    }

    //TODO Currently our signing fetcher assumes it's being called from the makeRequest handler and the $_GET and $_POST should be relayed.
    // Here that's not the case, so we reset our super globals. We should refactor the signing fetcher to not make this assumption anymore.
    $_GET = array('st' => $_GET['st']);
    $_POST = array();

    $basicFetcher = new BasicRemoteContentFetcher();
    $basicRemoteContent = new BasicRemoteContent($basicFetcher, $signingFetcherFactory, $gadgetSigner);
    $response = $basicRemoteContent->fetch($request, $gadget->gadgetContext, $authz);
    echo $response->getResponseContent();
  }

  private function buildHref($view, $token) {
    $href = $view['href'];
    if (empty($href)) {
      throw new Exception("Invalid empty href in the gadget view");
    }    // add the required country and lang param to the URL
    $lang = isset($_GET['lang']) ? $_GET['lang'] : 'en';
    $country = isset($_GET['country']) ? $_GET['country'] : 'US';
    $firstSeperator = strpos($href, '?') === false ? '?' : '&';
    $href .= $firstSeperator . 'lang=' . urlencode($lang);
    $href .= '&country=' . urlencode($country);

    // our internal caching is based on the raw url, but the spec states that the container should only cache for a
    // unique url + lang + country + owner + viewer + appid, so we add those to the url too, so caching works as it should
    // (so in essense we *always* signOwner and signViewer)
    $href .= '&opensocial_owner_id=' . urlencode($token->getOwnerId());
    $href .= '&opensocial_viewer_id=' . urlencode($token->getViewerId());
    $href .= '&opensocial_app_id=' . urlencode($token->getAppId());
    $href .= "&opensocial_app_url=" . urlencode($token->getAppUrl());
    return $href;
  }

  private function getRefreshInterval($view) {
    return ! empty($view['refreshInterval']) && is_numeric($view['refreshInterval']) ? $view['refreshInterval'] : 3500;
  }

  private function getAuthz($view) {
    return ! empty($view['authz']) ? strtoupper($view['authz']) : 'NONE';
  }
}
