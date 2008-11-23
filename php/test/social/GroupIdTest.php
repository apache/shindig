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
 * GroupId test case.
 */
class GroupIdTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var GroupId
   */
  private $GroupId;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->GroupId = new GroupId('all', 1);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->GroupId = null;
    parent::tearDown();
  }

  /**
   * Constructs the test case.
   */
  public function __construct() {}

  /**
   * Tests GroupId->__construct()
   */
  public function test__construct() {
    $this->GroupId->__construct('all', 1);
  }

  /**
   * Tests GroupId->getGroupId()
   */
  public function testGetGroupId() {
    $this->assertEquals(1, $this->GroupId->getGroupId());
  }

  /**
   * Tests GroupId->getType()
   */
  public function testGetType() {
    $this->assertEquals('all', $this->GroupId->getType());
  }

  /**
   * Tests GroupId->fromJson()
   */
  public function testFromJson() {
    $json = 'jsonid';
    $fromJson = $this->GroupId->fromJson($json);
    $this->assertEquals('groupId', $fromJson->getType());
    $this->assertEquals('jsonid', $fromJson->getGroupId());
  }

}
