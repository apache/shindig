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
 * @version    SVN: $Id: MockObjectTest.php 1985 2007-12-26 18:11:55Z sb $
 * @link       http://www.phpunit.de/
 * @since      File available since Release 3.0.0
 */

require_once 'PHPUnit/Framework/TestCase.php';

require_once '_files/AnInterface.php';

/**
 *
 *
 * @category   Testing
 * @package    PHPUnit
 * @author     Sebastian Bergmann <sb@sebastian-bergmann.de>
 * @author     Patrick M??ller <elias0@gmx.net>
 * @copyright  2002-2008 Sebastian Bergmann <sb@sebastian-bergmann.de>
 * @license    http://www.opensource.org/licenses/bsd-license.php  BSD License
 * @version    Release: 3.2.9
 * @link       http://www.phpunit.de/
 * @since      Class available since Release 3.0.0
 */
class Framework_MockObjectTest extends PHPUnit_Framework_TestCase {

  public function testMockedMethodIsNeverCalled() {
    $mock = $this->getMock('AnInterface');
    $mock->expects($this->never())->method('doSomething');
  }

  public function testMockedMethodIsCalledAtLeastOnce() {
    $mock = $this->getMock('AnInterface');
    $mock->expects($this->atLeastOnce())->method('doSomething');
    
    $mock->doSomething();
  }

  public function testMockedMethodIsCalledAtLeastOnce2() {
    $mock = $this->getMock('AnInterface');
    $mock->expects($this->atLeastOnce())->method('doSomething');
    
    $mock->doSomething();
    $mock->doSomething();
  }

  public function testMockedMethodIsCalledOnce() {
    $mock = $this->getMock('AnInterface');
    $mock->expects($this->once())->method('doSomething');
    
    $mock->doSomething();
  }

  public function testMockedMethodIsCalledOnceWithParameter() {
    $mock = $this->getMock('AnInterface');
    $mock->expects($this->once())->method('doSomething')->with($this->equalTo('something'));
    
    $mock->doSomething('something');
  }

  public function testMockedMethodIsCalledExactly() {
    $mock = $this->getMock('AnInterface');
    $mock->expects($this->exactly(2))->method('doSomething');
    
    $mock->doSomething();
    $mock->doSomething();
  }

  public function testStubbedException() {
    $mock = $this->getMock('AnInterface');
    $mock->expects($this->any())->method('doSomething')->will($this->throwException(new Exception()));
    
    try {
      $mock->doSomething();
    } 

    catch (Exception $e) {
      return;
    }
    
    $this->fail();
  }

  public function testStubbedReturnValue() {
    $mock = $this->getMock('AnInterface');
    $mock->expects($this->any())->method('doSomething')->will($this->returnValue('something'));
    
    $this->assertEquals('something', $mock->doSomething());
  }
}
?>
