<?php
namespace apache\shindig\test\social;
use apache\shindig\social\sample\JsonDbOpensocialService;
use apache\shindig\social\servlet\DataServiceServlet;
use apache\shindig\common\sample\BasicSecurityToken;
use apache\shindig\common\Config;


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
class RestBase extends \PHPUnit_Framework_TestCase {
  
  private $securityToken;
  // The server to test against. You may need to add shindig to 127.0.0.1 mapping in /etc/hosts.
  private $restUrl = '';

  public function __construct() {
    $db = new JsonDbOpensocialService();
    $db->resetDb();
    $this->securityToken = BasicSecurityToken::createFromValues(1, 1, 1, 'partuza', 'test.com', 1, 0)->toSerialForm();
    $this->securityToken = urldecode($this->securityToken);
    $this->restUrl = 'http://localhost' . Config::get('web_prefix') . '/social/rest';
  }

  protected function curlRest($url, $postData, $contentType = 'application/json', $method = 'POST') {
    $_SERVER['CONTENT_TYPE'] = $contentType;
    $sep = strpos($url, '?') !== false ? '&' : '?';
    $_SERVER["REQUEST_URI"] = $this->restUrl . $url . $sep . 'st=' . $this->securityToken;
    $parsedUrl = parse_url($_SERVER["REQUEST_URI"]);
    $GLOBALS['HTTP_RAW_POST_DATA'] = $postData ? $postData : null;
    $_SERVER['REQUEST_METHOD'] = $method;
    $_SERVER['QUERY_STRING'] = $parsedUrl['query'];
    $_SERVER['HTTP_HOST'] = $parsedUrl['host'];
    $_GET = array('st' => $this->securityToken);
    $servlet = new DataServiceServlet();
    $servletMethod = 'do' . ucfirst(strtolower($method));
    $servlet->noHeaders = true; // prevents "modify header information" errors
    ob_start();
    $servlet->$servletMethod();
    $ret = ob_get_clean();
    //var_dump($ret);
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



