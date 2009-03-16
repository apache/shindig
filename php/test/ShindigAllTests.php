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

/*
 * This file is meant to be run through a php command line or cruiscontrol build, and not called directly
 * through the web browser. To run these tests from the command line:
 * # cd /path/to/shindig
 * # phpunit ShindigAllTests php/test/ShindigAllTests.php   
 */

function __autoload($className) {
  $basePath = realpath('./php');
  $locations = array('src/common', 'src/common/sample', 'src/gadgets', 'src/gadgets/http', 'src/gadgets/oauth', 
      'src/gadgets/sample', 'src/social', 'src/social/http', 'src/social/service', 
      'src/social/converters', 'src/social/opensocial', 'src/social/spi', 'src/social/model', 
      'src/social/sample', 'src/social/oauth');
  $extension_class_paths = Config::get('extension_class_paths');
  if (! empty($extension_class_paths)) {
    $locations = array_merge(explode(',', $extension_class_paths), $locations);
  }
  // Check for the presense of this class in our all our directories.
  $fileName = $className . '.php';
  foreach ($locations as $path) {
    if (file_exists("$basePath/{$path}/$fileName")) {
      require "{$path}/$fileName";
      break;
    }
  }
}

set_include_path(get_include_path() . PATH_SEPARATOR . realpath('./php') . PATH_SEPARATOR . realpath('./php/external'));
error_reporting(E_ALL | E_STRICT);
require_once 'src/common/Config.php';
require_once 'test/TestContext.php';

if (defined('PHPUnit_MAIN_METHOD') === false) {
  define('PHPUnit_MAIN_METHOD', 'ShindigAllTests::main');
}

class ShindigAllTests {

  public static function main() {
    PHPUnit_TextUI_TestRunner::run(self::suite(), array());
  }

  public static function suite() {
    $suite = new PHPUnit_Framework_TestSuite();
    $suite->setName('Shindig');
    $path = realpath('./php/test/');
    $testTypes = array('common', 'gadgets', 'social');
    foreach ($testTypes as $type) {
      foreach (glob("$path/{$type}/*Test.php") as $file) {
        if (is_readable($file)) {
          require_once $file;
          $className = str_replace('.php', '', basename($file));
          $suite->addTestSuite($className);
        }
      }
    }
    return $suite;
  }
}

if (PHPUnit_MAIN_METHOD === 'ShindigAllTests::main') {
  ShindigAllTests::main();
}
