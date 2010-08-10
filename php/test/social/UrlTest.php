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
 * Url test case.
 */
class UrlTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var Url
   */
  private $Url;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->Url = new Url('A', 'T', 'L');
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->Url = null;
    parent::tearDown();
  }

  /**
   * Tests Url->getAddress()
   */
  public function testGetAddress() {
    $this->assertEquals('A', $this->Url->getValue());
  }

  /**
   * Tests Url->getLinkText()
   */
  public function testGetLinkText() {
    $this->assertEquals('L', $this->Url->getLinkText());
  }

  /**
   * Tests Url->getType()
   */
  public function testGetType() {
    $this->assertEquals('T', $this->Url->getType());
  }

  /**
   * Tests Url->setAddress()
   */
  public function testSetAddress() {
    $this->Url->setValue('a');
    $this->assertEquals('a', $this->Url->getValue());
  }

  /**
   * Tests Url->setLinkText()
   */
  public function testSetLinkText() {
    $this->Url->setLinkText('l');
    $this->assertEquals('l', $this->Url->getLinkText());
  }

  /**
   * Tests Url->setType()
   */
  public function testSetType() {
    $this->Url->setType('t');
    $this->assertEquals('t', $this->Url->type);
  }
}