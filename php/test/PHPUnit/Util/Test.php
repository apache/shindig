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
 * @version    SVN: $Id: Test.php 1997 2007-12-27 05:07:58Z sb $
 * @link       http://www.phpunit.de/
 * @since      File available since Release 3.0.0
 */

require_once '../PHPUnit/Util/Filter.php';

PHPUnit_Util_Filter::addFileToFilter(__FILE__, 'PHPUNIT');

/**
 * Test helpers.
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
class PHPUnit_Util_Test
{
    /**
     * @param  PHPUnit_Framework_Test $test
     * @param  boolean                $asString
     * @return mixed
     * @access public
     * @static
     */
    public static function describe(PHPUnit_Framework_Test $test, $asString = TRUE)
    {
        if ($asString) {
            if ($test instanceof PHPUnit_Framework_SelfDescribing) {
                return $test->toString();
            } else {
                return get_class($test);
            }
        } else {
            if ($test instanceof PHPUnit_Framework_TestCase) {
                return array(
                  get_class($test), $test->getName()
                );
            }

            else if ($test instanceof PHPUnit_Framework_SelfDescribing) {
                return array('', $test->toString());
            }

            else {
                return array('', get_class($test));
            }
        }
    }

    /**
     * @param  PHPUnit_Framework_Test       $test
     * @param  PHPUnit_Framework_TestResult $result
     * @return mixed
     * @access public
     * @static
     */
    public static function lookupResult(PHPUnit_Framework_Test $test, PHPUnit_Framework_TestResult $result)
    {
        $testName = self::describe($test);

        foreach ($result->errors() as $error) {
            if ($testName == self::describe($error->failedTest())) {
                return $error;
            }
        }

        foreach ($result->failures() as $failure) {
            if ($testName == self::describe($failure->failedTest())) {
                return $failure;
            }
        }

        foreach ($result->notImplemented() as $notImplemented) {
            if ($testName == self::describe($notImplemented->failedTest())) {
                return $notImplemented;
            }
        }

        foreach ($result->skipped() as $skipped) {
            if ($testName == self::describe($skipped->failedTest())) {
                return $skipped;
            }
        }

        return PHPUnit_Runner_BaseTestRunner::STATUS_PASSED;
    }

    /**
     * Returns the files and lines a test method wants to cover.
     *
     * @param  string $className
     * @param  string $methodName
     * @return array
     * @access public
     * @static
     * @since  Method available since Release 3.2.0
     */
    public static function getLinesToBeCovered($className, $methodName)
    {
        $result = array();

        try {
            $class      = new ReflectionClass($className);
            $method     = new ReflectionMethod($className, $methodName);
            $docComment = $class->getDocComment() . $method->getDocComment();

            if (preg_match_all('/@covers[\s]+([\:\.\w]+)/', $docComment, $matches)) {
                foreach ($matches[1] as $method) {
                    if (strpos($method, '::') !== FALSE) {
                        list($className, $methodName) = explode('::', $method);

                        $_method   = new ReflectionMethod($className, $methodName);
                        $fileName  = $_method->getFileName();
                        $startLine = $_method->getStartLine();
                        $endLine   = $_method->getEndLine();

                        if (!isset($result[$fileName])) {
                            $result[$fileName] = array();
                        }

                        $result[$fileName] = array_merge(
                          $result[$fileName],
                          range($startLine, $endLine)
                        );
                    }
                }
            }
        }

        catch (ReflectionException $e) {
        }

        return $result;
    }

    /**
     * Returns the groups for a test class or method.
     *
     * @param  Reflector $reflector
     * @param  array     $groups
     * @return array
     * @access public
     * @static
     * @since  Method available since Release 3.2.0
     */
    public static function getGroups(Reflector $reflector, array $groups = array())
    {
        $docComment = $reflector->getDocComment();

        if (preg_match_all('/@group[\s]+([\.\w]+)/', $docComment, $matches)) {
            $groups = array_merge($groups, $matches[1]);
        }

        return $groups;
    }

    /**
     * Returns the provided data for a method.
     *
     * @param  string $className
     * @param  string $methodName
     * @return array
     * @access public
     * @static
     * @since  Method available since Release 3.2.0
     */
    public static function getProvidedData($className, $methodName)
    {
        $method     = new ReflectionMethod($className, $methodName);
        $docComment = $method->getDocComment();

        if (preg_match('/@dataProvider[\s]+([:\.\w]+)/', $docComment, $matches)) {
            try {
                $dataProvider           = explode('::', $matches[1]);
                $dataProviderMethodName = array_pop($dataProvider);

                if (!empty($dataProvider)) {
                    $dataProviderClassName = join('::', $dataProvider);
                } else {
                    $dataProviderClassName = $className;
                }

                $dataProviderMethod = new ReflectionMethod(
                  $dataProviderClassName, $dataProviderMethodName
                );

                return $dataProviderMethod->invoke(NULL);
            }

            catch (ReflectionException $e) {
            }
        }
    }
}
?>
