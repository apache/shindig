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

/**
 * Lexer for parsing the os-template / os-data expression language, which is based on the JSP EL syntax
 * For reference on the language see:
 * JSP EL: https://jsp.dev.java.net/spec/jsp-2_1-fr-spec-el.pdf
 * OS Templates: http://opensocial-resources.googlecode.com/svn/spec/0.9/OpenSocial-Templating.xml
 * OS Data pipelining: http://opensocial-resources.googlecode.com/svn/spec/0.9/OpenSocial-Data-Pipelining.xml
 *
 * This parser accepts the output token stream produced by ExpLexer.
 */

class ExpParser {

  private static function scopePop(&$expression, &$scopes, $dataContext) {
    $value = $expression->evaluate($dataContext);
    $expression = array_pop($scopes);
    $expression->append($value);
  }

  private static function scopePush(&$expression, &$scopes, $token) {
    array_push($scopes, $expression);
    $expression = new PrimitiveExp($token);
  }

  public static function parse($tokenStream, $dataContext) {
    $scopes = array();
    $expression = new PrimitiveExp(new Token('final'));

    while ($token = array_shift($tokenStream)) {
      // split non-primitive expression into primitive ones
      switch ($token->type) {
        case ExpType::$PAREN:
          switch ($token->value) {
            case '[':
              $expression->append(new Token(ExpType::$DOT, '.'));  // drop through
            case '(':
              ExpParser::scopePush($expression, $scopes, $token);
              break;
            case ']':
              if ($expression->reason->value != '[') throw new ExpParserException("Unbalanced [], should be detected in Lexer");
              ExpParser::scopePop($expression, $scopes, $dataContext);
              break;
            case ')':
              if ($expression->reason->value != '(') throw new ExpParserException("Unbalanced (), should be detected in Lexer");
              ExpParser::scopePop($expression, $scopes, $dataContext);
              // close if it's a function
              if ($expression->reason->type == ExpType::$FUNCTION) ExpParser::scopePop($expression, $scopes, $dataContext);
              break;
            default:
              throw new ExpParserException("Token error: " . print_r($token, true));
          }
          break;
        case ExpType::$FUNCTION:
          ExpParser::scopePush($expression, $scopes, $token);
          break;
        case ExpType::$TERNARY:
          if ($token->value != '?') throw new ExpParserException("Ternary token error");
          $nextTernary = ExpParser::findNextTernary($tokenStream, 1);
          $indicator = ExpType::coerceToBool($expression->evaluate($dataContext));
          if ($indicator->value) {
            // parsed?todo:skip
            $nextCloseSymbol = ExpParser::findNextCloseSymbol($tokenStream, $nextTernary + 1);
            array_splice($tokenStream, $nextTernary, $nextCloseSymbol - $nextTernary);
          } else {
            // parsed?skip:todo
            $tokenStream = array_slice($tokenStream, $nextTernary + 1);
          }
          $expression = new PrimitiveExp($expression->reason);
          break;
        case ExpType::$COMMA:
          if ($expression->reason->value != '(') throw new ExpParserException("Unbalanced (), should be detected in Lexer");
          ExpParser::scopePop($expression, $scopes, $dataContext);
          ExpParser::scopePush($expression, $scopes, new Token(ExpType::$PAREN, '('));
          break;
        default:
          $expression->append($token);
      }
    }
    if ($expression->reason->type != 'final') throw new ExpParserException("Gramma error on the non-primitive expression");
    return $expression->evaluate($dataContext);
  }

  private static function findNextTernary($tokenStream, $startPos) {
    $stackDepth = 0;
    for ($i = $startPos; $i < count($tokenStream); $i ++) {
      $token = $tokenStream[$i];
      if ($token->type == ExpType::$TERNARY && $token->value == '?') $stackDepth ++;
      if ($token->type == ExpType::$TERNARY && $token->value == ':') $stackDepth --;
      if ($stackDepth < 0) break;
    }
    return $i;
  }

  private static function findNextCloseSymbol($tokenStream, $startPos) {
    $stackDepth = 0;
    for ($i = $startPos; $i < count($tokenStream); $i ++) {
      $token = $tokenStream[$i];
      if ($stackDepth == 0 && ExpParser::isCloseSymbol($token, false)) break;
      if (ExpParser::isOpenSymbol($token)) $stackDepth ++;
      if (ExpParser::isCloseSymbol($token)) $stackDepth --;
    }
    return $i;
  }

  private static function isOpenSymbol($token) {
    if ($token->type == ExpType::$PAREN && in_array($token->value, array('[', '('))) return true;
    return false;
  }

  private static function isCloseSymbol($token, $rigid = true) {
    if ($token->type == ExpType::$PAREN && in_array($token->value, array(']', ')'))) return true;
    if (! $rigid && ($token->type == ExpType::$COMMA || $token->type == ExpType::$TERNARY)) return true;
    return false;
  }
}
