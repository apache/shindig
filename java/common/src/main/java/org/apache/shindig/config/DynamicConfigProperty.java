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

import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.expressions.Expressions;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ValueExpression;


/**
 * String property that can be interpreted using a container context.
 *
 * Implements CharSequence strictly as a marker. Only toString is supported.
 */
public class DynamicConfigProperty implements CharSequence {
  private static final String classname = DynamicConfigProperty.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);
  private final ELContext context;
  private final ValueExpression expression;

  public DynamicConfigProperty(String value, Expressions expressions, ELContext context) {
    this.context = context;
    this.expression = expressions.parse(value, String.class);
  }

  @Override
  public String toString() {
    try {
      return (String) expression.getValue(context);
    } catch (ELException e) {
        if (LOG.isLoggable(Level.WARNING)) {
          //log the i18n expression
          LOG.logp(Level.WARNING, classname, "toString", MessageKeys.EVAL_EL_FAILED, new Object[] {expression.getExpressionString()});
          //now log the stacktrace
          LOG.logp(Level.WARNING, classname, "toString", MessageKeys.EVAL_EL_FAILED, e);
        }
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
