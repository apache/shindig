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
 * Address test case.
 */
class AddressTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var Address
   */
  private $Address;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->Address = new Address('UNSTRUCTUREDADDRESS');
    $this->Address->country = 'COUNTRY';
    $this->Address->extendedAddress = 'EXTENDEDADDRESS';
    $this->Address->latitude = 'LATITUDE';
    $this->Address->longitude = 'LONGITUDE';
    $this->Address->locality = 'LOCALITY';
    $this->Address->poBox = 'POBOX';
    $this->Address->postalCode = 'POSTALCODE';
    $this->Address->region = 'REGION';
    $this->Address->streetAddress = 'STREETADDRESS';
    $this->Address->type = 'TYPE';
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->Address = null;
    parent::tearDown();
  }

  /**
   * Tests Address->getCountry()
   */
  public function testGetCountry() {
    $this->assertEquals('COUNTRY', $this->Address->getCountry());
  }

  /**
   * Tests Address->getLatitude()
   */
  public function testGetLatitude() {
    $this->assertEquals('LATITUDE', $this->Address->getLatitude());
  }

  /**
   * Tests Address->getLocality()
   */
  public function testGetLocality() {
    $this->assertEquals('LOCALITY', $this->Address->getLocality());
  }

  /**
   * Tests Address->getLongitude()
   */
  public function testGetLongitude() {
    $this->assertEquals('LONGITUDE', $this->Address->getLongitude());
  }

  /**
   * Tests Address->getPostalCode()
   */
  public function testGetPostalCode() {
    $this->assertEquals('POSTALCODE', $this->Address->getPostalCode());
  }

  /**
   * Tests Address->getRegion()
   */
  public function testGetRegion() {
    $this->assertEquals('REGION', $this->Address->getRegion());
  }

  /**
   * Tests Address->getStreetAddress()
   */
  public function testGetStreetAddress() {
    $this->assertEquals('STREETADDRESS', $this->Address->getStreetAddress());
  }

  /**
   * Tests Address->getType()
   */
  public function testGetType() {
    $this->assertEquals('TYPE', $this->Address->getType());
  }

  /**
   * Tests Address->getFormatted()
   */
  public function testGetFormatted() {
    $this->assertEquals('UNSTRUCTUREDADDRESS', $this->Address->getFormatted());
  }

  /**
   * Tests Address->setCountry()
   */
  public function testSetCountry() {
    $this->Address->setCountry('country');
    $this->assertEquals('country', $this->Address->getCountry());
  }

  /**
   * Tests Address->setLatitude()
   */
  public function testSetLatitude() {
    $this->Address->setLatitude('latitude');
    $this->assertEquals('latitude', $this->Address->getLatitude());
  }

  /**
   * Tests Address->setLocality()
   */
  public function testSetLocality() {
    $this->Address->setLocality('locality');
    $this->assertEquals('locality', $this->Address->getLocality());
  }

  /**
   * Tests Address->setLongitude()
   */
  public function testSetLongitude() {
    $this->Address->setLongitude('longitude');
    $this->assertEquals('longitude', $this->Address->getLongitude());
  }

  /**
   * Tests Address->setPostalCode()
   */
  public function testSetPostalCode() {
    $this->Address->setPostalCode('postalcode');
    $this->assertEquals('postalcode', $this->Address->getPostalCode());
  }

  /**
   * Tests Address->setRegion()
   */
  public function testSetRegion() {
    $this->Address->setRegion('religion');
    $this->assertEquals('religion', $this->Address->getRegion());
  }

  /**
   * Tests Address->setStreetAddress()
   */
  public function testSetStreetAddress() {
    $this->Address->setStreetAddress('streetaddress');
    $this->assertEquals('streetaddress', $this->Address->getStreetAddress());
  }

  /**
   * Tests Address->setType()
   */
  public function testSetType() {
    $this->Address->setType('type');
    $this->assertEquals('type', $this->Address->getType());
  }

  /**
   * Tests Address->setFormatted()
   */
  public function testSetFormatted() {
    $this->Address->setFormatted('unstructuredaddress');
    $this->assertEquals('unstructuredaddress', $this->Address->getFormatted());
  }
}
