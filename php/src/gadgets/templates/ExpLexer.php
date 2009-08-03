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
 * This lexer could handle special formats like floating 12.3e-10 and string 'tell "me" \'yes\\\'' correctly.
 */

require_once 'ExpType.php';

class ExpLexerException extends Exception {
}

class ExpLexer {

  private static $BLANK = "/[\s]+/";
  private static $FUNCTION_PATTERN = "/osx\:(parseJson|decodeBase64|urlEncode|urlDecode)/";
  private static $TERNARY_PATTERN = "/[\?\:]/";
  private static $PAREN_PATTERN = "/[\[\]\(\)]/";
  private static $COMMA_PATTERN = "/[\,]/";
  private static $OPERATOR_PATTERN = "/\>\=|\<\=|\=\=|\!\=|\&\&|\|\||\*|\/|\%|\>|\<|\!/";  // No +/-: conflict with floating
  private static $OPERATOR_PATTERN2 = "/^(and|or|div|mod|gt|lt|ge|le|eq|ne|not|empty)$/";
  private static $NUMBER_PATTERN = "/^(([0-9]+\.[0-9]*)|(\.[0-9]+)|([0-9]+))([eE][\+\-]?[0-9]+)?$/";
  private static $IDENTITY_PATTERN = "/^[a-zA-Z][a-zA-Z0-9_]*$/";
  private static $DOT_PATTERN = "/[\.]/";

  private static $OPERATOR_TRANS = array('and' => '&&', 'or' => '||', 'div' => '/', 'mod' => '%', 'gt' => '>',
      'lt' => '<', 'ge' => '>=', 'le' => '<=', 'eq' => '==', 'ne' => '!=', 'not' => '!',
      'empty' => 'empty');

  private static $PAIRS = array(')' => '(', ']' => '[', ':' => '?');
  private static $ENDS = array(')', ']', ':');

  public static function process($str) {
    $tokenStream = ExpLexer::strToTokens($str);
    ExpLexer::evaluateLiterals($tokenStream);
    ExpLexer::validateTokens($tokenStream);
    return $tokenStream;
  }

  private static function strToTokens($str) {
    // Multi-pass segmentation
    $node = new Token(ExpType::$RAW, $str);
    $tokenStream = array($node);
    $tokenStream = ExpLexer::divideString($tokenStream);
    $tokenStream = ExpLexer::divideBlank($tokenStream);
    $tokenStream = ExpLexer::divideKey($tokenStream, ExpLexer::$FUNCTION_PATTERN, 'functionHandler');
    $tokenStream = ExpLexer::divideKey($tokenStream, ExpLexer::$TERNARY_PATTERN, 'ternaryHandler');
    $tokenStream = ExpLexer::divideKey($tokenStream, ExpLexer::$PAREN_PATTERN, 'parenHandler');
    $tokenStream = ExpLexer::divideKey($tokenStream, ExpLexer::$COMMA_PATTERN, 'commaHandler');
    $tokenStream = ExpLexer::divideKey($tokenStream, ExpLexer::$OPERATOR_PATTERN, 'operatorHandler');
    $tokenStream = ExpLexer::divideFloating($tokenStream);
    $tokenStream = ExpLexer::divideKey($tokenStream, ExpLexer::$OPERATOR_PATTERN2, 'operatorHandler2');
    $tokenStream = ExpLexer::divideDot($tokenStream);
    ExpLexer::distinguishOperator($tokenStream);
    return $tokenStream;
  }

  private static function isEscaped($str, $pos) {
    $numEscaper = 0;
    $posEscaper = $pos - 1;
    while ($posEscaper >= 0) {
      if ($str[$posEscaper] != '\\') break;
      $numEscaper ++;
      $posEscaper --;
    }
    return ($numEscaper % 2 == 1);
  }

