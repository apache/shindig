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
 * IdSpec test case.
 */
class IdSpecTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var IdSpec
   */
  private $IdSpec;
  private $jsonspec;
  private $type;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->jsonspec = array(1, 2, 3, 4, 5, 6, 7, 8, 9);
    $this->type = 'VIEWER';
    $this->IdSpec = new IdSpec($this->jsonspec, $this->type);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->IdSpec = null;
    $this->jsonspec = null;
    $this->type = null;
    parent::tearDown();
  }

  /**
   * Tests IdSpec->fetchUserIds()
   */
  public function testFetchUserIds() {
    $this->assertEquals($this->jsonspec, $this->IdSpec->fetchUserIds());
  }

  /**
   * Tests IdSpec::fromJson()
   */
  public function testFromJson() {
    $result = IdSpec::fromJson('OWNER');
    $this->assertTrue($result instanceof IdSpec);
  }

  /**
   * Tests IdSpec->getType()
   */
  public function testGetType() {
    $this->assertEquals('VIEWER', $this->IdSpec->getType());
  }
}
