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

class GadgetFactoryTest extends PHPUnit_Framework_TestCase {
    private $oldGet;
    private $oldPost;
    private $token;
    public function setUp()
    {
        $this->oldGet = $_GET;
        $this->oldPost = $_POST;
        $this->token = BasicSecurityToken::createFromValues(1, 1, 1, 'example.com', 'http://example.com/gadget', 1, 1);
    }

    public function tearDown()
    {
        $_GET = $this->oldGet;
        $_POST = $this->oldPost;
    }
    
    public function testCreateGadgetFromRawXml()
    {
        $_GET = array(
            'rawxml' => '<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="title">
    <Require feature="opensocial-0.8" />
    <Require feature="dynamic-height" />
    <Require feature="flash" />
    <Require feature="minimessage" />
  </ModulePrefs>
  <Content type="html" view="home">
  <![CDATA[
    <h1>Hello, world!</h1>
  ]]>
  </Content>
</Module>'
        );
        $_POST = array();
        $context = new GadgetContext('GADGET');
        $gadgetFactory = new GadgetFactory($context, $this->token);
        $gadget = $gadgetFactory->createGadget();

        $this->assertEquals('title', $gadget->gadgetSpec->title);
        $this->assertEquals('<h1>Hello, world!</h1>', trim($gadget->gadgetSpec->views['home']['content']));
    }

    public function testCreateGadgetFromRawXmlInPost()
    {
        $_POST = array(
            'rawxml' => '<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="title">
    <Require feature="opensocial-0.8" />
    <Require feature="dynamic-height" />
    <Require feature="flash" />
    <Require feature="minimessage" />
  </ModulePrefs>
  <Content type="html" view="home">
  <![CDATA[
    <h1>Hello, world!</h1>
  ]]>
  </Content>
</Module>'
        );
        $_GET = array();
        $context = new GadgetContext('GADGET');
        $gadgetFactory = new GadgetFactory($context, $this->token);
        $gadget = $gadgetFactory->createGadget();

        $this->assertEquals('title', $gadget->gadgetSpec->title);
        $this->assertEquals('<h1>Hello, world!</h1>', trim($gadget->gadgetSpec->views['home']['content']));
    }
}