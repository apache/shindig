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
 * RestFulCollection test case.
 */
class RestFulCollectionTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var RestFulCollection
   */
  private $RestFulCollection;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $entry = array('Entry');
    $this->RestFulCollection = new RestfulCollection($entry, 1, 1);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->RestFulCollection = null;
    parent::tearDown();
  }

  /**
   * Constructs the test case.
   */
  public function __construct() {}

  /**
   * Tests RestFulCollection->__construct()
   */
  public function test__construct() {
    $entry = array('Entry');
    $this->RestFulCollection->__construct($entry, 1, 1);
  }

  /**
   * Tests RestFulCollection->getEntry()
   */
  public function testGetEntry() {
    $entry = array('Entry');
    $this->RestFulCollection->setEntry($entry);
    $this->assertEquals($entry, $this->RestFulCollection->getEntry());
  }

  /**
   * Tests RestFulCollection->getStartIndex()
   */
  public function testGetStartIndex() {
    $this->RestFulCollection->setStartIndex(1);
    $this->assertEquals(1, $this->RestFulCollection->getStartIndex());
  }

  /**
   * Tests RestFulCollection->getTotalResults()
   */
  public function testGetTotalResults() {
    $this->RestFulCollection->setTotalResults(1);
    $this->assertEquals(1, $this->RestFulCollection->getTotalResults());
  }

  /**
   * Tests RestFulCollection->createFromEntry()
   */
  public function testCreateFromEntry() {
    $entry = array('Entry');
    $restFulCollection = RestFulCollection::createFromEntry($entry);
    $this->assertEquals(1, $restFulCollection->getTotalResults());
    $this->assertEquals($entry, $restFulCollection->getEntry());
    $this->assertEquals(0, $restFulCollection->getStartIndex());
  }
}
