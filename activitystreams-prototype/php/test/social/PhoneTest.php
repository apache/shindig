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
 * Phone test case.
 */
class PhoneTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var Phone
   */
  private $Phone;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->Phone = new Phone('number', 'type');
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->Phone = null;
    parent::tearDown();
  }

  /**
   * Tests Phone->getNumber()
   */
  public function testGetNumber() {
    $this->assertEquals('number', $this->Phone->getValue());
  }

  /**
   * Tests Phone->getType()
   */
  public function testGetType() {
    $this->assertEquals('type', $this->Phone->getType());
  }

  /**
   * Tests Phone->setNumber()
   */
  public function testSetNumber() {
    $this->Phone->setValue('NUMBER');
    $this->assertEquals('NUMBER', $this->Phone->getValue());
  }

  /**
   * Tests Phone->setType()
   */
  public function testSetType() {
    $this->Phone->setType('TYPE');
    $this->assertEquals('TYPE', $this->Phone->type);
  }
}
