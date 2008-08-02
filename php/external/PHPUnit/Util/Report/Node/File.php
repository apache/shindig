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
 * @version    SVN: $Id: File.php 1985 2007-12-26 18:11:55Z sb $
 * @link       http://www.phpunit.de/
 * @since      File available since Release 3.2.0
 */

require_once 'PHPUnit/Util/Filter.php';
require_once 'PHPUnit/Util/Filesystem.php';
require_once 'PHPUnit/Util/Template.php';
require_once 'PHPUnit/Util/Report/Node.php';

PHPUnit_Util_Filter::addFileToFilter(__FILE__, 'PHPUNIT');

/**
 *
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
class PHPUnit_Util_Report_Node_File extends PHPUnit_Util_Report_Node
{
    /**
     * @var    array
     * @access protected
     */
    protected $codeLines;

    /**
     * @var    array
     * @access protected
     */
    protected $codeLinesFillup = array();

    /**
     * @var    array
     * @access protected
     */
    protected $executedLines;

    /**
     * @var    boolean
     * @access protected
     */
    protected $yui = TRUE;

    /**
     * @var    boolean
     * @access protected
     */
    protected $highlight = FALSE;

    /**
     * @var    integer
     * @access protected
     */
    protected $numExecutableLines = 0;

    /**
     * @var    integer
     * @access protected
     */
    protected $numExecutedLines = 0;

    /**
     * @var    array
     * @access protected
     */
    protected $classes = array();

    /**
     * @var    integer
     * @access protected
     */
    protected $numClasses = 0;

    /**
     * @var    integer
     * @access protected
     */
    protected $numCalledClasses = 0;

    /**
     * @var    integer
     * @access protected
     */
    protected $numMethods = 0;

    /**
     * @var    integer
     * @access protected
     */
    protected $numCalledMethods = 0;

    /**
     * @var    string
     * @access protected
     */
    protected $yuiPanelJS = '';

    /**
     * Constructor.
     *
     * @param  string                   $name
     * @param  PHPUnit_Util_Report_Node $parent
     * @param  array                    $executedLines
     * @param  boolean                  $yui
     * @param  boolean                  $highlight
     * @throws RuntimeException
     * @access public
     */
    public function __construct($name, PHPUnit_Util_Report_Node $parent = NULL, array $executedLines, $yui = TRUE, $highlight = FALSE)
    {
        parent::__construct($name, $parent);

        $path = $this->getPath();

        if (!file_exists($path)) {
            throw new RuntimeException;
        }

        $this->executedLines = $executedLines;
        $this->highlight     = $highlight;
        $this->yui           = $yui;
        $this->codeLines     = $this->loadFile($path);

        $this->calculateStatistics();
    }

    /**
     * Returns the classes of this node.
     *
     * @return array
     * @access public
     */
    public function getClasses()
    {
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
        if ($this->yui) {
            $template = new PHPUnit_Util_Template(
              PHPUnit_Util_Report::$templatePath . 'file.html'
            );

            $yuiTemplate = new PHPUnit_Util_Template(
              PHPUnit_Util_Report::$templatePath . 'yui_item.js'
            );
        } else {
            $template = new PHPUnit_Util_Template(
              PHPUnit_Util_Report::$templatePath . 'file_no_yui.html'
            );
        }

        $i      = 1;
        $lines  = '';
        $ignore = FALSE;

        foreach ($this->codeLines as $line) {
            if (strpos($line, '@codeCoverageIgnore') !== FALSE) {
                if (strpos($line, '@codeCoverageIgnoreStart') !== FALSE) {
                    $ignore = TRUE;
                }

                else if (strpos($line, '@codeCoverageIgnoreEnd') !== FALSE) {
                    $ignore = FALSE;
                }
            }

            $css = '';

            if (!$ignore && isset($this->executedLines[$i])) {
                $count = '';

                // Array: Line is executable and was executed.
                // count(Array) = Number of tests that hit this line.
                if (is_array($this->executedLines[$i])) {
                    $color    = 'lineCov';
                    $numTests = count($this->executedLines[$i]);
                    $count    = sprintf('%8d', $numTests);

                    if ($this->yui) {
                        $buffer = '';

                        foreach ($this->executedLines[$i] as $test) {
                            if (!isset($test->__liHtml)) {
                                $test->__liHtml = '';

                                if ($test instanceof PHPUnit_Framework_SelfDescribing) {
                                    $testName = $test->toString();

                                    if ($test instanceof PHPUnit_Framework_TestCase) {
                                        switch ($test->getStatus()) {
                                            case PHPUnit_Runner_BaseTestRunner::STATUS_PASSED: {
                                                $testCSS = ' class=\"testPassed\"';
                                            }
                                            break;

                                            case PHPUnit_Runner_BaseTestRunner::STATUS_FAILURE: {
                                                $testCSS = ' class=\"testFailure\"';
                                            }
                                            break;

                                            case PHPUnit_Runner_BaseTestRunner::STATUS_ERROR: {
                                                $testCSS = ' class=\"testError\"';
                                            }
                                            break;

                                            case PHPUnit_Runner_BaseTestRunner::STATUS_INCOMPLETE:
                                            case PHPUnit_Runner_BaseTestRunner::STATUS_SKIPPED: {
                                                $testCSS = ' class=\"testIncomplete\"';
                                            }
                                            break;

                                            default: {
                                                $testCSS = '';
                                            }
                                        }
                                    }
                                }

                                $test->__liHtml .= sprintf(
                                  '<li%s>%s</li>',

                                  $testCSS,
                                  $testName
                                );
                            }

                            $buffer .= $test->__liHtml;
                        }

                        if ($numTests > 1) {
                            $header = $numTests . ' tests cover';
                        } else {
                            $header = '1 test covers';
                        }

                        $header .= ' line ' . $i;

                        $yuiTemplate->setVar(
                          array(
                            'line'   => $i,
                            'header' => $header,
                            'tests'  => $buffer
                          ),
                          FALSE
                        );

                        $this->yuiPanelJS .= $yuiTemplate->render();
                    }
                }

                // -1: Line is executable and was not executed.
                else if ($this->executedLines[$i] == -1) {
                    $color = 'lineNoCov';
                    $count = sprintf('%8d', 0);
                }

                // -2: Line is dead code.
                else {
                    $color = 'lineDeadCode';
                    $count = '        ';
                }

                $css = sprintf(
                  '<span class="%s">       %s : ',

                  $color,
                  $count
                );
            }

            $fillup = array_shift($this->codeLinesFillup);

            if ($fillup > 0) {
                $line .= str_repeat(' ', $fillup);
            }

            $lines .= sprintf(
              '<span class="lineNum" id="container%d"><a name="%d"></a><a href="#%d" id="line%d">%8d</a> </span>%s%s%s' . "\n",

              $i,
              $i,
              $i,
              $i,
              $i,
              !empty($css) ? $css : '                : ',
              !$this->highlight ? htmlspecialchars($line) : $line,
              !empty($css) ? '</span>' : ''
            );

            $i++;
        }

        $items = '';

        foreach ($this->classes as $className => $classData) {
            $numCalledClasses     = $classData['executedLines'] > 0 ? 1   : 0;
            $calledClassesPercent = $numCalledClasses == 1          ? 100 : 0;

            $numCalledMethods = 0;
            $numMethods       = count($classData['methods']);

            foreach ($classData['methods'] as $method) {
                if ($method['executedLines'] > 0) {
                    $numCalledMethods++;
                }
            }

            $items .= $this->doRenderItem(
              array(
                'name'                 => sprintf(
                  '<b><a href="#%d">%s</a></b>',

                  $classData['startLine'],
                  $className
                ),
                'numClasses'           => 1,
                'numCalledClasses'     => $numCalledClasses,
                'calledClassesPercent' => sprintf('%01.2f', $calledClassesPercent),
                'numMethods'           => $numMethods,
                'numCalledMethods'     => $numCalledMethods,
                'calledMethodsPercent' => $this->calculatePercent(
                  $numCalledMethods, $numMethods
                ),
                'numExecutableLines'   => $classData['executableLines'],
                'numExecutedLines'     => $classData['executedLines'],
                'executedLinesPercent' => $this->calculatePercent(
                  $classData['executedLines'], $classData['executableLines']
                )
              ),
              $lowUpperBound,
              $highLowerBound
            );

            foreach ($classData['methods'] as $methodName => $methodData) {
                $numCalledMethods     = $methodData['executedLines'] > 0 ? 1  : 0;
                $calledMethodsPercent = $numCalledMethods == 1           ? 100 : 0;

                $items .= $this->doRenderItem(
                  array(
                    'name'                 => sprintf(
                      '&nbsp;<a href="#%d">%s</a>',

                      $methodData['startLine'],
                      PHPUnit_Util_Class::getMethodSignature(
                        new ReflectionMethod($className, $methodName)
                      )
                    ),
                    'numClasses'           => '',
                    'numCalledClasses'     => '',
                    'calledClassesPercent' => '',
                    'numMethods'           => 1,
                    'numCalledMethods'     => $numCalledMethods,
                    'calledMethodsPercent' => sprintf('%01.2f', $calledMethodsPercent),
                    'numExecutableLines'   => $methodData['executableLines'],
                    'numExecutedLines'     => $methodData['executedLines'],
                    'executedLinesPercent' => $this->calculatePercent(
                      $methodData['executedLines'], $methodData['executableLines']
                    )
                  ),
                  $lowUpperBound,
                  $highLowerBound,
                  'method_item.html'
                );
            }
        }

        $this->setTemplateVars($template, $title, $charset);

        $template->setVar(
          array(
            'lines'      => $lines,
            'total_item' => $this->renderTotalItem($lowUpperBound, $highLowerBound, FALSE),
            'items'      => $items,
            'yuiPanelJS' => $this->yuiPanelJS
          )
        );

        $cleanId = PHPUnit_Util_Filesystem::getSafeFilename($this->getId());
        $template->renderTo($target . $cleanId . '.html');
    }

    /**
     * Calculates coverage statistics for the file.
     *
     * @access protected
     */
    protected function calculateStatistics()
    {
        $classes = PHPUnit_Util_Class::getClassesInFile($this->getPath());

        $startLines = array();
        $endLines   = array();

        foreach ($classes as $class) {
            if (!$class->isInterface()) {
                $className      = $class->getName();
                $classStartLine = $class->getStartLine();
                $classEndLine   = $class->getEndLine();

                $this->classes[$className] = array(
                  'methods'         => array(),
                  'startLine'       => $classStartLine,
                  'executableLines' => 0,
                  'executedLines'   => 0
                );

                $startLines[$classStartLine] = &$this->classes[$className];
                $endLines[$classEndLine]     = &$this->classes[$className];

                foreach ($class->getMethods() as $method) {
                    if (!$method->isAbstract() &&
                        $method->getDeclaringClass()->getName() == $className) {
                        $methodName      = $method->getName();
                        $methodStartLine = $method->getStartLine();
                        $methodEndLine   = $method->getEndLine();

                        $this->classes[$className]['methods'][$methodName] = array(
                          'startLine'       => $methodStartLine,
                          'executableLines' => 0,
                          'executedLines'   => 0
                        );

                        $startLines[$methodStartLine] = &$this->classes[$className]['methods'][$methodName];
                        $endLines[$methodEndLine]     = &$this->classes[$className]['methods'][$methodName];

                        $this->numMethods++;
                    }
                }

                $this->numClasses++;
            }
        }

        $ignoreStart = -1;
        $lineNumber  = 1;

        foreach ($this->codeLines as $line) {
            if (isset($startLines[$lineNumber])) {
                // Start line of a class.
                if (isset($startLines[$lineNumber]['methods'])) {
                    $currentClass = &$startLines[$lineNumber];
                }

                // Start line of a method.
                else {
                    $currentMethod = &$startLines[$lineNumber];
                }
            }

            if (strpos($line, '@codeCoverageIgnore') !== FALSE) {
                if (strpos($line, '@codeCoverageIgnoreStart') !== FALSE) {
                    $ignoreStart = $line;
                }

                else if (strpos($line, '@codeCoverageIgnoreEnd') !== FALSE) {
                    $ignoreStart = -1;
                }
            }

            if (isset($this->executedLines[$lineNumber])) {
                // Array: Line is executable and was executed.
                if (is_array($this->executedLines[$lineNumber])) {
                    if (isset($currentClass)) {
                        $currentClass['executableLines']++;
                        $currentClass['executedLines']++;
                    }

                    if (isset($currentMethod)) {
                        $currentMethod['executableLines']++;
                        $currentMethod['executedLines']++;
                    }

                    $this->numExecutableLines++;
                    $this->numExecutedLines++;
                }

                // -1: Line is executable and was not executed.
                else if ($this->executedLines[$lineNumber] == -1) {
                    if (isset($currentClass)) {
                        $currentClass['executableLines']++;
                    }

                    if (isset($currentMethod)) {
                        $currentMethod['executableLines']++;
                    }

                    $this->numExecutableLines++;

                    if ($ignoreStart != -1 && $line > $ignoreStart) {
                        if (isset($currentClass)) {
                            $currentClass['executedLines']++;
                        }

                        if (isset($currentMethod)) {
                            $currentMethod['executedLines']++;
                        }

                        $this->numExecutedLines++;
                    }
                }
            }

            if (isset($endLines[$lineNumber])) {
                // End line of a class.
                if (isset($endLines[$lineNumber]['methods'])) {
                    unset($currentClass);
                }

                // End line of a method.
                else {
                    unset($currentMethod);
                }
            }

            $lineNumber++;
        }

        foreach ($this->classes as $class) {
            foreach ($class['methods'] as $method) {
                if ($method['executedLines'] > 0) {
                    $this->numCalledMethods++;
                }
            }

            if ($class['executedLines'] > 0) {
                $this->numCalledClasses++;
            }
        }
    }

    /**
     * @author Aidan Lister <aidan@php.net>
     * @author Sebastian Bergmann <sb@sebastian-bergmann.de>
     * @param  string  $file
     * @return array
     * @access protected
     */
    protected function loadFile($file)
    {
        $lines  = explode("\n", str_replace("\t", '    ', file_get_contents($file)));
        $result = array();

        if (count($lines) == 0) {
            return $result;
        }

        $lines       = array_map('rtrim', $lines);
        $linesLength = array_map('strlen', $lines);
        $width       = max($linesLength);

        foreach ($linesLength as $line => $length) {
            $this->codeLinesFillup[$line] = $width - $length;
        }

        if (!$this->highlight) {
            return $lines;
        }

        $tokens     = token_get_all(file_get_contents($file));
        $stringFlag = FALSE;
        $i          = 0;
        $result[$i] = '';

        foreach ($tokens as $j => $token) {
            if (is_string($token)) {
                if ($token === '"' && $tokens[$j - 1] !== '\\') {
                    $result[$i] .= sprintf(
                      '<span class="string">%s</span>',

                      htmlspecialchars($token)
                    );

                    $stringFlag = !$stringFlag;   
                } else {
                    $result[$i] .= sprintf(
                      '<span class="keyword">%s</span>',

                      htmlspecialchars($token)
                    );
                }

                continue;
            }

            list ($token, $value) = $token;

            $value = str_replace(
              array("\t", ' '),
              array('&nbsp;&nbsp;&nbsp;&nbsp;', '&nbsp;'),
              htmlspecialchars($value)
            );

            if ($value === "\n") {
                $result[++$i] = '';
            } else {
                $lines = explode("\n", $value);              

                foreach ($lines as $jj => $line) {
                    $line = trim($line);

                    if ($line !== '') {
                        if ($stringFlag) {
                            $colour = 'string';
                        } else {
                            switch ($token) {
                                case T_INLINE_HTML: {
                                    $colour = 'html';
                                }
                                break;

                                case T_COMMENT:
                                case T_DOC_COMMENT: {
                                    $colour = 'comment';
                                }
                                break;

                                case T_ABSTRACT:
                                case T_ARRAY:
                                case T_ARRAY_CAST:
                                case T_AS:
                                case T_BOOLEAN_AND:
                                case T_BOOLEAN_OR:
                                case T_BOOL_CAST:
                                case T_BREAK:
                                case T_CASE:
                                case T_CATCH:
                                case T_CLASS:
                                case T_CLONE:
                                case T_CONCAT_EQUAL:
                                case T_CONTINUE:
                                case T_DEFAULT:
                                case T_DOUBLE_ARROW:
                                case T_DOUBLE_CAST:
                                case T_ECHO:
                                case T_ELSE:
                                case T_ELSEIF:
                                case T_EMPTY:
                                case T_ENDDECLARE:
                                case T_ENDFOR:
                                case T_ENDFOREACH:
                                case T_ENDIF:
                                case T_ENDSWITCH:
                                case T_ENDWHILE:
                                case T_END_HEREDOC:
                                case T_EXIT:
                                case T_EXTENDS:
                                case T_FINAL:
                                case T_FOREACH:
                                case T_FUNCTION:
                                case T_GLOBAL:
                                case T_IF:
                                case T_INC:
                                case T_INCLUDE:
                                case T_INCLUDE_ONCE:
                                case T_INSTANCEOF:
                                case T_INT_CAST:
                                case T_ISSET:
                                case T_IS_EQUAL:
                                case T_IS_IDENTICAL:
                                case T_IS_NOT_IDENTICAL:
                                case T_IS_SMALLER_OR_EQUAL:
                                case T_NEW:
                                case T_OBJECT_CAST:
                                case T_OBJECT_OPERATOR:
                                case T_PAAMAYIM_NEKUDOTAYIM:
                                case T_PRIVATE:
                                case T_PROTECTED:
                                case T_PUBLIC:
                                case T_REQUIRE:
                                case T_REQUIRE_ONCE:
                                case T_RETURN:
                                case T_SL:
                                case T_SL_EQUAL:
                                case T_SR:
                                case T_SR_EQUAL:
                                case T_START_HEREDOC:
                                case T_STATIC:
                                case T_STRING_CAST:
                                case T_THROW:
                                case T_TRY:
                                case T_UNSET_CAST:
                                case T_VAR:
                                case T_WHILE: {
                                    $colour = 'keyword';
                                }
                                break;

                                default: {
                                    $colour = 'default';
                                }
                            }
                        }

                        $result[$i] .= sprintf(
                          '<span class="%s">%s</span>',

                          $colour,
                          $line
                        );
                    }

                    if (isset($lines[$jj + 1])) {
                        $result[++$i] = '';
                    }
                }
            }
        }

        unset($result[count($result)-1]);

        return $result;
    }
}
?>
