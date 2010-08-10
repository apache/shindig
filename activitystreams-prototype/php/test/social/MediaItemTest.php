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
 * MediaItem test case.
 */
class MediaItemTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var MediaItem
   */
  private $MediaItem;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->MediaItem = new MediaItem('MIMETYPE', 'AUDIO', 'URL');
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->MediaItem = null;
    parent::tearDown();
  }

  /**
   * Tests MediaItem->getMimeType()
   */
  public function testGetMimeType() {
    $this->assertEquals('MIMETYPE', $this->MediaItem->getMimeType());
  }

  /**
   * Tests MediaItem->getType()
   */
  public function testGetType() {
    $this->assertEquals('AUDIO', $this->MediaItem->getType());
  }

  /**
   * Tests MediaItem->getUrl()
   */
  public function testGetUrl() {
    $this->assertEquals('URL', $this->MediaItem->getUrl());
  }

  /**
   * Tests MediaItem->setMimeType()
   */
  public function testSetMimeType() {
    $this->MediaItem->setMimeType('mimetype');
    $this->assertEquals('mimetype', $this->MediaItem->mimeType);
  }

  /**
   * Tests MediaItem->setType()
   */
  public function testSetType() {
    $this->MediaItem->setType('VIDEO');
    $this->assertEquals('VIDEO', $this->MediaItem->type);
  }

  /**
   * Tests MediaItem->setUrl()
   */
  public function testSetUrl() {
    $this->MediaItem->setUrl('url');
    $this->assertEquals('url', $this->MediaItem->url);
  }
}
