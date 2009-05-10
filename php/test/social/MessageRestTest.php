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

require_once 'RestBase.php';

class MessageRestTest extends RestBase {
	
  private function getMessages($url) {
    $ret = $this->curlRest($url, '', 'application/json', 'GET');
    $retDecoded = json_decode($ret, true);
    $this->assertTrue($ret != $retDecoded && $ret != null, "Invalid json response: $retDecoded");
    return $retDecoded['entry'];
  }
  
  private function verifyLifeCycle($postData, $postDataFormat, $randomTitle) {
    $url = '/messages/1/notification';
    
    $cnt = count($this->getMessages($url));
    
    // Creates the message.
    $ret = $this->curlRest($url, $postData, $postDataFormat, 'POST');
    $this->assertTrue(empty($ret), "Create message failed. Response: $ret");
    
    // Gets the message.
    $messages = $this->getMessages($url);
    $this->assertEquals($cnt + 1, count($messages), "Size of the messages is not right.");
    $fetchedMessage = null;
    foreach ($messages as $m) {
      if ($m['title'] == $randomTitle) {
        $fetchedMessage = $m;
      }
    }
    $this->assertNotNull($fetchedMessage, "Couldn't find the created message with title $randomTitle");
    
    // Deletes the message.
    $ret = $this->curlRest($url . '/' . urlencode($fetchedMessage['id']), '', 'application/json', 'DELETE');
    $this->assertTrue(empty($ret), "Delete the created message failed. Response: $ret");
    
    $messages = $this->getMessages($url, $randomTitle);
    $this->assertEquals($cnt, count($messages), "Size of the messages is not right after deletion.");
  }
  
  public function testLifeCycleInJson() {
    $randomTitle = "[" . rand(0, 2048) . "] message test title.";
    $postData = '{
      "id" : "msgid",
      "recipients" : [1, 2, 3],
      "title" : "' . $randomTitle . '",
      "titleId" : "541141091700",
      "body" : "Short message from Joe to some friends",
      "bodyId" : "5491155811231",
      "type" : "privateMessage",
      "status" : "unread"
    }';
    $this->verifyLifeCycle($postData, 'application/json', $randomTitle);
  }

  public function testLifeCycleInXml() {
    $randomTitle = "[" . rand(0, 2048) . "] message test title.";
    $postData = '<message xmlns="http://ns.opensocial.org/2008/opensocial">
      <recipient>1</recipient>
      <recipient>2</recipient>
      <recipient>3</recipient>
      <title>' . $randomTitle . '</title>
      <id>msgid</id>
      <body>Click <a href="http://app.example.org/invites/{msgid}">here</a> to review your invitation.</body>
    </message>';
    $this->verifyLifeCycle($postData, 'application/xml', $randomTitle);
  }
  
  public function testLifeCycleInAtom() {
    $randomTitle = "[" . rand(0, 2048) . "] message test title.";
    $postData = '<entry xmlns="http://www.w3.org/2005/Atom"
             xmlns:osapi="http://opensocial.org/2008/opensocialapi">
      <osapi:recipient>1</osapi:recipient>
      <osapi:recipient>2</osapi:recipient>
      <osapi:recipient>3</osapi:recipient>
      <title>' . $randomTitle . '</title>
      <id>{msgid}</id>
      <link rel="alternate" href="http://app.example.org/invites/{msgid}"/>
      <content>Click <a href="http://app.example.org/invites/{msgid}">here</a> to review your invitation.</content>
    </entry>';
    $this->verifyLifeCycle($postData, 'application/atom+xml', $randomTitle);
  }
  
  public function testMessageCollectionLifeCycle() {
    $url = '/messages/1';
    $randomTitle = "[" . rand(0, 2048) . "] message collection test title.";
    
    $ret = $this->curlRest($url, '', 'application/json', 'GET');
    $retDecoded = json_decode($ret, true);
    $this->assertTrue($ret != $retDecoded && $ret != null, "Invalid json response: $retDecoded");
    $cnt = count($retDecoded['entry']);
    
    $id = 'msgCollId';
    
    $postData = '{
      "id" : "' . $id . '",
      "title" : "' . $randomTitle . '"
    }';
    $ret = $this->curlRest($url, $postData, 'application/json', 'POST');
    $this->assertTrue(empty($ret), "Create message collection failed. Response: $ret");

    $ret = $this->curlRest($url, '', 'application/json', 'GET');
    $retDecoded = json_decode($ret, true);
    $this->assertTrue($ret != $retDecoded && $ret != null, "Invalid json response: $retDecoded");
    $this->assertEquals($cnt + 1, count($retDecoded['entry']), "Wrong size of the collections. $ret");
    
    $ret = $this->curlRest($url . "/$id", '', 'application/json', 'DELETE');
    
    $ret = $this->curlRest($url, '', 'application/json', 'GET');
    $retDecoded = json_decode($ret, true);
    $this->assertTrue($ret != $retDecoded && $ret != null, "Invalid json response: $retDecoded");
    $this->assertEquals($cnt, count($retDecoded['entry']), "Wrong size of the collections. $ret");
  }
}

