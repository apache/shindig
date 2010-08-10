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
 * OutputXmlConverter test case.
 */
class OutputXmlConverterTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var OutputXmlConverter
   */
  private $OutputXmlConverter;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->OutputXmlConverter = new OutputXmlConverter();
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->OutputXmlConverter = null;
    parent::tearDown();
  }

  /**
   * Tests OutputXmlConverter->outputResponse()
   */
  public function testOutputResponse() {
    $inputConverter = new InputXmlConverter();
    $outputConverter = new OutputXmlConverter();
    $servletRequest = array('url' => '/people/1/@self');
    $token = BasicSecurityToken::createFromValues('owner', 'viewer', 'app', 'domain', 'appUrl', '1', 'default');
    $requestItem = RestRequestItem::createWithRequest($servletRequest, $token, $inputConverter, $outputConverter);
    $requestItem->applyUrlTemplate("/people/{userId}/{groupId}/{personId}");
    $entry = array('isOwner' => false, 'isViewer' => false,
                   'displayName' => '1 1', 'id' => '1');
    $response = array('entry' => $entry);
    $responseItem = new ResponseItem(null, null, $response);
    ob_start();
    $outputConverter->outputResponse($responseItem, $requestItem);
    $output = ob_get_clean();
    $expected = '<?xml version="1.0" encoding="UTF-8"?>
<response>
  <entry>
    <isOwner></isOwner>
    <isViewer></isViewer>
    <displayName>1 1</displayName>
    <id>1</id>
  </entry>
</response>
';
    $outputXml = simplexml_load_string($output);
    $expectedXml = simplexml_load_string($expected);
    $this->assertEquals($expectedXml, $outputXml);
  }

}

