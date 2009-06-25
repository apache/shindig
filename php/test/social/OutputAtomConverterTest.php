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
 * OutputAtomConverter test case.
 */
class OutputAtomConverterTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var OutputAtomConverter
   */
  private $OutputAtomConverter;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->OutputAtomConverter = new OutputAtomConverter();
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->OutputAtomConverter = null;
    parent::tearDown();
  }

  /**
   * Tests OutputAtomConverter->outputResponse()
   */
  public function testOutputResponse() {
    $inputConverter = new InputAtomConverter();
    $outputConverter = new OutputAtomConverter();
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
    $expected = '<entry xmlns="http://www.w3.org/2005/Atom">
  <title>person entry for shindig:1</title>
  <author>
    <uri>urn:guid:1</uri>
    <name>shindig:1</name>
  </author>
  <id>urn:guid:1</id>
  <updated>2008-12-11T19:58:31+01:00</updated>
  <content type="application/xml">
    <entry xmlns="http://ns.opensocial.org/2008/opensocial">
      <isOwner></isOwner>
      <isViewer></isViewer>
      <displayName>1 1</displayName>
      <id>1</id>
    </entry>
  </content>
</entry>
';
    $outputXml = simplexml_load_string($output);
    $expectedXml = simplexml_load_string($expected);
    $expectedXml->updated = $outputXml->updated;
    // Prefix may be 'shindig' or something else.
    $expectedXml->title = $outputXml->title; 
    $expectedXml->author->name = $outputXml->author->name;
    $this->assertEquals($expectedXml, $outputXml);
  }

}