  private static function divideString($tokenStream) {
    $newTokenStream = array();
    foreach ($tokenStream as $node) {
      if ($node->type != ExpType::$RAW) {
        // bypass the proceed nodes
        array_push($newTokenStream, $node);
        continue;
      }
      $str = $node->value;
      $state = 0; // 0: seeking, 1: found "'", 2: found '"'
      $pos = 0;
      for ($i = 0; $i < strlen($str); $i ++) {
        if ($str[$i] != "'" && $str[$i] != '"') continue;
        $targetState = $str[$i] == "'" ? 1 : 2;
        if ($state == 0) {
          // found "'" or '"'
          $state = $targetState;
          if ($pos != $i) {
            // output the previous segment
            $newnode = new Token(ExpType::$RAW, substr($str, $pos, $i - $pos));
            array_push($newTokenStream, $newnode);
          }
          $pos = $i + 1;
        } elseif ($state == $targetState) {
          // test whether it has been escaped
          if (ExpLexer::isEscaped($str, $i)) continue;  // bypass the escaped ones
          // output the string segment
          $string = ExpType::coerceToString(new Token(ExpType::$RAW, substr($str, $pos, $i - $pos)));
          array_push($newTokenStream, $string);
          $pos = $i + 1;
          $state = 0;
        }
      }
      if ($state != 0) {
        // there's open quote exists
        throw new ExpLexerException("Unterminated string: " . $str);
      }
      if ($pos != strlen($str)) {
        // output the remaining segment
        $newnode = new Token(ExpType::$RAW, substr($str, $pos, strlen($str) - $pos));
        array_push($newTokenStream, $newnode);
      }
    }
    return $newTokenStream;
  }

  private static function divideBlank($tokenStream) {
    $newTokenStream = array();
    foreach ($tokenStream as $node) {
      if ($node->type != ExpType::$RAW) {
        // bypass the proceed nodes
        array_push($newTokenStream, $node);
        continue;
      }
      $subNodes = preg_split(ExpLexer::$BLANK, $node->value);
      foreach ($subNodes as $subNode) {
        $newnode = new Token(ExpType::$RAW, $subNode);
        array_push($newTokenStream, $newnode);
      }
    }
    return $newTokenStream;
  }

  private static function divideKey($tokenStream, $keyPattern, $keyHandler) {
    $newTokenStream = array();
    foreach ($tokenStream as $node) {
      if ($node->type != ExpType::$RAW) {
        // bypass the proceed nodes
        array_push($newTokenStream, $node);
        continue;
      }
      $str = $node->value;
      preg_match_all($keyPattern, $str, $matchs, PREG_OFFSET_CAPTURE);
      $pos = 0;
      foreach ($matchs[0] as $match) {
        if ($pos != $match[1]) {
          // output the previous segment
          $newnode = new Token(ExpType::$RAW, substr($str, $pos, $match[1] - $pos));
          array_push($newTokenStream, $newnode);
        }
        // output the value.
        array_push($newTokenStream, ExpLexer::$keyHandler($match[0]));
        $pos = $match[1] + strlen($match[0]);
      }
      if ($pos != strlen($str)) {
        // output the remaining segment
        $newnode = new Token(ExpType::$RAW, substr($str, $pos, strlen($str) - $pos));
        array_push($newTokenStream, $newnode);
      }
    }
    return $newTokenStream;
  }

  private static function functionHandler($str) {
    // output the opensocial function name.
    return new Token(ExpType::$FUNCTION, $str);
  }

  private static function ternaryHandler($str) {
    // output the ternary.
    return new Token(ExpType::$TERNARY, $str);
  }

  private static function parenHandler($str) {
    // output the paren.
    return new Token(ExpType::$PAREN, $str);
  }

  private static function commaHandler($str) {
    // output the comma.
    return new Token(ExpType::$COMMA, $str);
  }

  private static function operatorHandler($str) {
    // output the operator.
    return new Token(ExpType::$RAW_OP, $str);
  }

  private static function operatorHandler2($str) {
    // output the operator.
    return new Token(ExpType::$RAW_OP, ExpLexer::$OPERATOR_TRANS[$str]);
  }

