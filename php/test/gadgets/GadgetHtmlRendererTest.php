<?php
namespace apache\shindig\test\gadgets;
use apache\shindig\gadgets\GadgetContext;
use apache\shindig\gadgets\render\GadgetHtmlRenderer;
use apache\shindig\common\OpenSocialVersion;
use apache\shindig\gadgets\GadgetFactory;
use apache\shindig\common\Cache;
use apache\shindig\common\Config;
use apache\shindig\gadgets\GadgetSpec;

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
    <Require feature="dynamic-height" />
    <Require feature="flash" />
    <Require feature="minimessage" />
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
class GadgetHtmlRendererTest extends \PHPUnit_Framework_TestCase {

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
    $_SERVER['HTTP_HOST'] = 'localhost';
    $featureCache = Cache::createCache(Config::get('feature_cache'), 'FeatureCache');
    $key = md5(implode(',', Config::get('features_path')));
    $featureCache->delete($key);
    parent::setUp();

    $this->gadgetContext = new GadgetContext('GADGET');
    $gadgetSpecFactory = new MockHtmlGadgetFactory($this->gadgetContext, null);
    $gadgetSpecFactory->fetchGadget = null;
    $this->gadget = $gadgetSpecFactory->createGadget();
    $this->view = $this->gadget->gadgetSpec->views['home'];
    // init gadgetRenderer;
    $this->gadgetHtmlRenderer = new GadgetHtmlRenderer($this->gadgetContext);

    // init $this->doc
    $this->domDocument = new \DOMDocument(null, 'utf-8');
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
    unset($_SERVER['HTTP_HOST']);
    $this->gadget = null;
    $this->gadgetContext = null;
    $this->gadgetHtmlRenderer = null;
    $this->view = null;
    $this->domDocument = null;
    $this->domElement = null;

    parent::tearDown();
  }

  public function testTest() {
    $this->assertTrue(true);
  }

  public function testGetJavaScriptsExternal() {
    $oldForcedJsLibs = Config::get('forcedJsLibs');
    $oldForcedAppendJsLibs = Config::get('forcedAppendedJsLibs');
    Config::set('forcedJsLibs', 'dynamic-height:views');
    Config::set('forcedAppendedJsLibs', array('flash'));
    $this->gadgetHtmlRenderer->dataContext = array(
        'Msg' => array(
            'message1' => 'one',
            'message2' => 'two',
         ),
        'UserPrefs' => array(
            'key1' => 'value1',
            'key2' => 'value2',
         ),
    );
    $this->gadgetHtmlRenderer->gadget = $this->gadget;
    $javaScripts = $this->gadgetHtmlRenderer->getJavaScripts();
//    Config::set('forcedJsLibs', $oldForcedJsLibs);
//    Config::set('forcedAppendedJsLibs', $oldForcedAppendJsLibs);
//    $hasExtern = false;
//    $hasInline = false;
//    foreach ($javaScripts as $script) {
//        switch ($script['type']) {
//            case 'extern':
//                if ($hasExtern) {
//                    $this->fail('two entries with script type extern');
//                }
//                $hasExtern = true;
//                $this->assertEquals(0, strpos($script['content'], '/gadgets/js/dynamic-height:views:core.js?'), 'could not find string "/gadgets/js/dynamic-height:views:core.js?" in: '.  PHP_EOL . $script['content']);
//                break;
//            case 'inline':
//                if ($hasInline) {
//                    $this->fail('two entries with script type inline');
//                }
//                //this is from dynamic height and should not be included
//                $this->assertFalse(strpos($script['content'], 'gadgets.window = gadgets.window || {};'));
//                //minimessage should be included
//                $miniMessagePos = strpos($script['content'], 'gadgets.MiniMessage = function');
//                $this->assertTrue($miniMessagePos > 0);
//                //we force flash to be appended, so it should be after minimessage
//                $this->assertTrue(strpos($script['content'], 'gadgets.flash = gadgets.flash || {};') > $miniMessagePos);
//                $hasInline = true;
//                break;
//            default:
//                $this->fail('invalid script type ' . $script['type']);
//        }
//    }
//    $this->assertTrue($hasExtern);
//    $this->assertTrue($hasInline);
  }
