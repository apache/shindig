<?php
namespace apache\shindig\social\servlet;
use apache\shindig\social\service\ResponseItem;
use apache\shindig\social\service\SocialSpiException;
use apache\shindig\common\HttpServlet;
use apache\shindig\common\Config;
use apache\shindig\common\AuthenticationMode;
use apache\shindig\common\sample\BasicSecurityToken;
use apache\shindig\common\SecurityToken;
use apache\shindig\social\service\ResponseError;
use apache\shindig\social\service\RequestItem;

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
 * Common base class for API servlets.
 */
abstract class ApiServlet extends HttpServlet {
  public $handlers = array();

  protected static $DEFAULT_ENCODING = "UTF-8";

  public function __construct() {
    parent::__construct();
    $this->setNoCache(true);
    if (isset($_SERVER['CONTENT_TYPE']) && (strtolower($_SERVER['CONTENT_TYPE']) != $_SERVER['CONTENT_TYPE'])) {
      // make sure the content type is in all lower case since that's what we'll check for in the handlers
      $_SERVER['CONTENT_TYPE'] = strtolower($_SERVER['CONTENT_TYPE']);
    }
    $acceptedContentTypes = array('application/atom+xml', 'application/xml', 'application/json', 'application/json-rpc', 'application/jsonrequest', 'application/javascript');
    if (isset($_SERVER['CONTENT_TYPE'])) {
      // normalize things like "application/json; charset=utf-8" to application/json
      foreach ($acceptedContentTypes as $contentType) {
        if (strpos($_SERVER['CONTENT_TYPE'], $contentType) !== false) {
          $_SERVER['CONTENT_TYPE'] = $contentType;
          $this->setContentType($contentType);
          break;
        }
      }
    }
    if (isset($GLOBALS['HTTP_RAW_POST_DATA'])) {
      if (! isset($_SERVER['CONTENT_TYPE']) || ! in_array($_SERVER['CONTENT_TYPE'], $acceptedContentTypes)) {
        $prefix = substr($_SERVER['CONTENT_TYPE'], 0, strpos($_SERVER['CONTENT_TYPE'], '/'));
        $acceptedMediaPrefixes = array('image', 'video', 'audio');
        if (! in_array($prefix, $acceptedMediaPrefixes)) {
          throw new \Exception("When posting to the social end-point you have to specify a content type,
              supported content types are: 'application/json', 'application/xml' and 'application/atom+xml'.
              For content upload, content type can be 'image/*', 'audio/*' and 'video/*'");
        }
      }
    }
  }

  /**
   *
   * @return SecurityToken
   */
  public function getSecurityToken() {
    // Support a configurable host name ('http_host' key) so that OAuth signatures don't fail in reverse-proxy type situations
    $scheme = (! isset($_SERVER['HTTPS']) || $_SERVER['HTTPS'] != "on") ? 'http' : 'https';
    $http_url = $scheme . '://' . (Config::get('http_host') ? Config::get('http_host') : $_SERVER['HTTP_HOST']) . $_SERVER['REQUEST_URI'];
    // see if we have an OAuth request
    $request = \OAuthRequest::from_request(null, $http_url, null);
    $appUrl = $request->get_parameter('oauth_consumer_key');
    $userId = $request->get_parameter('xoauth_requestor_id'); // from Consumer Request extension (2-legged OAuth)
    $signature = $request->get_parameter('oauth_signature');
    if ($appUrl && $signature) {
      //if ($appUrl && $signature && $userId) {
      // look up the user and perms for this oauth request
      $oauthLookupService = Config::get('oauth_lookup_service');
      $oauthLookupService = new $oauthLookupService();
      $token = $oauthLookupService->getSecurityToken($request, $appUrl, $userId, $this->getContentType());
      if ($token) {
        $token->setAuthenticationMode(AuthenticationMode::$OAUTH_CONSUMER_REQUEST);
        return $token;
      } else {
        return null; // invalid oauth request, or 3rd party doesn't have access to this user
      }
    } // else, not a valid oauth request, so don't bother


    // look for encrypted security token
    $token = BasicSecurityToken::getTokenStringFromRequest();
    if (empty($token)) {
      if (Config::get('allow_anonymous_token')) {
        // no security token, continue anonymously, remeber to check
        // for private profiles etc in your code so their not publicly
        // accessable to anoymous users! Anonymous == owner = viewer = appId = modId = 0
        // create token with 0 values, no gadget url, no domain and 0 duration
        $gadgetSigner = Config::get('security_token');
        return new $gadgetSigner(null, 0, SecurityToken::$ANONYMOUS, SecurityToken::$ANONYMOUS, 0, '', '', 0, Config::get('container_id'));
      } else {
        return null;
      }
    }
    $gadgetSigner = Config::get('security_token_signer');
    $gadgetSigner = new $gadgetSigner();
    return $gadgetSigner->createToken($token);
  }

  /**
   * @param ResponseItem $responseItem
   */
  protected abstract function sendError(ResponseItem $responseItem);

  protected function sendSecurityError() {
    $this->sendError(new ResponseItem(ResponseError::$UNAUTHORIZED, "The request did not have a proper security token nor oauth message and unauthenticated requests are not allowed"));
  }

  /**
   * Delivers a request item to the appropriate DataRequestHandler.
   *
   * @param RequestItem $requestItem
   * @return ResponseItem
   */
  protected function handleRequestItem(RequestItem $requestItem) {
    // lazy initialization of the service handlers, no need to instance them all for each request
 
    $service = $requestItem->getService();
 
    if (! isset($this->handlers[$service])) {
 
      $handlerClasses = Config::get('service_handler');
 
      if (isset($handlerClasses[$service])) {
          $handlerClass = $handlerClasses[$service];
          $this->handlers[$service] = new $handlerClass();
      } else {
          throw new SocialSpiException("The service " . $service . " is not implemented", ResponseError::$NOT_IMPLEMENTED);
      }
 
    }
    $handler = $this->handlers[$service];
    return $handler->handleItem($requestItem);
  }

  /**
   *
   * @param mixed $result
   * @return ResponseItem 
   */
  protected function getResponseItem($result) {
    if ($result instanceof ResponseItem) {
      return $result;
    } else {
      return new ResponseItem(null, null, $result);
    }
  }

  /**
   *
   * @param Exception $e
   * @return ResponseItem
   */
  protected function responseItemFromException($e) {
    if ($e instanceof SocialSpiException) {
      return new ResponseItem($e->getCode(), $e->getMessage(), null);
    }
    return new ResponseItem(ResponseError::$INTERNAL_ERROR, $e->getMessage());
  }
}
