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
 * Activity test case.
 */
class ActivityTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var Activity
   */
  private $Activity;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->Activity = new Activity(1, 1);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->Activity = null;
    parent::tearDown();
  }

  /**
   * Constructs the test case.
   */
  public function __construct() {}

  /**
   * Tests Activity->__construct()
   */
  public function test__construct() {
    $this->Activity->__construct(1, 1);
  }

  /**
   * Tests Activity->getAppId()
   */
  public function testGetAppId() {
    $this->Activity->setAppId(1);
    $this->assertEquals(1, $this->Activity->getAppId());
  }

  /**
   * Tests Activity->getBody()
   */
  public function testGetBody() {
    $testStr = '<b>test <i>me</i></b>';
    $this->Activity->setBody($testStr);
    $this->assertEquals($testStr, $this->Activity->getBody());
  }

  /**
   * Tests Activity->getBodyId()
   */
  public function testGetBodyId() {
    $bodyId = '123';
    $this->Activity->setBodyId($bodyId);
    $this->assertEquals($bodyId, $this->Activity->getBodyId());
  }

  /**
   * Tests Activity->getExternalId()
   */
  public function testGetExternalId() {
    $extId = '456';
    $this->Activity->setExternalId($extId);
    $this->assertEquals($extId, $this->Activity->getExternalId());
  }

  /**
   * Tests Activity->getId()
   */
  public function testGetId() {
    $this->assertEquals(1, $this->Activity->getId());
  }

  /**
   * Tests Activity->getMediaItems()
   */
  public function testGetMediaItems() {
    $mediaItems = array('foo' => 'bar');
    $this->Activity->setMediaItems($mediaItems);
    $this->assertEquals($mediaItems, $this->Activity->getMediaItems());
  }

  /**
   * Tests Activity->getPostedTime()
   */
  public function testGetPostedTime() {
    $time = time();
    $this->Activity->setPostedTime($time);
    $this->assertEquals($time, $this->Activity->getPostedTime());
  }

  /**
   * Tests Activity->getPriority()
   */
  public function testGetPriority() {
    $priority = 1;
    $this->Activity->setPriority($priority);
    $this->assertEquals($priority, $this->Activity->getPriority());
  }

  /**
   * Tests Activity->getStreamFaviconUrl()
   */
  public function testGetStreamFaviconUrl() {
    $url = 'http://www.google.com/ig/modules/horoscope_content/virgo.gif';
    $this->Activity->setStreamFaviconUrl($url);
    $this->assertEquals($url, $this->Activity->getStreamFaviconUrl());
  }

  /**
   * Tests Activity->getStreamSourceUrl()
   */
  public function testGetStreamSourceUrl() {
    $url = 'http://api.example.org/activity/foo/1';
    $this->Activity->setStreamSourceUrl($url);
    $this->assertEquals($url, $this->Activity->getStreamSourceUrl());
  }

  /**
   * Tests Activity->getStreamTitle()
   */
  public function testGetStreamTitle() {
    $title = 'Foo Activity';
    $this->Activity->setStreamTitle($title);
    $this->assertEquals($title, $this->Activity->getStreamTitle());
  }

  /**
   * Tests Activity->getStreamUrl()
   */
  public function testGetStreamUrl() {
    $streamUrl = 'http://api.example.org/activityStream/foo/1';
    $this->Activity->setStreamUrl($streamUrl);
    $this->assertEquals($streamUrl, $this->Activity->getStreamUrl());
  }

  /**
   * Tests Activity->getTemplateParams()
   */
  public function testGetTemplateParams() {
    $params = array('fooParam' => 'barParam');
    $this->Activity->setTemplateParams($params);
    $this->assertEquals($params, $this->Activity->getTemplateParams());
  }

  /**
   * Tests Activity->getTitle()
   */
  public function testGetTitle() {
    $title = 'Foo Activity Title';
    $this->Activity->setTitle($title);
    $this->assertEquals($title, $this->Activity->getTitle());
  }

  /**
   * Tests Activity->getTitleId()
   */
  public function testGetTitleId() {
    $titleId = '976';
    $this->Activity->setTitleId($titleId);
    $this->assertEquals($titleId, $this->Activity->getTitleId());
  }

  /**
   * Tests Activity->getUrl()
   */
  public function testGetUrl() {
    $url = 'http://api.example.org/url';
    $this->Activity->setUrl($url);
    $this->assertEquals($url, $this->Activity->getUrl());
  }

  /**
   * Tests Activity->getUserId()
   */
  public function testGetUserId() {
    $this->assertEquals(1, $this->Activity->getUserId());
  }
}
