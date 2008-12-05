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
 * Message test case.
 */
class MessageTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var Message
   */
  private $Message;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->Message = new Message('BODY', 'TITLE', 'NOTIFICATION');
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->Message = null;
    parent::tearDown();
  }

  /**
   * Tests Message->getBody()
   */
  public function testGetBody() {
    $this->assertEquals('BODY', $this->Message->getBody());
  }

  /**
   * Tests Message->getTitle()
   */
  public function testGetTitle() {
    $this->assertEquals('TITLE', $this->Message->getTitle());
  }

  /**
   * Tests Message->getType()
   */
  public function testGetType() {
    $this->assertEquals('NOTIFICATION', $this->Message->getType());
  }

  /**
   * Tests Message->sanitizeHTML()
   */
  public function testSanitizeHTML() {
    $this->assertEquals('ABC', $this->Message->sanitizeHTML('ABC'));
  }

  /**
   * Tests Message->setBody()
   */
  public function testSetBody() {
    $this->Message->setBody('body');
    $this->assertEquals('body', $this->Message->body);
  }

  /**
   * Tests Message->setTitle()
   */
  public function testSetTitle() {
    $this->Message->setTitle('title');
    $this->assertEquals('title', $this->Message->title);
  }

  /**
   * Tests Message->setType()
   */
  public function testSetType() {
    $this->Message->setType('EMAIL');
    $this->assertEquals('EMAIL', $this->Message->type);
  }
}
