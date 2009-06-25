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

class MediaItemRestTest extends RestBase {
  
  private function verifyLifeCycle($postData, $postDataFormat) {
    $url = '/mediaitems/1/@self/44332211';
    $ret = $this->curlRest($url, $postData, $postDataFormat);
    $mediaItem = json_decode($ret, true);
    $mediaItem = $mediaItem['entry'];
    
    $ret = $this->curlRest($url . '/' . urlencode($mediaItem['id']), '', 'application/json', 'GET');
    $this->assertFalse(empty($ret));
    $fetched = json_decode($ret, true);
    $fetched = $fetched['entry'][0];
    $this->assertEquals('http://pages.example.org/images/11223344.png', $fetched['url'], "url should be same.");
    $this->assertEquals('http://pages.example.org/images/11223344-tn.png', $fetched['thumbnailUrl'], "thumbnailUrl should be same.");
    $this->assertEquals('image/png', $fetched['mimeType'], "mimeType should be same.");
    $this->assertEquals('IMAGE', $fetched['type'], "type should be same.");
    
    $fetched['thumbnailUrl'] = 'http://changed.com/tn.png';
    $ret = $this->curlRest($url . '/' . urlencode($mediaItem['id']), json_encode($fetched), 'application/json', 'PUT');
    $ret = $this->curlRest($url . '/' . urlencode($mediaItem['id']), '', 'application/json', 'GET');
    $this->assertFalse(empty($ret));
    $fetched = json_decode($ret, true);
    $fetched = $fetched['entry'][0];
    $this->assertEquals('http://pages.example.org/images/11223344.png', $fetched['url'], "url should be same.");
    $this->assertEquals('http://changed.com/tn.png', $fetched['thumbnailUrl'], "thumbnailUrl should be same.");
    $this->assertEquals('image/png', $fetched['mimeType'], "mimeType should be same.");
    $this->assertEquals('IMAGE', $fetched['type'], "type should be same.");
    
    $ret = $this->curlRest($url . '/' . urlencode($mediaItem['id']), '', 'application/json', 'DELETE');
    $this->assertTrue(empty($ret), "Delete the created mediaItem failed. Response: $ret");
    
    $ret = $this->curlRest($url . '/' . urlencode($mediaItem['id']), '', 'application/json', 'GET');
    $fetched = json_decode($ret, true);
    $fetched = $fetched['entry'];
    $this->assertTrue(empty($fetched));
  }
  
  public function testLifeCycleInJson() {
    $postData = '{ "id" : "11223344",
                   "thumbnailUrl" : "http://pages.example.org/images/11223344-tn.png",
                   "mimeType" : "image/png",
                   "type" : "image",
                   "url" : "http://pages.example.org/images/11223344.png",
                   "albumId" : "44332211"
                 }';
    $this->verifyLifeCycle($postData, 'application/json');
  }
  
  public function testLifeCycleInXml() {
    $postData = '<?xml version="1.0" encoding="UTF-8"?>
                 <mediaItem xmlns="http://ns.opensocial.org/2008/opensocial">
                   <id>11223344</id>
                   <thumbnailUrl>http://pages.example.org/images/11223344-tn.png</thumbnailUrl>
                   <mimeType>image/png</mimeType>
                   <type>image</type>
                   <url>http://pages.example.org/images/11223344.png</url>
                   <albumId>44332211</albumId>
                 </mediaItem>';
    $this->verifyLifeCycle($postData, 'application/xml');
  }
  
  public function testLifeCycleInAtom() {
    $postData = '<entry xmlns="http://www.w3.org/2005/Atom">
                   <content type="application/xml">
                     <mediaItem xmlns="http://ns.opensocial.org/2008/opensocial">
                       <id>11223344</id>
                       <thumbnailUrl>http://pages.example.org/images/11223344-tn.png</thumbnailUrl>
                       <mimeType>image/png</mimeType>
                       <type>image</type>
                       <url>http://pages.example.org/images/11223344.png</url>
                       <albumId>44332211</albumId>
                     </mediaItem>
                   </content>
                   <title/>
                   <updated>2003-12-13T18:30:02Z</updated>
                   <author><url>example.org:55443322</url></author>
                   <id>urn:guid:example.org:11223344</id>
                 </entry>';
    $this->verifyLifeCycle($postData, 'application/atom+xml');
  }
}