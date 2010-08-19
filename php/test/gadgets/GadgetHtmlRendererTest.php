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

class MockHtmlGadgetFactory extends GadgetFactory {
  public function __construct(GadgetContext $context, $token) {
    parent::__construct($context, $token);
  }
  
  protected function fetchGadget($gadgetUrl) {
    return '<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="title">
    <Require feature="opensocial-0.8" />
    <Require feature="dynamic-height" />
  </ModulePrefs>
  <Content type="html" view="home">
  <![CDATA[
    <h1>Hello, world!</h1>
  ]]>
  </Content>
</Module>';
  }
}

/**
 * GadgetRendererTest test case.
 */
class GadgetHtmlRendererTest extends PHPUnit_Framework_TestCase {

  /**
   * @var Gadget
   */
  private $gadget;
  
  /**
   * @var GadgetContext
   */
  private $gadgetContext;
  
  /**
   * @var GadgetHtmlRender
   */
  private $gadgetHtmlRenderer;
  
  /**
   * @var view
   */
  private $view;
  
  /**
   * @var DomElement
   */
  private $domElement;
  
  /**
   * @var DomDocument
   */
  private $domDocument;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();

    $this->gadgetContext = new GadgetContext('GADGET');
    $gadgetSpecFactory = new MockHtmlGadgetFactory($this->gadgetContext, null);
    $gadgetSpecFactory->fetchGadget = null;
    $this->gadget = $gadgetSpecFactory->createGadget();

    // init gadgetRenderer;
    $this->gadgetHtmlRenderer = new GadgetHtmlRenderer($this->gadgetContext);

    // init $this->doc
    $this->domDocument = new DOMDocument(null, 'utf-8');
    $this->domDocument->preserveWhiteSpace = true;
    $this->domDocument->formatOutput = false;
    $this->domDocument->strictErrorChecking = false;
    $this->domDocument->recover = false;    

    // init $this->element
    $this->domElement = $this->domDocument->createElement('test');
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->gadget = null;
    $this->gadgetContext = null;
    $this->gadgetHtmlRenderer = null;
    $this->view = null;
    $this->domDocument = null;
    $this->domElement = null;

    parent::tearDown();
  }

  /**
   * Tests GadgetHtmlRenderer->renderGadget()
   */
  public function testRenderGadget() {
    Config::set('P3P', ''); // prevents "modify header information" errors
    ob_start();
    $this->gadgetHtmlRenderer->renderGadget($this->gadget, $this->view);
    ob_end_clean();
  }

  /**
   * Tests GadgetHtmlRenderer->addBodyTags()
   */
  public function testAddBodyTags() {
    $this->gadgetHtmlRenderer->addBodyTags($this->domElement, $this->domDocument);
    $tmpNodeList = $this->domElement->getElementsByTagName("script");
    foreach($tmpNodeList as $tmpNode) {
      $this->assertEquals('gadgets.util.runOnLoadHandlers();', $tmpNode->nodeValue);
    }
  }

  /**
   * Tests GadgetHtmlRenderer->addHeadTags()
   */
  public function testAddHeadTags() {
    ob_start();
    $this->gadgetHtmlRenderer->renderGadget($this->gadget, $this->view);
    ob_end_clean();
    $this->gadgetHtmlRenderer->addHeadTags($this->domElement, $this->domDocument);

    // TODO: currently we just test the script part
    $tmpNodeList = $this->domElement->getElementsByTagName("script");
    $scripts = $this->gadgetHtmlRenderer->getJavaScripts();

    $idx = 0;
    foreach($tmpNodeList as $tmpNode) {
      $script = $scripts[$idx++];
      if ($script['type'] == 'inline') {
        $this->assertEquals('text/javascript', $tmpNode->getAttribute('type'));
        $this->assertEquals(trim($script['content']), trim($tmpNode->nodeValue));
      } else {
        $this->assertEquals($script['content'], $tmpNode->getAttribute('src'));
      }
    }
  }

}
?>
