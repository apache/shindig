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
 * Provides an implementation of RemoteContentFetcher which can be controlled
 * by unit tests and does not actually make any external requests.
 * RemoteContentRequest objects added to this class are added to an internal
 * FIFO queue.  Every time a request is sent to fetchRequest, the next
 * RemoteContentRequest object in the queue is returned as a response.
 * Requests sent to fetchRequest are also stored in a queue for later retrieval
 * and examination.
 */
class MockMakeRequestFetcher extends RemoteContentFetcher {
  private $responses;
  private $requests;

  /**
   * Constructor.
   */
  public function __construct() {
    $this->responses = array();
    $this->requests = array();
  }

  /**
   * Adds a response object to an internal queue of responses.
   * @param RemoteContentRequest $response The response to return.
   */
  public function enqueueResponse(RemoteContentRequest $response) {
    $this->responses[] = $response;
  }

  /**
   * Returns a request object that was sent to fetchRequest.  If multiple
   * requests have been sent to fetchRequest, they are returned by this
   * method in the order they were requested.
   * @return RemoteContentRequest
   */
  public function dequeueRequest() {
    return array_shift($this->requests);
  }

  /**
   * Fakes a content request to a remote server.
   * @param RemoteContentRequest $request  The external request information.
   * @return RemoteContentRequest The next response which was enqueued by
   *     calling enqueueResponse.
   */
  public function fetchRequest(RemoteContentRequest $request) {
    $this->requests[] = $request;
    return array_shift($this->responses);
  }

  /**
   * Fakes multiple requests to a remote server.  Calls fetchRequest for
   * each request passed to this method.
   * @param array $requests An array of RemoteContentRequests.
   * @return An array of RemoteContentRequests corresponging to the responses
   *    returned for each of the request inputs.
   */
  public function multiFetchRequest(Array $requests) {
    $responses = array();
    foreach ($requests as $key => $request) {
      $responses[$key] = $this->fetchRequest($request);
    }
    return $responses;
  }
}

/**
 * Unit tests for the MakeRequest class.
 */
class MakeRequestTest extends PHPUnit_Framework_TestCase {
  private $fetcher;
  private $makeRequest;
  private $context;
  private $response;
  
  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->fetcher = new MockMakeRequestFetcher();
    $this->makeRequest = new MakeRequest($this->fetcher);
    $this->context = new GadgetContext('GADGET');

