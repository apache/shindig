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
 * MessageBundleParser test case.
 */
class MessageBundleParserTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var MessageBundleParser
   */
  private $MessageBundleParser;
  private $MessageBundle;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    
    $this->MessageBundleParser = new MessageBundleParser(/* parameters */);
  
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    
    $this->MessageBundleParser = null;
    $this->MessageBundle = null;
    parent::tearDown();
  }

  /**
   * Tests MessageBundleParser->parse()
   */
  public function testParse() {
    $xml = '<?xml version="1.0" encoding="UTF-8" ?>
<doc>
	<msg name="name1">Message 1</msg>
	<msg name="name2">Message 2</msg>
	<msg name="name3">Message 3</msg>
	<msg name="name4">Message 4</msg>
</doc>';
    
    $this->MessageBundle = $this->MessageBundleParser->parse($xml);
    
    $this->assertEquals('Message 1', $this->MessageBundle['name1']);
    $this->assertEquals('Message 2', $this->MessageBundle['name2']);
    $this->assertEquals('Message 3', $this->MessageBundle['name3']);
    $this->assertEquals('Message 4', $this->MessageBundle['name4']);
  }
  
  /**
   * Tests MessageBundleParser->parse() on error xml string.
   */
  public function testParseOnError() {
    $xml = '<?xml version="1.0" encoding="UTF-8" ?>
<doc>
  <msg name="name1">Message 1</msg>
  <msg name="name2">Message 2</msg>
  <msg name="name3">Message 3</msg>
  <msg name="name4">Message 4</msg>';
    $this->setExpectedException('Exception');
    $this->MessageBundle = $this->MessageBundleParser->parse($xml);  	
  }
}

