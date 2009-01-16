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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.apache.shindig.expressions.ElException;
import org.apache.shindig.expressions.Expression;
import org.apache.shindig.expressions.ExpressionContext;
import org.apache.shindig.expressions.Expressions;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;


/**
 * Tests of the Expressions class.
 */
public class ExpressionsTest {
  private FakeContext context;

  @Before
  public void setUp() {
    context = new FakeContext();
  }
  
  @Test
  public void constantExpressions() throws Exception {
    assertEquals("foo", Expressions.parse("foo", String.class).evaluate(context));
    assertEquals(1, Expressions.parse("1", Integer.class).evaluate(context).intValue());
  }

  @Test
  public void simpleExpressions() throws Exception {
    context.variables.put("var", "value");
    context.variables.put("int", 1);
    
    Expression<String> var = Expressions.parse("${var}", String.class);
    assertEquals("value", var.evaluate(context));
    
    Expression<Integer> intExpression = Expressions.parse("${int}", Integer.class);
    assertEquals(1, intExpression.evaluate(context).intValue());
  }

  @Test
  public void variableNotFoundIsNull() throws Exception {
    Expression<String> var = Expressions.parse("${var}", String.class);
    assertNull(var.evaluate(context));
  }

  @Test
  public void propertyEvaluationForMaps() throws Exception {
    context.variables.put("var", ImmutableMap.of("one", 1, "two", 2));
    
    Expression<Integer> var = Expressions.parse("${var.one}${var.two}", Integer.class);
    // 1 and 2 concatenated make 12, not 3
    assertEquals(12, var.evaluate(context).intValue());    
  }

  @Test
  public void propertyEvaluationForJson() throws Exception {
    context.variables.put("var", new JSONObject("{top: {middle: {inner: 'value'}}}"));
    
    Expression<String> var = Expressions.parse("${var.top.middle.inner}", String.class);
    assertEquals("value", var.evaluate(context));    
  }

  @Test(expected = ElException.class)
  public void exceptionWhenCoercionFails() throws Exception {
    context.variables.put("var", "value");
    
    Expression<Integer> var = Expressions.parse("${var}", Integer.class);
    var.evaluate(context);
  }

  @Test(expected = ElException.class)
  public void exceptionIfExpressionNotClosed() throws Exception {
    Expressions.parse("${var${foo", Integer.class);
  }

  @Test(expected = ElException.class)
  public void exceptionWhenPropertyEvaluationFails() throws Exception {
    Expression<String> var = Expressions.parse("${var.property}", String.class);
    var.evaluate(context);
  }

  @Test
  public void concatExpressions() throws Exception {
    context.variables.put("var", "value");
    
    Expression<String> concat = Expressions.parse("foo${var}bar", String.class);
    assertEquals("foovaluebar", concat.evaluate(context));
  }

  static public class FakeContext implements ExpressionContext {
    public final Map<String, Object> variables = Maps.newHashMap();
    
    public Object getVariable(String name) {
      return variables.get(name);
    }
  }

}
