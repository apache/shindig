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
 * UrlGenerator test case.
 */
class UrlGeneratorTest extends PHPUnit_Framework_TestCase {
  
  private $UrlGenerator;
  private $context;
  private $gadget;
  private $gadgetXML;

  protected function setUp() {
    parent::setUp();
    
    $this->gadgetXML = simplexml_load_string('<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="Test" />
  <Content type="html">
  <![CDATA[
    <h1>Hello, test case!</h1>
  ]]>
  </Content>
</Module>');
    
    $this->UrlGenerator = new UrlGenerator(/* parameters */);
    $this->context = new GadgetContext('GADGET');
    $this->gadget = new Gadget(false, $this->context);
    $this->gadget->views = array(DEFAULT_VIEW => new ViewSpec('test', $this->gadgetXML->Content));
  
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    
    $this->UrlGenerator = null;
    $this->context = null;
    $this->gadget = null;
    
    parent::tearDown();
  }

  public function testGetIframeURL() {
    
    $uri = UrlGenerator::getIframeURL($this->gadget, $this->context);
    
    $query = parse_url($uri, PHP_URL_QUERY);
    
    $query = explode('&', $query);
    
    $args = array();
    
    foreach ($query as $param) {
      $param = explode('=', $param);
      list($key, $value) = $param;
      $args[$key] = $value ? $value : '';
    }
    
    $this->assertArrayHasKey('container', $args);
    $this->assertArrayHasKey('lang', $args);
    $this->assertArrayHasKey('country', $args);
    $this->assertArrayHasKey('view', $args);
    $this->assertArrayHasKey('url', $args);
  }

}
