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
 * @version    SVN: $Id: TestResult.php 2126 2008-01-16 06:21:19Z sb $
 * @link       http://www.phpunit.de/
 * @since      File available since Release 2.0.0
 */

require_once 'PHPUnit/Framework.php';
require_once 'PHPUnit/Util/ErrorHandler.php';
require_once 'PHPUnit/Util/Filter.php';
require_once 'PHPUnit/Util/Printer.php';
require_once 'PHPUnit/Util/Test.php';
require_once 'PHPUnit/Util/Timer.php';

PHPUnit_Util_Filter::addFileToFilter(__FILE__, 'PHPUNIT');

if (! class_exists('PHPUnit_Framework_TestResult', FALSE)) {

  /**
   * A TestResult collects the results of executing a test case.
   *
   * @category   Testing
   * @package    PHPUnit
   * @author     Sebastian Bergmann <sb@sebastian-bergmann.de>
   * @copyright  2002-2008 Sebastian Bergmann <sb@sebastian-bergmann.de>
   * @license    http://www.opensource.org/licenses/bsd-license.php  BSD License
   * @version    Release: 3.2.9
   * @link       http://www.phpunit.de/
   * @since      Class available since Release 2.0.0
   */
  class PHPUnit_Framework_TestResult implements Countable {
    protected static $xdebugLoaded = null;
    protected static $useXdebug = null;
    
    /**
     * @var    array
     * @access protected
     */
    protected $errors = array();
    
    /**
     * @var    array
     * @access protected
     */
    protected $failures = array();
    
    /**
     * @var    array
     * @access protected
     */
    protected $notImplemented = array();
    
    /**
     * @var    array
     * @access protected
     */
    protected $skipped = array();
    
    /**
     * @var    array
     * @access protected
     */
    protected $listeners = array();
    
    /**
     * @var    integer
     * @access protected
     */
    protected $runTests = 0;
    
    /**
     * @var    float
     * @access protected
     */
    protected $time = 0;
    
    /**
     * @var    PHPUnit_Framework_TestSuite
     * @access protected
     */
    protected $topTestSuite = NULL;
    
    /**
     * Code Coverage information provided by Xdebug.
     *
     * @var    array
     * @access protected
     */
    protected $codeCoverageInformation = array();
    
    /**
     * @var    boolean
     * @access protected
     */
    protected $collectCodeCoverageInformation = FALSE;
    
    /**
     * @var    boolean
     * @access protected
     */
    protected $stop = FALSE;
    
    /**
     * @var    boolean
     * @access protected
     */
    protected $stopOnFailure = FALSE;

    /**
     * Registers a TestListener.
     *
     * @param  PHPUnit_Framework_TestListener
     * @access public
     */
    public function addListener(PHPUnit_Framework_TestListener $listener) {
      $this->listeners[] = $listener;
    }

    /**
     * Unregisters a TestListener.
     *
     * @param  PHPUnit_Framework_TestListener $listener
     * @access public
     */
    public function removeListener(PHPUnit_Framework_TestListener $listener) {
      foreach ($this->listeners as $key => $_listener) {
        if ($listener === $_listener) {
          unset($this->listeners[$key]);
        }
      }
    }

    /**
     * Flushes all flushable TestListeners.
     *
     * @access public
     * @since  Method available since Release 3.0.0
     */
    public function flushListeners() {
      foreach ($this->listeners as $listener) {
        if ($listener instanceof PHPUnit_Util_Printer) {
          $listener->flush();
        }
      }
    }

    /**
     * Adds an error to the list of errors.
     * The passed in exception caused the error.
     *
     * @param  PHPUnit_Framework_Test $test
     * @param  Exception              $e
     * @param  float                  $time
     * @access public
     */
    public function addError(PHPUnit_Framework_Test $test, Exception $e, $time) {
      if ($e instanceof PHPUnit_Framework_IncompleteTest) {
        $this->notImplemented[] = new PHPUnit_Framework_TestFailure($test, $e);
        $notifyMethod = 'addIncompleteTest';
      } 

      else if ($e instanceof PHPUnit_Framework_SkippedTest) {
        $this->skipped[] = new PHPUnit_Framework_TestFailure($test, $e);
        $notifyMethod = 'addSkippedTest';
      } 

      else {
        $this->errors[] = new PHPUnit_Framework_TestFailure($test, $e);
        $notifyMethod = 'addError';
        
        if ($this->stopOnFailure) {
          $this->stop();
        }
      }
      
      foreach ($this->listeners as $listener) {
        $listener->$notifyMethod($test, $e, $time);
      }
    }

    /**
     * Adds a failure to the list of failures.
     * The passed in exception caused the failure.
     *
     * @param  PHPUnit_Framework_Test                 $test
     * @param  PHPUnit_Framework_AssertionFailedError $e
     * @param  float                                  $time
     * @access public
     */
    public function addFailure(PHPUnit_Framework_Test $test, PHPUnit_Framework_AssertionFailedError $e, $time) {
      if ($e instanceof PHPUnit_Framework_IncompleteTest) {
        $this->notImplemented[] = new PHPUnit_Framework_TestFailure($test, $e);
        $notifyMethod = 'addIncompleteTest';
      } 

      else if ($e instanceof PHPUnit_Framework_SkippedTest) {
        $this->skipped[] = new PHPUnit_Framework_TestFailure($test, $e);
        $notifyMethod = 'addSkippedTest';
      } 

      else {
        $this->failures[] = new PHPUnit_Framework_TestFailure($test, $e);
        $notifyMethod = 'addFailure';
        
        if ($this->stopOnFailure) {
          $this->stop();
        }
      }
      
      foreach ($this->listeners as $listener) {
        $listener->$notifyMethod($test, $e, $time);
      }
    }

    /**
     * Informs the result that a testsuite will be started.
     *
     * @param  PHPUnit_Framework_TestSuite $suite
     * @access public
     * @since  Method available since Release 2.2.0
     */
    public function startTestSuite(PHPUnit_Framework_TestSuite $suite) {
      if ($this->topTestSuite === NULL) {
        $this->topTestSuite = $suite;
      }
      
      foreach ($this->listeners as $listener) {
        $listener->startTestSuite($suite);
      }
    }

    /**
     * Informs the result that a testsuite was completed.
     *
     * @param  PHPUnit_Framework_TestSuite $suite
     * @access public
     * @since  Method available since Release 2.2.0
     */
    public function endTestSuite(PHPUnit_Framework_TestSuite $suite) {
      foreach ($this->listeners as $listener) {
        $listener->endTestSuite($suite);
      }
    }

    /**
     * Informs the result that a test will be started.
     *
     * @param  PHPUnit_Framework_Test $test
     * @access public
     */
    public function startTest(PHPUnit_Framework_Test $test) {
      $this->runTests += count($test);
      
      foreach ($this->listeners as $listener) {
        $listener->startTest($test);
      }
    }

    /**
     * Informs the result that a test was completed.
     *
     * @param  PHPUnit_Framework_Test $test
     * @param  float                  $time
     * @access public
     */
    public function endTest(PHPUnit_Framework_Test $test, $time) {
      foreach ($this->listeners as $listener) {
        $listener->endTest($test, $time);
      }
    }

    /**
     * Returns TRUE if no incomplete test occured.
     *
     * @return boolean
     * @access public
     */
    public function allCompletlyImplemented() {
      return $this->notImplementedCount() == 0;
    }

    /**
     * Gets the number of incomplete tests.
     *
     * @return integer
     * @access public
     */
    public function notImplementedCount() {
      return count($this->notImplemented);
    }

    /**
     * Returns an Enumeration for the incomplete tests.
     *
     * @return array
     * @access public
     */
    public function notImplemented() {
      return $this->notImplemented;
    }

    /**
     * Returns TRUE if no test has been skipped.
     *
     * @return boolean
     * @access public
     * @since  Method available since Release 3.0.0
     */
    public function noneSkipped() {
      return $this->skippedCount() == 0;
    }

    /**
     * Gets the number of skipped tests.
     *
     * @return integer
     * @access public
     * @since  Method available since Release 3.0.0
     */
    public function skippedCount() {
      return count($this->skipped);
    }

    /**
     * Returns an Enumeration for the skipped tests.
     *
     * @return array
     * @access public
     * @since  Method available since Release 3.0.0
     */
    public function skipped() {
      return $this->skipped;
    }

    /**
     * Gets the number of detected errors.
     *
     * @return integer
     * @access public
     */
    public function errorCount() {
      return count($this->errors);
    }

    /**
     * Returns an Enumeration for the errors.
     *
     * @return array
     * @access public
     */
    public function errors() {
      return $this->errors;
    }

    /**
     * Gets the number of detected failures.
     *
     * @return integer
     * @access public
     */
    public function failureCount() {
      return count($this->failures);
    }

    /**
     * Returns an Enumeration for the failures.
     *
     * @return array
     * @access public
     */
    public function failures() {
      return $this->failures;
    }

    /**
     * Returns the (top) test suite.
     *
     * @return PHPUnit_Framework_TestSuite
     * @access public
     * @since  Method available since Release 3.0.0
     */
    public function topTestSuite() {
      return $this->topTestSuite;
    }

    /**
     * Enables or disables the collection of Code Coverage information.
     *
     * @param  boolean $flag
     * @throws InvalidArgumentException
     * @access public
     * @since  Method available since Release 2.3.0
     */
    public function collectCodeCoverageInformation($flag) {
      if (is_bool($flag)) {
        $this->collectCodeCoverageInformation = $flag;
      } else {
        throw new InvalidArgumentException();
      }
    }

    /**
     * Returns whether code coverage information should be collected.
     *
     * @return boolean If code coverage should be collected
     * @since  Method available since Release 3.2.0
     */
    public function getCollectCodeCoverageInformation() {
      return $this->collectCodeCoverageInformation;
    }

    /**
     * Appends code coverage information to the test
     *
     * @param PHPUnit_Framework_Test $test
     * @param array                  $data
     * @since Method available since Release 3.2.0
     */
    public function appendCodeCoverageInformation(PHPUnit_Framework_Test $test, $data) {
      if ($test instanceof PHPUnit_Framework_TestCase) {
        $linesToBeCovered = PHPUnit_Util_Test::getLinesToBeCovered(get_class($test), $test->getName());
        
        if (! empty($linesToBeCovered)) {
          $filesToBeCovered = array_keys($linesToBeCovered);
          $filesCovered = array_keys($data);
          $filesCovered = array_intersect($filesCovered, $filesToBeCovered);
          
          foreach ($filesCovered as $file) {
            $linesCovered = array_keys($data[$file]);
            $linesNotToCover = array_diff($linesCovered, $linesToBeCovered[$file]);
            
            foreach ($linesNotToCover as $line) {
              if ($data[$file][$line] > 0) {
                $data[$file][$line] = - 1;
              }
            }
          }
        }
      }
      
      $this->codeCoverageInformation[] = array('test' => $test, 'files' => $data);
    }

    /**
     * Returns Code Coverage data per test case.
     *
     * Format of the result array:
     *
     * <code>
     * array(
     *   array(
     *     'test'  => PHPUnit_Framework_Test
     *     'files' => array(
     *       "/tested/code.php" => array(
     *         linenumber => flag
     *       )
     *     )
     *   )
     * )
     * </code>
     *
     * flag < 0: Line is executable but was not executed.
     * flag > 0: Line was executed.
     *
     * @param  boolean $filterTests
     * @param  boolean $filterPHPUnit
     * @return array
     * @access public
     */
    public function getCodeCoverageInformation($filterTests = TRUE, $filterPHPUnit = TRUE) {
      return PHPUnit_Util_Filter::getFilteredCodeCoverage($this->codeCoverageInformation, $filterTests, $filterPHPUnit);
    }

    /**
     * Returns unfiltered Code Coverage data per test case.
     * Returns data in the same form as getCodeCoverageInformation().
     *
     * @return array
     * @access public
     */
    public function getUncoveredWhitelistFiles() {
      list(, $missing) = PHPUnit_Util_Filter::getFileCodeCoverageDisposition($this->codeCoverageInformation);
      
      return ($missing);
    }

    /**
     * Runs a TestCase.
     *
     * @param  PHPUnit_Framework_Test $test
     * @access public
     */
    public function run(PHPUnit_Framework_Test $test) {
      $error = FALSE;
      $failure = FALSE;
      
      $this->startTest($test);
      
      $errorHandlerSet = FALSE;
      
      $oldErrorHandler = set_error_handler('PHPUnit_Util_ErrorHandler', E_ALL | E_STRICT);
      
      if ($oldErrorHandler === NULL) {
        $errorHandlerSet = TRUE;
      } else {
        restore_error_handler();
      }
      $oldErrorHandler = set_error_handler('PHPUnit_Util_ErrorHandler', E_ALL | E_STRICT);
      
      if ($oldErrorHandler === NULL) {
        $errorHandlerSet = TRUE;
      } else {
        restore_error_handler();
      }
      
      if (self::$xdebugLoaded === NULL) {
        self::$xdebugLoaded = extension_loaded('xdebug');
        self::$useXdebug = self::$xdebugLoaded;
      }
      
      $useXdebug = self::$useXdebug && $this->collectCodeCoverageInformation && ! $test instanceof PHPUnit_Extensions_SeleniumTestCase;
      
      if ($useXdebug) {
        xdebug_start_code_coverage(XDEBUG_CC_UNUSED | XDEBUG_CC_DEAD_CODE);
      }
      
      PHPUnit_Util_Timer::start();
      
      try {
        $test->runBare();
      } 

      catch (PHPUnit_Framework_AssertionFailedError $e) {
        $failure = TRUE;
      } 

      catch (Exception $e) {
        $error = TRUE;
      }
      
      $time = PHPUnit_Util_Timer::stop();
      
      if ($useXdebug) {
        $codeCoverage = xdebug_get_code_coverage();
        xdebug_stop_code_coverage();
        
        $this->appendCodeCoverageInformation($test, $codeCoverage);
      }
      
      if ($errorHandlerSet === TRUE) {
        restore_error_handler();
      }
      
      if ($error === TRUE) {
        $this->addError($test, $e, $time);
      } 

      else if ($failure === TRUE) {
        $this->addFailure($test, $e, $time);
      }
      
      $this->endTest($test, $time);
      
      $this->time += $time;
    }

    /**
     * Gets the number of run tests.
     *
     * @return integer
     * @access public
     */
    public function count() {
      return $this->runTests;
    }

    /**
     * Checks whether the test run should stop.
     *
     * @return boolean
     * @access public
     */
    public function shouldStop() {
      return $this->stop;
    }

    /**
     * Marks that the test run should stop.
     *
     * @access public
     */
    public function stop() {
      $this->stop = TRUE;
    }

    /**
     * Enables or disables the stopping when a failure or error occurs.
     *
     * @param  boolean $flag
     * @throws InvalidArgumentException
     * @access public
     * @since  Method available since Release 3.1.0
     */
    public function stopOnFailure($flag) {
      if (is_bool($flag)) {
        $this->stopOnFailure = $flag;
      } else {
        throw new InvalidArgumentException();
      }
    }

    /**
     * Returns the time spent running the tests.
     *
     * @return float
     * @access public
     */
    public function time() {
      return $this->time;
    }

    /**
     * Returns whether the entire test was successful or not.
     *
     * @return boolean
     * @access public
     */
    public function wasSuccessful() {
      return empty($this->errors) && empty($this->failures);
    }
  }

}
?>
