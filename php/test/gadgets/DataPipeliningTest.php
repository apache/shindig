<?php
namespace apache\shindig\test\gadgets;
use apache\shindig\gadgets\templates\DataPipelining;

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
 * ContainerConfig test case.
 */
class DataPipeliningTest extends \PHPUnit_Framework_TestCase {
  /**
   * @var Gadget
   */
  private $viewNode = '<?xml version="1.0" encoding="UTF-8" ?>
    <script xmlns:os="http://ns.opensocial.org/2008/markup" type="text/os-data">
        <os:PeopleRequest key="viewer" userId="@viewer" groupId="@self"/>
        <os:PeopleRequest key="viewerFriends" userId="@viewer" groupId="@friends" foo="bar"/>
        <os:ViewerRequest />
        <os:OwnerRequest />
        <os:PersonAppDataRequest key="appdata" userId="@viewer" fields="field" />
        <os:PersonAppDataRequest key="appdataFriends" userId="@viewer" groupId="@friends" fields="field" />
        <os:ActivitiesRequest />
        <os:HttpRequest href="http://example.com" />
    </script>
    ';

  public function testParse() {
      $doc = new \DomDocument();
      $doc->loadXml($this->viewNode);
      $contentBlocks = $doc->getElementsByTagName('script');
      $tags = array();
      foreach ($contentBlocks as $content) {
        $tags[] = DataPipelining::parse($content);
      }
      $this->assertEquals(1, count($tags));

      $expected = array(
          array(
              'type' => 'os:DataRequest',
              'key' => 'viewer',
              'userId' => '@viewer',
              'groupId' => '@self',
              'method' => 'people.get',
          ),
          array(
              'type' => 'os:DataRequest',
              'key' => 'viewerFriends',
              'userId' => '@viewer',
              'groupId' => '@friends',
              'method' => 'people.get',
          ),
          array(
              'type' => 'os:DataRequest',
              'method' => 'people.get',
              'userId' => '@viewer',
              'groupId' => '@self',
          ),
          array(
              'type' => 'os:DataRequest',
              'method' => 'people.get',
              'userId' => '@owner',
              'groupId' => '@self',
          ),
          array(
              'type' => 'os:DataRequest',
              'key' => 'appdata',
              'userId' => '@viewer',
              'fields' => 'field',
              'method' => 'appdata.get',
          ),
          array(
              'type' => 'os:DataRequest',
              'key' => 'appdataFriends',
              'userId' => '@viewer',
              'groupId' => '@friends',
              'fields' => 'field',
              'method' => 'appdata.get',
          ),
          array(
              'type' => 'os:DataRequest',
              'method' => 'activities.get',
          ),
          array(
              'type' => 'os:HttpRequest',
              'href' => 'http://example.com',
          ),
      );

      $this->assertEquals($expected, $tags[0]);
      
  }
}
