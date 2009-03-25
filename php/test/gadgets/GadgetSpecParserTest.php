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
 * GadgetSpecParser test case.
 */
class GadgetSpecParserTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var GadgetSpecParser
   */
  private $GadgetSpecParser;
  
  /**
   * @var Gadget
   */
  private $Gadget = '<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="Test" />
  <Content type="html" view="home">
  <![CDATA[
    <h1>Hello, world!</h1>
  ]]>
  </Content>
</Module>';
  
  /**
   * @var Context
   */
  private $Context;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    
    $this->GadgetSpecParser = new GadgetSpecParser();
    $this->Context = new GadgetContext('GADGET');
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->GadgetSpecParser = null;
    
    parent::tearDown();
  }

  /**
   * Tests GadgetSpecParser->parse() exception
   */
  public function testParseExeption() {
    $this->setExpectedException('GadgetSpecException');
    $this->assertTrue($this->GadgetSpecParser->parse('<', $this->Context));
  }

  /**
   * Tests GadgetSpecParser->parse()
   */
  public function testParse() {
    $gadgetParsed = $this->GadgetSpecParser->parse($this->Gadget, $this->Context);
    $view = $gadgetParsed->views['home'];
    $this->assertEquals('<h1>Hello, world!</h1>', trim($view['content']));
  }
}

