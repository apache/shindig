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

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import javax.el.ELContext;
import javax.el.ValueExpression;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ExpressionsTest {
  private Expressions expressions;
  private ELContext context;
  
  @Before
  public void setUp() {
    expressions = new Expressions();
    context = expressions.newELContext(new RootELResolver());
  }
    
  @Test
  public void arraySupport() {
    addVariable("array", new String[]{"foo", "bar"});
    String result = evaluate("${array[0]}${array[1]}", String.class);
    assertEquals("foobar", result);
  }
  
  @Test
  public void listSupport() {
    addVariable("list", ImmutableList.of("foo", "bar"));
    String result = evaluate("${list[0]}${list[1]}", String.class);
    assertEquals("foobar", result);
  }
  
  @Test
  public void mapSupport() {
    addVariable("map", ImmutableMap.of("foo", "bar"));
    String result = evaluate("${map.foo}${map['foo']}", String.class);
    assertEquals("barbar", result);
  }

  @Test
  public void jsonObjectSupport() throws Exception {
    addVariable("object", new JSONObject("{foo: 125}"));
    int result = evaluate("${object.foo}", Integer.class);
    assertEquals(125, result);
  }

  @Test
  public void jsonArraySupport() throws Exception {
    addVariable("array", new JSONArray("[1, 2]"));
    int result = evaluate("${array[0] + array[1]}", Integer.class);
    assertEquals(3, result);
  }

  @Test
  public void jsonArrayCoercionOfStatic() throws Exception {
    JSONArray result = evaluate("first,second", JSONArray.class);
    JSONArray expected = new JSONArray("['first', 'second']");
    assertEquals(expected.toString(), result.toString());
  }
  
  @Test
  public void jsonArrayCoercion() throws Exception {
    addVariable("foo", "first,second");
    JSONArray result = evaluate("${foo}", JSONArray.class);
    JSONArray expected = new JSONArray("['first', 'second']");
    assertEquals(expected.toString(), result.toString());
  }
  
  private <T> T evaluate(String expression, Class<T> type) {
    ValueExpression expr = expressions.parse(expression, type);
    return type.cast(expr.getValue(context));
  }

  private void addVariable(String key, Object value) {
    context.getELResolver().setValue(context, null, key, value);
  }
}
