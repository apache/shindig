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
 * Organization test case.
 */
class OrganizationTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var Organization
   */
  private $Organization;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    
    $this->Organization = new Organization('NAME');
    $this->Organization->address = 'ADDRESS';
    $this->Organization->description = 'DESCRIPTION';
    $this->Organization->endDate = 'ENDDATE';
    $this->Organization->field = 'FIELD';
    $this->Organization->name = 'NAME';
    $this->Organization->salary = 'SALARY';
    $this->Organization->startDate = 'STARTDATE';
    $this->Organization->subField = 'SUBFIELD';
    $this->Organization->title = 'TITLE';
    $this->Organization->webpage = 'WEBPAGE';
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->Organization = null;
    parent::tearDown();
  }

  /**
   * Tests Organization->getAddress()
   */
  public function testGetAddress() {
    $this->assertEquals('ADDRESS', $this->Organization->getAddress());
  }

  /**
   * Tests Organization->getDescription()
   */
  public function testGetDescription() {
    $this->assertEquals('DESCRIPTION', $this->Organization->getDescription());
  }

  /**
   * Tests Organization->getEndDate()
   */
  public function testGetEndDate() {
    $this->assertEquals('ENDDATE', $this->Organization->getEndDate());
  }

  /**
   * Tests Organization->getField()
   */
  public function testGetField() {
    $this->assertEquals('FIELD', $this->Organization->getField());
  }

  /**
   * Tests Organization->getName()
   */
  public function testGetName() {
    $this->assertEquals('NAME', $this->Organization->getName());
  }

  /**
   * Tests Organization->getSalary()
   */
  public function testGetSalary() {
    $this->assertEquals('SALARY', $this->Organization->getSalary());
  }

  /**
   * Tests Organization->getStartDate()
   */
  public function testGetStartDate() {
    $this->assertEquals('STARTDATE', $this->Organization->getStartDate());
  }

  /**
   * Tests Organization->getSubField()
   */
  public function testGetSubField() {
    $this->assertEquals('SUBFIELD', $this->Organization->getSubField());
  }

  /**
   * Tests Organization->getTitle()
   */
  public function testGetTitle() {
    $this->assertEquals('TITLE', $this->Organization->getTitle());
  }

  /**
   * Tests Organization->getWebpage()
   */
  public function testGetWebpage() {
    $this->assertEquals('WEBPAGE', $this->Organization->getWebpage());
  }

  /**
   * Tests Organization->setAddress()
   */
  public function testSetAddress() {
    $this->Organization->setAddress('address');
    $this->assertEquals('address', $this->Organization->address);
  }

  /**
   * Tests Organization->setDescription()
   */
  public function testSetDescription() {
    $this->Organization->setDescription('description');
    $this->assertEquals('description', $this->Organization->description);
  }

  /**
   * Tests Organization->setEndDate()
   */
  public function testSetEndDate() {
    $this->Organization->setEndDate('enddate');
    $this->assertEquals('enddate', $this->Organization->endDate);
  }

  /**
   * Tests Organization->setField()
   */
  public function testSetField() {
    $this->Organization->setField('field');
    $this->assertEquals('field', $this->Organization->field);
  }

  /**
   * Tests Organization->setName()
   */
  public function testSetName() {
    $this->Organization->setName('name');
    $this->assertEquals('name', $this->Organization->name);
  }

  /**
   * Tests Organization->setSalary()
   */
  public function testSetSalary() {
    $this->Organization->setSalary('salary');
    $this->assertEquals('salary', $this->Organization->salary);
  }

  /**
   * Tests Organization->setStartDate()
   */
  public function testSetStartDate() {
    $this->Organization->setStartDate('startdate');
    $this->assertEquals('startdate', $this->Organization->startDate);
  }

  /**
   * Tests Organization->setSubField()
   */
  public function testSetSubField() {
    $this->Organization->setSubField('subfield');
    $this->assertEquals('subfield', $this->Organization->subField);
  }

  /**
   * Tests Organization->setTitle()
   */
  public function testSetTitle() {
    $this->Organization->setTitle('title');
    $this->assertEquals('title', $this->Organization->title);
  }

  /**
   * Tests Organization->setWebpage()
   */
  public function testSetWebpage() {
    $this->Organization->setWebpage('webpage');
    $this->assertEquals('webpage', $this->Organization->webpage);
  }
}
