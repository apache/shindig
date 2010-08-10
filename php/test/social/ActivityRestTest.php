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

class ActivityRestTest extends RestBase {

  private function verifyLifeCycle($postData, $postDataFormat, $randomTitle) {
    $url = '/activities/1/@self';
    $ret = $this->curlRest($url, $postData, $postDataFormat);
    $this->assertTrue(empty($ret), "Create activity failed. Response: $ret");
    
    // Verifyies data was written correctly
    $ret = $this->curlRest($url . '?count=20', '', 'application/json', 'GET');
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
    $ret = $this->curlRest($url . "/@app/$activityId", '', 'application/json', 'DELETE');
    $this->assertTrue(empty($ret), "Delete activity failed. Repsonse: $ret");
  }
  
  public function testLifeCycleInJson() {
    $randomTitle = "[" . rand(0, 2048) . "] test activity";
    $postData = '{
      "id" : "http://example.org/activities/example.org:87ead8dead6beef/self/af3778",
      "title" : "' . $randomTitle . '",
      "updated" : "2008-02-20T23:35:37.266Z",
      "body" : "Some details for some activity",
      "bodyId" : "383777272",
      "url" : "http://api.example.org/activity/feeds/.../af3778",
      "userId" : "example.org:34KJDCSKJN2HHF0DW20394"
    }';
    $this->verifyLifeCycle($postData, 'application/json', $randomTitle);
  }
  
  public function testLifeCycleInAtom() {
    $randomTitle = "[" . rand(0, 2048) . "] test activity";
    $postData = '<entry xmlns="http://www.w3.org/2005/Atom">
        <category term="status"/>
        <id>http://example.org/activities/example.org:87ead8dead6beef/self/af3778</id>
        <title>' . $randomTitle . '</title>
        <summary>Some details for some activity</summary>
        <updated>2008-02-20T23:35:37.266Z</updated>
        <link rel="self" type="application/atom+xml" href="http://api.example.org/activity/feeds/.../af3778"/>
        <author><uri>urn:guid:example.org:34KJDCSKJN2HHF0DW20394</uri></author>
        <content>
          <activity xmlns="http://ns.opensocial.org/2008/opensocial">
            <bodyId>383777272</bodyId>
          </activity>
        </content>
      </entry>';
    $this->verifyLifeCycle($postData, 'application/atom+xml', $randomTitle);
  }
  
  public function testLifeCycleInXml() {
    $randomTitle = "[" . rand(0, 2048) . "] test activity";
    $postData = '<activity xmlns="http://ns.opensocial.org/2008/opensocial">
      <id>http://example.org/activities/example.org:87ead8dead6beef/self/af3778</id>
      <title>' . $randomTitle . '</title>
      <updated>2008-02-20T23:35:37.266Z</updated>
      <body>Some details for some activity</body>
      <bodyId>383777272</bodyId>
      <url>http://api.example.org/activity/feeds/.../af3778</url>
      <userId>example.org:34KJDCSKJN2HHF0DW20394</userId>
      </activity>';
    $this->verifyLifeCycle($postData, 'application/xml', $randomTitle);
  }
}

