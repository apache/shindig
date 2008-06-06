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
 * @version    SVN: $Id: Directory.php 1985 2007-12-26 18:11:55Z sb $
 * @link       http://www.phpunit.de/
 * @since      File available since Release 3.2.0
 */

require_once '../PHPUnit/Util/Filter.php';
require_once '../PHPUnit/Util/Filesystem.php';
require_once '../PHPUnit/Util/Template.php';
require_once '../PHPUnit/Util/Report/Node.php';
require_once '../PHPUnit/Util/Report/Node/File.php';

PHPUnit_Util_Filter::addFileToFilter(__FILE__, 'PHPUNIT');

/**
 * Represents a directory in the code coverage information tree.
 *
 * @category   Testing
 * @package    PHPUnit
 * @author     Sebastian Bergmann <sb@sebastian-bergmann.de>
 * @copyright  2002-2008 Sebastian Bergmann <sb@sebastian-bergmann.de>
 * @license    http://www.opensource.org/licenses/bsd-license.php  BSD License
 * @version    Release: 3.2.9
 * @link       http://www.phpunit.de/
 * @since      Class available since Release 3.2.0
 */
class PHPUnit_Util_Report_Node_Directory extends PHPUnit_Util_Report_Node
{
    /**
     * @var    PHPUnit_Util_Report_Node[]
     * @access protected
     */
    protected $children = array();

    /**
     * @var    PHPUnit_Util_Report_Node_Directory[]
     * @access protected
     */
    protected $directories = array();

    /**
     * @var    PHPUnit_Util_Report_Node_File[]
     * @access protected
     */
    protected $files = array();

    /**
     * @var    array
     * @access protected
     */
    protected $classes;

    /**
     * @var    integer
     * @access protected
     */
    protected $numExecutableLines = -1;

    /**
     * @var    integer
     * @access protected
     */
    protected $numExecutedLines = -1;

    /**
     * @var    integer
     * @access protected
     */
    protected $numClasses = -1;

    /**
     * @var    integer
     * @access protected
     */
    protected $numCalledClasses = -1;

    /**
     * @var    integer
     * @access protected
     */
    protected $numMethods = -1;

    /**
     * @var    integer
     * @access protected
     */
    protected $numCalledMethods = -1;

    /**
     * Adds a new directory.
     *
     * @return PHPUnit_Util_Report_Node_Directory
     * @access public
     */
    public function addDirectory($name)
    {
        $directory = new PHPUnit_Util_Report_Node_Directory($name, $this);

        $this->children[]    = $directory;
        $this->directories[] = &$this->children[count($this->children) - 1];

        return $directory;
    }

    /**
     * Adds a new file.
     *
     * @param  string  $name
     * @param  array   $lines
     * @param  boolean $yui
     * @param  boolean $highlight
     * @return PHPUnit_Util_Report_Node_File
     * @throws RuntimeException
     * @access public
     */
    public function addFile($name, array $lines, $yui, $highlight)
    {
        $file = new PHPUnit_Util_Report_Node_File(
          $name, $this, $lines, $yui, $highlight
        );

        $this->children[] = $file;
        $this->files[]    = &$this->children[count($this->children) - 1];

        $this->numExecutableLines = -1;
        $this->numExecutedLines   = -1;

        return $file;
    }

    /**
     * Returns the directories in this directory.
     *
     * @return
     * @access public
     */
    public function getDirectories()
    {
        return $this->directories;
    }

    /**
     * Returns the files in this directory.
     *
     * @return
     * @access public
     */
    public function getFiles()
    {
        return $this->files;
    }

    /**
     * Returns the classes of this node.
     *
     * @return array
     * @access public
     */
    public function getClasses()
    {
        if ($this->classes === NULL) {
            $this->classes = array();

            foreach ($this->children as $child) {
                $this->classes = array_merge($this->classes, $child->getClasses());
            }
        }

        return $this->classes;
    }

    /**
     * Returns the number of executable lines.
     *
     * @return integer
     * @access public
     */
    public function getNumExecutableLines()
    {
        if ($this->numExecutableLines == -1) {
            $this->numExecutableLines = 0;

            foreach ($this->children as $child) {
                $this->numExecutableLines += $child->getNumExecutableLines();
            }
        }

        return $this->numExecutableLines;
    }

    /**
     * Returns the number of executed lines.
     *
     * @return integer
     * @access public
     */
    public function getNumExecutedLines()
    {
        if ($this->numExecutedLines == -1) {
            $this->numExecutedLines = 0;

            foreach ($this->children as $child) {
                $this->numExecutedLines += $child->getNumExecutedLines();
            }
        }

        return $this->numExecutedLines;
    }

