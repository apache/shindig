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

class MockGadgetFactory extends GadgetFactory {
  public function __construct(GadgetContext $context, $token) {
  	parent::__construct($context, $token);
  }
  
	protected function fetchGadget($gadgetUrl) {
		return '<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="title" author="authorTest"
   author_aboutme="authorAboutMeTest" author_affiliation="authorAffiliation"
   author_email="authorEmail" author_link="authorLink"
   author_location="authorLocation" author_photo="authorPhoto"
   author_quote="authorQuote" category="category" category2="category2"
   description="description" directory_title="directoryTitle" height="100"
   width="100" screenshot="screenshot" singleton="true" thumbnail="thumbnail"
   string="string" title_url="titleUrl" render_inline="never" scaling="true"
   scrolling="true" show_in_directory="true" show_stats="false"
  />
  <UserPref name="name1" default_value="0" datatype="hidden"/>
  <UserPref name="name2" default_value="value" datatype="hidden"/>
  <Content type="html" view="home">
  <![CDATA[
    <h1>Hello, world!</h1>
  ]]>
  </Content>
</Module>';
	}
}

/**
 * Gadget test case.
 */
class GadgetTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var Gadget
   */
  private $gadget;
  private $context;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    
    $this->context = new GadgetContext('GADGET');
    $gadgetSpecFactory = new MockGadgetFactory($this->context, null);
    $gadgetSpecFactory->fetchGadget = null;
    $this->gadget = $gadgetSpecFactory->createGadget();
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    
    $this->gadget = null;
    
    parent::tearDown();
  }

  /**
   * Tests Gadget->getAuthor()
   */
  public function testGetAuthor() {
    $this->assertEquals('authorTest', $this->gadget->getAuthor());
  }

  /**
   * Tests Gadget->getAuthorAboutme()
   */
  public function testGetAuthorAboutme() {
    $this->assertEquals('authorAboutMeTest', $this->gadget->getAuthorAboutme());
  }

  /**
   * Tests Gadget->getAuthorAffiliation()
   */
  public function testGetAuthorAffiliation() {
    $this->assertEquals('authorAffiliation', $this->gadget->getAuthorAffiliation());
  }

  /**
   * Tests Gadget->getAuthorEmail()
   */
  public function testGetAuthorEmail() {
    $this->assertEquals('authorEmail', $this->gadget->getAuthorEmail());
  }

  /**
   * Tests Gadget->getAuthorLink()
   */
  public function testGetAuthorLink() {
    $this->assertEquals('authorLink', $this->gadget->getAuthorLink());
  }

  /**
   * Tests Gadget->getAuthorLocation()
   */
  public function testGetAuthorLocation() {
    $this->assertEquals('authorLocation', $this->gadget->getAuthorLocation());
  }

  /**
   * Tests Gadget->getAuthorPhoto()
   */
  public function testGetAuthorPhoto() {
    $this->assertEquals('authorPhoto', $this->gadget->getAuthorPhoto());
  }

  /**
   * Tests Gadget->getAuthorQuote()
   */
  public function testGetAuthorQuote() {
    $this->assertEquals('authorQuote', $this->gadget->getAuthorQuote());
  }

  /**
   * Tests Gadget->getCategory()
   */
  public function testGetCategory() {
    $this->assertEquals('category', $this->gadget->getCategory());
  }

  /**
   * Tests Gadget->getCategory2()
   */
  public function testGetCategory2() {
    $this->assertEquals('category2', $this->gadget->getCategory2());
  }

  /**
   * Tests Gadget->getDescription()
   */
  public function testGetDescription() {
    $this->assertEquals('description', $this->gadget->getDescription());
  }

  /**
   * Tests Gadget->getDirectoryTitle()
   */
  public function testGetDirectoryTitle() {
    $this->assertEquals('directoryTitle', $this->gadget->getDirectoryTitle());
  }

  /**
   * Tests Gadget->getHeight()
   */
  public function testGetHeight() {
    $this->assertEquals('100', $this->gadget->getHeight());
  }

  /**
   * Tests Gadget->getRenderInline()
   */
  public function testGetRenderInline() {
    $this->assertEquals("never", $this->gadget->getRenderInline());
  }

  /**
   * Tests Gadget->getScaling()
   */
  public function testGetScaling() {
    $this->assertEquals("true", $this->gadget->getScaling());
  }

  /**
   * Tests Gadget->getScreenshot()
   */
  public function testGetScreenshot() {
    $this->assertEquals('screenshot', $this->gadget->getScreenshot());
  }

  /**
   * Tests Gadget->getScrolling()
   */
  public function testGetScrolling() {
    $this->assertEquals("true", $this->gadget->getScrolling());
  }

  /**
   * Tests Gadget->getShowInDirectory()
   */
  public function testGetShowInDirectory() {
    $this->assertEquals("true", $this->gadget->getShowInDirectory());
  }

  /**
   * Tests Gadget->getShowStats()
   */
  public function testGetShowStats() {
    $this->assertEquals("false", $this->gadget->getShowStats());
  }

  /**
   * Tests Gadget->getSingleton()
   */
  public function testGetSingleton() {
    $this->assertEquals('true', $this->gadget->getSingleton());
  }

  /**
   * Tests Gadget->getString()
   */
  public function testGetString() {
    $this->assertEquals('string', $this->gadget->getString());
  }

  /**
   * Tests Gadget->getThumbnail()
   */
  public function testGetThumbnail() {
    $this->assertEquals('thumbnail', $this->gadget->getThumbnail());
  }

  /**
   * Tests Gadget->getTitle()
   */
  public function testGetTitle() {
    $this->assertEquals('title', $this->gadget->getTitle());
  }

  /**
   * Tests Gadget->getTitleUrl()
   */
  public function testGetTitleUrl() {
    $this->assertEquals('titleUrl', $this->gadget->getTitleUrl());
  }

  /**
   * Tests Gadget->getUserPrefs()
   */
  public function testGetUserPrefs() {
    $userPrefs = $this->gadget->getUserPrefs();
    $this->assertEquals("name1", $userPrefs[0]['name']);
    $this->assertEquals("0", $userPrefs[0]['defaultValue']);
    $this->assertEquals("0", $userPrefs[0]['value']);
    $this->assertEquals("name2", $userPrefs[1]['name']);
    $this->assertEquals("value", $userPrefs[1]['defaultValue']);
    $this->assertEquals("value", $userPrefs[1]['value']);
  }

  /**
   * Tests Gadget->getWidth()
   */
  public function testGetWidth() {
    $this->assertEquals("100", $this->gadget->getWidth());
  }
}