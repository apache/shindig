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
package org.apache.shindig.expressions.juel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.el.ELContext;
import javax.el.ValueExpression;

import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.expressions.RootELResolver;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Maps;

public class JuelExpressionsTest {

  private Expressions expressions;
  private ELContext context;
  private Map<String, Object> variables;

  @Before
  public void setUp() {
    expressions = Expressions.forTesting(null);
    variables = Maps.newHashMap();
    context = expressions.newELContext(new RootELResolver(variables));
  }

  @Test
  public void booleanCoercionOfStringsFails() throws Exception {

    addVariable("bool", "FALSE");
    assertFalse(evaluate("${!bool}", Boolean.class));

    addVariable("bool", "booga");
    assertFalse(evaluate("${!bool}", Boolean.class));
  }

  @Test
  public void booleanCoercionOfNumbersFails() throws Exception {
    addVariable("bool", 0);
    assertTrue(evaluate("${!bool}", Boolean.class));

    addVariable("bool", 1);
    assertFalse(evaluate("${!bool}", Boolean.class));

    evaluate("${true && 5}", String.class);
  }

  private <T> T evaluate(String expression, Class<T> type) {

    ValueExpression expr = expressions.parse(expression, type);
    return type.cast(expr.getValue(context));
  }

  private void addVariable(String key, Object value) {
    variables.put(key, value);
  }

}
