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

require_once 'src/common/sample/BasicSecurityToken.php';

require_once 'external/PHPUnit/Framework/TestCase.php';

/**
 * BasicSecurityToken test case.
 */
class BasicSecurityTokenTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var BasicSecurityToken
   */
  private $BasicSecurityToken;
  
  /**
   * @var BasicSecurityToken
   */
  private $anonymousToken;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->BasicSecurityToken = BasicSecurityToken::createFromValues('owner', 'viewer', 'app', 'domain', 'appUrl', '1', 'default');
    $this->anonymousToken = BasicSecurityToken::createFromValues(SecurityToken::$ANONYMOUS, SecurityToken::$ANONYMOUS, 'app', 'domain', 'appUrl', '1', 'default');
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->BasicSecurityToken = null;
    $this->anonymousToken = null;
    parent::tearDown();
  }

  /**
   * Tests BasicSecurityToken::createFromValues(), toSerialForm() and createFromToken() 
   */
  public function testCreateFromValues() {
    $token = BasicSecurityToken::createFromValues('owner', 'viewer', 'app', 'domain', 'appUrl', '1', 'default');
    $this->assertEquals('owner', $token->getOwnerId());
    $this->assertEquals('viewer', $token->getViewerId());
    $this->assertEquals('app', $token->getAppId());
    $this->assertEquals('domain', $token->getDomain());
    $this->assertEquals('appUrl', $token->getAppUrl());
    $this->assertEquals('1', $token->getModuleId());
    
    $stringToken = urldecode($token->toSerialForm());
    $duplicatedToken = BasicSecurityToken::createFromToken($stringToken, Config::get('token_max_age'));
    $this->assertEquals('owner', $duplicatedToken->getOwnerId());
    $this->assertEquals('viewer', $duplicatedToken->getViewerId());
    $this->assertEquals('app', $duplicatedToken->getAppId());
    $this->assertEquals('domain', $duplicatedToken->getDomain());
    $this->assertEquals('appUrl', $duplicatedToken->getAppUrl());
    $this->assertEquals('1', $duplicatedToken->getModuleId());
  }

  /**
   * Tests BasicSecurityToken->getAppId()
   */
  public function testGetAppId() {
    $this->assertEquals('app', $this->BasicSecurityToken->getAppId());
    $this->setExpectedException('Exception');
    $this->anonymousToken->getAppId();
  }

  /**
   * Tests BasicSecurityToken->getAppUrl()
   */
  public function testGetAppUrl() {
    $this->assertEquals('appUrl', $this->BasicSecurityToken->getAppUrl());
    $this->setExpectedException('Exception');
    $this->anonymousToken->getAppUrl();
  }

  /**
   * Tests BasicSecurityToken->getDomain()
   */
  public function testGetDomain() {
    $this->assertEquals('domain', $this->BasicSecurityToken->getDomain());
    $this->setExpectedException('Exception');
    $this->anonymousToken->getDomain();
  }

  /**
   * Tests BasicSecurityToken->getModuleId()
   */
  public function testGetModuleId() {
    $this->assertEquals(1, $this->BasicSecurityToken->getModuleId());
    $this->setExpectedException('Exception');
    $this->anonymousToken->getModuleId();
  }

  /**
   * Tests BasicSecurityToken->getOwnerId()
   */
  public function testGetOwnerId() {
    $this->assertEquals('owner', $this->BasicSecurityToken->getOwnerId());
    $this->setExpectedException('Exception');
    $this->anonymousToken->getOwnerId();
  }

  /**
   * Tests BasicSecurityToken->getViewerId()
   */
  public function testGetViewerId() {
    $this->assertEquals('viewer', $this->BasicSecurityToken->getViewerId());
    $this->setExpectedException('Exception');
    $this->anonymousToken->getViewerId();
  }

  /**
   * Tests BasicSecurityToken->isAnonymous()
   */
  public function testIsAnonymous() {
    $this->assertFalse($this->BasicSecurityToken->isAnonymous());
  }
}