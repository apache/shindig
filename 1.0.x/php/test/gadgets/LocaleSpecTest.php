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
 * LocaleSpec test case.
 */
class LocaleSpecTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var LocaleSpec
   */
  private $LocaleSpec;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->LocaleSpec = new LocaleSpec();
    $this->LocaleSpec->url = 'url';
    $this->LocaleSpec->locale = 'locale';
    $this->LocaleSpec->rightToLeft = 'rtl';
    $this->LocaleSpec->localeMessageBundles = array('foo');
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->LocaleSpec = null;
    parent::tearDown();
  }

  /**
   * Tests LocaleSpec->getLocale()
   */
  public function testGetLocale() {
    $this->assertEquals('locale', $this->LocaleSpec->getLocale());
  }

  /**
   * Tests LocaleSpec->getLocaleMessageBundles()
   */
  public function testGetLocaleMessageBundles() {
    $this->assertEquals(array('foo'), $this->LocaleSpec->getLocaleMessageBundles());
  }

  /**
   * Tests LocaleSpec->getURI()
   */
  public function testGetURI() {
    $this->assertEquals('url', $this->LocaleSpec->getURI());
  }

  /**
   * Tests LocaleSpec->isRightToLeft()
   */
  public function testIsRightToLeft() {
    $this->assertEquals('rtl', $this->LocaleSpec->isRightToLeft());
  }
}
