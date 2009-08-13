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
  
  protected function setUp() {
    $postData = '{ "id" : "44332211",
       "thumbnailUrl" : "http://pages.example.org/albums/4433221-tn.png",
       "title" : "Example Album",
       "description" : "This is an example album, and this text is an example description",
       "location" : { "latitude": 0, "longitude": 0 },
       "ownerId" : "example.org:55443322",
       "mediaType" : "VIDEO"
    }';
    
    $url = '/albums/1/@self';
    $ret = $this->curlRest($url, $postData, 'application/json');
    $this->assertFalse(empty($ret));
    $album = json_decode($ret, true);
    $this->album = $album['entry'];
  }
  
  protected function tearDown() {
//    $url = '/albums/1/@self';
//    $ret = $this->curlRest($url . '/' . urlencode($this->album['id']), '', 'application/json', 'DELETE');
//    $this->assertTrue(empty($ret), "Delete the created album failed. Response: $ret");
  }
  
  private function curlGet($url) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_HEADER, 0);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    $ret = curl_exec($ch);
    curl_close($ch);
    return $ret;
  }
  
  private function verifyLifeCycle($postData, $postDataFormat) {
    $url = '/mediaitems/1/@self/' . $this->album['id'];
    $ret = $this->curlRest($url, $postData, $postDataFormat);
    $mediaItem = json_decode($ret, true);
    $mediaItem = $mediaItem['entry'];
    
    $ret = $this->curlRest($url . '/' . urlencode($mediaItem['id']), '', 'application/json', 'GET');
    $this->assertFalse(empty($ret));
    $fetched = json_decode($ret, true);
    $fetched = $fetched['entry'][0];
    $data = $this->curlGet($fetched['url']);
    $this->assertTrue(substr($data, 0, strlen('GIF')) == 'GIF');
    $this->assertEquals('http://pages.example.org/images/11223344-tn.png', $fetched['thumbnailUrl'], "thumbnailUrl should be same.");
    $this->assertEquals('image/gif', $fetched['mimeType'], "mimeType should be same.");
    $this->assertEquals('IMAGE', $fetched['type'], "type should be same.");
    $fetched['thumbnailUrl'] = 'http://changed.com/tn.png';
    $ret = $this->curlRest($url . '/' . urlencode($mediaItem['id']), json_encode($fetched), 'application/json', 'PUT');
    $ret = $this->curlRest($url . '/' . urlencode($mediaItem['id']), '', 'application/json', 'GET');
    $this->assertFalse(empty($ret));
    $fetched = json_decode($ret, true);
    $fetched = $fetched['entry'][0];
    $this->assertEquals('http://changed.com/tn.png', $fetched['thumbnailUrl'], "thumbnailUrl should be same.");
    $this->assertEquals('image/gif', $fetched['mimeType'], "mimeType should be same.");
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
                   "url" : "http://www.google.com/intl/en_ALL/images/logo.gif",
                   "albumId" : "' . $this->album['id'] . '"
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
                   <url>http://www.google.com/intl/en_ALL/images/logo.gif</url>
                   <albumId>' . $this->album['id'] . '</albumId>
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
                       <url>http://www.google.com/intl/en_ALL/images/logo.gif</url>
                       <albumId>' . $this->album['id'] . '</albumId>
                     </mediaItem>
                   </content>
                   <title/>
                   <updated>2003-12-13T18:30:02Z</updated>
                   <author><url>example.org:55443322</url></author>
                   <id>urn:guid:example.org:11223344</id>
                 </entry>';
    $this->verifyLifeCycle($postData, 'application/atom+xml');
  }
  
  public function testLifeCycleWithActivity() {
    // Creates the media item.
    $postData = '{ "id" : "11223344",
               "thumbnailUrl" : "http://pages.example.org/images/11223344-tn.png",
               "mimeType" : "image/png",
               "type" : "image",
               "url" : "http://pages.example.org/images/11223344.png",
               "albumId" : "' . $this->album['id'] . '"
             }';
    $url = '/mediaitems/1/@self/' . $this->album['id'];
    $ret = $this->curlRest($url, $postData, 'application/json');
    $mediaItem = json_decode($ret, true);
    $mediaItem = $mediaItem['entry'];
    // Creates the activity.
    $activityUrl = '/activities/1/@self';
    $randomTitle = "[" . rand(0, 2048) . "] test activity";
    $postData = '{
      "id" : "http://example.org/activities/example.org:87ead8dead6beef/self/af3778",
      "title" : "' . $randomTitle . '",
      "updated" : "2008-02-20T23:35:37.266Z",
      "body" : "Some details for some activity",
      "bodyId" : "383777272",
      "url" : "http://api.example.org/activity/feeds/.../af3778",
      "userId" : "example.org:34KJDCSKJN2HHF0DW20394",
      "mediaItems" : [ {
          "id": ' . $mediaItem['id'] . ',
          "albumId": "' . $mediaItem['albumId'] . '"
        }
      ]
    }';
    $ret = $this->curlRest($activityUrl, $postData, 'application/json');
    $this->assertTrue(empty($ret), "Create activity failed. Response: $ret");
    // Verifyies data was written correctly
    $ret = $this->curlRest($activityUrl . '?count=20', '', 'application/json', 'GET');
    $retDecoded = json_decode($ret, true);
    $this->assertTrue($ret != $retDecoded && $ret != null, "Invalid json string in return: $ret");
    // Sees if we can find our just created activity
    $activityId = null;
    foreach ($retDecoded['entry'] as $entry) {
      if ($entry['title'] == $randomTitle) {
        $activityId = $entry['id'];
        break;
      }
    }
    $this->assertNotNull($activityId, "Couldn't find created activity.");

    
    $ret = $this->curlRest($activityUrl . "/@app/$activityId", '', 'application/json', 'DELETE');
    $this->assertTrue(empty($ret), "Delete activity failed. Repsonse: $ret");
    
    $ret = $this->curlRest($url . '/' . urlencode($mediaItem['id']), '', 'application/json', 'DELETE');
    $this->assertTrue(empty($ret), "Delete the created mediaItem failed. Response: $ret");
  }
}