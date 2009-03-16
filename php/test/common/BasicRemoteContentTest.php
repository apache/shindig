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

require_once 'src/common/sample/BasicRemoteContent.php';

require_once 'external/PHPUnit/Framework/TestCase.php';

class MockRemoteContentFetcher extends RemoteContentFetcher {
  private $expectedRequest = array();

  private $expectedMultiRequest = array();

  private $actualRequest = array();

  private $actualMultiRequest = array();
  
  private $valid = array(true, true, true, true);

  public function fetchRequest(RemoteContentRequest $request) {
    $this->actualRequest[] = $request;
    $this->fetch($request);
    return $request;
  }

  public function multiFetchRequest(Array $requests) {
    $this->actualMultiRequest[] = $requests;
    foreach ($requests as $request) {
      $this->fetch($request);
    }
    return $requests;
  }

  public function expectFetchRequest(RemoteContentRequest $request) {
    $this->expectedRequest[] = $request;
  }

  public function expectMultiFetchRequest(Array $requests) {
    $this->expectedMultiRequest[] = $requests;
  }

  public function verify() {
    $result = ($this->expectedRequest == $this->actualRequest) &&
              ($this->expectedMultiRequest == $this->actualMultiRequest);
    $this->clean();
    return $result;
  }

  public function clean() {
    $this->actualRequest = array();
    $this->actualMultiRequest = array();
    $this->expectedRequest = array();
    $this->expectedMultiRequest = array();
  }

  private function fetch(RemoteContentRequest $request) {
    if ($request->getUrl() == 'http://test.chabotc.com/ok.html') {
      $request->setHttpCode(200);
      $request->setContentType('text/html; charset=UTF-8');
      $request->setResponseContent('OK');
    } else if ($request->getUrl() == 'http://test.chabotc.com/fail.html') {
      $request->setHttpCode(404);
    } else if (preg_match('/http:\/\/test\.chabotc\.com\/valid(\d)\.html/',
                          $request->getUrl(), $matches) > 0) {
      if ($this->valid[intval($matches[1])]) {
        $this->valid[intval($matches[1])] = false;
        $request->setHttpCode(200);
        $request->setContentType('text/html; charset=UTF-8');
        $request->setResponseContent('OK');
      } else {
        $request->setHttpCode(404);
      }
    }
  }
}

/**
 * BasicRemoteContent test case.
 */
class BasicRemoteContentTest extends PHPUnit_Framework_TestCase {

  /**
   * @var BasicRemoteContent
   */
  private $basicRemoteContent = null;

