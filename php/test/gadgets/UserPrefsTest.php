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
 * UserPrefs test case.
 */
class UserPrefsTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var UserPrefs
   */
  private $UserPrefs;
  
  /**
   * @var UserPrefsArrays
   */
  private $UserPrefsArrays = array('Test1' => 'value for test1', 'Test2' => 'value for test2');

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->UserPrefs = new UserPrefs($this->UserPrefsArrays);
  
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->UserPrefs = null;
    
    parent::tearDown();
  }

  /**
   * Tests UserPrefs->getPref()
   */
  public function testGetPref() {
    $this->assertEquals($this->UserPrefsArrays['Test1'], $this->UserPrefs->getPref('Test1'));
  
  }

  /**
   * Tests UserPrefs->getPrefs()
   */
  public function testGetPrefs() {
    $this->assertEquals($this->UserPrefsArrays, $this->UserPrefs->getPrefs());
  
  }

  /**
   * Tests UserPrefs->getPrefs()
   */
  public function testGetPrefsReturn() {
    $key = 'Test1';
    $this->assertEquals($this->UserPrefsArrays[$key], $this->UserPrefs->getPref($key));
  
  }

  /**
   * Tests UserPrefs->getPrefs()
   */
  public function testGetPrefsReturnNull() {
    $key = 'non_existing_key';
    $this->assertNull($this->UserPrefs->getPref($key));
  
  }
}

