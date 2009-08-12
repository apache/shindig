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

class MockSigningFetcherFactory {
  private $keyName;
  private $privateKey;

  /**
   * Produces a signing fetcher that will sign requests and delegate actual
   * network retrieval to the {@code networkFetcher}
   *
   * @param RemoteContentFetcher $networkFetcher The fetcher that will be doing actual work.
   * @return SigningFetcher
   * @throws GadgetException
   */
  public function getSigningFetcher(RemoteContentFetcher $networkFetcher) {
    return SigningFetcher::makeFromB64PrivateKey($networkFetcher, $this->keyName, $this->privateKey);
  }

  /**
   * @here will create a private key.
   */
  public function __construct() {
    $privkey = openssl_pkey_new();
    $phrase = Config::get('private_key_phrase') != '' ? (Config::get('private_key_phrase')) : null;
    openssl_pkey_export($privkey, $rsa_private_key, $phrase);
    
    if (! $rsa_private_key = @openssl_pkey_get_private($rsa_private_key, $phrase)) {
      throw new Exception("Could not create the key");
    }
    $this->privateKey = $rsa_private_key;
    $this->keyName = 'http://' . $_SERVER["HTTP_HOST"] . Config::get('web_prefix') . '/public.cer';
  }
}

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
    } else if (strpos($request->getUrl(), 'http://test.chabotc.com/signing.html') == 0) {
      $url = parse_url($request->getUrl());
      $query = array();
      parse_str($url['query'], $query);
      $request->setHttpCode(200);
      $request->setContentType('text/html; charset=UTF-8');
      if ($query['xoauth_signature_publickey'] && $query['oauth_signature']) {
        $request->setResponseContent('OK');
      } else {
        $request->setResponseContent('FAILED');
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
    $signingFetcherFactory = new MockSigningFetcherFactory();
    $this->basicRemoteContent = new BasicRemoteContent($this->fetcher, $signingFetcherFactory, new BasicSecurityTokenDecoder());
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
    $ret = $this->basicRemoteContent->fetch($request);
    $content = $ret->getResponseContent();
    $this->assertEquals("OK", trim($content));
  }

  /**
   * Tests BasicRemoteContent->fetch() 404 response
   */
  public function testFetch404() {
    $request = new RemoteContentRequest('http://test.chabotc.com/fail.html');
    $ret = $this->basicRemoteContent->fetch($request);
    $this->assertEquals('404', $ret->getHttpCode());
  }
  
  /**
   * Tests BasicRemoteContent->fetch() with different response
   */
  public function testFetchValid() {
    $this->fetcher->clean();
    $request = new RemoteContentRequest('http://test.chabotc.com/valid0.html');
    $this->basicRemoteContent->invalidate($request);
    $this->fetcher->expectFetchRequest($request);
    $ret = $this->basicRemoteContent->fetch($request);
    $this->assertTrue($this->fetcher->verify());
    $content = $ret->getResponseContent();
    $this->assertEquals("OK", trim($content));
    
    $request = new RemoteContentRequest('http://test.chabotc.com/valid0.html');
    $this->basicRemoteContent->invalidate($request);
    $this->fetcher->expectFetchRequest($request);
    $ret = $this->basicRemoteContent->fetch($request);
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
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/valid1.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/valid2.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/valid3.html');
    $this->basicRemoteContent->invalidate($requests[0]);
    $this->basicRemoteContent->invalidate($requests[1]);
    $this->basicRemoteContent->invalidate($requests[2]);
    $this->fetcher->expectMultiFetchRequest($requests);
    $rets = $this->basicRemoteContent->multiFetch($requests);
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
    $this->basicRemoteContent->invalidate($requests[0]);
    $this->basicRemoteContent->invalidate($requests[1]);
    $this->basicRemoteContent->invalidate($requests[2]);
    $this->fetcher->expectMultiFetchRequest($requests);
    $rets = $this->basicRemoteContent->multiFetch($requests);
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
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/ok.html');

    $rets = $this->basicRemoteContent->multiFetch($requests);
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
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/fail.html');

    $rets = $this->basicRemoteContent->multiFetch($requests);
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
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/fail.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/fail.html');
    $requests[] = new RemoteContentRequest('http://test.chabotc.com/fail.html');
    $rets = $this->basicRemoteContent->multiFetch($requests);
    $this->assertEquals('404', $rets[0]->getHttpCode());
    $this->assertEquals('404', $rets[1]->getHttpCode());
    $this->assertEquals('404', $rets[2]->getHttpCode());
  }

  /**
   * Tests BasicRemoteContent->invalidate()
   */
  public function testInvalidate() {
    // Fetches url for the first time.
    $request = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $ret = $this->basicRemoteContent->fetch($request);
    $this->fetcher->clean();
    $content = $ret->getResponseContent();
    $this->assertEquals("OK", trim($content));

    // Fetches url again and $this->fetcher->fetchRequest will not be called.
    $request = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $ret = $this->basicRemoteContent->fetch($request);
    $this->assertTrue($this->fetcher->verify());
    $content = $ret->getResponseContent();
    $this->assertEquals("OK", trim($content));

    // Invalidates cache and fetches url.
    // $this->fetcher->fetchRequest will be called.
    $request = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $this->fetcher->expectFetchRequest($request);
    $this->basicRemoteContent->invalidate($request);
    $ret = $this->basicRemoteContent->fetch($request);
    $this->assertTrue($this->fetcher->verify());
    $content = $ret->getResponseContent();
    $this->assertEquals("OK", trim($content));
  }
  
  /**
   * Tests through SigningFetcher
   */
  public function testSigningFetch() {
    $request1 = new RemoteContentRequest('http://test.chabotc.com/signing.html');
    $token = BasicSecurityToken::createFromValues('owner', 'viewer', 'app', 'domain', 'appUrl', '1', 'default');
    $request1->setToken($token);
    $request1->setAuthType(RemoteContentRequest::$AUTH_SIGNED);
    $request2 = new RemoteContentRequest('http://test.chabotc.com/ok.html');
    $this->basicRemoteContent->invalidate($request1);
    $this->basicRemoteContent->invalidate($request2);
    $requests = array($request1, $request2);
    $this->basicRemoteContent->multiFetch($requests);
    $content = $request1->getResponseContent();
    $this->assertEquals("OK", trim($content));
    $content = $request2->getResponseContent();
    $this->assertEquals("OK", trim($content));
  }
}
