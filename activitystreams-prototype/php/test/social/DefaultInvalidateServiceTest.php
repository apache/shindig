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
 * DefaultInvalidateService test case.
 */
class DefaultInvalidateServiceTest extends PHPUnit_Framework_TestCase {

  /**
   * @var DefaultInvalidateService
   */
  private $service;
  
  /**
   * @var Cache
   */
  private $cache;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->cache = Cache::createCache('CacheStorageFile', 'TestCache');
    $this->service = new DefaultInvalidateService($this->cache);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->service = null;
    $this->cache = null;
    parent::tearDown();
  }

  public function testInvalidateApplicationResources() {
    $token = BasicSecurityToken::createFromValues('owner', 'viewer', 'app', 'domain', 'appUrl', '1', 'default');
    $request1 = new RemoteContentRequest('http://url1');
    $request1->setToken($token);
    $request2 = new RemoteContentRequest('http://url2');
    $request2->setToken($token);
    $this->service->markResponse($request1);
    $this->service->markResponse($request2);
    $this->cache->set($request1->toHash(), $request1);
    $this->cache->set($request2->toHash(), $request2);
    $this->assertTrue($this->service->isValid($request1));
    $this->assertTrue($this->service->isValid($request2));
    $this->assertEquals($request1, $this->cache->get($request1->toHash()));
    $this->assertEquals($request2, $this->cache->get($request2->toHash()));
    $resource = array('http://url1', 'http://url2');
    $this->service->invalidateApplicationResources($resource, $token);
    $this->assertFalse($this->cache->get($request1->toHash()));
    $this->assertFalse($this->cache->get($request2->toHash()));
  }
  
  public function testInvalidateUserResources() {
    $token = BasicSecurityToken::createFromValues('owner', 'viewer', 'app', 'domain', 'appUrl', '1', 'default');
    $token->setAuthenticationMode(AuthenticationMode::$OAUTH_CONSUMER_REQUEST);
    $request = new RemoteContentRequest('http://url');
    $request->setToken($token);
    $request->setAuthType(RemoteContentRequest::$AUTH_SIGNED);
    $this->service->markResponse($request);
    $opensocialIds = array('owner');
    $this->service->invalidateUserResources($opensocialIds, $token);
    $this->assertFalse($this->service->isValid($request));
    $this->service->markResponse($request);
    $this->assertTrue($this->service->isValid($request));
  }

  public function testInvalidateUserResourcesWithEmptyAppId() {
    $token = BasicSecurityToken::createFromValues('owner', 'viewer', null, 'domain', 'appUrl', '1', 'default');
    $token->setAuthenticationMode(AuthenticationMode::$OAUTH_CONSUMER_REQUEST);
    $request = new RemoteContentRequest('http://url');
    $request->setToken($token);
    $request->setAuthType(RemoteContentRequest::$AUTH_SIGNED);
    $this->service->markResponse($request);
    $opensocialIds = array('owner');
    $this->service->invalidateUserResources($opensocialIds, $token);
    $this->assertFalse($this->service->isValid($request));
    $this->service->markResponse($request);
    $this->assertTrue($this->service->isValid($request));
  }
}
