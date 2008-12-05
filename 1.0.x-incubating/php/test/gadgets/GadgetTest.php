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
 * Gadget test case.
 */
class GadgetTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var Gadget
   */
  private $Gadget;
  private $context;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    
    $this->context = new GadgetContext('GADGET');
    $this->Gadget = new Gadget(false, $this->context);
  
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    
    $this->Gadget = null;
    
    parent::tearDown();
  }

  /**
   * Tests Gadget->addJsLibrary()
   */
  public function testAddJsLibrary() {
    
    $this->Gadget->addJsLibrary('A');
    $this->Gadget->addJsLibrary('B');
    $this->Gadget->addJsLibrary('C');
    $this->Gadget->addJsLibrary('D');
    
    $string = implode('', $this->Gadget->getJsLibraries());
    
    $this->assertEquals($string, 'ABCD');
  
  }

  /**
   * Tests Gadget->getAuthor()
   */
  public function testGetAuthor() {
    
    $this->Gadget->author = 'authorTest';
    
    $this->assertEquals('authorTest', $this->Gadget->getAuthor());
  
  }

  /**
   * Tests Gadget->getAuthorAboutme()
   */
  public function testGetAuthorAboutme() {
    
    $this->Gadget->authorAboutMe = 'authorAboutMeTest';
    
    $this->assertEquals('authorAboutMeTest', $this->Gadget->getAuthorAboutme());
  
  }

  /**
   * Tests Gadget->getAuthorAffiliation()
   */
  public function testGetAuthorAffiliation() {
    
    $this->Gadget->authorAffiliation = 'authorAffiliation';
    
    $this->assertEquals('authorAffiliation', $this->Gadget->getAuthorAffiliation());
  
  }

  /**
   * Tests Gadget->getAuthorEmail()
   */
  public function testGetAuthorEmail() {
    
    $this->Gadget->authorEmail = 'authorEmail';
    
    $this->assertEquals('authorEmail', $this->Gadget->getAuthorEmail());
  
  }

  /**
   * Tests Gadget->getAuthorLink()
   */
  public function testGetAuthorLink() {
    $this->Gadget->authorLink = 'authorLink';
    $this->assertEquals('authorLink', $this->Gadget->getAuthorLink());
  }

  /**
   * Tests Gadget->getAuthorLocation()
   */
  public function testGetAuthorLocation() {
    $this->Gadget->authorLocation = 'authorLocation';
    $this->assertEquals('authorLocation', $this->Gadget->getAuthorLocation());
  }

  /**
   * Tests Gadget->getAuthorPhoto()
   */
  public function testGetAuthorPhoto() {
    $this->Gadget->authorPhoto = 'authorPhoto';
    $this->assertEquals('authorPhoto', $this->Gadget->getAuthorPhoto());
  }

  /**
   * Tests Gadget->getAuthorQuote()
   */
  public function testGetAuthorQuote() {
    
    $this->Gadget->authorQuote = 'authorQuote';
    
    $this->assertEquals('authorQuote', $this->Gadget->getAuthorQuote());
  
  }

  /**
   * Tests Gadget->getCategory()
   */
  public function testGetCategory() {
    
    $this->Gadget->category = 'category';
    
    $this->assertEquals('category', $this->Gadget->getCategory());
  
  }

  /**
   * Tests Gadget->getCategory2()
   */
  public function testGetCategory2() {
    
    $this->Gadget->category2 = 'category2';
    
    $this->assertEquals('category2', $this->Gadget->getCategory2());
  
  }

  /**
   * Tests Gadget->getDescription()
   */
  public function testGetDescription() {
    
    $this->Gadget->description = 'description';
    
    $this->assertEquals('description', $this->Gadget->getDescription());
  
  }

  /**
   * Tests Gadget->getDirectoryTitle()
   */
  public function testGetDirectoryTitle() {
    
    $this->Gadget->directoryTitle = 'directoryTitle';
    
    $this->assertEquals('directoryTitle', $this->Gadget->getDirectoryTitle());
  
  }

  /**
   * Tests Gadget->getHeight()
   */
  public function testGetHeight() {
    
    $this->Gadget->height = 'height';
    
    $this->assertEquals('height', $this->Gadget->getHeight());
  
  }

  /**
   * Tests Gadget->getId()
   */
  public function testGetId() {
    
    $this->Gadget->id = 'id';
    
    $this->assertEquals('id', $this->Gadget->getId());
  
  }

  /**
   * Tests Gadget->getJsLibraries()
   */
  public function testGetJsLibraries() {
    
    $this->Gadget->addJsLibrary('A');
    $this->Gadget->addJsLibrary('B');
    $this->Gadget->addJsLibrary('C');
    $this->Gadget->addJsLibrary('D');
    
    $string = implode('', $this->Gadget->getJsLibraries());
    
    $this->assertEquals('ABCD', $string);
  
  }

  /**
   * Tests Gadget->getLocaleSpecs()
   */
  public function testGetLocaleSpecs() {
    
    $this->Gadget->localeSpecs = 'localeSpecs';
    
    $this->assertEquals('localeSpecs', $this->Gadget->getLocaleSpecs());
  
  }

  /**
   * Tests Gadget->getMessageBundle()
   */
  public function testGetMessageBundle() {
    
    $this->Gadget->setMessageBundle('messageBundle');
    
    $this->assertEquals('messageBundle', $this->Gadget->getMessageBundle());
  
  }

  /**
   * Tests Gadget->getPreloads()
   */
  public function testGetPreloads() {
    
    $this->Gadget->preloads = array(0 => 'A', 1 => 'B');
    
    $this->assertEquals(array(0 => 'A', 1 => 'B'), $this->Gadget->getPreloads());
  
  }

  /**
   * Tests Gadget->getRenderInline()
   */
  public function testGetRenderInline() {
    
    $this->Gadget->renderInline = true;
    
    $this->assertTrue($this->Gadget->getRenderInline());
  
  }

  /**
   * Tests Gadget->getRequires()
   */
  public function testGetRequires() {
    
    $this->Gadget->requires = 'requires';
    
    $this->assertEquals('requires', $this->Gadget->getRequires());
  
  }

  /**
   * Tests Gadget->getScaling()
   */
  public function testGetScaling() {
    
    $this->Gadget->scaling = true;
    
    $this->assertTrue($this->Gadget->getScaling());
  
  }

  /**
   * Tests Gadget->getScreenshot()
   */
  public function testGetScreenshot() {
    
    $this->Gadget->screenshot = 'screenshot';
    
    $this->assertEquals('screenshot', $this->Gadget->getScreenshot());
  
  }

  /**
   * Tests Gadget->getScrolling()
   */
  public function testGetScrolling() {
    
    $this->Gadget->scrolling = true;
    
    $this->assertTrue($this->Gadget->getScrolling());
  
  }

  /**
   * Tests Gadget->getShowInDirectory()
   */
  public function testGetShowInDirectory() {
    
    $this->Gadget->showInDirectory = true;
    
    $this->assertTrue($this->Gadget->getShowInDirectory());
  
  }

  /**
   * Tests Gadget->getShowStats()
   */
  public function testGetShowStats() {
    
    $this->Gadget->showStats = true;
    
    $this->assertTrue($this->Gadget->getShowStats());
  
  }

  /**
   * Tests Gadget->getSingleton()
   */
  public function testGetSingleton() {
    
    $this->Gadget->singleton = 'singleton';
    
    $this->assertEquals('singleton', $this->Gadget->getSingleton());
  
  }

  /**
   * Tests Gadget->getString()
   */
  public function testGetString() {
    
    $this->Gadget->string = 'string';
    
    $this->assertEquals('string', $this->Gadget->getString());
  
  }

  /**
   * Tests Gadget->getSubstitutions()
   */
  public function testGetSubstitutions() {
    
    $this->assertTrue($this->Gadget->getSubstitutions() instanceof Substitutions);
  
  }

  /**
   * Tests Gadget->getThumbnail()
   */
  public function testGetThumbnail() {
    
    $this->Gadget->thumbnail = 'thumbnail';
    
    $this->assertEquals('thumbnail', $this->Gadget->getThumbnail());
  
  }

  /**
   * Tests Gadget->getTitle()
   */
  public function testGetTitle() {
    
    $this->Gadget->title = 'title';
    
    $this->assertEquals('title', $this->Gadget->getTitle());
  
  }

  /**
   * Tests Gadget->getTitleUrl()
   */
  public function testGetTitleUrl() {
    
    $this->Gadget->titleUrl = 'titleUrl';
    
    $this->assertEquals('titleUrl', $this->Gadget->getTitleUrl());
  
  }

  /**
   * Tests Gadget->getUserPrefs()
   */
  public function testGetUserPrefs() {
    
    $this->Gadget->userPrefs = array(0 => 'A', 1 => 'B');
    
    $this->assertEquals(array(0 => 'A', 1 => 'B'), $this->Gadget->getUserPrefs());
  
  }

  /**
   * Tests Gadget->getUserPrefValues()
   */
  public function testGetUserPrefValues() {
    
    $this->Gadget->setPrefs(array(0 => 'A', 1 => 'B'));
    
    $this->assertEquals(array(0 => 'A', 1 => 'B'), $this->Gadget->getUserPrefValues());
  
  }

  /**
   * Tests Gadget->getView()
   */
  public function testGetView() {
    
    $this->Gadget->views = array(0 => 'A', 1 => 'B');
    
    $this->assertEquals('B', $this->Gadget->getView(1));
  
  }

  /**
   * Tests Gadget->getViews()
   */
  public function testGetViews() {
    
    $this->Gadget->views = array(0 => 'A', 1 => 'B');
    
    $this->assertEquals(array(0 => 'A', 1 => 'B'), $this->Gadget->getViews());
  
  }

  /**
   * Tests Gadget->getWidth()
   */
  public function testGetWidth() {
    
    $this->Gadget->width = 100;
    
    $this->assertEquals(100, $this->Gadget->getWidth());
  
  }

  /**
   * Tests Gadget->setId()
   */
  public function testSetId() {
    
    $this->Gadget->setId('id');
    
    $this->assertEquals('id', $this->Gadget->id);
  
  }

  /**
   * Tests Gadget->setMessageBundle()
   */
  public function testSetMessageBundle() {
    
    $this->Gadget->setMessageBundle('messageBundle');
    
    $this->assertEquals('messageBundle', $this->Gadget->getMessageBundle());
  
  }

  /**
   * Tests Gadget->setPrefs()
   */
  public function testSetPrefs() {
    
    $this->Gadget->setPrefs('prefs');
    
    $this->assertEquals('prefs', $this->Gadget->getUserPrefValues());
  
  }
}