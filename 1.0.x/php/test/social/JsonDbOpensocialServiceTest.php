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
 * JsonDbOpensocialService test case.
 */
class JsonDbOpensocialServiceTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var JsonDbOpensocialService
   */
  private $service;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->service = new JsonDbOpensocialService();
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->service = null;
    parent::tearDown();
  }

  /**
   * Constructs the test case.
   */
  public function __construct() {}

  /**
   * Tests JsonDbOpensocialService->getActivities() with paging.
   */
  public function testGetActivities() {
  	$token = BasicSecurityToken::createFromValues('jane.doe', 'jane.doe', 1, 1, 1, 1);
  	$userId = new UserId('owner', null);
  	$userIds = array($userId);
  	$groupId = new GroupId('self', null);
  	$startIndex = 1;
  	$count = 1;
  	
    $ret = $this->service->getActivities($userIds, $groupId, 1, null, null, null, null, $startIndex, $count, null, 1, $token);
    $this->assertEquals($startIndex, $ret->startIndex);
    $this->assertEquals($count, count($ret->entry));
    $this->assertEquals(2, $ret->totalResults);
    $this->assertEquals('2', $ret->entry[0]['id']);
    $this->assertEquals('Jane says George likes yoda!', $ret->entry[0]['title']);
    $this->assertEquals('or is it you?', $ret->entry[0]['body']);
  }

}
