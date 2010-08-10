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
 * Base class for the REST api integration tests.
 */
class RestBase extends PHPUnit_Framework_TestCase {
  
  /* Modify this if you want to test in a different social graph then partuza. The security token
   * only works if ALLOW_PLAINTEXT_TOKEN is set to true in the file shindig/php/config/containerphp
   * The format of the plain text token is owner:viewer:appId:domain:appUrl:moduleId:containerId
   */
  private $securityToken = '1:1:1:partuza:test.com:1:0';
  // The server to test against. You may need to add shindig to 127.0.0.1 mapping in /etc/hosts.
  private $restUrl = '';

  public function __construct() {
    $this->restUrl = 'http://' . $_SERVER["HTTP_HOST"] . Config::get('web_prefix') . '/social/rest';
  }

  protected function curlRest($url, $postData, $contentType, $method = 'POST') {
    $ch = curl_init();
    if (substr($url, 0, 1) != '/') {
      $url = '/' . $url;
    }
    $sep = strpos($url, '?') !== false ? '&' : '?';
    curl_setopt($ch, CURLOPT_URL, $this->restUrl . $url . $sep . 'st=' . $this->securityToken);
    curl_setopt($ch, CURLOPT_HTTPHEADER, array("Content-Type: $contentType"));
    curl_setopt($ch, CURLOPT_HEADER, 0);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    $ret = curl_exec($ch);
    curl_close($ch);
    return $ret;
  }

  protected function getSecurityToken() {
    return $this->securityToken;
  }
  
  protected function setSecurityToken($token) {
    $this->securityToken = $token;
  }
  
  protected function getRestUrl() {
    return $this->restUrl;
  }
  
  protected function setRestUrl($url) {
    $this->restUrl = $url; 
  }
}



