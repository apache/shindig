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
 * GadgetFeatureRegistry test case.
 */
class GadgetFeatureRegistryTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var GadgetFeatureRegistry
   */
  private $GadgetFeatureRegistry;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->GadgetFeatureRegistry = new GadgetFeatureRegistry(Config::get('features_path'));
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->GadgetFeatureRegistry = null;
    parent::tearDown();
  }

  /**
   * Tests GadgetFeatureRegistry->__construct()
   */
  public function test__construct() {
    $this->GadgetFeatureRegistry->__construct(Config::get('features_path'));
  }

  /**
   * Tests GadgetFeatureRegistry->getAllFeatures()
   */
  public function testGetAllFeatures() {
    $this->assertGreaterThan(0, count($this->GadgetFeatureRegistry->getAllFeatures()));
  }

  /**
   * Tests GadgetFeatureRegistry->getEntry()
   */
  public function testGetEntry() {
    $entry = $this->GadgetFeatureRegistry->getEntry('core');
    $this->assertTrue($entry instanceof GadgetFeatureRegistryEntry);
    $this->assertEquals('core', $entry->getName());
    $this->assertTrue(is_array($entry->getDependencies()));
    $this->assertTrue($entry->getFeature() instanceof JsLibraryFeatureFactory);
  }

  /**
   * Tests GadgetFeatureRegistry->getIncludedFeatures()
   */
  public function testGetIncludedFeatures() {
    $needed = array('flash', 'opensocial-0.8', 'settitle', 'setprefs', 'foobar');
    $resultsFound = array();
    $resultsMissing = array();
    $this->GadgetFeatureRegistry->getIncludedFeatures($needed, $resultsFound, $resultsMissing);
    $this->assertTrue(in_array('foobar', $resultsMissing));
    $this->assertTrue(in_array('settitle', $resultsFound));
    $this->assertTrue(in_array('setprefs', $resultsFound));
    $this->assertTrue(in_array('rpc', $resultsFound));
    $this->assertTrue(in_array('flash', $resultsFound));
    $this->assertTrue(in_array('core', $resultsFound));
    $this->assertTrue(in_array('core.io', $resultsFound));
    $this->assertTrue(in_array('opensocial-0.8', $resultsFound));
  }
}