  private static function divideFloating($tokenStream) {
    $newTokenStream = array();
    $TAG_PATTERN = "/[eE][\+\-]|[eE]|[\+\-]/";
    $FLOAT_PATTERN = "/^[0-9\.]+$/";
    $INTEGER_PATTERN = "/^[0-9]+$/";
    foreach ($tokenStream as $node) {
      if ($node->type != ExpType::$RAW) {
        // bypass the proceed nodes
        array_push($newTokenStream, $node);
        continue;
      }
      $candidateTokenStream = ExpLexer::divideKey(array($node), $TAG_PATTERN, 'tagHandler');
      $tokenStack = array();
      while ($token = array_shift($candidateTokenStream)) {
        if ($token->type != 'tag') {
          $tokenLeft = array_pop($tokenStack);
          if (! $tokenLeft) {
            array_push($tokenStack, $token);
          } elseif ($tokenLeft->type == ExpType::$RAW) {
            $tokenLeft->value = $tokenLeft->value . $token->value;
            array_push($tokenStack, $tokenLeft);
          } else {
            array_push($tokenStack, $tokenLeft);
            array_push($tokenStack, $token);
          }
        } else {
          if (($token->value == '+') || ($token->value == '-')) {
            // regular operator
            $token->type = ExpType::$RAW_OP;
            array_push($tokenStack, $token);
          } else {
            // 'e', 'e+' or 'e-' encountered.
            $tokenLeft = array_pop($tokenStack);
            if ($tokenLeft == null) $tokenLeft = new Token(ExpType::$RAW, '');
            if ($tokenLeft->type != ExpType::$RAW) {
              array_push($tokenStack, $tokenLeft); // push the token back
              $tokenLeft = new Token(ExpType::$RAW, '');
            }
            if (preg_match($FLOAT_PATTERN, $tokenLeft->value) == 0) {
              // it's '[eE]', or a normal operator '[eE][+-]'
              $tokenLeft->value = $tokenLeft->value . $token->value[0];
              array_push($tokenStack, $tokenLeft);
              if (strlen($token->value) == 2) {
                $token->type = ExpType::$RAW_OP;
                $token->value = $token->value[1];
                array_push($tokenStack, $token);
              }
            } else {
              // might be a floating number, check the right side.
              $tokenRight = array_shift($candidateTokenStream);
              if ($tokenRight == null || $tokenRight->type != ExpType::$RAW || preg_match($INTEGER_PATTERN, $tokenRight->value) == 0) throw new ExpLexerException("Mal-format floating contained: " . $node->value);
              // Okay, all tests passed.  It's a floating number.  Put all parts together.  Will evaluate its value in the latter step.
              $tokenLeft->value = $tokenLeft->value . $token->value . $tokenRight->value;
              array_push($tokenStack, $tokenLeft);
            }
          }
        }
      }
      $newTokenStream = array_merge($newTokenStream, $tokenStack);
    }
    return $newTokenStream;
  }

  private static function tagHandler($str) {
    // output the tag, it might be a part of floating number, or a regular operator.
    return new Token('tag', $str);
  }

  private static function divideDot($tokenStream) {
    $newTokenStream = array();
    foreach ($tokenStream as $node) {
      if ($node->type != ExpType::$RAW) {
        // bypass the proceed nodes
        array_push($newTokenStream, $node);
        continue;
      }
      if (preg_match(ExpLexer::$NUMBER_PATTERN, $node->value) == 0) {
        $candidateTokenStream = ExpLexer::divideKey(array($node), ExpLexer::$DOT_PATTERN, 'dotHandler');
        $newTokenStream = array_merge($newTokenStream, $candidateTokenStream);
      } else {
        array_push($newTokenStream, $node);
      }
    }
    return $newTokenStream;
  }

  private static function dotHandler($str) {
    // output the tag, it might be a part of floating number, or a regular operator.
    return new Token(ExpType::$DOT, $str);
  }

  private static function distinguishOperator(&$tokenStream) {
    for ($i = 0; $i < count($tokenStream); $i ++) {
      $node = $tokenStream[$i];
      if ($node->type != ExpType::$RAW_OP) continue;
      $str = $node->value;
      if ($str == 'empty' || $str == '!') {
        $node->type = ExpType::$UNARY_OP;
      } elseif ($str != '-') {
        $node->type = ExpType::$BINARY_OP;
      } else {
        // distinguish unary/binary operator '-'
        $node->type = ExpLexer::hasUnaryLeft($tokenStream, $i) ? ExpType::$UNARY_OP : ExpType::$BINARY_OP;
        $node->value = $node->type == ExpType::$UNARY_OP ? ' -' : ' - ';
      }
    }
  }
  
