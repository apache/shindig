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

class ShindigOAuthUtil extends \OAuthUtil {
  public static $AUTH_SCHEME = "OAuth";
  private static $AUTHORIZATION = "\ *[a-zA-Z0-9*]\ +(.*)";
  private static $NVP = "(\\S*)\\s*\\=\\s*\"([^\"]*)\"";


  /**
   * Needed in OAuthFetcher.php
   *
   * @param array $params
   * @return string
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
   *
   * @param string $contentType
   * @return boolean
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
   *
   * @param string $url
   * @param array $oauthParams
   * @return string
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
   *
   * @param string $form
   * @return array
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
   *
   * @param string $authorization
   * @return array
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
