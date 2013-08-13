<?php
namespace apache\shindig\gadgets\oauth;

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
 * The OAuth service located in the gadget xml inside ModulePrefs -> OAuth or ModulePrefs -> OAuth2.
 *
 * Since OAuth and OAuth2 implementation are similar we are using the same OAuthService for both implementations
 * as well for now. The only difference is, that OAuth2 services don't need an request token endpoint
 **/
class OAuthService {

  private static $URL_ATTR = "url";
  private static $PARAM_LOCATION_ATTR = "param_location";
  private static $METHOD_ATTR = "method";

  /**
   * @var string
   */
  private $name;

  /**
   * @var string EndPoint
   */
  private $requestUrl;

  /**
   * @var string EndPoint
   */
  private $authorizationUrl;

  /**
   * @var string EndPoint
   */
  private $accessUrl;

  /**
   *
   * @param DOMElement $service
   * @throws SpecParserException
   */
  public function __construct(\DOMElement $service) {
    $this->name = (string)$service->getAttribute('name');
    $elements = $service->getElementsByTagName('*');
    foreach ($elements as $element) {
      $type = $element->tagName;
      if ($type == 'Request') {
        if ($this->requestUrl) {
          throw new SpecParserException("Multiple OAuth/Service/Request elements");
        }
        $this->requestUrl = $this->parseEndPoint($element);
      } else if ($type == 'Authorization') {
        if ($this->authorizationUrl) {
          throw new SpecParserException("Multiple OAuth/Service/Authorization elements");
        }
        $this->authorizationUrl = $this->parseEndPoint($element);
      } else if ($type == 'Access' || $type == 'Token') {
        if ($this->accessUrl) {
          throw new SpecParserException("Multiple OAuth/Service/Access elements");
        }
        $this->accessUrl = $this->parseEndPoint($element);
      }
    }
    if ($this->accessUrl == null) {
      throw new SpecParserException("/OAuth/Service/Access is required");
    }
    if ($this->authorizationUrl == null) {
      throw new SpecParserException("/OAuth/Service/Authorization is required");
    }
    if ($this->requestUrl && $this->requestUrl->location != $this->accessUrl->location) {
      throw new SpecParserException(
          "Access@location must be identical to Request@location");
    }
    if ($this->requestUrl && $this->requestUrl->method != $this->accessUrl->method) {
      throw new SpecParserException(
          "Access@method must be identical to Request@method");
    }
    if ($this->requestUrl && $this->requestUrl->location == Location::$body &&
        $this->requestUrl->method == Method::$GET) {
      throw new SpecParserException("Incompatible parameter location, cannot" +
          "use post-body with GET requests");
    }
  }

  /**
   *
   * @param DOMElement $element
   * @return EndPoint
   */
  private function parseEndPoint($element) {
    $url = trim($element->getAttribute(OAuthService::$URL_ATTR));
    if (empty($url)) {
      throw new SpecParserException("Not an HTTP url");
    }
    $location = Location::$header;
    $locationString = trim($element->getAttribute(OAuthService::$PARAM_LOCATION_ATTR));
    if (! empty($locationString)) {
      $location = $locationString;
    }
    $method = Method::$GET;
    $methodString = trim($element->getAttribute(OAuthService::$METHOD_ATTR));
    if (! empty($methodString)) {
      $method = $methodString;
    }
    return new EndPoint($url, $method, $location);
  }

  /**
   * @return string
   */
  public function getName() {
    return $this->name;
  }

  /**
   * @return string
   */
  public function getRequestUrl() {
    return $this->requestUrl;
  }

  /**
   * @return string
   */
  public function getAuthorizationUrl() {
    return $this->authorizationUrl;
  }

  /**
   * @return string
   */
  public function getAccessUrl() {
    return $this->accessUrl;
  }
}
