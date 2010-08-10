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
 * JsMin test case.
 */
class JsMinTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var JsMin
   */
  private $JsMin;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->JsMin = new JsMin('');
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->JsMin = null;
    parent::tearDown();
  }

  /**
   * Constructs the test case.
   */
  public function __construct() {}

  /**
   * Tests JsMin->__construct()
   */
  public function test__construct() {
    $this->JsMin->__construct('');
  }

  /**
   * Tests JsMin::minify()
   */
  public function testMinify() {
    $expect = "\nfunction foo(bar)\n{if(bar==2){return true;}else{return false;}}";
    $javascript = '
function foo(bar)
{
	if (bar == 2) {
		return true;
	} else {
		return false;
	}
}
';
    $this->assertEquals($expect, JsMin::minify($javascript));
  }
}