    /**
     * Returns the number of classes.
     *
     * @return integer
     * @access public
     */
    public function getNumClasses()
    {
        if ($this->numClasses == -1) {
            $this->numClasses = 0;

            foreach ($this->children as $child) {
                $this->numClasses += $child->getNumClasses();
            }
        }

        return $this->numClasses;
    }

    /**
     * Returns the number of classes of which at least one method
     * has been called at least once.
     *
     * @return integer
     * @access public
     */
    public function getNumCalledClasses()
    {
        if ($this->numCalledClasses == -1) {
            $this->numCalledClasses = 0;

            foreach ($this->children as $child) {
                $this->numCalledClasses += $child->getNumCalledClasses();
            }
        }

        return $this->numCalledClasses;
    }

    /**
     * Returns the number of methods.
     *
     * @return integer
     * @access public
     */
    public function getNumMethods()
    {
        if ($this->numMethods == -1) {
            $this->numMethods = 0;

            foreach ($this->children as $child) {
                $this->numMethods += $child->getNumMethods();
            }
        }

        return $this->numMethods;
    }

    /**
     * Returns the number of methods that has been called at least once.
     *
     * @return integer
     * @access public
     */
    public function getNumCalledMethods()
    {
        if ($this->numCalledMethods == -1) {
            $this->numCalledMethods = 0;

            foreach ($this->children as $child) {
                $this->numCalledMethods += $child->getNumCalledMethods();
            }
        }

        return $this->numCalledMethods;
    }

    /**
     * Renders this node.
     *
     * @param string  $target
     * @param string  $title
     * @param string  $charset
     * @param boolean $highlight
     * @param integer $lowUpperBound
     * @param integer $highLowerBound
     * @access public
     */
    public function render($target, $title, $charset = 'ISO-8859-1', $highlight = FALSE, $lowUpperBound = 35, $highLowerBound = 70)
    {
        $this->doRender(
          $target, $title, $charset, $highlight, $lowUpperBound, $highLowerBound
        );

        foreach ($this->children as $child) {
            $child->render(
              $target, $title, $charset, $highlight, $lowUpperBound, $highLowerBound
            );
        }
    }

    /**
     * @param string  $target
     * @param string  $title
     * @param string  $charset
     * @param boolean $highlight
     * @param integer $lowUpperBound
     * @param integer $highLowerBound
     * @access protected
     */
    protected function doRender($target, $title, $charset, $highlight, $lowUpperBound, $highLowerBound)
    {
        $cleanId = PHPUnit_Util_Filesystem::getSafeFilename($this->getId());
        $file    = $target . $cleanId . '.html';

        $template = new PHPUnit_Util_Template(
          PHPUnit_Util_Report::$templatePath . 'directory.html'
        );

        $this->setTemplateVars($template, $title, $charset);

        $totalClassesPercent = $this->getCalledClassesPercent();

        list($totalClassesColor, $totalClassesLevel) = $this->getColorLevel(
          $totalClassesPercent, $lowUpperBound, $highLowerBound
        );

        $totalMethodsPercent = $this->getCalledMethodsPercent();

        list($totalMethodsColor, $totalMethodsLevel) = $this->getColorLevel(
          $totalMethodsPercent, $lowUpperBound, $highLowerBound
        );

        $totalLinesPercent = $this->getLineExecutedPercent();

        list($totalLinesColor, $totalLinesLevel) = $this->getColorLevel(
          $totalLinesPercent, $lowUpperBound, $highLowerBound
        );

        $template->setVar(
          array(
            'total_item'       => $this->renderTotalItem($lowUpperBound, $highLowerBound),
            'items'            => $this->renderItems($lowUpperBound, $highLowerBound),
            'low_upper_bound'  => $lowUpperBound,
            'high_lower_bound' => $highLowerBound
          )
        );

        $template->renderTo($file);
    }

    /**
     * @return string
     * @access protected
     */
    protected function renderItems($lowUpperBound, $highLowerBound)
    {
        $items  = $this->doRenderItems($this->directories, $lowUpperBound, $highLowerBound);
        $items .= $this->doRenderItems($this->files, $lowUpperBound, $highLowerBound);

        return $items;
    }

    /**
     * @param  array    $items
     * @return string
     * @access protected
     */
    protected function doRenderItems(array $items, $lowUpperBound, $highLowerBound)
    {
        $result = '';

        foreach ($items as $item) {
            $result .= $this->doRenderItemObject($item, $lowUpperBound, $highLowerBound);
        }

        return $result;
    }
}
?>
