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

/*
 * TODO Dynamically evaluate the limited EL subset expressions on the following tags:
 * Any attribute on os:DataRequest other than @key and @method
 * @userId
 * @groupId
 * @fields
 * @startIndex
 * @count
 * @sortBy
 * @sortOrder
 * @filterBy
 * @filterOp
 * @filterValue
 * @activityIds
 * @href
 * @params
 * Example:
 * <os:PeopleRequest key="PagedFriends" userId="@owner" groupId="@friends" startIndex="${ViewParams.first}" count="20"/>
 * <os:HttpRequest href="http://developersite.com/api?ids=${PagedFriends.ids}"/>
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

    $dataPipelining = false;
    if (count($view['dataPipelining'])) {
      $dataPipelining = $this->fetchDataPipelining($view['dataPipelining']);
    }

    /* TODO
     * We should really re-add OAuth fetching support some day, uses these view atributes:
     * $view['oauthServiceName'], $view['oauthTokenName'], $view['oauthRequestToken'], $view['oauthRequestTokenSecret'];
    */
    $authz = $this->getAuthz($view);
    $refreshInterval = $this->getRefreshInterval($view);
    $href = $this->buildHref($view);

    // rewrite our $_GET to match the outgoing request, this is currently needed for the oauth library
    // to generate it's correct signature
    $uri = parse_url($href);
    parse_str($uri['query'], $_GET);

    if ($dataPipelining) {
      // if data-pipeling results are set in $dataPipelining, post the json encoded version to the remote url
      $request = new RemoteContentRequest($href, "Content-type: application/json\n", json_encode($dataPipelining));
      $request->setMethod('POST');
    } else {
      // no data-pipelining set, use GET and set cache/refresh interval options
      $request = new RemoteContentRequest($href);
      $request->setMethod('GET');
      $request->setRefreshInterval($refreshInterval);
      $request->getOptions()->ignoreCache = $gadget->gadgetContext->getIgnoreCache();
    }

    $signingFetcherFactory = $gadgetSigner = false;
    if ($authz != 'none') {
      $gadgetSigner = Config::get('security_token_signer');
      $gadgetSigner = new $gadgetSigner();
      $token = $gadget->gadgetContext->extractAndValidateToken($gadgetSigner);
      $request->setToken($token);
      $request->setAuthType($authz);
      $signingFetcherFactory = new SigningFetcherFactory(Config::get("private_key_file"));
      $_GET = $_POST = array();
    }

    $basicFetcher = new BasicRemoteContentFetcher();
    $basicRemoteContent = new BasicRemoteContent($basicFetcher, $signingFetcherFactory, $gadgetSigner);
    $response = $basicRemoteContent->fetch($request);
    echo $response->getResponseContent();
  }

  /**
   * Fetches the requested data-pipeling info
   *
   * @param array $dataPipelining contains the parsed data-pipelining tags
   * @return array result
   */
  private function fetchDataPipelining($dataPipelining) {
    $result = array();
    do {
      // See which requests we can batch together, that either don't use dynamic tags or who's tags are resolvable
      $requestQueue = array();
      foreach ($dataPipelining as $key => $request) {
        if (($resolved = $this->resolveRequest($request, $result)) !== false) {
          $requestQueue[] = $resolved;
          unset($dataPipelining[$key]);
        }
      }
      if (count($requestQueue)) {
        $result = array_merge($this->performRequests($requestQueue), $result);
      }
    } while (count($requestQueue));
    return $result;
  }

  /**
   * Peforms the actual http fetching of the data-pipelining requests, all social requests
   * are made to $_SERVER['SERVER_NAME'] (the virtual host name of this server) / (optional) web_prefix / social / rpc, and
   * the httpRequest's are made to $_SERVER['SERVER_NAME'] (the virtual host name of this server) / (optional) web_prefix / gadgets / makeRequest
   * both request types use the current security token ($_GET['st']) when performing the requests so they happen in the correct context
   *
   * @param array $requests
   * @return array response
   */
  private function performRequests($requests) {
    $jsonRequests = array();
    $httpRequests = array();
    $decodedResponse = array();
    // Using the same gadget security token for all social & http requests so everything happens in the right context
    $securityToken = $_GET['st'];
    foreach ($requests as $request) {
      switch ($request['type']) {
        case 'os:DataRequest':
          // Add to the social request batch
          $id = $request['key'];
          $method = $request['method'];
          // remove our internal fields so we can use the remainder as params
          unset($request['key']);
          unset($request['method']);
          unset($request['type']);
          $jsonRequests[] = array('method' => $method, 'id' => $id, 'params' => $request);
          break;
        case 'os:HttpRequest':
          $id = $request['key'];
          $url = $request['href'];
          unset($request['key']);
          unset($request['type']);
          unset($request['href']);
          $httpRequests[] = array('id' => $id, 'url' => $url, 'queryStr' => implode('&', $request));
          break;
      }
    }
    if (count($jsonRequests)) {
      // perform social api requests
      $request = new RemoteContentRequest($_SERVER['SERVER_NAME'] . Config::get('web_prefix') . '/social/rpc?st=' . urlencode($securityToken) . '&format=json', "Content-type: application/json\n", json_encode($jsonRequests));
      $request->setMethod('POST');
      $basicFetcher = new BasicRemoteContentFetcher();
      $basicRemoteContent = new BasicRemoteContent($basicFetcher);
      $response = $basicRemoteContent->fetch($request);
      $decodedResponse = json_decode($response->getResponseContent(), true);
    }
    if (count($httpRequests)) {
      $requestQueue = array();
      foreach ($httpRequests as $request) {
        $req = new RemoteContentRequest($_SERVER['SERVER_NAME'] . Config::get('web_prefix') . '/gadgets/makeRequest?url=' . urlencode($request['url']) . '&st=' . urlencode($securityToken) . (! empty($request['queryStr']) ? '&' . $request['queryStr'] : ''));
        $req->getOptions()->ignoreCache = $this->context->getIgnoreCache();
        $req->setNotSignedUri($request['url']);
        $requestQueue[] = $req;
      }
      $basicRemoteContent = new BasicRemoteContent();
      $resps = $basicRemoteContent->multiFetch($requestQueue);
      foreach ($resps as $response) {
        // strip out the UNPARSEABLE_CRUFT (see makeRequestHandler.php) on assigning the body
        $resp = json_decode(str_replace("throw 1; < don't be evil' >", '', $response->getResponseContent()), true);
        if (is_array($resp)) {
          //FIXME: make sure that this is the format that java-shindig produces as well, the spec doesn't really state
          $decodedResponse = array_merge($resp, $decodedResponse);
        }
      }
    }
    return $decodedResponse;
  }

  /**
   * If a request (data-pipelining tag) doesn't include any dynamic tags, it's returned as is. If
   * however it does contain said tag, this function will attempt to resolve it using the $result
   * array, returning the parsed request on success, or FALSE on failure to resolve.
   *
   * @param array $request
   */
  private function resolveRequest($request, $result) {
    foreach ($request as $key => $val) {
      if (($pos = strpos($val, '${')) !== false) {
        $key = substr($val, $pos + 2);
        $key = substr($key, 0, strpos($key, '}'));
        if (($resolved = $this->resolveExpression($key, $result)) !== null) {
          $request[$key] = str_replace('${' . $key . '}', $resolved, $val);
        } else {
          return false;
        }
      }
    }
    return $request;
  }

  /**
   * Resolves simplified JSP-EL expressions if the matching entry exists in $data
   *
   * @param string $expression
   * @param array $data
   */
  private function resolveExpression($expression, $data) {
    //TODO implement this, see http://opensocial-resources.googlecode.com/svn/spec/draft/OpenSocial-Data-Pipelining.xml#rfc.section.14
    // always return null (aka can't resolve) until it's implemented
    return null;
  }

  /**
   * Builds the outgoing URL by taking the href attribute of the view and appending
   * the country, lang, and opensocial query params to it
   *
   * @param array $view
   * @param SecurityToken $token
   * @return string the url
   */
  private function buildHref($view) {
    $href = $view['href'];
    if (empty($href)) {
      throw new Exception("Invalid empty href in the gadget view");
    } // add the required country and lang param to the URL
    $lang = isset($_GET['lang']) ? $_GET['lang'] : 'en';
    $country = isset($_GET['country']) ? $_GET['country'] : 'US';
    $firstSeperator = strpos($href, '?') === false ? '?' : '&';
    $href .= $firstSeperator . 'lang=' . urlencode($lang);
    $href .= '&country=' . urlencode($country);
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
