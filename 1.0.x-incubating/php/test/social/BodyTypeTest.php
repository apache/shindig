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
 * BodyType test case.
 */
class BodyTypeTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var BodyType
   */
  private $BodyType;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->BodyType = new BodyType();
    $this->BodyType->build = 'BUILD';
    $this->BodyType->eyeColor = 'EYECOLOR';
    $this->BodyType->hairColor = 'HAIRCOLOR';
    $this->BodyType->height = 'HEIGHT';
    $this->BodyType->weight = 'WEIGHT';
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->BodyType = null;
    parent::tearDown();
  }

  /**
   * Tests BodyType->getBuild()
   */
  public function testGetBuild() {
    $this->assertEquals('BUILD', $this->BodyType->getBuild());
  }

  /**
   * Tests BodyType->getEyeColor()
   */
  public function testGetEyeColor() {
    $this->assertEquals('EYECOLOR', $this->BodyType->getEyeColor());
  }

  /**
   * Tests BodyType->getHairColor()
   */
  public function testGetHairColor() {
    $this->assertEquals('HAIRCOLOR', $this->BodyType->getHairColor());
  }

  /**
   * Tests BodyType->getHeight()
   */
  public function testGetHeight() {
    $this->assertEquals('HEIGHT', $this->BodyType->getHeight());
  }

  /**
   * Tests BodyType->getWeight()
   */
  public function testGetWeight() {
    $this->assertEquals('WEIGHT', $this->BodyType->getWeight());
  }

  /**
   * Tests BodyType->setBuild()
   */
  public function testSetBuild() {
    $this->BodyType->setBuild('build');
    $this->assertEquals('build', $this->BodyType->getBuild());
  }

  /**
   * Tests BodyType->setEyeColor()
   */
  public function testSetEyeColor() {
    $this->BodyType->setEyeColor('eyecolor');
    $this->assertEquals('eyecolor', $this->BodyType->getEyeColor());
  }

  /**
   * Tests BodyType->setHairColor()
   */
  public function testSetHairColor() {
    $this->BodyType->setHairColor('haircolor');
    $this->assertEquals('haircolor', $this->BodyType->getHairColor());
  }

  /**
   * Tests BodyType->setHeight()
   */
  public function testSetHeight() {
    $this->BodyType->setHeight('height');
    $this->assertEquals('height', $this->BodyType->getHeight());
  }

  /**
   * Tests BodyType->setWeight()
   */
  public function testSetWeight() {
    $this->BodyType->setWeight('weight');
    $this->assertEquals('weight', $this->BodyType->getWeight());
  }
}
