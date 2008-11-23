<?php
/**
 * PHPUnit
 *
 * Copyright (c) 2002-2008, Sebastian Bergmann <sb@sebastian-bergmann.de>.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *
 *   * Neither the name of Sebastian Bergmann nor the names of his
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @category   Testing
 * @package    PHPUnit
 * @author     Sebastian Bergmann <sb@sebastian-bergmann.de>
 * @copyright  2002-2008 Sebastian Bergmann <sb@sebastian-bergmann.de>
 * @license    http://www.opensource.org/licenses/bsd-license.php  BSD License
 * @version    SVN: $Id: Command.php 1985 2007-12-26 18:11:55Z sb $
 * @link       http://www.phpunit.de/
 * @since      File available since Release 3.0.0
 */

require_once 'PHPUnit/TextUI/TestRunner.php';
require_once 'PHPUnit/Util/Log/PMD.php';
require_once 'PHPUnit/Util/Log/TAP.php';
require_once 'PHPUnit/Util/Configuration.php';
require_once 'PHPUnit/Util/Fileloader.php';
require_once 'PHPUnit/Util/Filter.php';
require_once 'PHPUnit/Util/Getopt.php';
require_once 'PHPUnit/Util/Skeleton.php';
require_once 'PHPUnit/Util/TestDox/ResultPrinter/Text.php';

PHPUnit_Util_Filter::addFileToFilter(__FILE__, 'PHPUNIT');

/**
 * A TestRunner for the Command Line Interface (CLI)
 * PHP SAPI Module.
 *
 * @category   Testing
 * @package    PHPUnit
 * @author     Sebastian Bergmann <sb@sebastian-bergmann.de>
 * @copyright  2002-2008 Sebastian Bergmann <sb@sebastian-bergmann.de>
 * @license    http://www.opensource.org/licenses/bsd-license.php  BSD License
 * @version    Release: 3.2.9
 * @link       http://www.phpunit.de/
 * @since      Class available since Release 3.0.0
 */
class PHPUnit_TextUI_Command {

  /**
   * @access public
   * @static
   */
  public static function main() {
    $arguments = self::handleArguments();
    $runner = new PHPUnit_TextUI_TestRunner();
    
    if (is_object($arguments['test']) && $arguments['test'] instanceof PHPUnit_Framework_Test) {
      $suite = $arguments['test'];
    } else {
      $suite = $runner->getTest($arguments['test'], $arguments['testFile'], $arguments['syntaxCheck']);
    }
    
    if ($suite->testAt(0) instanceof PHPUnit_Framework_Warning && strpos($suite->testAt(0)->getMessage(), 'No tests found in class') !== FALSE) {
      $skeleton = new PHPUnit_Util_Skeleton($arguments['test'], $arguments['testFile']);
      
      $result = $skeleton->generate(TRUE);
      
      if (! $result['incomplete']) {
        eval(str_replace(array('<?php', '?>'), '', $result['code']));
        $suite = new PHPUnit_Framework_TestSuite($arguments['test'] . 'Test');
      }
    }
    
    try {
      $result = $runner->doRun($suite, $arguments);
    } 

    catch (Exception $e) {
      throw new RuntimeException('Could not create and run test suite: ' . $e->getMessage());
    }
    
    if ($result->wasSuccessful()) {
      exit(PHPUnit_TextUI_TestRunner::SUCCESS_EXIT);
    } 

    else if ($result->errorCount() > 0) {
      exit(PHPUnit_TextUI_TestRunner::EXCEPTION_EXIT);
    } 

    else {
      exit(PHPUnit_TextUI_TestRunner::FAILURE_EXIT);
    }
  }

