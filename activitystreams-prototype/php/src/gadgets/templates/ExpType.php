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
 * Lexer for parsing the os-template / os-data expression language, which is based on the JSP EL syntax
 * For reference on the language see:
 * JSP EL: https://jsp.dev.java.net/spec/jsp-2_1-fr-spec-el.pdf
 * OS Templates: http://opensocial-resources.googlecode.com/svn/spec/0.9/OpenSocial-Templating.xml
 * OS Data pipelining: http://opensocial-resources.googlecode.com/svn/spec/0.9/OpenSocial-Data-Pipelining.xml
 *
 * This ExpType handles all type identifications and conversions in ExpLexer and ExpParser.
 */

class Token {
  public $type;
  public $value;

  function __construct($type, $value = null) {
    $this->type = $type;
    $this->value = $value;
  }
}

class ExpTypeException extends Exception {
}

class ExpType {

  // operators
  public static $FUNCTION = 'function';
  public static $TERNARY = 'ternary';
  public static $PAREN = 'paren';
  public static $COMMA = 'comma';
  public static $RAW_OP = 'raw_operator';
  public static $BINARY_OP = 'binary_operator';
  public static $UNARY_OP = 'unary_operator';
  public static $DOT = 'dot';

  // values
  public static $RAW = 'raw';
  public static $INT = 'int';
  public static $FLOAT = 'float';
  public static $STRING = 'string';
  public static $BOOL = 'bool';
  public static $NULL = 'null';
  public static $IDENTITY = 'identity';
  public static $ARRAY = 'array';
  public static $OBJECT = 'object';

  private static $ESCAPE_CHARS = array('\"' => '"', "\'" => "'", '\\\\' => '\\');

  public static function detectType($value) {
    if (is_int($value)) {
      return ExpType::$INT;
    } elseif (is_float($value)) {
      return ExpType::$FLOAT;
    } elseif (is_string($value)) {
      return ExpType::$STRING;
    } elseif (is_bool($value)) {
      return ExpType::$BOOL;
    } elseif (is_null($value)) {
      return ExpType::$NULL;
    } elseif (is_array($value)) {
      return ExpType::$ARRAY;
    } elseif (is_object($value)) {
      return ExpType::$OBJECT;
    } else {
      throw new ExpParserException("Un-recogonized variable type of identity: " . $value);
    }
  }

  public static function coerceToNumber($token) {
    $INTEGER_PATTERN = "/^[0-9]+$/";
    $type = $token->type;
    if (in_array($type, array(ExpType::$INT, ExpType::$FLOAT))) return $token;
    if (in_array($type, array(ExpType::$BOOL, ExpType::$NULL)) || in_array($type, array(ExpType::$RAW, ExpType::$STRING)) && preg_match($INTEGER_PATTERN, $token->value) == 1) {
      $int = new Token(ExpType::$INT, (int)($token->value));
      return $int;
    }
    if (in_array($type, array(ExpType::$RAW, ExpType::$STRING))) {
      $float = new Token(ExpType::$FLOAT, (float)($token->value));
      return $float;
    }
    throw new ExpTypeException("Unable to coerce token " . print_r($token, true) . " to number");
  }

  public static function coerceToString($token) {
    $PRIMITIVE_TYPES = array(ExpType::$INT, ExpType::$FLOAT, ExpType::$STRING, ExpType::$BOOL, ExpType::$NULL);
    $COMPOSITE_TYPES = array(ExpType::$ARRAY, ExpType::$OBJECT);
    $type = $token->type;
    if ($type == ExpType::$STRING) return $token;
    $string = new Token(ExpType::$STRING);
    if ($type == ExpType::$RAW) {
      $string->value = strtr($token->value, ExpType::$ESCAPE_CHARS);
    } elseif ($type == ExpType::$BOOL) {
      $string->value = ($token->value) ? 'true' : 'false';
    } elseif ($type == ExpType::$NULL) {
      $string->value = 'null';
    } elseif (in_array($type, $PRIMITIVE_TYPES)) {
      $string->value = (string)($token->value);
    } elseif (in_array($type, $COMPOSITE_TYPES)) {
      $string->value = print_r($token->value, true); // maybe call .toString()?
    } else {
      throw new ExpTypeException("Unable to coerce token" . print_r($token, true) . " to string");
    }
    return $string;
  }

  public static function coerceToBool($token) {
    $PRIMITIVE_TYPES = array(ExpType::$INT, ExpType::$FLOAT, ExpType::$STRING, ExpType::$BOOL, ExpType::$NULL);
    $COMPOSITE_TYPES = array(ExpType::$ARRAY, ExpType::$OBJECT);
    $type = $token->type;
    if ($type == ExpType::$BOOL) return $token;
    $bool = new Token(ExpType::$BOOL);
    if ($type == ExpType::$RAW) {
      $bool->value = strtolower($token->value) == 'true' ? true : false;
    } elseif (in_array($type, $PRIMITIVE_TYPES)) {
      $bool->value = (bool)($token->value);
    } elseif (in_array($type, $COMPOSITE_TYPES)) {
      $bool->value = $token->value != null ? true : false;
    } else {
      throw new ExpTypeException("Unable to coerce token" . print_r($token, true) . " to bool");
    }
    return $bool;
  }

  public static function coerceToNull($token) {
    $COMPOSITE_TYPES = array(ExpType::$ARRAY, ExpType::$OBJECT);
    $type = $token->type;
    if ($type == ExpType::$NULL) return $token;
    $null = new Token(ExpType::$NULL);
    $value = $token->value;
    if ($type == ExpType::$RAW && strtolower($value) == 'null' || in_array($type, $COMPOSITE_TYPES) && $token->value == null) {
      $null->value = null;
    } else {
      throw new ExpTypeException("Unable to coerce token" . print_r($token, true) . " to null");
    }
    return $null;
  }
}