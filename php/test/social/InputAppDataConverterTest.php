<?php
namespace apache\shindig\test\social;
use apache\shindig\social\converters\InputAppDataConverter;

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
 * InputAppDataConverter test case.
 */
class InputAppDataConverterTest extends \PHPUnit_Framework_TestCase {

  /**
   * @var InputAppDataConverter
   */
  private $inputConverter;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->inputConverter = new InputAppDataConverter();
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->inputConverter = null;
    parent::tearDown();
  }

  public function testConvertAtom() {
    $xml = '<?xml version="1.0" encoding="UTF-8"?>
<entry>
    <content type="application/xml">
      <appdata xmlns="http://ns.opensocial.org/2008/opensocial">
        <sign>Virgo</sign>
      </appdata>
    </content>
    <author>
      <uri>urn:guid:1</uri>
      <name>api.example.org:1</name>
    </author>
    <id>urn:guid:1</id>
    <title>appdata id 1</title>
    <updated>2008-08-06T22:36:20+02:00</updated>
  </entry>';
    $appdata = $this->inputConverter->convertAtom($xml);
    $expect = array('sign' => 'Virgo');
    $this->assertEquals($expect, $appdata);
  }

  public function testConvertJson() {
    $json = '{
 		"pokes" : 3,
		"last_poke" : "2008-02-13T18:30:02Z"
		}';
    $appData = $this->inputConverter->convertJson($json);
    $this->assertEquals('3', $appData['pokes']);
    $this->assertEquals('2008-02-13T18:30:02Z', $appData['last_poke']);
  }

  public function testConvertXml() {
    $xml = '<?xml version="1.0" encoding="UTF-8"?>
<response>
  <entry>
    <key>sign</key>
    <value>Virgo</value>
  </entry>
</response>';
    $appdata = $this->inputConverter->convertXml($xml);
    $expect = array('sign' => 'Virgo');
    $this->assertEquals($expect, $appdata);
  }
}