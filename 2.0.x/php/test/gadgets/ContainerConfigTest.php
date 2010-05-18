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
 * ContainerConfig test case.
 */
class ContainerConfigTest extends PHPUnit_Framework_TestCase {

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
   }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    parent::tearDown();
  }

  /**
   * Tests ContainerConfig->getConfig()
   */
  public function testGetConfig() {
    $containerConfig = new ContainerConfig(Config::get('container_path'));
    $config = $containerConfig->getConfig('default', 'gadgets.features');
    $this->assertArrayHasKey('core.io', $config);
    $this->assertArrayHasKey('views', $config);
    $this->assertArrayHasKey('rpc', $config);
    $this->assertArrayHasKey('skins', $config);
    $this->assertArrayHasKey('opensocial', $config);
    $this->assertArrayHasKey('path', $config['opensocial']);
  }
  
  /**
   * Tests ContainerConfig::removeComments()
   */
  public function testRemoveComments() {
    $jsFile = <<<EOD
/*
 * Comments
 */

// Comments
{"gadgets.container" : ["default"],
"gadgets.parent" : null,
"gadgets.lockedDomainSuffix" : "-a.example.com:8080",
"gadgets.iframeBaseUri" : "/gadgets/ifr",
"gadgets.jsUriTemplate" : "http://%host%/gadgets/js/%js%",
"gadgets.oauthGadgetCallbackTemplate" : "//%host%/gadgets/oauthcallback"
}
EOD;
    $uncommented = ContainerConfig::removeComments($jsFile);
    $jsonObj = json_decode($uncommented, true);
    $this->assertNotEquals($uncommented, $jsonObj);
    $this->assertEquals(array("default"), $jsonObj["gadgets.container"]);
    $this->assertEquals(null, $jsonObj["gadgets.parent"]);
    $this->assertEquals("-a.example.com:8080", $jsonObj["gadgets.lockedDomainSuffix"]);
    $this->assertEquals("/gadgets/ifr", $jsonObj["gadgets.iframeBaseUri"]);
    $this->assertEquals("http://%host%/gadgets/js/%js%", $jsonObj["gadgets.jsUriTemplate"]);
    $this->assertEquals("//%host%/gadgets/oauthcallback", $jsonObj["gadgets.oauthGadgetCallbackTemplate"]);
  }
}
