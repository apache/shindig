<?php
namespace apache\shindig\test\common;
use apache\shindig\common\RequestTime;
use apache\shindig\common\Cache;

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

class MockRequestTimeMc extends RequestTime {
  private $time = 0;

  public function getRequestTime() {
    return $this->time;
  }

  public function sleep($second) {
    $this->time += $second;
  }
}
/**
 * CacheMemcache test case.
 */
class CacheMemcacheTest extends \PHPUnit_Framework_TestCase {

  /**
   * @var Cache
   */
  private $cache;

  /**
   * @var MockRequestTime
   */
  private $time;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    if (!extension_loaded('memcache')) {
      $message = 'memcache requires the memcache extention';
      $this->markTestSkipped($message);
    }
    parent::setUp();
    $this->time = new MockRequestTimeMc();
    try {
      $this->cache = Cache::createCache('apache\shindig\common\sample\CacheStorageMemcache', 'TestCache', $this->time);
    } catch (\Exception $e) {
      $message = 'memcache server can not connect';
      $this->markTestSkipped($message);
    }
    if (! is_resource($this->cache)) {
      $message = 'memcache server can not connect';
      $this->markTestSkipped($message);
    }
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->cache = null;
    $this->time = null;
    parent::tearDown();
  }

  /**
   * Tests cache->delete()
   */
  public function testDelete() {
    $this->cache->set("test", "testing");
    $this->assertTrue(false != $this->cache->get("test"));
    $this->cache->delete("test");
    $this->assertFalse($this->cache->get("test"));
  }

  /**
   * Tests cache->delete()
   */
  public function testDeleteException() {
    $this->setExpectedException("CacheException");
    $this->cache->delete("test");
  }

  /**
   * Tests cache->get()
   */
  public function testGet() {
    $this->cache->set("test", "testing");
    $this->assertEquals("testing", $this->cache->get("test"));
    $this->cache->delete("test");
  }

  /**
   * Tests cache->get()
   */
  public function testExpiredGet() {
    $this->cache->set("test", "testing", 1);
    $this->time->sleep(100);
    $this->assertFalse($this->cache->get("test"));
    $expected = array("found" => true, "ttl" => 1,
                      "valid" => true, "data" => "testing");
    $output = $this->cache->expiredGet("test");
    $expected["time"] = $output["time"];
    $this->assertEquals($expected, $output);
    $this->cache->delete("test");
  }

  /**
   * Tests cache->set()
   */
  public function testSet() {
    $this->cache->set("test", "testing");
    $this->assertEquals("testing", $this->cache->get("test"));
    $expected = array("found" => true,
                      "valid" => true, "data" => "testing");
    $output = $this->cache->expiredGet("test");
    $expected["time"] = $output["time"];
    $expected["ttl"] = $output["ttl"];
    $this->assertEquals($expected, $output);
    $this->cache->delete("test");
  }

  /**
   * Tests cache->invalidate()
   */
  public function testInvalidation() {
    $this->cache->set("test", "testing");
    $this->cache->invalidate("test");
    $this->assertEquals(false, $this->cache->get("test"));
    $expected = array("found" => true,
                      "valid" => false, "data" => "testing");
    $output = $this->cache->expiredGet("test");
    $expected["time"] = $output["time"];
    $expected["ttl"] = $output["ttl"];
    $this->assertEquals($expected, $output);
  }
}