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

class MockRewriterGadgetFactory extends GadgetFactory {
  public function __construct(GadgetContext $context, $token) {
    parent::__construct($context, $token);
  }

  protected function fetchGadget($gadgetUrl) {
    return <<<EOD
<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="title">
    <Require feature="opensocial-0.8" />
    <Require feature="dynamic-height" />
  </ModulePrefs>
  <Content type="html" view="profile">
  <![CDATA[
    <script>var test='<b>BIG WORDS</b>'</script>
    <h1>Hello, world!</h1>
  ]]>
  </Content>
</Module>
EOD;
  }
}

/**
 * GadgetRendererTest test case.
 */
class GadgetRewriterTest extends PHPUnit_Framework_TestCase {

  /**
   * @var Gadget
   */
  private $gadget;

  /**
   * @var GadgetContext
   */
  private $gadgetContext;

  /**
   * @var GadgetRewriter
   */
  private $gadgetRewriter;

  /**
   * @var view
   */
  private $view;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();

    $this->gadgetContext = new GadgetContext('GADGET');
    $gadgetSpecFactory = new MockRewriterGadgetFactory($this->gadgetContext, null);
    $gadgetSpecFactory->fetchGadget = null;
    $this->gadget = $gadgetSpecFactory->createGadget();
    $this->gadgetRewriter = new GadgetRewriter($this->gadgetContext);
    $this->view = $this->gadget->getView($this->gadgetContext->getView());
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->gadget = null;
    $this->gadgetContext = null;
    $this->gadgetRewriter = null;
    $this->view = null;

    parent::tearDown();
  }

  /**
   * Tests GadgetHtmlRenderer->renderGadget()
   */
  public function testRewrite() {
    preg_match_all('|<script>(.*?)</script>|', $this->gadgetRewriter->rewrite($this->view["content"], $this->gadget), $tmp, PREG_SET_ORDER);
    $desc_string = $tmp[0][1];
    $source_string = "var test='<b>BIG WORDS</b>'";
    $this->assertEquals($source_string, $desc_string);
  }
}
?>