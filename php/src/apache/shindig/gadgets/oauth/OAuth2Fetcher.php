<?php
namespace apache\shindig\gadgets\oauth;
use apache\shindig\common\ShindigOAuthProtocolException;
use apache\shindig\gadgets\GadgetException;
use apache\shindig\common\RemoteContentRequest;
use apache\shindig\common\Config;
use apache\shindig\common\ShindigOAuth;

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
 * implements \the OAuth 2.0 dance for gadgets.
 *
 *
 * This class is not thread-safe; create a new one for each request that
 * requires OAuth signing.
 */
class OAuth2Fetcher extends OAuthFetcher {
  /**
   * @param RemoteContentRequest $request
   * @return RemoteContentRequest
   */
  public function fetchRequest(RemoteContentRequest $request) {
  	$this->realRequest = $request;
    $this->checkCanApprove();
    if ($this->needApproval()) {
      $this->buildAznUrl();
      // break out of the content fetching chain, we need permission from
      // the user to do this
      return $this->buildOAuthApprovalResponse();
    } elseif ($this->needAccessToken()) {
      $this->getAccessToken($request);
      $this->saveAccessToken();
      $this->buildClientAccessState();
    }
    return $this->fetchData();
  }  
  
  /**
   * Do we need to get the user's approval to access the data?
   *
   * @return boolean
   */
  protected function needApproval() {
    if ($this->accessorInfo == NULL) {
      return true;
    } else {
      return ($this->accessorInfo->getAccessor()->accessToken == null && ! $this->requestParams->getReceivedCallback());
    }
  }
  
  /**
   * Do we need to exchange a request token for an access token?
   *
   * @return boolean
   */
  protected function needAccessToken() {
    return ($this->accessorInfo->getAccessor()->accessToken == null && $this->requestParams->getReceivedCallback());
  }
  
  /**
   * Get honest-to-goodness user data.
   *
   * @return RemoteContentRequest
   */
  protected function fetchData() {
    try {
      $headers = 'Authorization: Bearer ' . $this->accessorInfo->getAccessor()->accessToken;
      $this->realRequest->setHeaders($headers);
      $remoteFetcherClass = Config::get('remote_content_fetcher');
      $fetcher = new $remoteFetcherClass();
      $content = $fetcher->fetchRequest($this->realRequest);
      $statusCode = $content->getHttpCode();
      //TODO is there a better way to detect an SP error? For example: http://wiki.oauth.net/ProblemReporting
      if ($statusCode == 401) {
        $tokenKey = $this->buildTokenKey();
        $this->tokenStore->removeTokenAndSecret($tokenKey);
      } else if ($statusCode >= 400 && $statusCode < 500) {
        $message = $this->parseAuthHeader(null, $content);
        if ($message->get_parameter(ShindigOAuth::$OAUTH_PROBLEM) != null) {
          throw new ShindigOAuthProtocolException($message);
        }
      }
      // Track metadata on the response
      $this->addResponseMetadata($content);
      return $content;
    } catch (\Exception $e) {
      throw new GadgetException("INTERNAL SERVER ERROR: " . $e);
    }
  }
  
  /**
   * Builds the URL the client needs to visit to approve access.
   */
  protected function buildAznUrl() {
    // At some point we can be clever and use a callback URL to improve
    // the user experience, but that's too complex for now.
    $accessor = $this->accessorInfo->getAccessor();
    $azn = $accessor->consumer->callback_url->userAuthorizationURL;
    $authUrl = $azn->url;
    if (strstr($authUrl, "?") == FALSE) {
      $authUrl .= "?";
    } else {
      $authUrl .= "&";
    }
    $authUrl .= "client_id=";
    $authUrl .= urlencode($accessor->consumer->key);
    $authUrl .= '&response_type=code';
    $callbackState = new OAuthCallbackState($this->oauthCrypter);
    $callbackUrl = "http://" . getenv('HTTP_HOST') . "/gadgets/oauthcallback";
    $callbackState->setRealCallbackUrl($callbackUrl);
    $state = $callbackState->getEncryptedState();
    $authUrl .= "&state=" . urlencode($state);
    $this->aznUrl = $authUrl;
  }
  
  /**
   *
   * @param RemoteContentRequest $request
   * @throws GadgetException
   */
  protected function getAccessToken(RemoteContentRequest $request) {
    try {
      $accessor = $this->accessorInfo->getAccessor();
      $url = $accessor->consumer->callback_url->accessTokenURL;
      $msgParams = array();
      $callbackUrl = $this->requestParams->getReceivedCallback();
      if (strlen($callbackUrl) > 0) {
        $parsed_url = parse_url($callbackUrl);
        parse_str($parsed_url["query"], $url_params);
        $this->handleErrorResponse($url_params);
        if (strlen($url_params["code"])) {
          $msgParams['code'] = $url_params["code"];
          $msgParams['grant_type'] = 'authorization_code';
        } else {
          throw new GadgetException("Invalid received callback URL: ".$callbackUrl);
        }
      }
      $msgParams['client_id'] = urlencode($accessor->consumer->key);
      $msgParams['client_secret'] = urlencode($accessor->consumer->secret);
      $msgParams['redirect_uri'] = "http://" . getenv('HTTP_HOST') . "/gadgets/oauthcallback";
      
      $request = new RemoteContentRequest($url->url);
      $request->setMethod('POST');
      $request->setPostBody($msgParams);
      
      $remoteFetcherClass = Config::get('remote_content_fetcher');
      $fetcher = new $remoteFetcherClass();
      $content = $fetcher->fetchRequest($request);
      $responseObject = json_decode($content->getResponseContent(), true);
      $this->handleErrorResponse($responseObject);
      if (! isset($responseObject['access_token'])) {
        throw new GadgetException("invalid access token response");  
      }
      
      $accessor->accessToken = $responseObject['access_token'];
    } catch (\Exception $e) {
      // It's unfortunate the OAuth libraries throw a generic Exception.
      throw new GadgetException("INTERNAL SERVER ERROR: " . $e);
    }
  }
  
  /**
   *
   * @param array $parameters 
   */
  protected function handleErrorResponse(array $parameters) {
    if (isset($parameters['error'])) {
      throw new GadgetException('Received OAuth error ' . $parameters['error'] . 
              (isset($parameters['error_description']) ? ' ' . $parameters['error_description'] : '') .
              (isset($parameters['error_uri']) ? ' see: ' . $parameters['error_uri'] : ''));
    }
  }
}