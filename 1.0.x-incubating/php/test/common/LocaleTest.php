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
 * Locale test case.
 */
class LocaleTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var Locale
   */
  private $Locale;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->Locale = new Locale('EN', 'US');
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->Locale = null;
    parent::tearDown();
  }

  /**
   * Constructs the test case.
   */
  public function __construct() {}

  /**
   * Tests Locale->__construct()
   */
  public function test__construct() {
    $this->Locale->__construct('EN', 'US');
  }

  /**
   * Tests Locale->equals()
   */
  public function testEquals() {
    $locale = new Locale('EN', 'US');
    $this->assertTrue($this->Locale->equals($locale));
  }

  /**
   * Tests Locale->getCountry()
   */
  public function testGetCountry() {
    $this->assertEquals('US', $this->Locale->getCountry());
  }

  /**
   * Tests Locale->getLanguage()
   */
  public function testGetLanguage() {
    $this->assertEquals('EN', $this->Locale->getLanguage());
  }
}
