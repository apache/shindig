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

@date_default_timezone_set(@date_default_timezone_get());
set_include_path(realpath("../") . PATH_SEPARATOR . realpath("../external/"));
error_reporting(E_ALL | E_STRICT);
set_time_limit(60);

require_once "PHPUnit/Framework/TestSuite.php";
require_once "PHPUnit/TextUI/TestRunner.php";
require_once realpath('../') . "/src/common/Config.php";
require_once realpath('../') . "/test/TestContext.php";

function __autoload($className) {
  $basePath = realpath('../');
  $locations = array('src/common', 'src/common/sample', 'src/gadgets', 'src/gadgets/http', 'src/gadgets/oauth',
      'src/gadgets/render', 'src/gadgets/rewrite', 'src/gadgets/sample', 'src/gadgets/templates', 'src/social',
      'src/social/http', 'src/social/service', 'src/social/converters', 'src/social/opensocial',
      'src/social/spi', 'src/social/model', 'src/social/sample', 'src/social/oauth');
  $extension_class_paths = Config::get('extension_class_paths');
  if (! empty($extension_class_paths)) {
    $locations = array_merge(explode(',', $extension_class_paths), $locations);
  }
  // Check for the presense of this class in our all our directories.
  $fileName = $className . '.php';
  foreach ($locations as $path) {
    if (file_exists("$basePath/{$path}/$fileName")) {
      require "$basePath/{$path}/$fileName";
      break;
    }
  }
}

class AllTests {

  public static function main() {
    PHPUnit_TextUI_TestRunner::run(self::suite(), array());
  }

  public static function suite() {
    $suite = new PHPUnit_Framework_TestSuite();
    $suite->setName('Shindig');
    $path = dirname($_SERVER['SCRIPT_FILENAME']);
    $testTypes = array('common', 'gadgets', 'social');
    foreach ($testTypes as $type) {
      if (isset($_GET['test'])) {
        $file_array = glob("$path/{$type}/{$_GET['test']}.php");
        if (isset($file_array[0]) && is_readable($file_array[0])) {
          require_once $file_array[0];
          $className = $_GET['test'];
          $suite->addTestSuite($className);
        }
      } else {
        foreach (glob("$path/{$type}/*Test.php") as $file) {
          if (is_readable($file)) {
            require_once $file;
            $className = str_replace('.php', '', basename($file));
            $suite->addTestSuite($className);
          }
        }
      }
    }
    return $suite;
  }
}

ob_start();
echo "<html><body><pre>";
AllTests::main();
echo "</pre></body></html>";
$output = ob_get_clean();

// make sure the result page isn't cached, some of the tests set caching headers which is bad here
header('Content-Type: text/html', true);
header("Expires: Mon, 26 Jul 1997 05:00:00 GMT", true);
header('Last-Modified: ' . gmdate('D, d M Y H:i:s') . ' GMT', true);
header('Cache-Control: no-store, no-cache, must-revalidate', true);
header('Cache-Control: pre-check=0, post-check=0, max-age=0', true);
header("Pragma: no-cache", true);

echo $output;