//
//  /**
//   * Tests GadgetHtmlRenderer->renderGadget()
//   */
//  public function testRenderGadgetDefaultDoctype() {
//    Config::set('P3P', ''); // prevents "modify header information" errors
//    ob_start();
//    $this->gadgetHtmlRenderer->renderGadget($this->gadget, $this->view);
//    $content = ob_get_clean();
//    $this->assertTrue(strpos($content, '!DOCTYPE HTML>') > 0, $content);
//  }
//
//  public function testLegacyDoctypeBecauseOfOldOpenSocialVersion() {
//    Config::set('P3P', ''); // prevents "modify header information" errors
//    $this->gadget->gadgetSpec->specificationVersion = new OpenSocialVersion('1.0.0');
//    ob_start();
//    $this->gadgetHtmlRenderer->renderGadget($this->gadget, $this->view);
//    $content = ob_get_clean();
//    $this->assertTrue(strpos($content, '!DOCTYPE HTML PUBLIC') > 0);
//  }
//
//  public function testCustomDoctypeDoctype() {
//    Config::set('P3P', ''); // prevents "modify header information" errors
//    $this->gadget->gadgetSpec->doctype = 'CUSTOM';
//    ob_start();
//    $this->gadgetHtmlRenderer->renderGadget($this->gadget, $this->view);
//    $content = ob_get_clean();
//    $this->assertTrue(strpos($content, '!DOCTYPE CUSTOM') > 0);
//  }
//
//  public function testQuirksModeBecauseOfQuirksDoctype() {
//    Config::set('P3P', ''); // prevents "modify header information" errors
//    $this->gadget->gadgetSpec->doctype = GadgetSpec::DOCTYPE_QUIRKSMODE;
//    ob_start();
//    $this->gadgetHtmlRenderer->renderGadget($this->gadget, $this->view);
//    $content = ob_get_clean();
//    $this->assertTrue(strpos($content, '!DOCTYPE') === false);
//  }
//
//  public function testQuirksModeBecauseOfContentBlockAttribute() {
//    Config::set('P3P', ''); // prevents "modify header information" errors
//    $this->view['quirks'] = true;
//    ob_start();
//    $this->gadgetHtmlRenderer->renderGadget($this->gadget, $this->view);
//    $content = ob_get_clean();
//    $this->assertTrue(strpos($content, '!DOCTYPE') === false);
//  }
//
//  /**
//   * Tests GadgetHtmlRenderer->addBodyTags()
//   */
//  public function testAddBodyTags() {
//    $this->gadgetHtmlRenderer->addBodyTags($this->domElement, $this->domDocument);
//    $tmpNodeList = $this->domElement->getElementsByTagName("script");
//    foreach($tmpNodeList as $tmpNode) {
//      $this->assertEquals('gadgets.util.runOnLoadHandlers();', $tmpNode->nodeValue);
//    }
//  }
//
//  /**
//   * Tests GadgetHtmlRenderer->addHeadTags()
//   */
//  public function testAddHeadTags() {
//    ob_start();
//    $this->gadgetHtmlRenderer->renderGadget($this->gadget, $this->view);
//    ob_end_clean();
//    $this->gadgetHtmlRenderer->addHeadTags($this->domElement, $this->domDocument);
//
//    // TODO: currently we just test the script part
//    $tmpNodeList = $this->domElement->getElementsByTagName("script");
//    $scripts = $this->gadgetHtmlRenderer->getJavaScripts();
//
//    $idx = 0;
//    foreach($tmpNodeList as $tmpNode) {
//      $script = $scripts[$idx++];
//      if ($script['type'] == 'inline') {
//        $this->assertEquals('text/javascript', $tmpNode->getAttribute('type'));
//        $this->assertEquals(trim($script['content']), trim($tmpNode->nodeValue));
//      } else {
//        $this->assertEquals($script['content'], $tmpNode->getAttribute('src'));
//      }
//    }
//  }

}