  /**
   * @var MockRemoteContentFetcher
   */
  private $fetcher = null;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->fetcher = new MockRemoteContentFetcher();
    $this->basicRemoteContent = new BasicRemoteContent($this->fetcher);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->basicRemoteContent = null;
    $this->fetcher = null;
    parent::tearDown();
  }
  
  /**
   * Tests BasicRemoteContent->__construct()
   */
  public function testConstruct() {
    $basic = new BasicRemoteContent(new BasicRemoteContentFetcher(), null, false);
    $signing = new BasicRemoteContent(new BasicRemoteContentFetcher(), new SigningFetcherFactory(), new BasicSecurityTokenDecoder());
  }

  /**
   * Tests BasicRemoteContent->fetch()
   */
  public function testFetch() {
    $request = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $context = new TestContext();
    $ret = $this->basicRemoteContent->fetch($request, $context);
    $content = $ret->getResponseContent();
    $this->assertEquals("OK", trim($content));
  }

  /**
   * Tests BasicRemoteContent->fetch() 404 response
   */
  public function testFetch404() {
    $request = new RemoteContentRequest('http://test.chabotc.com/fail.html');
    $context = new TestContext();
    $ret = $this->basicRemoteContent->fetch($request, $context);
    $this->assertEquals('404', $ret->getHttpCode());
  }
  
  /**
   * Tests BasicRemoteContent->fetch() with different response
   */
  public function testFetchValid() {
    $this->fetcher->clean();
    $request = new RemoteContentRequest('http://test.chabotc.com/valid0.html');
    $context = new TestContext();
    $this->basicRemoteContent->invalidate($request);
    $this->fetcher->expectFetchRequest($request);
    $ret = $this->basicRemoteContent->fetch($request, $context);
    $this->assertTrue($this->fetcher->verify());
    $content = $ret->getResponseContent();
    $this->assertEquals("OK", trim($content));
    
    $request = new RemoteContentRequest('http://test.chabotc.com/valid0.html');
    $context = new TestContext();
    $this->basicRemoteContent->invalidate($request);
    $this->fetcher->expectFetchRequest($request);
    $ret = $this->basicRemoteContent->fetch($request, $context);
    $this->assertTrue($this->fetcher->verify());
    $content = $ret->getResponseContent();
    $this->assertEquals("OK", trim($content));
  }
  
  /**
   * Tests BasicRemoteContent->multiFetch() with different response
   */
  public function testmultiFetchValid() {
    $this->fetcher->clean();
    
    $requests = array();
    $contexts = array();
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/valid1.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/valid2.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/valid3.html');
    $contexts[] = new TestContext();
    $contexts[] = new TestContext();
    $contexts[] = new TestContext();
    $this->basicRemoteContent->invalidate($requests[0]);
    $this->basicRemoteContent->invalidate($requests[1]);
    $this->basicRemoteContent->invalidate($requests[2]);
    $this->fetcher->expectMultiFetchRequest($requests);
    $rets = $this->basicRemoteContent->multiFetch($requests, $contexts);
    $this->assertTrue($this->fetcher->verify());
    $content_0 = $rets[0]->getResponseContent();
    $content_1 = $rets[1]->getResponseContent();
    $content_2 = $rets[2]->getResponseContent();
    $this->assertEquals("OK", trim($content_0));
    $this->assertEquals("OK", trim($content_1));
    $this->assertEquals("OK", trim($content_2));
    $this->assertEquals('200', $rets[0]->getHttpCode());
    $this->assertEquals('200', $rets[1]->getHttpCode());
    $this->assertEquals('200', $rets[2]->getHttpCode());
    
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/valid1.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/valid2.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/valid3.html');
    $contexts[] = new TestContext();
    $contexts[] = new TestContext();
    $contexts[] = new TestContext();
    $this->basicRemoteContent->invalidate($requests[0]);
    $this->basicRemoteContent->invalidate($requests[1]);
    $this->basicRemoteContent->invalidate($requests[2]);
    $this->fetcher->expectMultiFetchRequest($requests);
    $rets = $this->basicRemoteContent->multiFetch($requests, $contexts);
    $this->assertTrue($this->fetcher->verify());
    $content_0 = $rets[0]->getResponseContent();
    $content_1 = $rets[1]->getResponseContent();
    $content_2 = $rets[2]->getResponseContent();
    $this->assertEquals("OK", trim($content_0));
    $this->assertEquals("OK", trim($content_1));
    $this->assertEquals("OK", trim($content_2));
    $this->assertEquals('200', $rets[0]->getHttpCode());
    $this->assertEquals('200', $rets[1]->getHttpCode());
    $this->assertEquals('200', $rets[2]->getHttpCode());
  }

  /**
   * Tests BasicRemoteContent->fetch() 200, 200 and 200 responses
   */
  public function testMultiFetch() {
    $requests = array();
    $contexts = array();
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $contexts[] = new TestContext();
    $contexts[] = new TestContext();
    $contexts[] = new TestContext();

    $rets = $this->basicRemoteContent->multiFetch($requests, $contexts);
    $content_0 = $rets[0]->getResponseContent();
    $content_1 = $rets[1]->getResponseContent();
    $content_2 = $rets[2]->getResponseContent();
    $this->assertEquals("OK", trim($content_0));
    $this->assertEquals("OK", trim($content_1));
    $this->assertEquals("OK", trim($content_2));
    $this->assertEquals('200', $rets[0]->getHttpCode());
    $this->assertEquals('200', $rets[1]->getHttpCode());
    $this->assertEquals('200', $rets[2]->getHttpCode());
  }

  /**
   * Tests BasicRemoteContent->Multifetch() 200, 200 and 404 responses
   */
  public function testMultiFetchMix() {
    $requests = array();
    $contexts = array();
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/fail.html');
    $contexts[] = new TestContext();
    $contexts[] = new TestContext();
    $contexts[] = new TestContext();

    $rets = $this->basicRemoteContent->multiFetch($requests, $contexts);
    $content_0 = $rets[0]->getResponseContent();
    $content_1 = $rets[1]->getResponseContent();
    $this->assertEquals("OK", trim($content_0));
    $this->assertEquals("OK", trim($content_1));
    $this->assertEquals('200', $rets[0]->getHttpCode());
    $this->assertEquals('200', $rets[1]->getHttpCode());
    $this->assertEquals('404', $rets[2]->getHttpCode());
  }

  /**
   * Tests BasicRemoteContent->Multifetch() 404, 404 and 404 responses
   */
  public function testMultiFetch404() {
    $requests = array();
    $contexts = array();
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/fail.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/fail.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/fail.html');
    $contexts[] = new TestContext();
    $contexts[] = new TestContext();
    $contexts[] = new TestContext();
    $rets = $this->basicRemoteContent->multiFetch($requests, $contexts);
    $this->assertEquals('404', $rets[0]->getHttpCode());
    $this->assertEquals('404', $rets[1]->getHttpCode());
    $this->assertEquals('404', $rets[2]->getHttpCode());
  }

  /**
   * Tests BasicRemoteContent->fetch.
   */
  public function testFeedFetch() {
    $fetcher = new BasicRemoteContentFetcher();
    $this->basicRemoteContent->setBasicFetcher($fetcher);
    $request = new RemoteContentRequest('http://adwordsapi.blogspot.com/atom.xml');
    $context = new TestContext();
    $ret = $this->basicRemoteContent->fetch($request, $context);
    $content = $ret->getResponseContent();
    $this->assertNotEquals(null, $content);
  }


  /**
   * Tests BasicRemoteContent->invalidate()
   */
  public function testInvalidate() {
    // Fetches url for the first time.
    $request = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $context = new TestContext();
    $ret = $this->basicRemoteContent->fetch($request, $context);
    $this->fetcher->clean();
    $content = $ret->getResponseContent();
    $this->assertEquals("OK", trim($content));

    // Fetches url again and $this->fetcher->fetchRequest will not be called.
    $request = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $context = new TestContext();
    $ret = $this->basicRemoteContent->fetch($request, $context);
    $this->assertTrue($this->fetcher->verify());
    $content = $ret->getResponseContent();
    $this->assertEquals("OK", trim($content));

    // Invalidates cache and fetches url.
    // $this->fetcher->fetchRequest will be called.
    $request = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $context = new TestContext();
    $this->fetcher->expectFetchRequest($request);
    $this->basicRemoteContent->invalidate($request);
    $ret = $this->basicRemoteContent->fetch($request, $context);
    $this->assertTrue($this->fetcher->verify());
    $content = $ret->getResponseContent();
    $this->assertEquals("OK", trim($content));
  }
}
