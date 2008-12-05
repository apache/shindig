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
 * Name test case.
 */
class NameTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var Name
   */
  private $Name;
  
  /**
   * @var additionalName
   */
  public $additionalName;
  
  /**
   * @var familyName
   */
  public $familyName;
  
  /**
   * @var givenName
   */
  public $givenName;
  
  /**
   * @var honorificPrefix
   */
  public $honorificPrefix;
  
  /**
   * @var honorificSuffix
   */
  public $honorificSuffix;
  
  /**
   * @var unstructured
   */
  public $unstructured = '';

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->Name = new Name($this->unstructured);
  
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->Name = null;
    
    parent::tearDown();
  }

  /**
   * Tests Name->getAdditionalName()
   */
  public function testGetAdditionalName() {
    $this->Name->additionalName = $this->additionalName;
    $this->assertEquals($this->Name->getAdditionalName(), $this->additionalName);
  }

  /**
   * Tests Name->getFamilyName()
   */
  public function testGetFamilyName() {
    $this->Name->familyName = $this->familyName;
    $this->assertEquals($this->Name->getFamilyName(), $this->familyName);
  }

  /**
   * Tests Name->getGivenName()
   */
  public function testGetGivenName() {
    $this->Name->givenName = $this->givenName;
    $this->assertEquals($this->Name->getGivenName(), $this->givenName);
  }

  /**
   * Tests Name->getHonorificPrefix()
   */
  public function testGetHonorificPrefix() {
    $this->Name->honorificPrefix = $this->honorificPrefix;
    $this->assertEquals($this->Name->getHonorificPrefix(), $this->honorificPrefix);
  }

  /**
   * Tests Name->getHonorificSuffix()
   */
  public function testGetHonorificSuffix() {
    $this->Name->honorificSuffix = $this->honorificSuffix;
    $this->assertEquals($this->Name->getHonorificSuffix(), $this->honorificSuffix);
  }

  /**
   * Tests Name->getUnstructured()
   */
  public function testGetUnstructured() {
    $this->Name->unstructured = $this->unstructured;
    $this->assertEquals($this->Name->getFormatted(), $this->unstructured);
  }

  /**
   * Tests Name->setAdditionalName()
   */
  public function testSetAdditionalName() {
    $this->Name->setAdditionalName($this->additionalName);
    $this->assertEquals($this->Name->getAdditionalName(), $this->additionalName);
  }

  /**
   * Tests Name->setFamilyName()
   */
  public function testSetFamilyName() {
    $this->Name->setFamilyName($this->familyName);
    $this->assertEquals($this->Name->getFamilyName(), $this->familyName);
  }

  /**
   * Tests Name->setGivenName()
   */
  public function testSetGivenName() {
    $this->Name->setGivenName($this->givenName);
    $this->assertEquals($this->Name->getGivenName(), $this->givenName);
  }

  /**
   * Tests Name->setHonorificPrefix()
   */
  public function testSetHonorificPrefix() {
    $this->Name->setHonorificPrefix($this->honorificPrefix);
    $this->assertEquals($this->Name->getHonorificPrefix(), $this->honorificPrefix);
  
  }

  /**
   * Tests Name->setHonorificSuffix()
   */
  public function testSetHonorificSuffix() {
    $this->Name->setHonorificSuffix($this->honorificSuffix);
    $this->assertEquals($this->Name->getHonorificSuffix(), $this->honorificSuffix);
  }

  /**
   * Tests Name->setUnstructured()
   */
  public function testSetUnstructured() {
    $this->Name->setFormatted($this->unstructured);
    $this->assertEquals($this->Name->getFormatted(), $this->unstructured);
  }
}
