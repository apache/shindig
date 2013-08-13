<?php
namespace apache\shindig\gadgets\templates;

/*
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

class PrimitiveExp {

  public $reason;
  private $exp;
  private $dataContext;

  function __construct($reason) {
    $this->reason = $reason;
    $this->exp = array();
  }

  public function append($token) {
    array_push($this->exp, $token);
  }

  public function evaluate($dataContext) {
    $this->dataContext = $dataContext;
    if ($this->reason->type == ExpType::$FUNCTION) {
      $result = $this->evaluateFunction();
    } else {
      $result = $this->evaluateExpression();
    }
    return $result;
  }

  private function evaluateExpression() {
    $OPERATOR_TYPES = array(ExpType::$UNARY_OP, ExpType::$BINARY_OP, ExpType::$DOT);
    $PRIMITIVE_TYPES = array(ExpType::$INT, ExpType::$FLOAT, ExpType::$STRING, ExpType::$BOOL,
        ExpType::$NULL);
    $COMPOSITE_TYPES = array(ExpType::$ARRAY, ExpType::$OBJECT);
    $OPERAND_TYPES = array_merge($PRIMITIVE_TYPES, $COMPOSITE_TYPES, array(ExpType::$IDENTITY));

    $OPERATOR_PRECEDENCE = array('||' => 0, '&&' => 1, '==' => 2, '!=' => 2, '>' => 3,
        '<' => 3, '>=' => 3, '<=' => 3, '+' => 4, ' - ' => 4, '*' => 5, '/' => 5, '%'  => 5,
        ' -' => 6, '!' => 6, 'empty' => 6, '.'  => 7);

    $operatorStack = array();
    $operandStack = array();
    foreach ($this->exp as $token) {
      if (in_array($token->type, $OPERAND_TYPES)) {
        array_push($operandStack, $token);
      } elseif (in_array($token->type, $OPERATOR_TYPES)) {
        $precedence = $OPERATOR_PRECEDENCE[$token->value];
        while (! empty($operatorStack)) {
          $previousPrecedence = $OPERATOR_PRECEDENCE[end($operatorStack)->value];
          if ($precedence > $previousPrecedence || $precedence == $previousPrecedence && $token->type == ExpType::$UNARY_OP) {
            break;
          } else {
            $operator = array_pop($operatorStack);
            $operandStack = $this->compute($operandStack, $operator);
          }
        }
        array_push($operatorStack, $token);
      } else {
        throw new ExpParserException("The expected primitive expression contains complex token: ", print_r($this->exp, true));
      }
    }
    while ($operator = array_pop($operatorStack)) {
      $operandStack = $this->compute($operandStack, $operator);
    }
    if (count($operandStack) != 1) {
      throw new ExpParserException("Gramma error on the primitive expression: " . print_r($this->exp, true));
    }
    return $this->evaluateIdentity(array_pop($operandStack));
  }

  private function compute($operandStack, $operator) {
    $ARITHMETIC_OPS = array('+', ' - ', '*', '/', '%'); // without unary operator ' -'
    $RELATIONAL_OPS = array('==', '!=', '<', '>', '<=', '>=');
    $LOGICAL_OPS = array('&&', '||'); // without '!'

    $sym = $operator->value;
    if ($operator->type == ExpType::$UNARY_OP) {
      $operand = $this->evaluateIdentity(array_pop($operandStack));
    } else {
      $rhs = array_pop($operandStack);
      $rhs = ($sym != '.') ? $this->evaluateIdentity($rhs) : $rhs;
      $lhs = $this->evaluateIdentity(array_pop($operandStack));
    }
    if ($sym == '.') {
      if ($lhs->type == ExpType::$NULL || $rhs->type == ExpType::$NULL) {  // Dealing with null type
        $result = new Token(ExpType::$NULL, null);
      } else {
        if ($lhs->type == ExpType::$ARRAY) {
          if ($rhs->type == ExpType::$INT || $rhs->type == ExpType::$STRING || $rhs->type == ExpType::$IDENTITY) $resval = isset($lhs->value[$rhs->value]) ? $lhs->value[$rhs->value] : null;
          else throw new ExpParserException("Can't reference key typ " . print_r($rhs, true) . " on array");
        } elseif ($lhs->type == ExpType::$OBJECT) {
          if ($rhs->type == ExpType::$IDENTITY) eval('$resval = isset($lhs->value->' . $rhs->value . ')? $lhs->value->' . $rhs->value . ': null;');
          else throw new ExpParserException("Can't reference key typ " . print_r($rhs, true) . " on object");
        } else {
          throw new ExpParserException("Can't perform ./[] operation on primitive type " . print_r($lhs, true));
        }
        $result = new Token(ExpType::detectType($resval), $resval);
      }
    } elseif ($sym == 'empty') {
      $result = new Token(ExpType::$BOOL, empty($operand->value));
    } elseif (in_array($sym, $ARITHMETIC_OPS)) {
      $lhs = ExpType::coerceToNumber($lhs);
      $rhs = ExpType::coerceToNumber($rhs);
      eval('$resval = $lhs->value ' . $sym . '$rhs->value;');
      $result = new Token(ExpType::detectType($resval), $resval);
    } elseif ($sym == ' -') {  // Unary operator '-'
      $result = ExpType::coerceToNumber($operand);
      $result = new Token($result->type, -($result->value));
    } elseif (in_array($sym, $RELATIONAL_OPS)) {
      $result = new Token(ExpType::$BOOL);
      // special case: one of the operator is null
      if ($lhs->type == ExpType::$NULL && $rhs->type != ExpType::$NULL || $lhs->type != ExpType::$NULL && $rhs->type == ExpType::$NULL) $result->value = in_array($sym, array('<', '>', '<=', '>=', '==')) ? false : true;
      eval('$result->value = $lhs->value ' . $sym . ' $rhs->value;');
    } elseif (in_array($sym, $LOGICAL_OPS)) {
      $result = new Token(ExpType::$BOOL);
      $lhs = ExpType::coerceToBool($lhs);
      $rhs = ExpType::coerceToBool($rhs);
      eval('$result->value = $lhs->value ' . $sym . ' $rhs->value;');
    } elseif ($sym == '!') {
      $result = ExpType::coerceToBool($operand);
      $result = new Token(ExpType::$BOOL, ! ($result->value));
    } else {
      throw new ExpParserException("Uncovered operator: " . $sym);
    }
    array_push($operandStack, $result);
    return $operandStack;
  }

  private function evaluateIdentity($token) {
    if ($token->type != ExpType::$IDENTITY) return $token;
    foreach (array(false, 'Cur', 'My', 'Top') as $scope) {
      $context = $scope ? $this->dataContext[$scope] : $this->dataContext;
      if (isset($context[$token->value])) {
        $val = $context[$token->value];
        $newToken = new Token(ExpType::detectType($val), $val);
        return $newToken;
      }
    }
    throw new ExpParserException("Un-recogonized identity name in the data context: " . $token->value);
  }

  private function evaluateFunction() {
    $FUNCTION_TRANS = array('osx:parseJson' => 'json_decode', 'osx:decodeBase64' => 'base64_decode',
        'osx:urlEncode' => 'rawurlencode', 'osx:urlDecode' => 'rawurldecode');
    $params = array();
    foreach ($this->exp as $param)
      array_push($params, $param->value);
    $val = call_user_func_array($FUNCTION_TRANS[$this->reason->value], $params);
    return new Token(ExpType::detectType($val), $val);
  }

}
