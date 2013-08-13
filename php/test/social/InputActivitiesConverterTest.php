<?php
namespace apache\shindig\test\social;
use apache\shindig\social\converters\InputActivitiesConverter;

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
 * InputActivitiesConverter test case.
 */
class InputActivitiesConverterTest extends \PHPUnit_Framework_TestCase {

  /**
   * @var InputActivitiesConverter
   */
  private $inputConverter;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->inputConverter = new InputActivitiesConverter();
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
    <activity xmlns="http://ns.opensocial.org/2008/opensocial">
        <mediaItems>
          <MediaItem>
            <mimeType>IMAGE</mimeType>
            <type>image</type>
            <url>http://cdn.davesdaily.com/pictures/784-awesome-hands.jpg</url>
            <types>
              <AUDIO>audio</AUDIO>
              <VIDEO>video</VIDEO>
              <IMAGE>image</IMAGE>
            </types>
          </MediaItem>
        </mediaItems>
      <streamTitle>activities</streamTitle>
      <streamId>1</streamId>
      <userId>1</userId>
    </activity>
  </content>
  <author>
    <uri>urn:guid:1</uri>
    <name>api.example.org:1</name>
  </author>
  <category term="status"/>
  <updated>2008-08-05T10:31:04+02:00</updated>
  <id>urn:guid:220</id>
  <title>example title</title>
  <summary>example summary</summary>
</entry>
';
    $activity = $this->inputConverter->convertAtom($xml);
    $this->assertEquals('urn:guid:220', $activity['id']);
    $this->assertEquals('example title', $activity['title']);
    $this->assertEquals('example summary', $activity['body']);
    $this->assertEquals('1', $activity['streamId']);
    $this->assertEquals('activities', $activity['streamTitle']);
    $this->assertEquals('2008-08-05T10:31:04+02:00', $activity['updated']);
    $this->assertEquals('image', $activity['mediaItems'][0]['type']);
    $this->assertEquals('IMAGE', $activity['mediaItems'][0]['mimeType']);
    $this->assertEquals('http://cdn.davesdaily.com/pictures/784-awesome-hands.jpg', $activity['mediaItems'][0]['url']);
  }

  public function testConvertJson() {
    $json = '{
		"body":"write back!",
		"id":"202",
		"mediaItems":[{"mimeType":"image","type":"image","url":"http:\/\/cdn.davesdaily.com\/pictures\/784-awesome-hands.jpg"}],
		"postedTime":"1217886794",
		"streamTitle":"activities",
		"title":"test title",
		"userId":"1"
		}';
    $activity = $this->inputConverter->convertJson($json);
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

  public function testConvertXml() {
    $xml = '<?xml version="1.0" encoding="UTF-8"?>
<response>
  <activity xmlns="http://ns.opensocial.org/2008/opensocial">
      <mediaItems>
        <MediaItem>
          <mimeType>IMAGE</mimeType>
          <type>image</type>
          <url>http://cdn.davesdaily.com/pictures/784-awesome-hands.jpg</url>
          <types>
            <AUDIO>audio</AUDIO>
            <VIDEO>video</VIDEO>
            <IMAGE>image</IMAGE>
          </types>
        </MediaItem>
      </mediaItems>
    <streamTitle>activities</streamTitle>
    <streamId>1</streamId>
    <userId>1</userId>
  </activity>
  <category term="status"/>
  <updated>2008-08-05T10:31:04+02:00</updated>
  <id>urn:guid:220</id>
  <title>example title</title>
  <summary>example summary</summary>
</response>
';
    $activity = $this->inputConverter->convertXml($xml);
    $this->assertEquals('urn:guid:220', $activity['id']);
    $this->assertEquals('example title', $activity['title']);
    $this->assertEquals('example summary', $activity['body']);
    $this->assertEquals('1', $activity['streamId']);
    $this->assertEquals('activities', $activity['streamTitle']);
    $this->assertEquals('2008-08-05T10:31:04+02:00', $activity['updated']);
    $this->assertEquals('image', $activity['mediaItems'][0]['type']);
    $this->assertEquals('IMAGE', $activity['mediaItems'][0]['mimeType']);
    $this->assertEquals('http://cdn.davesdaily.com/pictures/784-awesome-hands.jpg', $activity['mediaItems'][0]['url']);
  }
}
