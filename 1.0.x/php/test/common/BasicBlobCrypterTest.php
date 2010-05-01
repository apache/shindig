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
 * BasicBlobCrypter test case.
 */
class BasicBlobCrypterTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var BasicBlobCrypter
   */
  private $BasicBlobCrypter;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->BasicBlobCrypter = new BasicBlobCrypter();
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->BasicBlobCrypter = null;
    parent::tearDown();
  }

  /**
   * Tests BasicBlobCrypter->__construct()
   */
  public function test__construct() {
    $this->BasicBlobCrypter->__construct();
  }

  /**
   * Tests BasicBlobCrypter->wrap()
   */
  public function testWrap() {
    $test = array();
    $test['o'] = 'o';
    $test['v'] = 'v';
    $test['a'] = 'a';
    $test['d'] = 'd';
    $test['u'] = 'u';
    $test['m'] = 'm';
    $wrapped = $this->BasicBlobCrypter->wrap($test);
    $unwrapped = $this->BasicBlobCrypter->unwrap($wrapped, 3600);
    $this->assertEquals($unwrapped['o'], 'o');
    $this->assertEquals($unwrapped['v'], 'v');
    $this->assertEquals($unwrapped['a'], 'a');
    $this->assertEquals($unwrapped['d'], 'd');
    $this->assertEquals($unwrapped['u'], 'u');
    $this->assertEquals($unwrapped['m'], 'm');
  }

  /**
   * Tests BasicBlobCrypter->wrap() exception
   */
  public function testWrapException() {
    $this->setExpectedException('BlobExpiredException');
    $test = array();
    $test['o'] = 'o';
    $test['v'] = 'v';
    $test['a'] = 'a';
    $test['d'] = 'd';
    $test['u'] = 'u';
    $test['m'] = 'm';
    $wrapped = $this->BasicBlobCrypter->wrap($test);
    /* there is a 180 seconds clock skew allowed, so this way we make sure it's expired */
    $this->BasicBlobCrypter->unwrap($wrapped, - 4000);
  }

}

