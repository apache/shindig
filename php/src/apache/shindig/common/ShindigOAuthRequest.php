<?php
namespace apache\shindig\common;

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

class ShindigOAuthRequest extends \OAuthRequest {
  /**
   * Needed so that OAuthFetcher works with the correct type of requests.
   *
   * @param OAuthRequest $request
   */
  public function __construct(\OAuthRequest $request) {
    $this->parameters = $request->parameters;
    $this->http_url = $request->http_url;
    $this->http_method = $request->http_method;
  }

  /**
   * Needed so that OAuthFetcher works with the correct type of requests.
   * @return ShindigOAuthRequest
   */
  public static function from_consumer_and_token($consumer, $token, $http_method, $http_url, $parameters=NULL) {
    return new ShindigOAuthRequest(\OAuthRequest::from_consumer_and_token($consumer, $token, $http_method, $http_url, $parameters));
  }

  /**
   * Needed so that OAuthFetcher works with the correct type of requests.
   * @return ShindigOAuthRequest
   */
  public static function from_request($http_method=NULL, $http_url=NULL, $parameters=NULL) {
    return new ShindigOAuthRequest(\OAuthRequest::from_request($http_method, $http_url, $parameters));
  }

  /**
   * Needed in OAuthFetcher.php
   *
   * @param array $params
   * @return array
   */
  public function set_parameters($params) {
    return $this->parameters = $params;
  }

  /**
   * Needed in OAuthFetcher.php
   *
   * @param array $names
   */
  public function requireParameters($names) {
    $present = $this->parameters;
    $absent = array();
    foreach ($names as $required) {
      if (! in_array($required, $present)) {
        $absent[] = $required;
      }
    }
    if (count($absent) == 0) {
      throw new ShindigOAuthProblemException("oauth_parameters_absent: " . ShindigOAuthUtil::urlencodeRFC3986($absent));
    }
  }

  /**
   * Needed in OAuthFetcher.php
   *
   * @return string
   */
  public function get_url() {
    return $this->http_url;
  }

  /**
   * Needed in SigningFetcher.php
   *
   * @return string
   */
  public static function generate_nonce() {
    $mt = microtime();
    $rand = mt_rand();
    return md5($mt . $rand); // md5s look nicer than numbers
  }

  /**
   * Needed for from_consumer_and_token
   *
   * @return int
   */
  private static function generate_timestamp() {
    return time();
  }
}