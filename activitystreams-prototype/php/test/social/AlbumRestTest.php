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

class AlbumRestTest extends RestBase {
  
  private function verifyLifeCycle($postData, $postDataFormat) {
    $url = '/albums/1/@self';
    $ret = $this->curlRest($url, $postData, $postDataFormat);
    $album = json_decode($ret, true);
    $album = $album['entry'];
    
    $ret = $this->curlRest($url . '/' . urlencode($album['id']), '', 'application/json', 'GET');
    $this->assertFalse(empty($ret));
    $fetched = json_decode($ret, true);
    $fetched = $fetched['entry'][0];
    $this->assertEquals('Example Album', $fetched['title'], "Title should be same.");
    $this->assertEquals('This is an example album, and this text is an example description', $fetched['description'], "Description should be same.");
    $this->assertEquals('VIDEO', $fetched['mediaType'], "mediaType should be same.");
    
    $fetched['thumbnailUrl'] = 'http://changed.com/tn.png';
    $ret = $this->curlRest($url . '/' . urlencode($album['id']), json_encode($fetched), 'application/json', 'PUT');
    $ret = $this->curlRest($url . '/' . urlencode($album['id']), '', 'application/json', 'GET');
    $this->assertFalse(empty($ret));
    $fetched = json_decode($ret, true);
    $fetched = $fetched['entry'][0];
    $this->assertEquals('http://changed.com/tn.png', $fetched['thumbnailUrl'], "thumbnailUrl should be same.");
    $this->assertEquals('Example Album', $fetched['title'], "Title should be same.");
    $this->assertEquals('This is an example album, and this text is an example description', $fetched['description'], "Description should be same.");
    $this->assertEquals('VIDEO', $fetched['mediaType'], "mediaType should be same.");
        
    $ret = $this->curlRest($url . '/' . urlencode($album['id']), '', 'application/json', 'DELETE');
    $this->assertTrue(empty($ret), "Delete the created album failed. Response: $ret");
    
    $ret = $this->curlRest($url . '/' . urlencode($album['id']), '', 'application/json', 'GET');
    $fetched = json_decode($ret, true);
    $fetched = $fetched['entry'];
    $this->assertTrue(empty($fetched));
  }
  
  public function testLifeCycleInJson() {
    $postData = '{ "id" : "44332211",
       "thumbnailUrl" : "http://pages.example.org/albums/4433221-tn.png",
       "title" : "Example Album",
       "description" : "This is an example album, and this text is an example description",
       "location" : { "latitude": 0, "longitude": 0 },
       "ownerId" : "example.org:55443322",
       "mediaType" : "VIDEO"
    }';
    
    $this->verifyLifeCycle($postData, 'application/json');
  }
  
  public function testLifeCycleInXml() {
    $postData = '<album xmlns="http://ns.opensocial.org/2008/opensocial">
                   <id>44332211</id>
                   <thumbnailUrl>http://pages.example.org/albums/4433221-tn.png</thumbnailUrl>
                   <caption>Example Album</caption>
                   <description>This is an example album, and this text is an example description</description>
                   <location>
                     <latitude>0</latitude>
                     <longitude>0</longitude>
                   </location>
                   <ownerId>example.org:55443322</ownerId>
                   <mediaType>VIDEO</mediaType>
                 </album>';
    $this->verifyLifeCycle($postData, 'application/xml');
  }
  
  public function testLifeCycleInAtom() {
    $postData = '<entry xmlns="http://www.w3.org/2005/Atom">
                 <content type="application/xml">
                   <album xmlns="http://ns.opensocial.org/2008/opensocial">
                     <id>44332211</id>
                     <thumbnailUrl>http://pages.example.org/albums/4433221-tn.png</thumbnailUrl>
                     <caption>Example Album</caption>
                     <description>This is an example album, and this text is an example description</description>
                     <location>
                       <latitude>0</latitude>
                       <longitude>0</longitude>
                     </location>
                     <ownerId>example.org:55443322</ownerId>
                     <mediaType>VIDEO</mediaType>
                   </album>
                 </content>
                 <title/>
                 <updated>2003-12-13T18:30:02Z</updated>
                 <author><url>example.org:55443322</url></author>
                 <id>urn:guid:example.org:44332211</id>
                 </entry>';
    $this->verifyLifeCycle($postData, 'application/atom+xml');
  }
}
