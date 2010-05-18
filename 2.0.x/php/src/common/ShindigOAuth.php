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

require_once 'external/OAuth/OAuth.php';

class ShindigOAuthProblemException extends Exception {}

class ShindigOAuthProtocolException extends Exception {}

class ShindigOAuthNoDataException extends Exception {}

class ShindigRsaSha1SignatureMethod extends OAuthSignatureMethod_RSA_SHA1 {
  private $privateKey;
  private $publicKey;
  
  public function __construct($privateKey, $publicKey) {
    $this->privateKey = $privateKey;
    $this->publicKey = $publicKey;
  }

  protected function fetch_public_cert(&$request) {
    return $this->publicKey;
  }

  protected function fetch_private_cert(&$request) {
    return $this->privateKey;
  }
}


class ShindigOAuth {
  public static $VERSION_1_0 = "1.0";
  public static $ENCODING = "UTF-8";
  public static $FORM_ENCODED = "application/x-www-form-urlencoded";
  public static $OAUTH_CONSUMER_KEY = "oauth_consumer_key";
  public static $OAUTH_TOKEN = "oauth_token";
  public static $OAUTH_TOKEN_SECRET = "oauth_token_secret";
  public static $OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
  public static $OAUTH_SIGNATURE = "oauth_signature";
  public static $OAUTH_TIMESTAMP = "oauth_timestamp";
  public static $OAUTH_NONCE = "oauth_nonce";
  public static $OAUTH_VERIFIER = "oauth_verifier";
  public static $OAUTH_VERSION = "oauth_version";
  public static $HMAC_SHA1 = "HMAC_SHA1";
  public static $RSA_SHA1 = "RSA_SHA1";
  public static $BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
  public static $END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
  public static $OAUTH_PROBLEM = "oauth_problem";
}

class ShindigOAuthRequest extends OAuthRequest {
  /**
   * Needed so that OAuthFetcher works with the correct type of requests.
   */
  public function __construct(OAuthRequest $request) {
    $this->parameters = $request->parameters;
    $this->http_url = $request->http_url;
    $this->http_method = $request->http_method;
  }

  /**
   * Needed so that OAuthFetcher works with the correct type of requests.
   * @return ShindigOAuthRequest
   */
  public static function from_consumer_and_token($consumer, $token, $http_method, $http_url, $parameters=NULL) {
    return new ShindigOAuthRequest(OAuthRequest::from_consumer_and_token($consumer, $token, $http_method, $http_url, $parameters));
  }

  /**
   * Needed so that OAuthFetcher works with the correct type of requests.
   * @return ShindigOAuthRequest
   */
  public static function from_request($http_method=NULL, $http_url=NULL, $parameters=NULL) {
    return new ShindigOAuthRequest(OAuthRequest::from_request($http_method, $http_url, $parameters));
  }

  /**
   * Needed in OAuthFetcher.php
   */
  public function set_parameters($params) {
    return $this->parameters = $params;
  }

  /**
   * Needed in OAuthFetcher.php
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
   */
  public function get_url() {
    return $this->http_url;
  }

  /**
   * Needed in SigningFetcher.php
   */
  public static function generate_nonce() {
    $mt = microtime();
    $rand = mt_rand();
    return md5($mt . $rand); // md5s look nicer than numbers
  }

  /**
   * Needed for from_consumer_and_token
   */
  private static function generate_timestamp() {
    return time();
  }
}

class ShindigOAuthUtil extends OAuthUtil {
  public static $AUTH_SCHEME = "OAuth";
  private static $AUTHORIZATION = "\ *[a-zA-Z0-9*]\ +(.*)";
  private static $NVP = "(\\S*)\\s*\\=\\s*\"([^\"]*)\"";


  /**
   * Needed in OAuthFetcher.php
   */
  public static function getPostBodyString(Array $params) {
    $result = '';
    $first = true;
    foreach ($params as $key => $val) {
      if ($first) {
        $first = false;
      } else {
        $result .= '&';
      }
      $result .= ShindigOAuthUtil::urlencode_rfc3986($key) . "=" . ShindigOAuthUtil::urlencode_rfc3986($val);
    }
    return $result;
  }
  
  /**
   * Needed in OAuthFetcher.php
   * Return true if the given Content-Type header means FORM_ENCODED.
   */
  public static function isFormEncoded($contentType) {
    if (! isset($contentType)) {
      return false;
    }
    $semi = strpos($contentType, ";");
    if ($semi != false) {
      $contentType = substr($contentType, 0, $semi);
    }
    return strtolower(ShindigOAuth::$FORM_ENCODED) == strtolower(trim($contentType));
  }

  /**
   * Needed in OAuthFetcher.php
   */
  public static function addParameters($url, $oauthParams) {
    $url .= strchr($url, '?') === false ? '?' : '&';
    foreach ($oauthParams as $key => $value) {
      $url .= ShindigOAuthUtil::urlencode_rfc3986($key)."=".ShindigOAuthUtil::urlencode_rfc3986($value)."&";
    }
    return $url;
  }

  /**
   * Needed in OAuthFetcher.php
   */
  public static function decodeForm($form) {
    $parameters = array();
    $explodedForm = explode("&", $form);
    foreach ($explodedForm as $params) {
      $value = explode("=", $params);
      if (! empty($value[0]) && ! empty($value[1])) {
        $parameters[ShindigOAuthUtil::urldecode_rfc3986($value[0])] = ShindigOAuthUtil::urldecode_rfc3986($value[1]);
      }
    }
    return $parameters;
  }

  /**
   * Needed in OAuthFetcher.php
   * 
   * Parse the parameters from an OAuth Authorization or WWW-Authenticate
   * header. The realm is included as a parameter. If the given header doesn't
   * start with "OAuth ", return an empty list.
   */
  public static function decodeAuthorization($authorization) {
    $into = array();
    if ($authorization != null) {
      $m = ereg(ShindigOAuthUtil::$AUTHORIZATION, $authorization);
      if ($m !== false) {
        if (strpos($authorization, ShindigOAuthUtil::$AUTH_SCHEME) == 0) {
          $authorization = str_replace("OAuth ", "", $authorization);
          $authParams = explode(", ", $authorization);
          foreach ($authParams as $params) {
            $m = ereg(ShindigOAuthUtil::$NVP, $params);
            if ($m == 1) {
              $keyValue = explode("=", $params);
              $name = ShindigOAuthUtil::urlencode_rfc3986($keyValue[0]);
              $value = ShindigOAuthUtil::urlencode_rfc3986(str_replace("\"", "", $keyValue[1]));
              $into[$name] = $value;
            }
          }
        }
      }
    }
    return $into;
  }
}
