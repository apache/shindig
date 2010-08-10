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

/**
 * ExpType test case.
 */
class ExpTypeTest extends PHPUnit_Framework_TestCase {

  private $tokens;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    ExpType::$RAW;  // dummy here, for loading ExpType.php
    $int = new Token(ExpType::$INT, 1);
    $float = new Token(ExpType::$FLOAT, 1.0);
    $string = new Token(ExpType::$STRING, 'Jacky Wang');
    $bool = new Token(ExpType::$BOOL, true);
    $null = new Token(ExpType::$NULL, null);
    $array = new Token(ExpType::$ARRAY, array());
    $object = new Token(ExpType::$OBJECT, (object)("it's object"));
    $this->tokens = array('int' => $int, 'float' => $float, 'string' => $string, 'bool' => $bool, 'null' => $null, 'array' => $array, 'object' => $object);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    parent::tearDown();
  }
  
  /**
   * Tests ExpType::detectType
   */
  public function testDetectType() {
    foreach ($this->tokens as $token) {
      $this->assertEquals($token->type, ExpType::detectType($token->value));
    }
  }

  /**
   * Tests ExpType::coerce
   */
  public function testCoerce() {
    // coerce number
    $this->assertEquals($this->tokens['int'], ExpType::coerceToNumber($this->tokens['int']));
    $this->assertEquals($this->tokens['float'], ExpType::coerceToNumber($this->tokens['float']));
    $this->assertEquals(new Token(ExpType::$INT, 2), ExpType::coerceToNumber(new Token(ExpType::$RAW, '2')));
    $this->assertEquals(new Token(ExpType::$INT, 2), ExpType::coerceToNumber(new Token(ExpType::$STRING, '2')));
    $this->assertEquals(new Token(ExpType::$INT, 1), ExpType::coerceToNumber(new Token(ExpType::$BOOL, true)));
    $this->assertEquals(new Token(ExpType::$INT, 0), ExpType::coerceToNumber(new Token(ExpType::$BOOL, false)));
    $this->assertEquals(new Token(ExpType::$INT, 0), ExpType::coerceToNumber(new Token(ExpType::$NULL, null)));
    $this->assertEquals(new Token(ExpType::$FLOAT, 1.0), ExpType::coerceToNumber(new Token(ExpType::$RAW, '1.0')));
    $this->assertEquals(new Token(ExpType::$FLOAT, 1.0), ExpType::coerceToNumber(new Token(ExpType::$STRING, '1.0')));
    
    // coerce string
    $this->assertEquals($this->tokens['string'], ExpType::coerceToString($this->tokens['string']));
    $this->assertEquals(new Token(ExpType::$STRING, '2'), ExpType::coerceToString(new Token(ExpType::$RAW, '2')));
    $this->assertEquals(new Token(ExpType::$STRING, '2'), ExpType::coerceToString(new Token(ExpType::$INT, 2)));
    $this->assertEquals(new Token(ExpType::$STRING, '2'), ExpType::coerceToString(new Token(ExpType::$FLOAT, 2.0)));
    $this->assertEquals(new Token(ExpType::$STRING, '2.5'), ExpType::coerceToString(new Token(ExpType::$FLOAT, 2.5)));
    $this->assertEquals(new Token(ExpType::$STRING, 'true'), ExpType::coerceToString(new Token(ExpType::$BOOL, true)));
    $this->assertEquals(new Token(ExpType::$STRING, 'false'), ExpType::coerceToString(new Token(ExpType::$BOOL, false)));
    $this->assertEquals(new Token(ExpType::$STRING, 'null'), ExpType::coerceToString(new Token(ExpType::$NULL, null)));
    
    // coerce bool
    $this->assertEquals($this->tokens['bool'], ExpType::coerceToBool($this->tokens['bool']));
    $this->assertEquals(new Token(ExpType::$BOOL, true), ExpType::coerceToBool(new Token(ExpType::$RAW, 'True')));
    $this->assertEquals(new Token(ExpType::$BOOL, true), ExpType::coerceToBool(new Token(ExpType::$RAW, 'true')));
    $this->assertEquals(new Token(ExpType::$BOOL, false), ExpType::coerceToBool(new Token(ExpType::$RAW, 'False')));
    $this->assertEquals(new Token(ExpType::$BOOL, false), ExpType::coerceToBool(new Token(ExpType::$RAW, 'false')));
    $this->assertEquals(new Token(ExpType::$BOOL, true), ExpType::coerceToBool(new Token(ExpType::$STRING, 'false')));
    $this->assertEquals(new Token(ExpType::$BOOL, false), ExpType::coerceToBool(new Token(ExpType::$STRING, '')));
    $this->assertEquals(new Token(ExpType::$BOOL, true), ExpType::coerceToBool(new Token(ExpType::$INT, 2)));
    $this->assertEquals(new Token(ExpType::$BOOL, false), ExpType::coerceToBool(new Token(ExpType::$INT, 0)));
    $this->assertEquals(new Token(ExpType::$BOOL, true), ExpType::coerceToBool(new Token(ExpType::$FLOAT, 2.0)));
    $this->assertEquals(new Token(ExpType::$BOOL, false), ExpType::coerceToBool(new Token(ExpType::$FLOAT, 0.0)));
    $this->assertEquals(new Token(ExpType::$BOOL, false), ExpType::coerceToBool(new Token(ExpType::$NULL, null)));
    
    // coerce null
    $this->assertEquals($this->tokens['null'], ExpType::coerceToNull($this->tokens['null']));
    $this->assertEquals($this->tokens['null'], ExpType::coerceToNull(new Token(ExpType::$RAW, 'null')));
  }
}