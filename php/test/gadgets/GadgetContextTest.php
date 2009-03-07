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
 * GadgetContext test case.
 */
class GadgetContextTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var GadgetContext
   */
  private $GadgetContext;
  
  /**
   * @var testData
   */
  private $testData = array('url' => 'http://www.google.com/gadget', 'libs' => '', 'synd' => 'default', 
      'nocache' => '', 'container' => 'default', 'view' => 'default', 'mid' => '123', 
      'bcp' => '');
  
  /**
   * @var gadgetRenderingContext
   */
  private $gadgetRenderingContext = 'GADGET';

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    
    $_GET = $this->testData;
    $this->GadgetContext = new GadgetContext($this->gadgetRenderingContext);
  
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->GadgetContext = null;
    
    parent::tearDown();
  }

  /**
   * Tests GadgetContext->getBlacklist()
   */
  public function testGetBlacklist() {
    $this->assertTrue(is_object($this->GadgetContext->getBlackList()));
  
  }

  /**
   * Tests GadgetContext->getContainer()
   */
  public function testGetContainer() {
    $this->assertEquals($this->testData['container'], $this->GadgetContext->getContainer());
  
  }

  /**
   * Tests GadgetContext->getForcedJsLibs()
   */
  public function testGetForcedJsLibs() {
    $this->assertEquals($this->testData['libs'], $this->GadgetContext->getForcedJsLibs());
  
  }

  /**
   * Tests GadgetContext->getHttpFetcher()
   */
  public function testGetHttpFetcher() {
    $this->assertNotNull($this->GadgetContext->getHttpFetcher());
  
  }

  /**
   * Tests GadgetContext->getLocale()
   */
  public function testGetLocale() {
    $this->assertNotNull($this->GadgetContext->getLocale());
  
  }

  /**
   * Tests GadgetContext->getModuleId()
   */
  public function testGetModuleId() {
    $this->assertEquals($this->testData['mid'], $this->GadgetContext->getModuleId());
  
  }

  /**
   * Tests GadgetContext->getRegistry()
   */
  public function testGetRegistry() {
    $this->assertNotNull($this->GadgetContext->getRegistry());
  
  }

  /**
   * Tests GadgetContext->getRenderingContext()
   */
  public function testGetRenderingContext() {
    $this->assertEquals($this->gadgetRenderingContext, $this->GadgetContext->getRenderingContext());
  
  }

  /**
   * Tests GadgetContext->getUrl()
   */
  public function testGetUrl() {
    $this->assertEquals($this->testData['url'], $this->GadgetContext->getUrl());
  
  }

  /**
   * Tests GadgetContext->getView()
   */
  public function testGetView() {
    $this->assertEquals($this->testData['view'], $this->GadgetContext->getView());
  
  }

  /**
   * Tests GadgetContext->setRenderingContext()
   */
  public function testSetRenderingContext() {
    $redering_context = 'Dummie_rendering_context';
    $this->GadgetContext->setRenderingContext($redering_context);
    $this->assertAttributeEquals($redering_context, 'renderingContext', $this->GadgetContext);
  
  }

  /**
   * Tests GadgetContext->setUrl()
   */
  public function testSetUrl() {
    $url = 'Dummie_url';
    $this->GadgetContext->setUrl($url);
    $this->assertAttributeEquals($url, 'url', $this->GadgetContext);
  
  }

  /**
   * Tests GadgetContext->setView()
   */
  public function testSetView() {
    $view = 'Dummie_view';
    $this->GadgetContext->setView($view);
    $this->assertAttributeEquals($view, 'view', $this->GadgetContext);
  
  }

}

