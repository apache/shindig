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
 * CacheFile test case.
 */
class CacheFileTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var CacheFile
   */
  private $CacheFile;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->CacheFile = new CacheFile(/* parameters */);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->CacheFile = null;
    parent::tearDown();
  }

  /**
   * Tests CacheFile->delete()
   */
  public function testDelete() {
    @rmdir("/tmp/shindig/te");
    $cache = new CacheFile();
    $cache->set("test", "testing");
    $cache->delete("test");
    $this->assertEquals("", $cache->get("test"));
    $this->assertTrue(rmdir("/tmp/shindig/te"));
  }

  /**
   * Tests CacheFile->delete()
   */
  public function testDeleteException() {
    $cache = new CacheFile();
    $this->setExpectedException("CacheException");
    $cache->delete("test");
  }

  /**
   * Tests CacheFile->get()
   */
  public function testGet() {
    $this->CacheFile->set("test", "testing");
    $this->assertEquals("testing", $this->CacheFile->get("test"));
  }

  /**
   * Tests CacheFile->get()
   */
  public function testExpiredGet() {
    $this->CacheFile->set("test", "testing");
    @sleep(1);
    $this->assertFalse($this->CacheFile->get("test", 1));
  }

  /**
   * Tests CacheFile->set()
   */
  public function testSet() {
    @unlink("/tmp/shindig/te/test");
    @rmdir("/tmp/shindig/te");
    $this->CacheFile->set("test", "testing");
    $this->assertEquals("testing", $this->CacheFile->get("test"));
    @unlink("/tmp/shindig/te/test");
    @rmdir("/tmp/shindig/te");
  }

  /**
   * Tests CacheFile->set()
   */
  public function testSetException() {
    @rmdir("/tmp/shindig/te");
    $this->assertTrue(touch("/tmp/shindig/te"));
    $this->setExpectedException("CacheException");
    try {
      $this->CacheFile->set("test", "testing");
    } catch (Exception $e) {
      $this->assertTrue(unlink("/tmp/shindig/te"));
      throw $e;
    }
  }
}