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
 * JsFeatureLoader test case.
 */
class JsFeatureLoaderTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var JsFeatureLoader
   */
  private $JsFeatureLoader;
  
  /**
   * @var FeatureName
   */
  private $FeatureName = 'Dummie';
  
  /**
   * @var FeatureScript
   */
  private $FeatureScript = 'dummie_script.js';
  
  /**
   * @var JsFeaturesFileContent
   */
  private $JsFeaturesFileContent = '';

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->JsFeatureLoader = new JsFeatureLoader('');
  
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->JsFeatureLoader = null;
    parent::tearDown();
  }

  public function __construct() {
    $this->JsFeaturesFileContent = '<?xml version="1.0"?>	
<feature>
  <name>' . $this->FeatureName . '</name>
  <gadget>
    <script src="' . $this->FeatureScript . '"/>
  </gadget>
</feature>
		';
  }

  /**
   * Tests JsFeatureLoader->loadFeatures()
   */
  public function testLoadFeatures() {
    $registry = new GadgetFeatureRegistry('');
    if (file_put_contents($this->FeatureScript, $this->JsFeaturesFileContent)) {
      if (file_exists($this->FeatureScript) && is_readable($this->FeatureScript)) {
        $features = $this->JsFeatureLoader->loadFeatures($this->FeatureScript, $registry);
        foreach ($features as $feature) {
          $this->assertTrue($feature->name == $this->FeatureName);
        }
      }
    } else {
      $this->fail('Error creating the dummie fail');
    }
  
  }

}

