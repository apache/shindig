<?php
namespace apache\shindig\test\social;
use apache\shindig\social\spi\RestfulCollection;

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
 * RestfulCollection test case.
 */
class RestfulCollectionTest extends \PHPUnit_Framework_TestCase {

  /**
   * @var RestfulCollection
   */
  private $RestfulCollection;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $entry = array('Entry');
    $this->restfulCollection = new RestfulCollection($entry, 1, 1);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->restfulCollection = null;
    parent::tearDown();
  }

  /**
   * Constructs the test case.
   */
  public function __construct() {}

  /**
   * Tests RestfulCollection->__construct()
   */
  public function test__construct() {
    $entry = array('Entry');
    $this->restfulCollection->__construct($entry, 1, 1);
  }

  /**
   * Tests RestfulCollection->getEntry()
   */
  public function testGetEntry() {
    $entry = array('Entry');
    $this->restfulCollection->setEntry($entry);
    $this->assertEquals($entry, $this->restfulCollection->getEntry());
  }

  /**
   * Tests RestfulCollection->getStartIndex()
   */
  public function testGetStartIndex() {
    $this->restfulCollection->setStartIndex(1);
    $this->assertEquals(1, $this->restfulCollection->getStartIndex());
  }

  /**
   * Tests RestfulCollection->getTotalResults()
   */
  public function testGetTotalResults() {
    $this->restfulCollection->setTotalResults(1);
    $this->assertEquals(1, $this->restfulCollection->getTotalResults());
  }

  /**
   * Tests RestfulCollection->createFromEntry()
   */
  public function testCreateFromEntry() {
    $entry = array('Entry');
    $restfulCollection = RestfulCollection::createFromEntry($entry);
    $this->assertEquals(1, $restfulCollection->getTotalResults());
    $this->assertEquals($entry, $restfulCollection->getEntry());
    $this->assertEquals(0, $restfulCollection->getStartIndex());
  }
}