  private static function hasUnaryLeft($tokenStream, $i) {
    $OPERATOR_LEFT_TYPE = array(ExpType::$BINARY_OP, ExpType::$UNARY_OP, ExpType::$TERNARY,
        ExpType::$COMMA);
    $OPERATOR_LEFT_VALUE = array('(', '[');
    if ($i == 0) return true;
    $node = $tokenStream[$i - 1];
    if (in_array($node->type, $OPERATOR_LEFT_TYPE) || $node->type == ExpType::$PAREN && in_array($node->value, $OPERATOR_LEFT_VALUE)) return true;
    return false;
  }

  private static function hasBinaryRight($tokenStream, $i) {
    $OPERATOR_RIGHT_NO_TYPE = array(ExpType::$BINARY_OP, ExpType::$TERNARY, ExpType::$COMMA);
    $OPERATOR_RIGHT_NO_VALUE = array(')', ']');
    if ($i == count($tokenStream) - 1) return false;
    $node = $tokenStream[$i + 1];
    if (in_array($node->type, $OPERATOR_RIGHT_NO_TYPE) || $node->type == ExpType::$PAREN && in_array($node->value, $OPERATOR_RIGHT_NO_VALUE)) return false;
    return true;
  }

  private static function evaluateLiterals(&$tokenStream) {
    for ($i = 0; $i < count($tokenStream); $i ++) {
      $node = $tokenStream[$i];
      if ($node->type != ExpType::$RAW) continue;
      $str = strtolower($node->value);
      if ($str == 'true' || $str == 'false') {
        $tokenStream[$i] = ExpType::coerceToBool($node);
      } elseif ($str == 'null') {
        $tokenStream[$i] = ExpType::coerceToNull($node);
      } elseif (preg_match(ExpLexer::$NUMBER_PATTERN, $str) == 1) {
        $tokenStream[$i] = ExpType::coerceToNumber($node);
      } elseif (preg_match(ExpLexer::$IDENTITY_PATTERN, $str) == 1) {
        // is identity
        $tokenStream[$i]->type = ExpType::$IDENTITY;
      } else {
        // mal-format
        throw new ExpLexerException("Mal-format expression segment: " . $node->value);
      }
    }
  }

  private static function validateTokens($tokenStream) {
    // No return value --- throws exception when error encountered.
    $stack = array();
    for ($i = 0; $i < count($tokenStream); $i ++) {
      $node = $tokenStream[$i];
      switch ($node->type) {
        case ExpType::$TERNARY: // drop through
        case ExpType::$PAREN: // Check balance
          if (in_array($node->value, ExpLexer::$ENDS)) {
            $stackLeft = array_pop($stack);
            if (ExpLexer::$PAIRS[$node->value] != $stackLeft) throw new ExpLexerException("Unbalanced expression");
          } else {
            array_push($stack, $node->value);
          }
          break;
        case ExpType::$UNARY_OP:  // Check unary operator gramma
          if (! ExpLexer::hasUnaryLeft($tokenStream, $i)) throw new ExpLexerException("Mal-format unary operator");
          break;
        case ExpType::$BINARY_OP:  // Check binary operator gramma
          if (ExpLexer::hasUnaryLeft($tokenStream, $i) || ! ExpLexer::hasBinaryRight($tokenStream, $i)) throw new ExpLexerException("Mal-format binary operator");
          break;
        case ExpType::$DOT:  // Check dot gramma
          if ($i == count($tokenStream) - 1 || $tokenStream[$i + 1]->type != ExpType::$IDENTITY) throw new ExpLexerException("Mal-format dot");
          break;
        case ExpType::$FUNCTION:  // Check function gramma
          if ($i == count($tokenStream) - 1 || $tokenStream[$i + 1]->value != '(') throw new ExpLexerException("Mal-format function");
          break;
      }
    }
    if (count($stack) != 0) throw new ExpLexerException("Unbalanced expression");
  }
}