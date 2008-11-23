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
 * GadgetServer test case.
 */
class GadgetServerTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var GadgetServer
   */
  private $GadgetServer;
  private $gadget;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->GadgetServer = new GadgetServer();
    // prevent polution from other test cases + make sure we're not testing
    // a cached result
    $_GET = array('nocache' => 1);
    $_POST = array();
    $GadgetContext = new GadgetContext('GADGET');
    $GadgetContext->setUrl('http://test.chabotc.com/testGadget.xml');
    $this->gadget = $this->GadgetServer->processGadget($GadgetContext);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->GadgetServer = null;
    $this->gadget = null;
    parent::tearDown();
  }

  /**
   * Tests GadgetServer->processGadget()
   */
  public function testProcessGadget() {
    $this->assertTrue($this->gadget instanceof Gadget);
  }

  /**
   * Tests gadget html view
   */
  public function testGadgetHtmlView() {
    $view = $this->gadget->getView('canvas');
    $this->assertEquals('canvas', $view->getName());
    $this->assertEquals('HTML', $view->getType());
    $this->assertEquals('', $view->getHref());
    $this->assertEquals("content", $view->getContent());
    $this->assertEquals("400", $view->preferedHeight);
    $this->assertEquals("300", $view->preferedWidth);
    $this->assertFalse($view->getQuirks());
  }

  /**
   * Tests gadget url view
   */
  public function testGadgetUrlView() {
    $view = $this->gadget->getView('external');
    $this->assertEquals('external', $view->getName());
    $this->assertEquals('URL', $view->getType());
    $this->assertEquals('http://example.com/foo.html', $view->getHref());
    $this->assertTrue($view->getQuirks());
  }

  /**
   * Tests gadget user prefs
   */
  public function testGadgetUserPrefs() {
    $prefs = $this->gadget->getUserPrefs();
    $this->assertTrue(is_array($prefs));
    $this->assertTrue(isset($prefs[0]));
    $pref = $prefs[0];
    $this->assertEquals('testpref', $pref->getName());
    $this->assertEquals('test', $pref->getDefaultValue());
    $expectedEnumValues = array('bar' => 'foo', 'test' => 'test');
    $this->assertEquals($expectedEnumValues, $pref->getEnumValues());
  }

  /**
   * Tests gadget requires
   */
  public function testGadgetRequires() {
    $requires = $this->gadget->getRequires();
    $this->assertArrayHasKey('opensocial-0.7', $requires);
    $this->assertArrayHasKey('dynamic-height', $requires);
    $this->assertArrayHasKey('views', $requires);
    $this->assertArrayHasKey('tabs', $requires);
    $this->assertArrayHasKey('flash', $requires);
    $this->assertArrayHasKey('setprefs', $requires);
  }

  /**
   * Tests all the basic + extended gadget module preferences
   */
  public function testGadgetModulePreferences() {
    $this->assertEquals('example author', $this->gadget->getAuthor());
    $this->assertEquals('test@example.org', $this->gadget->getAuthorEmail());
    $this->assertEquals('example org', $this->gadget->getAuthorAffiliation());
    $this->assertEquals('example location', $this->gadget->getAuthorLocation());
    $this->assertEquals('example photo', $this->gadget->getAuthorPhoto());
    $this->assertEquals('example about me', $this->gadget->getAuthorAboutme());
    $this->assertEquals('example quote', $this->gadget->getAuthorQuote());
    $this->assertEquals('example link', $this->gadget->getAuthorLink());
    $this->assertEquals('true', $this->gadget->getShowStats());
    $this->assertEquals('true', $this->gadget->getShowInDirectory());
    $this->assertEquals('200', $this->gadget->getWidth());
    $this->assertEquals('100', $this->gadget->getHeight());
    $this->assertEquals('example category', $this->gadget->getCategory());
    $this->assertEquals('example category2', $this->gadget->getCategory2());
    $this->assertEquals('true', $this->gadget->getSingleton());
    $this->assertEquals('true', $this->gadget->getRenderInline());
    $this->assertEquals('true', $this->gadget->getScaling());
    $this->assertEquals('true', $this->gadget->getScrolling());
    $this->assertEquals('http://example.org', $this->gadget->getTitleUrl());
    $this->assertEquals('example title', $this->gadget->getTitle());
    $this->assertEquals('http://example.org/thumbnail.gif', $this->gadget->getThumbnail());
    $this->assertEquals('http://example.org/screenshot.gif', $this->gadget->getScreenshot());
    $this->assertEquals('example directory title', $this->gadget->getDirectoryTitle());
    $this->assertEquals('description', $this->gadget->getDescription());
  }
}
