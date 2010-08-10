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
 * HttpUtil test case.
 */
class HttpUtilTest extends PHPUnit_Framework_TestCase {
  
  private $GadgetServer;
  private $gadget;
  private $GadgetContext;

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->GadgetServer = null;
    $this->gadget = null;
    parent::tearDown();
  }

  /**
   * Tests HttpUtil::getView()
   */
  public function testGetView() {
    $this->GadgetServer = new GadgetServer();
    // prevent polution from other test cases + make sure we're not testing
    // a cached result
    $_GET = array('nocache' => 1, 'view' => 'profile');
    $_POST = array();
    $this->GadgetContext = new GadgetContext('GADGET');
    $this->GadgetContext->setUrl('http://test.chabotc.com/testGadget.xml');
    $this->gadget = $this->GadgetServer->processGadget($this->GadgetContext);
    $view = HttpUtil::getView($this->gadget, $this->GadgetContext);
    $this->assertEquals('profile', $view->getName());
    $this->assertEquals('HTML', $view->getType());
    $this->assertEquals('', $view->getHref());
    $this->assertEquals("content", $view->getContent());
    $this->assertEquals("400", $view->preferedHeight);
    $this->assertEquals("300", $view->preferedWidth);
    $this->assertFalse($view->getQuirks());
  }
}