  /**
   * @access protected
   * @static
   */
  protected static function handleArguments() {
    $arguments = array('syntaxCheck' => TRUE);
    
    $longOptions = array('configuration=', 'exclude-group=', 'filter=', 'group=', 'help', 'loader=', 
        'log-json=', 'log-tap=', 'log-xml=', 'repeat=', 'skeleton', 'stop-on-failure', 
        'tap', 'testdox', 'testdox-html=', 'testdox-text=', 'no-syntax-check', 'verbose', 
        'version', 'wait');
    
    if (class_exists('Image_GraphViz', FALSE)) {
      $longOptions[] = 'log-graphviz=';
    }
    
    if (extension_loaded('pdo')) {
      $longOptions[] = 'test-db-dsn=';
      $longOptions[] = 'test-db-log-rev=';
      $longOptions[] = 'test-db-log-prefix=';
      $longOptions[] = 'test-db-log-info=';
    }
    
    if (extension_loaded('xdebug')) {
      $longOptions[] = 'coverage-html=';
      $longOptions[] = 'coverage-xml=';
      $longOptions[] = 'log-metrics=';
      $longOptions[] = 'log-pmd=';
      $longOptions[] = 'report=';
    }
    
    try {
      $options = PHPUnit_Util_Getopt::getopt($_SERVER['argv'], 'd:', $longOptions);
    } 

    catch (RuntimeException $e) {
      PHPUnit_TextUI_TestRunner::showError($e->getMessage());
    }
    
    if (isset($options[1][0])) {
      $arguments['test'] = $options[1][0];
    }
    
    if (isset($options[1][1])) {
      $arguments['testFile'] = $options[1][1];
    } else {
      $arguments['testFile'] = '';
    }
    
    foreach ($options[0] as $option) {
      switch ($option[0]) {
        case '--configuration':
          {
            $arguments['configuration'] = $option[1];
          }
          break;
        
        case '--coverage-xml':
          {
            $arguments['coverageXML'] = $option[1];
          }
          break;
        
        case 'd':
          {
            $ini = explode('=', $option[1]);
            
            if (isset($ini[0])) {
              if (isset($ini[1])) {
                ini_set($ini[0], $ini[1]);
              } else {
                ini_set($ini[0], TRUE);
              }
            }
          }
          break;
        
        case '--help':
          {
            self::showHelp();
            exit(PHPUnit_TextUI_TestRunner::SUCCESS_EXIT);
          }
          break;
        
        case '--filter':
          {
            if (preg_match('/^[a-zA-Z0-9_]/', $option[1])) {
              $arguments['filter'] = '/^' . $option[1] . '$/';
            } else {
              $arguments['filter'] = $option[1];
            }
          }
          break;
        
        case '--group':
          {
            $arguments['groups'] = explode(',', $option[1]);
          }
          break;
        
        case '--exclude-group':
          {
            $arguments['excludeGroups'] = explode(',', $option[1]);
          }
          break;
        
        case '--loader':
          {
            self::handleLoader($option[1]);
          }
          break;
        
        case '--log-json':
          {
            $arguments['jsonLogfile'] = $option[1];
          }
          break;
        
        case '--log-graphviz':
          {
            $arguments['graphvizLogfile'] = $option[1];
          }
          break;
        
        case '--log-tap':
          {
            $arguments['tapLogfile'] = $option[1];
          }
          break;
        
        case '--log-xml':
          {
            $arguments['xmlLogfile'] = $option[1];
          }
          break;
        
        case '--log-pmd':
          {
            $arguments['pmdXML'] = $option[1];
          }
          break;
        
        case '--log-metrics':
          {
            $arguments['metricsXML'] = $option[1];
          }
          break;
        
        case '--repeat':
          {
            $arguments['repeat'] = (int)$option[1];
          }
          break;
        
        case '--stop-on-failure':
          {
            $arguments['stopOnFailure'] = TRUE;
          }
          break;
        
        case '--test-db-dsn':
          {
            $arguments['testDatabaseDSN'] = $option[1];
          }
          break;
        
        case '--test-db-log-rev':
          {
            $arguments['testDatabaseLogRevision'] = $option[1];
          }
          break;
        
        case '--test-db-prefix':
          {
            $arguments['testDatabasePrefix'] = $option[1];
          }
          break;
        
        case '--test-db-log-info':
          {
            $arguments['testDatabaseLogInfo'] = $option[1];
          }
          break;
        
        case '--coverage-html':
        case '--report':
          {
            $arguments['reportDirectory'] = $option[1];
          }
          break;
        
        case '--skeleton':
          {
            if (isset($arguments['test']) && isset($arguments['testFile'])) {
              self::doSkeleton($arguments['test'], $arguments['testFile']);
            } else {
              self::showHelp();
              exit(PHPUnit_TextUI_TestRunner::EXCEPTION_EXIT);
            }
          }
          break;
        
        case '--tap':
          {
            $arguments['printer'] = new PHPUnit_Util_Log_TAP();
          }
          break;
        
        case '--testdox':
          {
            $arguments['printer'] = new PHPUnit_Util_TestDox_ResultPrinter_Text();
          }
          break;
        
        case '--testdox-html':
          {
            $arguments['testdoxHTMLFile'] = $option[1];
          }
          break;
        
        case '--testdox-text':
          {
            $arguments['testdoxTextFile'] = $option[1];
          }
          break;
        
        case '--no-syntax-check':
          {
            $arguments['syntaxCheck'] = FALSE;
          }
          break;
        
        case '--verbose':
          {
            $arguments['verbose'] = TRUE;
          }
          break;
        
        case '--version':
          {
            PHPUnit_TextUI_TestRunner::printVersionString();
            exit(PHPUnit_TextUI_TestRunner::SUCCESS_EXIT);
          }
          break;
        
        case '--wait':
          {
            $arguments['wait'] = TRUE;
          }
          break;
      }
    }
    
    if (! isset($arguments['test']) && isset($arguments['configuration'])) {
      $configuration = new PHPUnit_Util_Configuration($arguments['configuration']);
      
      $testSuite = $configuration->getTestSuiteConfiguration();
      
      if ($testSuite !== NULL) {
        $arguments['test'] = $testSuite;
      }
    }
    
    if (! isset($arguments['test']) || (isset($arguments['testDatabaseLogRevision']) && ! isset($arguments['testDatabaseDSN']))) {
      self::showHelp();
      exit(PHPUnit_TextUI_TestRunner::EXCEPTION_EXIT);
    }
    
    return $arguments;
  }

