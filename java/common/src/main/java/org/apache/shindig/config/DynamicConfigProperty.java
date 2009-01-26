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
package org.apache.shindig.config;

import org.apache.shindig.expressions.ElException;
import org.apache.shindig.expressions.Expression;
import org.apache.shindig.expressions.ExpressionContext;
import org.apache.shindig.expressions.Expressions;


/**
 * String property that can be interpreted using a container context.
 *
 * Implements CharSequence strictly as a marker. Only toString is supported.
 */
public class DynamicConfigProperty implements CharSequence {
  private final ExpressionContext context;
  private final Expression<String> expression;

  public DynamicConfigProperty(String value, ExpressionContext context) {
    this.context = context;
    Expression<String> expression = null;
    try {
      expression = Expressions.parse(value, String.class);
    } catch (ElException e) {
      expression = new Expressions.ConstantExpression<String>(value);
    }
    this.expression = expression;
  }

  @Override
  public String toString() {
    try {
      return expression.evaluate(context);
    } catch (ElException e) {
      return "";
    }
  }

  public char charAt(int index) {
    throw new UnsupportedOperationException();
  }

  public int length() {
    throw new UnsupportedOperationException();
  }

  public CharSequence subSequence(int start, int end) {
    throw new UnsupportedOperationException();
  }
}
