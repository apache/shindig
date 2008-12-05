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
 * BasicGadgetBlacklist test case.
 */
class BasicGadgetBlacklistTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var BasicGadgetBlacklist
   */
  private $BasicGadgetBlacklist;
  
  private $tmpFile;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    // sys_get_temp_dir() requires php >= 5.2.1
    $this->tmpFile = tempnam(sys_get_temp_dir(), 'test-blacklist-');
    file_put_contents($this->tmpFile, "/www/i\n");
    $this->BasicGadgetBlacklist = new BasicGadgetBlacklist($this->tmpFile);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    @unlink($this->tmpFile);
    $this->BasicGadgetBlacklist = null;
    parent::tearDown();
  }

  /**
   * Tests BasicGadgetBlacklist->isBlacklisted()
   */
  public function testIsBlacklisted() {
    $this->assertTrue($this->BasicGadgetBlacklist->isBlacklisted('http://www.foo.com/bar.xml'));
  }
}
