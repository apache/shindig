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
 * Implementation of the os-template / os-data expression language, which is based on the JSP EL syntax
 * For reference on the language see:
 * JSP EL: https://jsp.dev.java.net/spec/jsp-2_1-fr-spec-el.pdf
 * OS Templates: http://opensocial-resources.googlecode.com/svn/spec/0.9/OpenSocial-Templating.xml
 * OS Data pipelining: http://opensocial-resources.googlecode.com/svn/spec/0.9/OpenSocial-Data-Pipelining.xml
 *
 */

require_once 'ExpLexer.php';
require_once 'ExpParser.php';

class ExpressionException extends Exception {
}

class ExpressionParser {

  /**
   * Evaluates the given $expression using the $dataContext as variable source.
   *
   * Internally the ExpressionParser uses a infix to postfix conversion to easily
   * be able to evaluate mathematical expressions
   *
   * @param string $expression
   * @param array $dataContext
   * @return string evaluated result or an exception of failure
   */
  public static function evaluate($expression, $dataContext) {
    $outputTokens = ExpLexer::process($expression);
    $result = ExpParser::parse($outputTokens, $dataContext);
    return $result->value;
  }

  /**
   * Misc function to convert an array to string, the reason a plain implode() doesn't
   * always work is because it'll complain about array to string conversions if
   * the array contains array's as entries
   *
   * @param $array
   * @return string
   */
  private static function arrayToString($array) {
    foreach ($array as $key => $entry) {
      if (is_array($entry)) {
        $array[$key] = self::arrayToString($entry);
      }
    }
    return implode(',', $array);
  }

  /**
   * Returns the string value of the (mixed) $val, ie:
   * on array, return "1, 2, 3, 4"
   * on int, return "1"
   * on string, return as is
   */
  public static function stringValue($val) {
    if (is_array($val)) {
      return self::arrayToString($val);
    } elseif (is_numeric($val)) {
      return (string)$val;
    } else {
      return $val;
    }
  }
}