    $this->response = new RemoteContentRequest('http://www.example.com');
    $this->response->setHttpCode(200);
    $this->response->setResponseContent("Basic response");
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    parent::tearDown();
  }

  /**
   * Executes a makeRequest call and returns the request object which would
   * have been sent externally (as opposed to the response).
   *
   * @param MakeRequestOptions $params
   * @param RemoteContentRequest $response The response to return for this
   *     request.
   * @return RemoteContentRequest The request object.
   */
  protected function catchRequest(MakeRequestOptions $params, RemoteContentRequest $response) {
    $this->fetcher->enqueueResponse($response);
    $result = $this->makeRequest->fetch($this->context, $params);
    return $this->fetcher->dequeueRequest();
  }

  /**
   * Tests that makeRequest calls with an invalid url throw an exception.
   */
  public function testInvalidUrl() {
    try {
      $params = new MakeRequestOptions('invalidurl');
      $this->makeRequest->fetch($params);
      $this->fail("Calling makeRequest with an invalid url should throw an exception.");
    } catch (Exception $ex) { }
  }

  /**
   * Tests that normal requests specify a GET to the supplied URL.
   */
  public function testBasicRequest() {
    $params = new MakeRequestOptions('http://www.example.com');
    $params->setNoCache(true);

    $request = $this->catchRequest($params, $this->response);
    $this->assertContains($request->getUrl(), 'http://www.example.com');
    $this->assertEquals('GET', $request->getMethod());
  }

  /**
   * Tests that signed requests generate appropriate oauth_ and opensocial_
   * parameters.
   */
  public function testSignedRequest() {
    $token = BasicSecurityToken::createFromValues('owner', 'viewer', 'app', 'domain', 'appUrl', '1', 'default');
    $params = new MakeRequestOptions('http://www.example.com');
    $params->setAuthz('SIGNED')
           ->setNoCache(true)
           ->setSecurityTokenString(urlencode(base64_encode($token->toSerialForm())));

    $request = $this->catchRequest($params, $this->response);

    $this->assertContains('oauth_signature', $request->getUrl());
    $this->assertContains('oauth_signature_method=RSA-SHA1', $request->getUrl());
    $this->assertContains('opensocial_app_url=appUrl', $request->getUrl());
    $this->assertContains('opensocial_viewer_id=viewer', $request->getUrl());
    $this->assertContains('opensocial_owner_id=owner', $request->getUrl());
    $this->assertEquals('GET', $request->getMethod());
  }

  /**
   * Tests that setting "sign_viewer" = false does not include viewer
   * information in the request.
   */
  public function testSignedNoViewerRequest() {
    $token = BasicSecurityToken::createFromValues('owner', 'viewer', 'app', 'domain', 'appUrl', '1', 'default');
    $params = new MakeRequestOptions('http://www.example.com');
    $params->setAuthz('SIGNED')
           ->setNoCache(true)
           ->setSignViewer(false)
           ->setSecurityTokenString(urlencode(base64_encode($token->toSerialForm())));

    $request = $this->catchRequest($params, $this->response);

    $this->assertContains('oauth_signature', $request->getUrl());
    $this->assertNotContains('opensocial_viewer_id=viewer', $request->getUrl());
    $this->assertContains('opensocial_owner_id=owner', $request->getUrl());
  }

  /**
   * Tests that setting "format" = "FEED" parses an atom feed into a JSON
   * structure.
   */
  public function testFeedRequest() {
    $params = new MakeRequestOptions('http://www.example.com');
    $params->setResponseFormat('FEED')
           ->setNoCache(true)
           ->setNumEntries(2);

    $sampleAtomPath = realpath(dirname(__FILE__) . "/../misc/sampleAtomFeed.xml");
    $sampleAtom = file_get_contents($sampleAtomPath);
    $this->response->setResponseContent($sampleAtom);
    $this->fetcher->enqueueResponse($this->response);
    $result = $this->makeRequest->fetch($this->context, $params);
    $feedJson = json_decode($result->getResponseContent(), true);

    $this->assertArrayHasKey('Entry', $feedJson);
    $this->assertEquals(2, count($feedJson['Entry']));
    $this->assertArrayHasKey('Title', $feedJson['Entry'][0]);
    $this->assertEquals("Atom-Powered Robots Run Amok", $feedJson['Entry'][0]['Title']);
  }

  /**
   * Tests that setting request headers are passed in the outgoing request.
   */
  public function testRequestHeaders(){
    $params = new MakeRequestOptions('http://www.example.com');
    $params->setRequestHeaders(array(
      "Content-Type" => "application/json",
      "Accept-Language" => "en-us"
    ));
    $params->setNoCache(true);

    $request = $this->catchRequest($params, $this->response);
    $this->assertTrue($request->hasHeaders());
    $this->assertEquals('application/json', $request->getHeader('Content-Type'));
    $this->assertEquals('en-us', $request->getHeader('Accept-Language'));
  }

  /**
   * Tests that setting invalid request headers are not passed in the outgoing
   * request.
   */
  public function testInvalidRequestHeaders(){
    $params = new MakeRequestOptions('http://www.example.com');
    $params->setRequestHeaders(array(
      "Content-Type" => "application/json",
      "Accept-Language" => "en-us",
      "Host" => "http://www.evil.com",
      "host" => "http://www.evil.com",
      "HOST" => "http://www.evil.com",
      "Accept" => "blah",
      "Accept-Encoding" => "blah"
    ));
    $params->setNoCache(true);

    $request = $this->catchRequest($params, $this->response);
    $this->assertTrue($request->hasHeaders());
    $this->assertEquals('application/json', $request->getHeader('Content-Type'));
    $this->assertEquals('en-us', $request->getHeader('Accept-Language'));

    $this->assertNull($request->getHeader('Host'));
    $this->assertNull($request->getHeader('Accept'));
    $this->assertNull($request->getHeader('Accept-Encoding'));
  }

  /**
   * Tests that setting request headers in a form urlencoded way are passed in the outgoing request.
   */
  public function testFormEncodedRequestHeaders(){
    $params = new MakeRequestOptions('http://www.example.com');
    $params->setFormEncodedRequestHeaders("Content-Type=application%2Fx-www-form-urlencoded&Accept-Language=en-us");
    $params->setNoCache(true);

    $request = $this->catchRequest($params, $this->response);
    $this->assertTrue($request->hasHeaders());
    $this->assertEquals('application/x-www-form-urlencoded', $request->getHeader('Content-Type'));
  }

  public function testResponseHeaders() {
    $params = new MakeRequestOptions('http://www.example.com');
    $params->setNoCache(true);

    $headers = array(
      'Content-Type' => 'text/plain'
    );
    $this->response->setResponseHeaders($headers);
    $this->fetcher->enqueueResponse($this->response);

    $result = $this->makeRequest->fetch($this->context, $params);
    $response_headers = $result->getResponseHeaders();

    $this->assertArrayHasKey('Content-Type', $response_headers);
    $this->assertEquals('text/plain', $response_headers['Content-Type']);
  }

  public function testCleanResponseHeaders() {
    $response_headers = array(
      'Content-Type' => 'text/plain',
      'Set-Cookie' => 'blah',
      'set-cookie' => 'blah',
      'SET-COOKIE' => 'blah',
      'sEt-cOoKiE' => 'blah',
      'Accept-Ranges' => 'blah',
      'Vary' => 'blah',
      'Expires' => 'blah',
      'Date' => 'blah',
      'Pragma' => 'blah',
      'Cache-Control' => 'blah',
      'Transfer-Encoding' => 'blah',
      'WWW-Authenticate' => 'blah'
    );
    
    $cleaned_headers = $this->makeRequest->cleanResponseHeaders($response_headers);

    $this->assertArrayHasKey('Content-Type', $cleaned_headers);
    $this->assertEquals('text/plain', $cleaned_headers['Content-Type']);
    $this->assertArrayNotHasKey('Set-Cookie', $cleaned_headers);
    $this->assertArrayNotHasKey('set-cookie', $cleaned_headers);
    $this->assertArrayNotHasKey('SET-COOKIE', $cleaned_headers);
    $this->assertArrayNotHasKey('sEt-cOoKiE', $cleaned_headers);
    $this->assertArrayNotHasKey('Accept-Ranges', $cleaned_headers);
    $this->assertArrayNotHasKey('Vary', $cleaned_headers);
    $this->assertArrayNotHasKey('Expires', $cleaned_headers);
    $this->assertArrayNotHasKey('Date', $cleaned_headers);
    $this->assertArrayNotHasKey('Pragma', $cleaned_headers);
    $this->assertArrayNotHasKey('Cache-Control', $cleaned_headers);
    $this->assertArrayNotHasKey('Transfer-Encoding', $cleaned_headers);
    $this->assertArrayNotHasKey('WWW-Authenticate', $cleaned_headers);
  }
}

