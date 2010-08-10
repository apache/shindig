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
 * InputJsonConverter test case.
 */
class InputJsonConverterTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var InputJsonConverter
   */
  private $InputJsonConverter;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->InputJsonConverter = new InputJsonConverter();
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->InputJsonConverter = null;
    parent::tearDown();
  }

  /**
   * Tests InputJsonConverter->convertActivities()
   */
  public function testConvertActivities() {
    $json = '{
		"body":"write back!",
		"id":"202",
		"mediaItems":[{"mimeType":"image","type":"image","url":"http:\/\/cdn.davesdaily.com\/pictures\/784-awesome-hands.jpg"}],
		"postedTime":"1217886794",
		"streamTitle":"activities",
		"title":"test title",
		"userId":"1"
		}';
    $activity = $this->InputJsonConverter->convertActivities($json);
    $this->assertEquals('write back!', $activity['body']);
    $this->assertEquals('202', $activity['id']);
    $this->assertEquals('image', $activity['mediaItems'][0]['mimeType']);
    $this->assertEquals('image', $activity['mediaItems'][0]['type']);
    $this->assertEquals('http://cdn.davesdaily.com/pictures/784-awesome-hands.jpg', $activity['mediaItems'][0]['url']);
    $this->assertEquals('1217886794', $activity['postedTime']);
    $this->assertEquals('activities', $activity['streamTitle']);
    $this->assertEquals('test title', $activity['title']);
    $this->assertEquals('1', $activity['userId']);
  }

  /**
   * Tests InputJsonConverter->convertAppData()
   */
  public function testConvertAppData() {
    $json = '{
 		"pokes" : 3,
		"last_poke" : "2008-02-13T18:30:02Z"
		}';
    $appData = $this->InputJsonConverter->convertAppData($json);
    $this->assertEquals('3', $appData['pokes']);
    $this->assertEquals('2008-02-13T18:30:02Z', $appData['last_poke']);
  }

  /**
   * Tests InputJsonConverter->convertMessages()
   */
  public function testConvertMessages() {
    $json = '{
 		"id" : "msgid",
		"title" : "You have an invitation from Joe",
		"body" : "Click here to review your invitation"
		}';
    $message = $this->InputJsonConverter->convertMessages($json);
    file_put_contents('/tmp/message.txt', print_r($json, true));
    $this->assertEquals('msgid', $message['id']);
    $this->assertEquals('You have an invitation from Joe', $message['title']);
    $this->assertEquals('Click here to review your invitation', $message['body']);
  }

  /**
   * Tests InputJsonConverter->convertPeople()
   */
  public function testConvertPeople() {
    $this->setExpectedException(Exception);
    $this->InputJsonConverter->convertPeople();
  }

}

