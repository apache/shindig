/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.expressions;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Implementation of the small subset of JSP EL that OpenSocial supports:
 * - String concatenation
 * - The "." operator
 * - Limited type coercion
 */
public class Expressions {
  /** Constant for the start of an expression */
  private static final String EXPRESSION_START = "${";
  /** Constant for the end of an expression */
  private static final char EXPRESSION_END = '}';

  /**
   * Parse a string into an Expression object.
   * @param <T> type of the expression
   * @param text the text in the expression
   * @param type type of object the expression will return
   * @throws ElException if errors are found in the expression while parsing.
   *     Unterminated expressions, empty expressions, and constant values that
   *     cannot be properly coerced will result in exceptions.
   */
  public static <T> Expression<T> parse(String text, Class<T> type) throws ElException {
    Preconditions.checkNotNull(text);
    Preconditions.checkNotNull(type);

    // Check for constant text (no EL), or one big EL expression
    int nextExpressionIndex = nextExpression(text, 0);
    if (nextExpressionIndex < 0) {
      T value = coerce(text, type);
      return new ConstantExpression<T>(value);
    } else if (nextExpressionIndex == 0 && endOfExpression(text, 2) == text.length() - 1) {
      return parseEL(text, 2, text.length() - 1, type);
    }

    // String concatenation case;  find each expression
    List<Expression<String>> expressions = Lists.newArrayList();
    int start = 0;
    while (nextExpressionIndex >= 0) {
      // Add a constant expression if needed
      if (start < nextExpressionIndex) {
        String constantText = text.substring(start, nextExpressionIndex);
        expressions.add(new ConstantExpression<String>(constantText));
      }

      int endOfExpression = endOfExpression(text, nextExpressionIndex + 2);
      if (endOfExpression < 0) {
        throw new ElException("Unterminated expression in " + text);
      }

      expressions.add(parseEL(text, nextExpressionIndex + 2, endOfExpression, String.class));
      start = endOfExpression + 1;
      nextExpressionIndex = nextExpression(text, start);
    }

    if (start < text.length()) {
      String constantText = text.substring(start);
      expressions.add(new ConstantExpression<String>(constantText));
    }

    return new ConcatExpression<T>(expressions, type);
  }

  /**
   * Parses an EL expression within a string.
   * @throws ElException
   */
  private static <T> Expression<T> parseEL(final String text, int from, int to,
      final Class<T> type) throws ElException {
    if (from == to) {
      throw new ElException("Empty expression in " + text);
    }

    // TODO: the spec only describes support for "a.b", not "a.b.c.d".  Update
    // the spec, or limit this function?
    final List<String> segments = splitSegments(text.substring(from, to));
    return new Expression<T>() {

      public T evaluate(ExpressionContext context) throws ElException {
        Object value = context.getVariable(segments.get(0));
        for (int i = 1; i < segments.size(); i++) {
          if (value == null) {
            throw new ElException("Could not find property \"" + segments.get(i - 1) + "\" in \""
                + text + '\"');
          }
          value = getProperty(value, segments.get(i));
        }

        return coerce(value, type);
      }

    };
  }

  private static List<String> splitSegments(String input) {
    List<String> segments = new ArrayList<String>();
    StringBuilder buf = new StringBuilder(16);
    for (int i = 0, j = input.length(); i < j; ++i) {
      char ch = input.charAt(i);
      if (ch == '\\' && i < j && input.charAt(i + 1) == '.') {
        // Escaped dot.
        buf.append('.');
        ++i;
      } else if (ch == '.') {
        // end of identifier
        segments.add(buf.toString());
        buf.setLength(0);
      } else {
        buf.append(input.charAt(i));
      }
    }

    segments.add(buf.toString());

    return segments;
  }

  private static Object getProperty(Object value, String propertyName) throws ElException {
    if (value instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) value;
      return map.get(propertyName);
    } else if (value instanceof JSONObject) {
      return ((JSONObject) value).opt(propertyName);
    } else if (value instanceof ExpressionContext) {
      return ((ExpressionContext) value).getVariable(propertyName);
    }

    throw new ElException("Unsupported property parent type " + value.getClass());
  }

  /**
   * Hardcoded Object-to-Object coercion logic.
   * - Strings are parsed with toString().
   * - Integers are parsed from numbers with intValue(), and with toString() and
   *   Integer.parseInt() for all other types.
   * - Booleans are false if they are numeric and equal to 0, if they are
   *   Boolean.FALSE, or if they are case-insensitive equal to "false".
   * - Arrays are parsed with comma separators.
   *
   */
  static <T> T coerce(Object value, Class<T> type) throws ElException {
    if (value == null) {
      return null;
    }

    if (type == String.class) {
      @SuppressWarnings("unchecked")
      T string = (T) value.toString();
      return string;
    } else if (type == Integer.class) {
      int intValue;
      if (value instanceof Number) {
        intValue = ((Number) value).intValue();
      } else {
        try {
          intValue = Integer.parseInt(value.toString());
        } catch (NumberFormatException nfe) {
          throw new ElException(nfe);
        }
      }

      @SuppressWarnings("unchecked")
      T integer = (T) Integer.valueOf(intValue);
      return integer;
    } else if (type == JSONArray.class) {
      JSONArray array;
      if (value instanceof JSONArray) {
        array = (JSONArray) value;
      } else {
        array = new JSONArray();
        StringTokenizer tokenizer = new StringTokenizer(value.toString(), ",");
        while (tokenizer.hasMoreTokens()) {
          array.put(tokenizer.nextToken());
        }
      }

      @SuppressWarnings("unchecked")
      T t = (T) array;
      return t;
    } else if (type == Boolean.class) {
      boolean boolValue;
      // TODO: spec question, does this coercion make sense?
      if (value instanceof Number) {
        boolValue = ((Number) value).intValue() != 0;
      } else if (value instanceof Boolean) {
        boolValue = Boolean.TRUE.equals(value);
      } else {
        // TODO: especially this case...
        boolValue = !"false".equalsIgnoreCase(value.toString());
      }

      @SuppressWarnings("unchecked")
      T t = (T) Boolean.valueOf(boolValue);
      return t;
    }

    // Fallback, see if the type is already correct
    try {
      return type.cast(value);
    } catch (ClassCastException cce) {
      throw new ElException("Could not cast " + value + " to " + type.getCanonicalName(),
          cce);
    }
  }

  /** Find the start of the next expression */
  private static int nextExpression(String text, int after) {
    // TODO: JSP EL supports escaping.  The Pipelining spec does not.
    // Add detection of "\${" to the spec?
    return text.indexOf(EXPRESSION_START, after);
  }

  /** Find the end of the current expression */
  private static int endOfExpression(String text, int after) {
    // TODO: support escaping
    return text.indexOf(EXPRESSION_END, after);
  }

  /** Expression class for constant values */
  public static class ConstantExpression<T> implements Expression<T> {
    private final T value;

    public ConstantExpression(T value) {
      this.value = value;
    }

    public T evaluate(ExpressionContext context) {
      return value;
    }
  }

  /** Expression class for string concatenation */
  public static class ConcatExpression<T> implements Expression<T> {

    private final List<Expression<String>> expressions;
    private final Class<T> type;

    public ConcatExpression(List<Expression<String>> expressions, Class<T> type) {
      this.expressions = expressions;
      this.type = type;
    }

    public T evaluate(ExpressionContext context) throws ElException {
      StringBuilder builder = new StringBuilder();
      for (Expression<String> expression : expressions) {
        builder.append(expression.evaluate(context));
      }

      return coerce(builder.toString(), type);
    }
  }
}
