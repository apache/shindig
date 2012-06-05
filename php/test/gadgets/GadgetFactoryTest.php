<?php
namespace apache\shindig\test\gadgets;
use apache\shindig\gadgets\GadgetContext;
use apache\shindig\gadgets\GadgetFactory;
use apache\shindig\common\sample\BasicSecurityToken;

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

class GadgetFactoryTest extends \PHPUnit_Framework_TestCase {
    private $oldGet;
    private $oldPost;
    private $token;
    public function setUp()
    {
        $_SERVER['HTTP_HOST'] = 'localhost';
        $this->oldGet = $_GET;
        $this->oldPost = $_POST;
        $this->token = BasicSecurityToken::createFromValues(1, 1, 1, 'example.com', 'http://example.com/gadget', 1, 1);
    }

    public function tearDown()
    {
        unset($_SERVER['HTTP_HOST']);
        $_GET = $this->oldGet;
        $_POST = $this->oldPost;
    }

    public function testCreateGadgetFromRawXml()
    {
        $_GET = array(
            'rawxml' => '<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="title">
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

    public function testParseFeaturesDependentOnCurrentView() {
        $_POST = array(
            'rawxml' => '<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="title">
    <Require feature="pubsub" views="canvas" />
    <Require feature="flash" views="canvas,profile" />
    <Optional feature="minimessage" />
    <Optional feature="invalid" />
    <Optional feature="pubsub2" views="canvas" />
    <Optional feature="opensocial-data" views="canvas, profile" />
  </ModulePrefs>
  <Content type="html" view="home">
  </Content>
</Module>'
        );
        $_GET = array();
        $context = new GadgetContext('GADGET');
        $context->setView('profile');
        $gadgetFactory = new GadgetFactory($context, $this->token);
        $gadget = $gadgetFactory->createGadget();

        $this->assertTrue(in_array('flash', $gadget->features));
        $this->assertTrue(in_array('minimessage', $gadget->features));
        $this->assertTrue(in_array('opensocial-data', $gadget->features));

        $this->assertFalse(in_array('pubsub', $gadget->features));
        $this->assertFalse(in_array('pubsub2', $gadget->features));
    }

    public function testRequiringInvalidFeatureThrowsException() {
        $this->setExpectedException('apache\shindig\gadgets\GadgetException', 'Unknown features: invalid');
        $_POST = array(
            'rawxml' => '<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="title">
    <Require feature="invalid" />
  </ModulePrefs>
  <Content type="html" view="home">
  </Content>
</Module>'
        );
        $_GET = array();
        $context = new GadgetContext('GADGET');
        $context->setView('profile');
        $gadgetFactory = new GadgetFactory($context, $this->token);
        $gadget = $gadgetFactory->createGadget();
    }

    public function testParsePreloadsDependentOnCurrentView() {
        $_POST = array(
            'rawxml' => '<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="title">
    <Preload href="http://www.example.com/one" />
    <Preload href="http://www.example.com/two" views="canvas"/>
    <Preload href="http://www.example.com/three" views="canvas,profile"/>
  </ModulePrefs>
  <Content type="html" view="home">
  </Content>
</Module>'
        );
        $_GET = array();
        $context = new GadgetContext('GADGET');
        $context->setView('profile');
        $gadgetFactory = new TestGadgetFactory($context, $this->token);
        $gadget = $gadgetFactory->createGadget();

        $this->assertEquals('http://www.example.com/one', $gadget->gadgetSpec->preloads[0]['id']);
        $this->assertEquals('http://www.example.com/three', $gadget->gadgetSpec->preloads[1]['id']);
        $this->assertEquals(2, count($gadget->gadgetSpec->preloads));
    }

    public function testParseLocalsDependentOnCurrentView() {
        $_POST = array(
            'rawxml' => '<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="title">
     <Locale messages="http://example.com/helloOne/en_ALL.xml"/>
     <Locale messages="http://example.com/helloTwo/en_ALL.xml" views="canvas"/>
     <Locale messages="http://example.com/helloThree/en_ALL.xml" views="canvas,profile"/>
  </ModulePrefs>
  <Content type="html" view="home">
  </Content>
</Module>'
        );
        $_GET = array();
        $context = new GadgetContext('GADGET');
        $context->setView('profile');
        $gadgetFactory = new TestGadgetFactory($context, $this->token);
        $gadget = $gadgetFactory->createGadget();
        $this->assertEquals(array('greetingOne' => 'Hello', 'greetingThree' => 'Hello'), $gadget->gadgetSpec->locales);
    }
}

class TestGadgetFactory extends GadgetFactory
{
    private $responses = array(
        'http://www.example.com/one' => 'preloadOne',
        'http://www.example.com/two' => 'preloadTwo',
        'http://www.example.com/three' => 'preloadThree',
        'http://example.com/helloOne/en_ALL.xml' => '<?xml version="1.0" encoding="UTF-8" ?>
<messagebundle>
  <msg name="greetingOne">
    Hello
  </msg>
</messagebundle>',
        'http://example.com/helloTwo/en_ALL.xml' => '<?xml version="1.0" encoding="UTF-8" ?>
<messagebundle>
  <msg name="greetingTwo">
    Hello
  </msg>
</messagebundle>',
        'http://example.com/helloThree/en_ALL.xml' => '<?xml version="1.0" encoding="UTF-8" ?>
<messagebundle>
  <msg name="greetingThree">
    Hello
  </msg>
</messagebundle>',
    );
    /**
     * mock request sending
     *
     * @param array $unsignedRequests
     * @param array $signedRequests
     * @return array
     */
    protected function performRequests($unsignedRequests, $signedRequests) {
        // Perform the non-signed requests
        $responses = array();
        if (count($unsignedRequests)) {
            foreach ($unsignedRequests as $request) {
                $responses[$request->getUrl()] = array(
                    'body' => $this->responses[$request->getUrl()],
                    'rc' => 200);
            }
        }

        // Perform the signed requests
        if (count($signedRequests)) {
            foreach ($signedRequests as $request) {
                $responses[$request->getUrl()] = array(
                    'body' => $this->responses[$request->getUrl()],
                    'rc' => 200);
            }
        }

        return $responses;
    }
}