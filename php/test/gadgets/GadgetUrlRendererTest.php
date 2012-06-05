<?php
namespace apache\shindig\test\gadgets;
use apache\shindig\gadgets\GadgetContext;
use apache\shindig\gadgets\render\GadgetUrlRenderer;
use apache\shindig\gadgets\GadgetFactory;
use apache\shindig\common\Cache;
use apache\shindig\common\Config;

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

class MockUrlGadgetFactory extends GadgetFactory {
  public function __construct(GadgetContext $context, $token) {
    parent::__construct($context, $token);
  }

  protected function fetchGadget($gadgetUrl) {
    return '<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="title">
    <Require feature="dynamic-height" />
  </ModulePrefs>
  <UserPref name="key" datatype="string" default_value="value" />
  <Content type="url" url="http://example.com/gadget.php" view="home">
  </Content>
</Module>';
  }
}

/**
 * GadgetUrlRendererTest test case.
 */
class GadgetUrlRendererTest extends \PHPUnit_Framework_TestCase {

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
  private $gadgetUrlRenderer;


  protected function setUp() {
    $_SERVER['HTTP_HOST'] = 'localhost';
    $featureCache = Cache::createCache(Config::get('feature_cache'), 'FeatureCache');
    $key = md5(implode(',', Config::get('features_path')));
    $featureCache->delete($key);
    parent::setUp();
    $this->gadgetContext = new GadgetContext('GADGET');
    $gadgetSpecFactory = new MockUrlGadgetFactory($this->gadgetContext, null);
    $gadgetSpecFactory->fetchGadget = null;
    $this->gadget = $gadgetSpecFactory->createGadget();
    $this->gadgetUrlRenderer = new GadgetUrlRenderer($this->gadgetContext);
  }

  public function testTest() {
    $this->assertTrue(true);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    unset($_SERVER['HTTP_HOST']);
    $this->gadget = null;
    $this->gadgetContext = null;
    $this->gadgetUrlRenderer = null;

    parent::tearDown();
  }
}
