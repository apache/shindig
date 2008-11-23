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
 * @version    SVN: $Id: Printer.php 2141 2008-01-17 10:49:39Z sb $
 * @link       http://www.phpunit.de/
 * @since      File available since Release 2.0.0
 */

require_once 'PHPUnit/Util/Filter.php';

PHPUnit_Util_Filter::addFileToFilter(__FILE__, 'PHPUNIT');

/**
 * Utility class that can print to STDOUT or write to a file.
 *
 * @category   Testing
 * @package    PHPUnit
 * @author     Sebastian Bergmann <sb@sebastian-bergmann.de>
 * @copyright  2002-2008 Sebastian Bergmann <sb@sebastian-bergmann.de>
 * @license    http://www.opensource.org/licenses/bsd-license.php  BSD License
 * @version    Release: 3.2.9
 * @link       http://www.phpunit.de/
 * @since      Class available since Release 2.0.0
 * @abstract
 */
abstract class PHPUnit_Util_Printer {
  /**
   * @var    resource
   * @access protected
   */
  protected $out;
  
  /**
   * @var    string
   * @access protected
   */
  protected $outTarget;
  
  /**
   * @var    boolean
   * @access protected
   */
  protected $printsHTML = FALSE;

  /**
   * Constructor.
   *
   * @param  mixed $out
   * @throws InvalidArgumentException
   * @access public
   */
  public function __construct($out = NULL) {
    if ($out !== NULL) {
      if (is_string($out)) {
        if (strpos($out, 'socket://') === 0) {
          $out = explode(':', str_replace('socket://', '', $out));
          
          if (sizeof($out) != 2) {
            throw new InvalidArgumentException();
          }
          
          $this->out = fsockopen($out[0], $out[1]);
        } else {
          $this->out = fopen($out, 'wt');
        }
        
        $this->outTarget = $out;
      } else {
        $this->out = $out;
      }
    }
  }

  /**
   * Flush buffer, optionally tidy up HTML, and close output.
   *
   * @access public
   */
  public function flush() {
    if ($this->out !== NULL) {
      fclose($this->out);
    }
    
    if ($this->printsHTML === TRUE && $this->outTarget !== NULL && extension_loaded('tidy')) {
      file_put_contents($this->outTarget, tidy_repair_file($this->outTarget));
    }
  }

  /**
   * @param  string $buffer
   * @access public
   */
  public function write($buffer) {
    if ($this->out !== NULL) {
      fwrite($this->out, $buffer);
    } else {
      if (php_sapi_name() != 'cli') {
        $buffer = htmlentities($buffer);
      }
      
      print $buffer;
    }
  }
}
?>
