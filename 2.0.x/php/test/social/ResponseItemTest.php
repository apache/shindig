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
 * ResponseItem test case.
 */
class ResponseItemTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var ResponseItem
   */
  private $ResponseItem;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->ResponseItem = new ResponseItem('error', 'errorMessage', array('foo' => null, 'bar' => 1));
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->ResponseItem = null;
    parent::tearDown();
  }

  /**
   * Tests ResponseItem->__construct()
   */
  public function test__construct() {
    $this->ResponseItem->__construct('error', 'errorMessage', array('foo' => null, 'bar' => 1));
  }

  /**
   * Tests ResponseItem->getError()
   */
  public function testGetError() {
    $this->assertEquals('error', $this->ResponseItem->getError());
  }

  /**
   * Tests ResponseItem->getErrorMessage()
   */
  public function testGetErrorMessage() {
    $this->assertEquals('errorMessage', $this->ResponseItem->getErrorMessage());
  
  }

  /**
   * Tests ResponseItem->getResponse()
   */
  public function testGetResponse() {
    $expected = array('bar' => 1);
    $this->assertEquals($expected, $this->ResponseItem->getResponse());
  }

  /**
   * Tests ResponseItem->setError()
   */
  public function testSetError() {
    $this->ResponseItem->setError('seterror');
    $this->assertEquals('seterror', $this->ResponseItem->getError());
  }

  /**
   * Tests ResponseItem->setErrorMessage()
   */
  public function testSetErrorMessage() {
    $this->ResponseItem->setErrorMessage('seterrormessage');
    $this->assertEquals('seterrormessage', $this->ResponseItem->getErrorMessage());
  }

  /**
   * Tests ResponseItem->setResponse()
   */
  public function testSetResponse() {
    $expected = array('bar' => 2);
    $this->ResponseItem->setResponse($expected);
    $this->assertEquals($expected, $this->ResponseItem->getResponse());
  }
}
