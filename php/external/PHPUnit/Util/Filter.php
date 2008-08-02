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
 * @version    SVN: $Id: Filter.php 2041 2008-01-08 10:00:39Z sb $
 * @link       http://www.phpunit.de/
 * @since      File available since Release 2.0.0
 */

require_once 'PHPUnit/Util/FilterIterator.php';

/**
 * Utility class for code filtering.
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
class PHPUnit_Util_Filter
{
    /**
     * @var    boolean
     * @access public
     * @static
     */
    public static $addUncoveredFilesFromWhitelist = TRUE;

    /**
     * @var    boolean
     * @access public
     * @static
     */
    public static $filterPHPUnit = TRUE;

    /**
     * @var    boolean
     * @access protected
     * @static
     */
    protected static $filter = TRUE;

    /**
     * @var    boolean
     * @access protected
     * @static
     */
    protected static $blackListConverstionForWindowsDone = FALSE;

    /**
     * Source files that are blacklisted.
     *
     * @var    array
     * @access protected
     * @static
     */
    protected static $blacklistedFiles = array(
      'DEFAULT' => array(),
      'PHPUNIT' => array(),
      'TESTS' => array(),
      'PEAR' => array(
        'Console/Getopt.php',
        'Image/GraphViz.php',
        'Log/composite.php',
        'Log/console.php',
        'Log/daemon.php',
        'Log/display.php',
        'Log/error_log.php',
        'Log/file.php',
        'Log/mail.php',
        'Log/mcal.php',
        'Log/mdb2.php',
        'Log/null.php',
        'Log/observer.php',
        'Log/sql.php',
        'Log/sqlite.php',
        'Log/syslog.php',
        'Log/win.php',
        'Log.php',
        'PEAR/Installer/Role/Common.php',
        'PEAR/Installer/Role.php',
        'PEAR/Config.php',
        'PEAR/DependencyDB.php',
        'PEAR/Registry.php',
        'PEAR/Remote.php',
        'PEAR/RunTest.php',
        'PEAR/XMLParser.php',
        'PEAR.php',
        'System.php'
      )
    );

    /**
     * Source files that are whitelisted.
     *
     * @var    array
     * @access protected
     * @static
     */
    protected static $whitelistedFiles = array();

    /**
     * Adds a directory to the blacklist (recursively).
     *
     * @param  string $directory
     * @param  string $suffix
     * @param  string $group
     * @access public
     * @static
     * @since  Method available since Release 3.1.5
     */
    public static function addDirectoryToFilter($directory, $suffix = '.php', $group = 'DEFAULT')
    {
        if (file_exists($directory)) {
            foreach (self::getIterator($directory, $suffix) as $file) {
                self::addFileToFilter($file->getPathName(), $group);
            }
        }
    }

    /**
     * Adds a new file to be filtered (blacklist).
     *
     * @param  string $filename
     * @param  string $group
     * @access public
     * @static
     * @since  Method available since Release 2.1.0
     */
    public static function addFileToFilter($filename, $group = 'DEFAULT')
    {
        if (file_exists($filename)) {
            $filename = realpath($filename);

            if (!isset(self::$blacklistedFiles[$group])) {
                self::$blacklistedFiles[$group] = array($filename);
            }

            else if (!in_array($filename, self::$blacklistedFiles[$group])) {
                self::$blacklistedFiles[$group][] = $filename;
            }
        }
    }

    /**
     * Removes a directory from the blacklist (recursively).
     *
     * @param  string $directory
     * @param  string $suffix
     * @param  string $group
     * @access public
     * @static
     * @since  Method available since Release 3.1.5
     */
    public static function removeDirectoryFromFilter($directory, $suffix = '.php', $group = 'DEFAULT')
    {
        if (file_exists($directory)) {
            foreach (self::getIterator($directory, $suffix) as $file) {
                self::removeFileFromFilter($file->getPathName(), $group);
            }
        }
    }

    /**
     * Removes a file from the filter (blacklist).
     *
     * @param  string $filename
     * @param  string $group
     * @access public
     * @static
     * @since  Method available since Release 2.1.0
     */
    public static function removeFileFromFilter($filename, $group = 'DEFAULT')
    {
        if (file_exists($filename)) {
            if (isset(self::$blacklistedFiles[$group])) {
                $filename = realpath($filename);

                foreach (self::$blacklistedFiles[$group] as $key => $_filename) {
                    if ($filename == $_filename) {
                        unset(self::$blacklistedFiles[$group][$key]);
                    }
                }
            }
        }
    }

    /**
     * Adds a directory to the whitelist (recursively).
     *
     * @param  string $directory
     * @param  string $suffix
     * @access public
     * @static
     * @since  Method available since Release 3.1.5
     */
    public static function addDirectoryToWhitelist($directory, $suffix = '.php')
    {
        if (file_exists($directory)) {
            foreach (self::getIterator($directory, $suffix) as $file) {
                self::addFileToWhitelist($file->getPathName());
            }
        }
    }

    /**
     * Adds a new file to the whitelist.
     *
     * When the whitelist is empty (default), blacklisting is used.
     * When the whitelist is not empty, whitelisting is used.
     *
     * @param  string $filename
     * @access public
     * @static
     * @since  Method available since Release 3.1.0
     */
    public static function addFileToWhitelist($filename)
    {
        if (file_exists($filename)) {
            $filename = realpath($filename);

            if (!in_array($filename, self::$whitelistedFiles)) {
                self::$whitelistedFiles[] = $filename;
            }
        }
    }

    /**
     * Removes a directory from the whitelist (recursively).
     *
     * @param  string $directory
     * @param  string $suffix
     * @access public
     * @static
     * @since  Method available since Release 3.1.5
     */
    public static function removeDirectoryFromWhitelist($directory, $suffix = '.php')
    {
        if (file_exists($directory)) {
            foreach (self::getIterator($directory, $suffix) as $file) {
                self::removeFileFromWhitelist($file->getPathName());
            }
        }
    }

    /**
     * Removes a file from the whitelist.
     *
     * @param  string $filename
     * @access public
     * @static
     * @since  Method available since Release 3.1.0
     */
    public static function removeFileFromWhitelist($filename)
    {
        if (file_exists($filename)) {
            $filename = realpath($filename);

            foreach (self::$whitelistedFiles as $key => $_filename) {
                if ($filename == $_filename) {
                    unset(self::$whitelistedFiles[$key]);
                }
            }
        }
    }

    /**
     * Returns data about files within code coverage information, specifically
     * which ones will be filtered out and which ones may be whitelisted but not
     * touched by coverage.
     * 
     * Returns a two-item array. The first item is an array indexed by filenames 
     * with a boolean payload of whether they should be filtered out.
     * 
     * The second item is an array of filenames which are 
     * whitelisted but which are absent from the coverage information.
     *
     * @param  array   $codeCoverageInformation
     * @param  boolean $filterTests
     * @return array
     * @access public
     * @static
     */
    public static function getFileCodeCoverageDisposition(array $codeCoverageInformation, $filterTests = TRUE)
    {
        if (!self::$filter) {
            return array(array(), array());
        }             

        $isFilteredCache = array();
        $coveredFiles    = array();

        foreach ($codeCoverageInformation as $k => $test) {
            foreach (array_keys($test['files']) as $file) {
                if (!isset($isFilteredCache[$file])) {
                    $isFilteredCache[$file] = self::isFiltered(
                      $file, $filterTests
                    );
                }
            }
        }        

        $coveredFiles = array_keys($isFilteredCache);
        $missedFiles  = array_diff(self::$whitelistedFiles,$coveredFiles);                
        $missedFiles  = array_filter($missedFiles,'file_exists');

        return array($isFilteredCache,$missedFiles);
    }
    
    /**
     * @param  array   $codeCoverageInformation
     * @param  boolean $addUncoveredFilesFromWhitelist
     * @return array
     * @access public
     * @static
     */
    public static function getFilteredCodeCoverage(array $codeCoverageInformation, $filterTests = TRUE)
    {
        if (self::$filter) {
            list($isFilteredCache, $missedFiles) = self::getFileCodeCoverageDisposition(
              $codeCoverageInformation, $filterTests
            );

            foreach ($codeCoverageInformation as $k => $test) {
                foreach (array_keys($test['files']) as $file) {
                    if ($isFilteredCache[$file]) {
                        unset($codeCoverageInformation[$k]['files'][$file]);
                    }
                }
            }

            if (self::$addUncoveredFilesFromWhitelist) {
                foreach ($missedFiles as $missedFile) {
                    xdebug_start_code_coverage(XDEBUG_CC_UNUSED | XDEBUG_CC_DEAD_CODE);
                    include_once $missedFile;
                    $coverage = xdebug_get_code_coverage();
                    xdebug_stop_code_coverage();

                    if (isset($coverage[$missedFile])) {
                        foreach ($coverage[$missedFile] as $line => $flag) {
                            if ($flag > 0) {
                                $coverage[$missedFile][$line] = -1;
                            }
                        }

                        $codeCoverageInformation[] = array(
                          'test'  => NULL,
                          'files' => array(
                            $missedFile => $coverage[$missedFile]
                          )
                        );
                    }
                }
            }
        }

        return $codeCoverageInformation;
    }

    /**
     * Filters stack frames from PHPUnit classes.
     *
     * @param  Exception $e
     * @param  boolean   $filterTests
     * @param  boolean   $asString
     * @return string
     * @access public
     * @static
     */
    public static function getFilteredStacktrace(Exception $e, $filterTests = TRUE, $asString = TRUE)
    {
        if ($asString === TRUE) {
            $filteredStacktrace = '';
        } else {
            $filteredStacktrace = array();
        }

        foreach ($e->getTrace() as $frame) {
            if (!self::$filter || (isset($frame['file']) && !self::isFiltered($frame['file'], $filterTests, TRUE))) {
                if ($asString === TRUE) {
                    $filteredStacktrace .= sprintf(
                      "%s:%s\n",

                      $frame['file'],
                      isset($frame['line']) ? $frame['line'] : '?'
                    );
                } else {
                    $filteredStacktrace[] = $frame;
                }
            }
        }

        return $filteredStacktrace;
    }

    /**
     * Activates or deactivates filtering.
     *
     * @param  boolean $filter
     * @throws InvalidArgumentException
     * @access public
     * @static
     * @since  Method available since Release 3.0.0
     */
    public static function setFilter($filter)
    {
        if (is_bool($filter)) {
            self::$filter = $filter;
        } else {
            throw new InvalidArgumentException;
        }
    }

    /**
     * Returns a PHPUnit_Util_FilterIterator that iterates
     * over all files in the given directory that have the
     * given suffix.
     *
     * @param  string $directory
     * @param  string $suffix
     * @return Iterator
     * @access protected
     * @static
     * @since  Method available since Release 3.1.5
     */
    protected static function getIterator($directory, $suffix)
    {
        return new PHPUnit_Util_FilterIterator(
          new RecursiveIteratorIterator(
            new RecursiveDirectoryIterator($directory)
          ),
          $suffix
        );
    }

    /**
     * @param  string  $filename
     * @param  boolean $filterTests
     * @param  boolean $ignoreWhitelist
     * @return boolean
     * @access protected
     * @static
     * @since  Method available since Release 2.1.3
     */
    protected static function isFiltered($filename, $filterTests = TRUE, $ignoreWhitelist = FALSE)
    {
        $filename = realpath($filename);

        // Use blacklist.
        if ($ignoreWhitelist || empty(self::$whitelistedFiles)) {
            if (DIRECTORY_SEPARATOR == '\\' &&
                !self::$blackListConverstionForWindowsDone) {
                $count = count(self::$blacklistedFiles['PEAR']);

                for ($i = 0; $i < $count; $i++) {
                    self::$blacklistedFiles['PEAR'][$i] = str_replace(
                      '/', '\\', self::$blacklistedFiles['PEAR'][$i]
                    );
                }

                self::$blackListConverstionForWindowsDone = TRUE;
            }

            $blacklistedFiles = array_merge(
              self::$blacklistedFiles['DEFAULT'],
              self::$blacklistedFiles['PEAR']
            );

            if ($filterTests) {
                $blacklistedFiles = array_merge(
                  $blacklistedFiles,
                  self::$blacklistedFiles['TESTS']
                );
            }

            if (self::$filterPHPUnit) {
                $blacklistedFiles = array_merge(
                  $blacklistedFiles,
                  self::$blacklistedFiles['PHPUNIT']
                );
            }

            if (in_array($filename, $blacklistedFiles)) {
                return TRUE;
            }

            foreach ($blacklistedFiles as $filteredFile) {
                if (strpos($filename, $filteredFile) !== FALSE) {
                    return TRUE;
                }
            }

            return FALSE;
        }

        // Use whitelist.
        else
        {
            if (in_array($filename, self::$whitelistedFiles)) {
                return FALSE;
            }

            return TRUE;
        }
    }
}

PHPUnit_Util_Filter::addFileToFilter(__FILE__, 'PHPUNIT');
?>