  /**
   * @param  string  $test
   * @param  string  $testFile
   * @access protected
   * @static
   */
  protected static function doSkeleton($test, $testFile) {
    if ($test !== FALSE) {
      PHPUnit_TextUI_TestRunner::printVersionString();
      
      try {
        $skeleton = new PHPUnit_Util_Skeleton($test, $testFile);
        $skeleton->write();
      } 

      catch (Exception $e) {
        print $e->getMessage() . "\n";
        
        printf('Could not write test class skeleton for "%s" to "%s".' . "\n", $test, $testFile);
        
        exit(PHPUnit_TextUI_TestRunner::FAILURE_EXIT);
      }
      
      printf('Wrote test class skeleton for "%s" to "%s".' . "\n", $test, $skeleton->getTestSourceFile());
      
      exit(PHPUnit_TextUI_TestRunner::SUCCESS_EXIT);
    }
  }

  /**
   * @param  string  $loaderName
   * @access protected
   * @static
   */
  protected static function handleLoader($loaderName) {
    if (! class_exists($loaderName, FALSE)) {
      PHPUnit_Util_Fileloader::checkAndLoad(str_replace('_', '/', $loaderName) . '.php');
    }
    
    if (class_exists($loaderName, FALSE)) {
      $class = new ReflectionClass($loaderName);
      
      if ($class->implementsInterface('PHPUnit_Runner_TestSuiteLoader') && $class->isInstantiable()) {
        $loader = $class->newInstance();
      }
    }
    
    if (! isset($loader)) {
      PHPUnit_TextUI_TestRunner::showError(sprintf('Could not use "%s" as loader.', 

      $loaderName));
    }
    
    PHPUnit_TextUI_TestRunner::setLoader($loader);
  }

  /**
   * @access public
   * @static
   */
  public static function showHelp() {
    PHPUnit_TextUI_TestRunner::printVersionString();
    
    print "Usage: phpunit [switches] UnitTest [UnitTest.php]\n\n";
    
    if (class_exists('Image_GraphViz', FALSE)) {
      print "  --log-graphviz <file>  Log test execution in GraphViz markup.\n";
    }
    
    print "  --log-json <file>      Log test execution in JSON format.\n" . "  --log-tap <file>       Log test execution in TAP format to file.\n" . "  --log-xml <file>       Log test execution in XML format to file.\n";
    
    if (extension_loaded('xdebug')) {
      print "  --log-metrics <file>   Write metrics report in XML format.\n" . "  --log-pmd <file>       Write violations report in PMD XML format.\n\n" . "  --coverage-html <dir>  Generate code coverage report in HTML format.\n" . "  --coverage-xml <file>  Write code coverage information in XML format.\n\n";
    }
    
    if (extension_loaded('pdo')) {
      print "  --test-db-dsn <dsn>    DSN for the test database.\n" . "  --test-db-log-rev <r>  Revision information for database logging.\n" . "  --test-db-prefix ...   Prefix that should be stripped from filenames.\n" . "  --test-db-log-info ... Additional information for database logging.\n\n";
    }
    
    print "  --testdox-html <file>  Write agile documentation in HTML format to file.\n" . "  --testdox-text <file>  Write agile documentation in Text format to file.\n\n" . "  --filter <pattern>     Filter which tests to run.\n" . "  --group ...            Only runs tests from the specified group(s).\n" . "  --exclude-group ...    Exclude tests from the specified group(s).\n\n" . "  --loader <loader>      TestSuiteLoader implementation to use.\n" . "  --repeat <times>       Runs the test(s) repeatedly.\n\n" . "  --tap                  Report test execution progress in TAP format.\n" . "  --testdox              Report test execution progress in TestDox format.\n\n" . "  --no-syntax-check      Disable syntax check of test source files.\n" . "  --stop-on-failure      Stop execution upon first error or failure.\n" . "  --verbose              Output more verbose information.\n" . "  --wait                 Waits for a keystroke after each test.\n\n" . "  --skeleton             Generate skeleton UnitTest class for Unit in Unit.php.\n\n" . "  --help                 Prints this usage information.\n" . "  --version              Prints the version and exits.\n\n" . "  --configuration <file> Read configuration from XML file.\n" . "  -d key[=value]         Sets a php.ini value.\n";
  }
}

define('PHPUnit_MAIN_METHOD', 'PHPUnit_TextUI_Command::main');
PHPUnit_TextUI_Command::main();
?>
