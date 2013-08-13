<?php
namespace apache\shindig\test\social;
use apache\shindig\social\converters\InputMessagesConverter;

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
 * InputAtomConverter test case.
 */
class InputMessagesConverterTest extends \PHPUnit_Framework_TestCase {

  /**
   * @var InputMessagesConverter
   */
  private $inputConverter;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->inputConverter = new InputMessagesConverter();
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
<entry xmlns="http://www.w3.org/2005/Atom"
         xmlns:osapi="http://opensocial.org/2008/opensocialapi">
  <osapi:recipient>example.org:AD38B3886625AAF</osapi:recipient>
  <osapi:recipient>example.org:997638BAA6F25AD</osapi:recipient>
  <title>You have an invitation from Joe</title>
  <id>{msgid}</id>
  <link rel="alternate" href="http://app.example.org/invites/{msgid}"/>
  <content>Click &lt;a href="http://app.example.org/invites/{msgid}"&gt;here&lt;/a&gt; to review your invitation.</content>
</entry>';
    $message = $this->inputConverter->convertAtom($xml);
    $this->assertEquals('{msgid}', $message['id']);
    $this->assertEquals('You have an invitation from Joe', $message['title']);
    $this->assertEquals('Click <a href="http://app.example.org/invites/{msgid}">here</a> to review your invitation.', $message['body']);
    $this->assertEquals('example.org:AD38B3886625AAF', $message['recipients'][0]);
    $this->assertEquals('example.org:997638BAA6F25AD', $message['recipients'][1]);
  }

  public function testConvertJson() {
    $json = '{
 		"id" : "msgid",
		"title" : "You have an invitation from Joe",
		"body" : "Click here to review your invitation"
		}';
    $message = $this->inputConverter->convertJson($json);
    file_put_contents(sys_get_temp_dir() . '/message.txt', print_r($json, true));
    $this->assertEquals('msgid', $message['id']);
    $this->assertEquals('You have an invitation from Joe', $message['title']);
    $this->assertEquals('Click here to review your invitation', $message['body']);
  }

  public function testConvertXml() {
    $xml = '<?xml version="1.0" encoding="UTF-8"?>
<response xmlns:osapi="http://opensocial.org/2008/opensocialapi">
  <osapi:recipient>example.org:AD38B3886625AAF</osapi:recipient>
  <osapi:recipient>example.org:997638BAA6F25AD</osapi:recipient>
  <title>You have an invitation from Joe</title>
  <id>{msgid}</id>
  <body>Click &lt;a href="http://app.example.org/invites/{msgid}"&gt;here&lt;/a&gt; to review your invitation.</body>
</response>';
    $message = $this->inputConverter->convertXml($xml);
    $this->assertEquals('{msgid}', $message['id']);
    $this->assertEquals('You have an invitation from Joe', $message['title']);
    $this->assertEquals('Click <a href="http://app.example.org/invites/{msgid}">here</a> to review your invitation.', $message['body']);
    $this->assertEquals('example.org:AD38B3886625AAF', $message['recipients'][0]);
    $this->assertEquals('example.org:997638BAA6F25AD', $message['recipients'][1]);
  }
}
