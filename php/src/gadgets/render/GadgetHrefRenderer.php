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

  /**
   * Renders a 'proxied content' view, for reference see:
   * http://opensocial-resources.googlecode.com/svn/spec/draft/OpenSocial-Data-Pipelining.xml
   *
   * @param Gadget $gadget
   * @param array $view
   */
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

    // rewrite our $_GET to match the outgoing request, this is currently needed for the oauth library
    // to generate it's correct signature
    $_GET = $_POST = array();
    $uri = parse_url($href);
    parse_str($uri['query'], $_GET);

    $request = new RemoteContentRequest($href);
    $request->setMethod('GET');
    $request->setToken($token);
    $request->setRefreshInterval($refreshInterval);
    $request->setAuthType($authz);

    $signingFetcherFactory = false;
    if ($authz != 'none') {
      $signingFetcherFactory = new SigningFetcherFactory(Config::get("private_key_file"));
    }

    $basicFetcher = new BasicRemoteContentFetcher();
    $basicRemoteContent = new BasicRemoteContent($basicFetcher, $signingFetcherFactory, $gadgetSigner);
    $response = $basicRemoteContent->fetch($request, $gadget->gadgetContext, $authz);
    echo $response->getResponseContent();
  }

  /**
   * Builds the outgoing URL by taking the href attribute of the view and appending
   * the country, lang, and opensocial query params to it
   *
   * @param array $view
   * @param SecurityToken $token
   * @return string the url
   */
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
    //NOTE should check how this will work in the limited cache invalidation scope
    $href .= '&opensocial_owner_id=' . urlencode($token->getOwnerId());
    $href .= '&opensocial_viewer_id=' . urlencode($token->getViewerId());
    $href .= '&opensocial_app_id=' . urlencode($token->getAppId());
    $href .= "&opensocial_app_url=" . urlencode($token->getAppUrl());
    $container = isset($_GET['container']) ? $_GET['container'] : (isset($_GET['synd']) ? $_GET['synd'] : 'default');
    $href .= "&oauth_consumer_key=" . urlencode($container);
    return $href;
  }

  /**
   * Returns the requested refreshInterval (cache time) of the view, or if none is specified
   * it will return the configured default_refresh_interval value
   *
   * @param array $view
   * @return int refresh interval
   */
  private function getRefreshInterval($view) {
    return ! empty($view['refreshInterval']) && is_numeric($view['refreshInterval']) ? $view['refreshInterval'] : Config::get('default_refresh_interval');
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
}
