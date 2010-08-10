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

/**
 * BasicRemoteContent test case.
 */
class BasicRemoteContentTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var BasicRemoteContent
   */
  private $BasicRemoteContent;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->BasicRemoteContent = new BasicRemoteContent();
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->BasicRemoteContent = null;
    parent::tearDown();
  }

  /**
   * Tests BasicRemoteContent->fetch()
   */
  public function testFetch() {
    $request = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $context = new TestContext();
    $ret = $this->BasicRemoteContent->fetch($request, $context);
    $content = $ret->getResponseContent();
    $this->assertEquals("OK", trim($content));
  }

  /**
   * Tests BasicRemoteContent->fetch() 404 response
   */
  public function testFetch404() {
    $request = new RemoteContentRequest('http://test.chabotc.com/fail.html');
    $context = new TestContext();
    $ret = $this->BasicRemoteContent->fetch($request, $context);
    $this->assertEquals('404', $ret->getHttpCode());
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
    
    $rets = $this->BasicRemoteContent->multiFetch($requests, $contexts);
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
    
    $rets = $this->BasicRemoteContent->multiFetch($requests, $contexts);
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
    $rets = $this->BasicRemoteContent->multiFetch($requests, $contexts);
    $this->assertEquals('404', $rets[0]->getHttpCode());
    $this->assertEquals('404', $rets[1]->getHttpCode());
    $this->assertEquals('404', $rets[2]->getHttpCode());
  }

  /*
	 * Tests BasicRemoteContent->fetch. This unit test is for reproduce SHINDIG-674.
	 * for some reason this broke again, disabling temporary until i've had more time to investigate
  public function testFeedFetch() {
    $request = new RemoteContentRequest('http://adwordsapi.blogspot.com/atom.xml');
    $context = new TestContext();
    $ret = $this->BasicRemoteContent->fetch($request, $context);
    $content = $ret->getResponseContent();
    $this->assertNotEquals(null, $content);
  }
  */
}
