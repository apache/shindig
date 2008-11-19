<?php
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
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
	protected function setUp()
	{
		parent::setUp();
		$this->OutputAtomConverter = new OutputAtomConverter();	
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->OutputAtomConverter = null;		
		parent::tearDown();
	}

	/**
	 * Tests OutputAtomConverter->outputResponse()
	 */
	public function testOutputResponse()
	{
		$inputConverter = new InputAtomConverter();
		$outputConverter = new OutputAtomConverter();
		$servletRequest = array('url' => '/people/1/@self');
		$requestItem = RestRequestItem::createWithRequest($servletRequest, null,
		    $inputConverter, $outputConverter);
		$requestItem->applyUrlTemplate("/people/{userId}/{groupId}/{personId}");
		$response = array( 'entry' => array('isOwner' => false,
		    'isViewer' => false, 'displayName' => '1 1', 'id' => '1'));
		$responseItem = new ResponseItem(null, null, $response);
		ob_start();
		$outputConverter->outputResponse($responseItem, $requestItem);
		$output = ob_get_clean();
		$expected = '<?xml version="1.0" encoding="UTF-8"?>
<entry xmlns="http://www.w3.org/2005/Atom">
  <title>person entry for shindig:1</title>
  <author>
    <uri>urn:guid:1</uri>
    <name>shindig:1</name>
  </author>
  <id>urn:guid:1</id>
  <updated>2008-11-17T11:24:39-08:00</updated>
  <content type="application/xml">
    <person xmlns="http://ns.opensocial.org/2008/opensocial">
      <entry>
        <isOwner></isOwner>
        <isViewer></isViewer>
        <displayName>1 1</displayName>
        <id>1</id>
      </entry>
    </person>
  </content>
</entry>
';
		$outputXml = simplexml_load_string($output);
		$expectedXml = simplexml_load_string($expected);
		$expectedXml->updated = $outputXml->updated;
		$this->assertEquals($expectedXml, $outputXml);
	}

}

