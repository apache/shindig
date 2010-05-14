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

class AppDataRestTest extends RestBase {
  
  public function testAppDataLifeCycleInJson() {
    $postData = '{
      "pokes" : 4,
      "last_poke" : "2008-06-13T18:30:02Z"
    }';
    // Creates the app data.
    $ret = $this->curlRest('/appdata/1/@self/1', $postData, 'application/json');
    $this->assertTrue(empty($ret), "Create app data failed. $ret.");
    // Verifies data was written correctly
    $ret = $this->curlRest('/appdata/1/@self/1?fields=pokes,last_poke', '', 'application/json', 'GET');
    $retDecoded = json_decode($ret, true);
    $this->assertTrue($ret != $retDecoded && $ret != null, "Invalid json string in return: $ret.");
    $this->assertTrue(isset($retDecoded['entry']) && isset($retDecoded['entry'][1])
        && isset($retDecoded['entry'][1]['last_poke']) && isset($retDecoded['entry'][1]['pokes'])
        && $retDecoded['entry'][1]['last_poke'] == '2008-06-13T18:30:02Z'
        && $retDecoded['entry'][1]['pokes'] == '4', "Unexpected return value: $ret.");
    // Deletes the app data.
    $ret = $this->curlRest('/appdata/1/@self/1?fields=pokes,last_poke', '', 'application/json', 'DELETE');
    $this->assertTrue(empty($ret), "Delete app data failed. $ret");       
  }
  
  public function testAppDataLifeCycleInXml() {
    $postData = '<appdata xmlns="http://ns.opensocial.org/2008/opensocial">
        <entry>
          <key>pokes</key>
          <value>1</value>
        </entry>
        <entry>
          <key>last_poke</key>
          <value>2008-02-13T18:30:02Z</value>
        </entry>
      </appdata>';
    // Creates or update the app data.
    $ret = $this->curlRest('/appdata/1/@self/1', $postData, 'application/xml');
    $this->assertTrue(empty($ret), "Create app data failed. $ret");
    
    // Verifies data was written correctly.
    $ret = $this->curlRest('/appdata/1/@self/1?fields=pokes,last_poke', '', 'application/json', 'GET');
    $retDecoded = json_decode($ret, true);
    $this->assertTrue($ret != $retDecoded && $ret != null, "Invalid json string in return: $ret.");
    $this->assertTrue(isset($retDecoded['entry']) && isset($retDecoded['entry'][1])
        && isset($retDecoded['entry'][1]['last_poke']) && isset($retDecoded['entry'][1]['pokes'])
        && $retDecoded['entry'][1]['last_poke'] == '2008-02-13T18:30:02Z'
        && $retDecoded['entry'][1]['pokes'] == '1', "Unexpected return value: $ret.");
        
    // Updates the app data.
    $updateData = '<appdata xmlns="http://ns.opensocial.org/2008/opensocial">
        <entry>
          <key>pokes</key>
          <value>100</value>
        </entry>
        <entry>
          <key>last_poke</key>
          <value>2009-02-13T18:30:02Z</value>
        </entry>
      </appdata>';
    $ret = $this->curlRest('/appdata/1/@self/1', $updateData, 'application/xml');
    $this->assertTrue(empty($ret), "Update app data failed. $ret");
    
    // Verifies data was written correctly.
    $ret = $this->curlRest('/appdata/1/@self/1?fields=pokes,last_poke', '', 'application/json', 'GET');
    $retDecoded = json_decode($ret, true);
    $this->assertTrue($ret != $retDecoded && $ret != null, "Invalid json string in return: $ret.");
    $this->assertTrue(isset($retDecoded['entry']) && isset($retDecoded['entry'][1])
        && isset($retDecoded['entry'][1]['last_poke']) && isset($retDecoded['entry'][1]['pokes'])
        && $retDecoded['entry'][1]['last_poke'] == '2009-02-13T18:30:02Z'
        && $retDecoded['entry'][1]['pokes'] == '100', "Unexpected return value: $ret.");
    
    // Deletes the app data.
    $ret = $this->curlRest('/appdata/1/@self/1?fields=pokes,last_poke', '', 'application/json', 'DELETE');
    $this->assertTrue(empty($ret), "Delete app data failed. $ret");        
  }
  
  public function testAppDataLifeCycleInAtom() {
    $postData = '<entry xmlns="http://www.w3.org/2005/Atom">
      <content type="text/xml">
        <appdata xmlns="http://opensocial.org/2008/opensocial">  
            <pokes>2</pokes>
            <last_poke>2003-12-14T18:30:02Z</last_poke>
          </appdata>
      </content>
      <title/>
      <updated>2003-12-14T18:30:02Z</updated>
      <author><url>urn:guid:example.org:34KJDCSKJN2HHF0DW20394</url></author>
      <id>urn:guid:example.org:34KJDCSKJN2HHF0DW20394</id>
    </entry>';
    // Creates the app data.
    $ret = $this->curlRest('/appdata/1/@self/1', $postData, 'application/atom+xml');
    $this->assertTrue(empty($ret), "Create app data failed. $ret");
    // Verifies data was written correctly
    $ret = $this->curlRest('/appdata/1/@self/1?fields=pokes,last_poke', '', 'application/json', 'GET');
    $retDecoded = json_decode($ret, true);
    $this->assertTrue($ret != $retDecoded && $ret != null, "Invalid json string in return: $ret.");
    $this->assertTrue(isset($retDecoded['entry']) && isset($retDecoded['entry'][1])
        && isset($retDecoded['entry'][1]['last_poke']) && isset($retDecoded['entry'][1]['pokes'])
        && $retDecoded['entry'][1]['last_poke'] == '2003-12-14T18:30:02Z'
        && $retDecoded['entry'][1]['pokes'] == '2', "Unexpected return value: $ret\n");
    // Deletes the app data.
    $ret = $this->curlRest('/appdata/1/@self/1?fields=pokes,last_poke', '', 'application/json', 'DELETE');
    $this->assertTrue(empty($ret), "Delete app data failed. $ret");       
  }
}
