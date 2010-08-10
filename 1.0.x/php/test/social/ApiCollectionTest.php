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
 * ApiCollection test case.
 */
class ApiCollectionTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var ApiCollection
   */
  private $ApiCollection;
  private $items;
  private $offset;
  private $totalSize;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->items = array('A', 'B', 'C');
    $this->offset = true;
    $this->totalSize = true;
    $this->ApiCollection = new ApiCollection($this->items, $this->offset, $this->totalSize);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->ApiCollection = null;
    $this->items = null;
    parent::tearDown();
  }

  /**
   * Tests ApiCollection->getItems()
   */
  public function testGetItems() {
    $this->assertEquals($this->items, $this->ApiCollection->getItems());
  }

  /**
   * Tests ApiCollection->getOffset()
   */
  public function testGetOffset() {
    $this->assertTrue($this->ApiCollection->getOffset());
  }

  /**
   * Tests ApiCollection->getTotalSize()
   */
  public function testGetTotalSize() {
    $this->assertTrue($this->ApiCollection->getTotalSize());
  }

  /**
   * Tests ApiCollection->setItems()
   */
  public function testSetItems() {
    $itemsToTestSetItems = array('a', 'b', 'c');
    $this->ApiCollection->setItems($itemsToTestSetItems);
    $this->assertEquals($itemsToTestSetItems, $this->ApiCollection->items);
  }

  /**
   * Tests ApiCollection->setOffset()
   */
  public function testSetOffset() {
    $offset = ! $this->ApiCollection->offset;
    $this->ApiCollection->setOffset($offset);
    $this->assertEquals($offset, $this->ApiCollection->offset);
  }

  /**
   * Tests ApiCollection->setTotalSize()
   */
  public function testSetTotalSize() {
    $totalSize = ! $this->ApiCollection->totalSize;
    $this->ApiCollection->setTotalSize($totalSize);
    $this->assertEquals($totalSize, $this->ApiCollection->totalSize);
  }
}
