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

/**
 * The OAuth service located in the gadget xml inside ModulePrefs -> OAuth.
 **/
class OAuthService {
  
  private static $URL_ATTR = "url";
  private static $PARAM_LOCATION_ATTR = "param_location";
  private static $METHOD_ATTR = "method";
  
  private $name;
  private $requestUrl;
  private $authorizationUrl;
  private $accessUrl;

  public function __construct($service) {
    $attrs = $service->attributes();
    $this->name = (string)$attrs['name'];
    if (isset($service->Request)) {
      $this->requestUrl = $this->parseEndPoint($service->Request->attributes());
    }
    if (isset($service->Authorization)) {
      $this->authorizationUrl = $this->parseEndPoint($service->Authorization->attributes());
    }
    if (isset($service->Access)) {
      $this->accessUrl = $this->parseEndPoint($service->Access->attributes());
    }
  }

  private function parseEndPoint($element) {
    $url = trim((string)$element[OAuthService::$URL_ATTR]);
    if (empty($url)) {
      throw new SpecParserException("Not an HTTP url");
    }
    $location = Location::$header;
    $locationString = trim((string)$element[OAuthService::$PARAM_LOCATION_ATTR]);
    if (! empty($locationString)) {
      $location = $locationString;
    }
    $method = Method::$GET;
    $methodString = trim((string)$element[OAuthService::$METHOD_ATTR]);
    if (! empty($methodString)) {
      $method = $methodString;
    }
    return new EndPoint($url, $method, $location);
  }

  public function getName() {
    return $this->name;
  }

  public function getRequestUrl() {
    return $this->requestUrl;
  }

  public function getAuthorizationUrl() {
    return $this->authorizationUrl;
  }

  public function getAccessUrl() {
    return $this->accessUrl;
  }
}

/**
 * Method to use for requests to an OAuth request token or access token URL.
 */
class Method {
  public static $GET = "GET";
  public static $POST = "POST";
}

/**
 * Location for OAuth parameters in requests to an OAuth request token,
 * access token, or resource URL.  (Lowercase to match gadget spec schema)
 */
class Location {
  public static $header = "auth-header";
  public static $url = "url-query";
  public static $body = "post-body";
}

/**
 * Description of an OAuth request token or access token URL.
 */
class EndPoint {
  public $url;
  public $method;
  public $location;

  public function __construct($url, $method, $location) {
    $this->url = $url;
    $this->method = $method;
    $this->location = $location;
  }
}
