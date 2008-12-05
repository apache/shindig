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
 * ProxyHandler test case.
 */
class ProxyHandlerTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var ProxyHandler
   */
  private $ProxyHandler;
  
  /**
   * @var context
   */
  private $context;
  
  /**
   * @var signingFetcherFactory
   */
  private $signingFetcherFactory;
  
  /**
   * @var signingFetcherFactory
   */
  private $url;
  
  /**
   * @var original_content
   */
  private $original_content;
  
  /**
   * @var proxy
   */
  private $proxy;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->context = new GadgetContext('GADGET');
    $this->url = 'http://' . $_SERVER["HTTP_HOST"] . Config::get('web_prefix') . '/test/gadgets/example.xml';
    $this->proxy = 'http://' . $_SERVER["HTTP_HOST"] . Config::get('web_prefix') . '/gadgets/proxy/';
    $this->original_content = file_get_contents($this->url);
    $this->ProxyHandler = new ProxyHandler($this->context);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->ProxyHandler = null;
    parent::tearDown();
  }

  private function getRemoteFile($url, $IfNoneMatch = null, $IfModifiedSince = null) {
    // get the host name and url path
    $parsedUrl = parse_url($url);
    $host = $parsedUrl['host'];
    if (isset($parsedUrl['path'])) {
      $path = $parsedUrl['path'];
    } else {
      // the url is pointing to the host like http://www.mysite.com
      $path = '/';
    }
    
    if (isset($parsedUrl['query'])) {
      $path .= '?' . $parsedUrl['query'];
    }
    
    if (isset($parsedUrl['port'])) {
      $port = $parsedUrl['port'];
    } else {
      $port = 80;
    }
    
    $timeout = 5;
    $response = '';
    
    // connect to the remote server
    $fp = @fsockopen($host, $port, $errno, $errstr, $timeout);
    
    if (! $fp) {
      echo "Cannot retrieve $url";
    } else {
      // send the necessary headers to get the file
      fputs($fp, "GET $path HTTP/1.0\r\n" . "Host: $host\r\n" . "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.0.3) Gecko/20060426 Firefox/1.5.0.3\r\n" . "Accept: */*\r\n" . ($IfNoneMatch !== null ? "If-None-Match: $IfNoneMatch\r\n" : '') . ($IfNoneMatch !== null ? "If-Modified-Since: $IfModifiedSince\r\n" : '') . "Keep-Alive: 300\r\n" . "Connection: keep-alive\r\n" . "Referer: http://$host\r\n\r\n");
      // retrieve the response from the remote server
      while ($line = fread($fp, 4096)) {
        $response .= $line;
      }
      
      fclose($fp);
    
    }
    
    // return the file content
    return $response;
  }

  private function getHTTPStatus($strHeaders) {
    return intval(substr($strHeaders, 8, 5));
  }

  /**
   * Tests ProxyHandler->fetch()
   */
  public function testFetch() {
    $out = file_get_contents($this->proxy . '?url=' . $this->url . '');
    $this->assertEquals($out, $this->original_content);
    $this->assertNotEquals($this->getHTTPStatus($this->getRemoteFile($this->proxy . '?url=' . $this->url . '')), $this->getHTTPStatus($this->getRemoteFile($this->proxy . '?url=' . $this->url . '', 'd9e124952eee27820768b8fadb0f0b78', gmdate("D, d M Y H:i:s", time() + 10000) . " GMT")), 'Checking HTTP 304 support');
  }

  /**
   * Tests ProxyHandler->fetchJson()
   */
  public function testFetchJson() {
    // FIXME we need a better test here 
    $out = file_get_contents($this->proxy . '?url=' . $this->url . '&output=js');
    $this->assertTrue(strpos($out, UNPARSEABLE_CRUFT) == 0);
  }

}

