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
 * OutputJsonConverter test case.
 */
class OutputJsonConverterTest extends PHPUnit_Framework_TestCase {

  /**
   * @var OutputJsonConverter
   */
  private $OutputJsonConverter;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->OutputJsonConverter = new OutputJsonConverter();
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->OutputJsonConverter = null;
    parent::tearDown();
  }

  /**
   * Tests OutputJsonConverter->outputResponse()
   */
  public function testOutputResponse() {
    $inputConverter = new InputJsonConverter();
    $outputConverter = new OutputJsonConverter();
    $servletRequest = array('url' => '/people/1/@self');
    $token = BasicSecurityToken::createFromValues('owner', 'viewer', 'app', 'domain', 'appUrl', '1', 'default');
    $requestItem = RestRequestItem::createWithRequest($servletRequest, $token, $inputConverter, $outputConverter);
    $requestItem->applyUrlTemplate("/people/{userId}/{groupId}/{personId}");
    $response = array(
        'entry' => array('isOwner' => false, 'isViewer' => false, 'displayName' => '1 1', 
            'id' => '1'));
    $responseItem = new ResponseItem(null, null, $response);
    ob_start();
    $outputConverter->outputResponse($responseItem, $requestItem);
    $output = ob_get_clean();
    $expected = '{
        "entry": {
          "isOwner": false,
          "isViewer": false,
          "displayName": "1 1",
          "id": "1"
        }
    }';
    $outputJson = json_decode($output);
    $expectedJson = json_decode($expected);
    $this->assertEquals($expectedJson, $outputJson);
  }

}

