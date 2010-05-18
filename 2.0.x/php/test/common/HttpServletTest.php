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
 * HttpServlet test case.
 */
class HttpServletTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var HttpServlet
   */
  private $HttpServlet;
  
  private $cacheTime = 60;
  private $contentType1 = "text/html";
  private $contentType2 = "text/javascript";
  private $lastModified = 500;
  private $noCache = false;
  public $contentType = 'utf-8';

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->HttpServlet = new HttpServlet(/* parameters */);
    
    $this->HttpServlet->setLastModified($this->lastModified);
    $this->HttpServlet->setNoCache($this->noCache);
    $this->HttpServlet->setContentType($this->contentType);
    $this->HttpServlet->setCacheTime($this->cacheTime);
  }

  /**
   * Cleans up the environment after running a test.
   */
  
  protected function tearDown() {
    $this->HttpServlet = null;
    parent::tearDown();
  }

  /**
   * Tests HttpServlet->getCacheTime()
   */
  public function testGetCacheTime() {
    $this->assertEquals($this->cacheTime, $this->HttpServlet->getCacheTime());
  }

  /**
   * Tests HttpServlet->getContentType()
   */
  public function testGetContentType() {
    $this->assertEquals($this->contentType, $this->HttpServlet->getContentType());
  }

  /**
   * Tests HttpServlet->getLastModified()
   */
  public function testGetLastModified() {
    $this->assertEquals($this->lastModified, $this->HttpServlet->getLastModified());
  }

  /**
   * Tests HttpServlet->getNoCache()
   */
  public function testGetNoCache() {
    $this->assertEquals($this->noCache, $this->HttpServlet->getNoCache());
  }

  /**
   * Tests HttpServlet->setCacheTime()
   */
  public function testSetCacheTime() {
    $this->HttpServlet->setCacheTime($this->cacheTime + 100);
    $this->assertEquals($this->cacheTime + 100, $this->HttpServlet->getCacheTime());
  }

  /**
   * Tests HttpServlet->setContentType()
   */
  public function testSetContentType() {
    $this->HttpServlet->setContentType($this->contentType2);
    $this->assertNotEquals($this->contentType1, $this->HttpServlet->getContentType());
  }

  /**
   * Tests HttpServlet->setLastModified()
   */
  public function testSetLastModified() {
    $this->HttpServlet->setLastModified($this->lastModified + 100);
    $this->assertEquals($this->lastModified + 100, $this->HttpServlet->getLastModified());
  }

  /**
   * Tests HttpServlet->setNoCache()
   */
  public function testSetNoCache() {
    $this->HttpServlet->setNoCache(! $this->noCache);
    $this->assertNotEquals($this->noCache, $this->HttpServlet->getNoCache());
  }

}