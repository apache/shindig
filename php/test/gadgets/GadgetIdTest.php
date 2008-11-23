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
 * GadgetId test case.
 */
class GadgetIdTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var GadgetId
   */
  private $GadgetId = 'ID';
  
  /**
   * 
   * @var uri
   */
  private $uri = 'http://www.example.com/xml.xml';
  
  /**
   *
   * @var moduleId
   */
  private $moduleId = 'MID';

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->GadgetId = new GadgetId($this->uri, $this->moduleId);
  
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->GadgetId = null;
    
    parent::tearDown();
  }

  /**
   * Tests GadgetId->getKey()
   */
  public function testGetKey() {
    $this->assertEquals($this->uri, $this->GadgetId->getKey());
  }

  /**
   * Tests GadgetId->getModuleId()
   */
  public function testGetModuleId() {
    $this->assertEquals($this->moduleId, $this->GadgetId->getModuleId());
  }

  /**
   * Tests GadgetId->getURI()
   */
  public function testGetURI() {
    $this->assertEquals($this->uri, $this->GadgetId->getURI());
  
  }

}

