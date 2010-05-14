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
 * RestRequestItem test case.
 */
class RestRequestItemTest extends PHPUnit_Framework_TestCase {

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    parent::tearDown();
  }

  /**
   * Tests RestRequestItem->createWithRequest()
   */
  public function testCreateWithRequest() {
    $expectedParams = array('oauth_nonce' => '10075052d8a3cd0087d11346edba8f1f',
                            'oauth_timestamp' => '1242011332',
                            'oauth_consumer_key' => 'consumerKey',
                            'fields' => 'gender,name',
                            'oauth_signature_method' => 'HMAC-SHA1',
                            'oauth_signature' => 'wDcyXTBqhxW70G+ddZtw7zPVGyE=');
    $urlencodedParams = array();
    foreach ($expectedParams as $key => $value) {
      $urlencodedParams[] = $key . '=' . urlencode($value);
    }
    $url = '/people/1/@self?' . join('&', $urlencodedParams);
    $inputConverter = new InputJsonConverter();
    $outputConverter = new OutputJsonConverter();
    $servletRequest = array('url' => $url);
    $token = BasicSecurityToken::createFromValues('owner', 'viewer', 'app', 'domain', 'appUrl', '1', 'default');
    $requestItem = RestRequestItem::createWithRequest($servletRequest, $token, $inputConverter, $outputConverter);
    $params = $requestItem->getParameters();
    $this->assertEquals($expectedParams, $params);
  }

}
