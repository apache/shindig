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

class MockUrlGadgetFactory extends GadgetFactory {
  public function __construct(GadgetContext $context, $token) {
    parent::__construct($context, $token);
  }

  protected function fetchGadget($gadgetUrl) {
    return '<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="title">
    <Require feature="opensocial-0.8" />
    <Require feature="dynamic-height" />
  </ModulePrefs>
  <UserPref name="key" datatype="string" default_value="value" />
  <Content type="url" url="http://example.com/gadget.php" view="home">
  </Content>
</Module>';
  }
}

/**
 * GadgetUrlRendererTest test case.
 */
class GadgetUrlRendererTest extends PHPUnit_Framework_TestCase {

  /**
   * @var Gadget
   */
  private $gadget;

  /**
   * @var GadgetContext
   */
  private $gadgetContext;
  
  /**
   * @var GadgetHtmlRender
   */
  private $gadgetUrlRenderer;


  protected function setUp() {
    parent::setUp();
    $this->gadgetContext = new GadgetContext('GADGET');
    $gadgetSpecFactory = new MockUrlGadgetFactory($this->gadgetContext, null);
    $gadgetSpecFactory->fetchGadget = null;
    $this->gadget = $gadgetSpecFactory->createGadget();
    $this->gadgetUrlRenderer = new GadgetUrlRenderer($this->gadgetContext);
  }

  public function testGetUrl() {
    $view = array(
      'href' => 'http://example.com/gadget.php',
    );
    $redirectUri = $this->gadgetUrlRenderer->getSubstitutedUrl($this->gadget, $view);
    $parsedUrl = parse_url($redirectUri);
    $queryParameters = array();
    parse_str($parsedUrl['query'], $queryParameters);
    $this->assertEquals('example.com', $parsedUrl['host']);
    $this->assertEquals('/gadget.php', $parsedUrl['path']);
    $this->assertEquals('dynamic-height:opensocial-0.8.js', $queryParameters['libs']);
    $this->assertEquals('en', $queryParameters['lang']);
    $this->assertEquals('US', $queryParameters['country']);
    $this->assertEquals('value',$queryParameters['up_key']);
  }

  public function testGetSubstitutedUrl() {
    $view = array(
      'href' => 'http://example.com/gadget.php?foo=bar&mid=__MODULE_ID__',
    );
    $redirectUri = $this->gadgetUrlRenderer->getSubstitutedUrl($this->gadget, $view);
    $parsedUrl = parse_url($redirectUri);
    $queryParameters = array();
    parse_str($parsedUrl['query'], $queryParameters);
    $this->assertEquals('example.com', $parsedUrl['host']);
    $this->assertEquals('/gadget.php', $parsedUrl['path']);
    $this->assertEquals('dynamic-height:opensocial-0.8.js', $queryParameters['libs']);
    $this->assertEquals('en', $queryParameters['lang']);
    $this->assertEquals('US', $queryParameters['country']);
    $this->assertEquals('bar',$queryParameters['foo']);
    $this->assertEquals('value',$queryParameters['up_key']);
    $this->assertEquals('0',$queryParameters['mid']);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->gadget = null;
    $this->gadgetContext = null;
    $this->gadgetUrlRenderer = null;

    parent::tearDown();
  }
}
