<?php
namespace apache\shindig\test\gadgets;
use apache\shindig\gadgets\GadgetFeatureRegistry;
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

/**
 * GadgetFeatureRegistry test case.
 */
class GadgetFeatureRegistryTest extends \PHPUnit_Framework_TestCase {

  /**
   * @var GadgetFeatureRegistry
   */
  private $GadgetFeatureRegistry;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    $_SERVER['HTTP_HOST'] = 'localhost';
    parent::setUp();
    $this->GadgetFeatureRegistry = new TestGadgetFeatureRegistry(Config::get('features_path'));
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    unset($_SERVER['HTTP_HOST']);
    $this->GadgetFeatureRegistry = null;
    parent::tearDown();
  }

  /**
   * Tests GadgetFeatureRegistry->__construct()
   */
  public function test__construct() {
    $this->GadgetFeatureRegistry->__construct(Config::get('features_path'));
  }

  public function testParseFeatureFileWithContainerGadgetAndAll() {
    $content = '<?xml version="1.0"?>
<feature>
  <name>featureName</name>
  <dependency>dependency1</dependency>
  <dependency>dependency2</dependency>
  <gadget>
    <script src="gadgetFile1.js"/>
    <script src="gadgetFile2.js"/>
    <script>alert(1);</script>
    <script src="res://example.com/file.js" />
  </gadget>
  <container>
    <script src="containerFile1.js"/>
    <script src="containerFile2.js"/>
    <script src="http://example.com/file.js" />
  </container>
  <all>
    <script src="file1.js"/>
    <script src="file2.js"/>
    <script src="https://example.com/file.js" />
  </all>
</feature>';
    $basePath = '/path';
    $feature = $this->GadgetFeatureRegistry->_parse($content, $basePath);

    $expected = array(
        'deps' => array(
            'dependency1' => 'dependency1',
            'dependency2' => 'dependency2',
        ),
        'basePath' => '/path',
        'name' => 'featureName',
        'gadgetJs' => array(
            array(
                'type' => 'FILE',
                'content' => 'gadgetFile1.js',
            ),
            array(
                'type' => 'FILE',
                'content' => 'gadgetFile2.js',
            ),
            array(
                'type' => 'INLINE',
                'content' => 'alert(1);',
            ),
            array(
                'type' => 'URL',
                'content' => 'http://localhost/gadgets/resources/example.com/file.js',
            ),
        ),
        'containerJs' => array(
            array(
                'type' => 'FILE',
                'content' => 'containerFile1.js',
            ),
            array(
                'type' => 'FILE',
                'content' => 'containerFile2.js',
            ),
            array(
                'type' => 'URL',
                'content' => 'http://example.com/file.js',
            ),
        )
    );

    $this->assertEquals($expected, $feature);
  }

  public function testParseFeatureFileWithContainerAndAllBlock() {
    $content = '<?xml version="1.0"?>
<feature>
  <name>featureName</name>
  <dependency>dependency1</dependency>
  <dependency>dependency2</dependency>
  <all>
    <script src="file1.js"/>
    <script src="file2.js"/>
    <script src="https://example.com/file.js" />
  </all>
</feature>';
    $basePath = '/path';
    $feature = $this->GadgetFeatureRegistry->_parse($content, $basePath);

    $expected = array(
        'deps' => array(
            'dependency1' => 'dependency1',
            'dependency2' => 'dependency2',
        ),
        'basePath' => '/path',
        'name' => 'featureName',
        'gadgetJs' => array(
            array(
                'type' => 'FILE',
                'content' => 'file1.js',
            ),
            array(
                'type' => 'FILE',
                'content' => 'file2.js',
            ),
            array(
                'type' => 'URL',
                'content' => 'https://example.com/file.js',
            ),
        ),
        'containerJs' => array(
            array(
                'type' => 'FILE',
                'content' => 'file1.js',
            ),
            array(
                'type' => 'FILE',
                'content' => 'file2.js',
            ),
            array(
                'type' => 'URL',
                'content' => 'https://example.com/file.js',
            ),
        )
    );

    $this->assertEquals($expected, $feature);
  }

  public function testParseFeatureFileWithAllBlock() {
    $content = '<?xml version="1.0"?>
<feature>
  <name>featureName</name>
  <dependency>dependency1</dependency>
  <dependency>dependency2</dependency>
  <container>
    <script src="containerFile1.js"/>
    <script src="containerFile2.js"/>
    <script src="http://example.com/file.js" />
  </container>
  <all>
    <script src="file1.js"/>
    <script src="file2.js"/>
    <script src="https://example.com/file.js" />
  </all>
</feature>';
    $basePath = '/path';
    $feature = $this->GadgetFeatureRegistry->_parse($content, $basePath);

    $expected = array(
        'deps' => array(
            'dependency1' => 'dependency1',
            'dependency2' => 'dependency2',
        ),
        'basePath' => '/path',
        'name' => 'featureName',
        'gadgetJs' => array(
            array(
                'type' => 'FILE',
                'content' => 'file1.js',
            ),
            array(
                'type' => 'FILE',
                'content' => 'file2.js',
            ),
            array(
                'type' => 'URL',
                'content' => 'https://example.com/file.js',
            ),
        ),
        'containerJs' => array(
            array(
                'type' => 'FILE',
                'content' => 'containerFile1.js',
            ),
            array(
                'type' => 'FILE',
                'content' => 'containerFile2.js',
            ),
            array(
                'type' => 'URL',
                'content' => 'http://example.com/file.js',
            ),
        )
    );

    $this->assertEquals($expected, $feature);
  }
}

class TestGadgetFeatureRegistry extends GadgetFeatureRegistry
{
    public function _parse($content, $basePath) {
        return $this->parse($content, $basePath);
    }
}
