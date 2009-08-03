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

class ObjEe {
  public $Ee;
}

/**
 * ExpressionParser test case.
 */
class ExpressionParserTest extends PHPUnit_Framework_TestCase {

  private $input;
  private $tokenStream;
  private $dataContext;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $encoded_json = base64_encode('{"array_attr": [0, 1]}');
    $attr_equ = "no_prefix_id.Ee+---((2-1)*(4.0-3)-1.0/2+5e-1)>-.5e+1+4 or not empty cur_id['empty_str']&&!top_id.null_attr";
    $this->input = 'osx:parseJson(osx:urlDecode(osx:urlEncode(osx:decodeBase64("' . $encoded_json . '"))))'
        . '.array_attr['
        . $attr_equ . "?0:1"
        . "]==1?(true?'no_prefix_id.Ee > 0':"
        . '"\'should never be here\'	\\\\\\""'
        . "):'no_prefix_id.Ee <= 0'";
    
    $this->tokenStream = array(
        new Token(ExpType::$FUNCTION, 'osx:parseJson'),
        new Token(ExpType::$PAREN, '('),
        new Token(ExpType::$FUNCTION, 'osx:urlDecode'),
        new Token(ExpType::$PAREN, '('),
        new Token(ExpType::$FUNCTION, 'osx:urlEncode'),
        new Token(ExpType::$PAREN, '('),
        new Token(ExpType::$FUNCTION, 'osx:decodeBase64'),
        new Token(ExpType::$PAREN, '('),
        new Token(ExpType::$STRING, 'eyJhcnJheV9hdHRyIjpbMSwgMF19'),
        new Token(ExpType::$PAREN, ')'),
        new Token(ExpType::$PAREN, ')'),
        new Token(ExpType::$PAREN, ')'),
        new Token(ExpType::$PAREN, ')'),
        new Token(ExpType::$DOT, '.'),
        new Token(ExpType::$IDENTITY, 'array_attr'),
        new Token(ExpType::$PAREN, '['),
        new Token(ExpType::$IDENTITY, 'no_prefix_id'),
        new Token(ExpType::$DOT, '.'),
        new Token(ExpType::$IDENTITY, 'Ee'),
        new Token(ExpType::$BINARY_OP, '+'),
        new Token(ExpType::$UNARY_OP, '- '),
        new Token(ExpType::$UNARY_OP, '- '),
        new Token(ExpType::$UNARY_OP, '- '),
        new Token(ExpType::$PAREN, '('),
        new Token(ExpType::$PAREN, '('),
        new Token(ExpType::$INT, 2),
        new Token(ExpType::$BINARY_OP, ' - '),
        new Token(ExpType::$INT, 1),
        new Token(ExpType::$PAREN, ')'),
        new Token(ExpType::$BINARY_OP, '*'),
        new Token(ExpType::$PAREN, '('),
        new Token(ExpType::$FLOAT, 4.0),
        new Token(ExpType::$BINARY_OP, ' - '),
        new Token(ExpType::$INT, 3),
        new Token(ExpType::$BINARY_OP, ' - '),
        new Token(ExpType::$FLOAT, 1.0),
        new Token(ExpType::$BINARY_OP, '/'),
        new Token(ExpType::$INT, 2),
        new Token(ExpType::$BINARY_OP, '+'),
        new Token(ExpType::$FLOAT, 0.5),
        new Token(ExpType::$PAREN, ')'),
        new Token(ExpType::$BINARY_OP, '>'),
        new Token(ExpType::$UNARY_OP, '- '),
        new Token(ExpType::$FLOAT, 5.0),
        new Token(ExpType::$BINARY_OP, '+'),
        new Token(ExpType::$INT, 4),
        new Token(ExpType::$BINARY_OP, '||'),
        new Token(ExpType::$UNARY_OP, '!'),
        new Token(ExpType::$UNARY_OP, 'empty'),
        new Token(ExpType::$IDENTITY, 'cur_id'),
        new Token(ExpType::$PAREN, '['),
        new Token(ExpType::$STRING, 'empty_str'),
        new Token(ExpType::$PAREN, ']'),
        new Token(ExpType::$BINARY_OP, '&&'),
        new Token(ExpType::$UNARY_OP, '!'),
        new Token(ExpType::$IDENTITY, 'top_id'),
        new Token(ExpType::$DOT, '.'),
        new Token(ExpType::$IDENTITY, 'null_attr'),
        new Token(ExpType::$TERNARY, '?'),
        new Token(ExpType::$INT, 0),
        new Token(ExpType::$TERNARY, ':'),
        new Token(ExpType::$INT, 1),
        new Token(ExpType::$PAREN, ']'),
        new Token(ExpType::$BINARY_OP, '=='),
        new Token(ExpType::$INT, 1),
        new Token(ExpType::$TERNARY, '?'),
        new Token(ExpType::$PAREN, '('),
        new Token(ExpType::$BOOL, true),
        new Token(ExpType::$TERNARY, '?'),
        new Token(ExpType::$STRING, 'no_prefix_id.Ee > 0'),
        new Token(ExpType::$TERNARY, ':'),
        new Token(ExpType::$STRING, '"\'should never be here\'	\\\\\\""'),
        new Token(ExpType::$PAREN, ')'),
        new Token(ExpType::$TERNARY, ':'),
        new Token(ExpType::$STRING, 'no_prefix_id.Ee <= 0')
    );
    
    $no_prefix_id = new ObjEe();
    $no_prefix_id->Ee = 1;  // change this number to see the difference
    $cur_id = array('empty_str' => '');
    $top_id = (object)'empty_object';
    $this->dataContext = array(
        'no_prefix_id' => $no_prefix_id,
        'Cur' => array('cur_id' => $cur_id),
        'My' => array(),
        'Top' => array('top_id' => $top_id)
    );
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    parent::tearDown();
  }
  
  /**
   * Tests ExpLexer::process
   */
  public function testProcess() {
    $actualTokenStream = ExpLexer::process($this->input);
    $this->assertEquals($this->tokenStream, $actualTokenStream);
  }

  /**
   * Tests ExpParser::parse
   */
  public function testParse() {
    $this->dataContext['no_prefix_id']->Ee = 1;
    $actualResult = ExpParser::parse($this->tokenStream, $this->dataContext);
    $this->assertEquals(new Token(ExpType::$STRING, 'no_prefix_id.Ee > 0'), $actualResult);
    
    $this->dataContext['no_prefix_id']->Ee = -1;
    $actualResult = ExpParser::parse($this->tokenStream, $this->dataContext);
    $this->assertEquals(new Token(ExpType::$STRING, 'no_prefix_id.Ee <= 0'), $actualResult);
  }

  /**
   * Tests ExpressionParser::evaluate
   */
  public function testEvaluate() {
    $actualOutput = ExpressionParser::evaluate($this->input, $this->dataContext);
    
    // Expected result
    $this->assertEquals('no_prefix_id.Ee > 0', $actualOutput);
  }

}
