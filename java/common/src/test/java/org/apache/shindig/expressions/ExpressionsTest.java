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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import javax.el.ELContext;
import javax.el.PropertyNotFoundException;
import javax.el.ValueExpression;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class ExpressionsTest {
  public Expressions expressions;
  private ELContext context;
  private Map<String, Object> variables;

  @Before
  public void setUp() {
    expressions = Expressions.forTesting();
    variables = Maps.newHashMap();
    context = expressions.newELContext(new RootELResolver(variables));
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

  @Test
  public void missingJsonSubproperty() throws Exception {
    addVariable("object", new JSONObject("{foo: 125}"));
    assertNull(evaluate("${object.bar.baz}", Object.class));
  }

  @Test
  public void missingMapSubproperty() throws Exception {
    addVariable("map", ImmutableMap.of("key", "value"));
    assertNull(evaluate("${map.bar.baz}", Object.class));
  }

  @Test(expected = PropertyNotFoundException.class)
  public void missingTopLevelVariable() throws Exception {
    // Top-level properties must throw a PropertyNotFoundException when
    // failing;  other properties must not.  Pipeline data batching
    // relies on this
    assertNull(evaluate("${map.bar.baz}", Object.class));
  }

  @Test
  public void booleanCoercionOfBooleans() throws Exception{
    addVariable("bool", false);
    assertFalse(evaluate("${bool}", Boolean.class));
    assertTrue(evaluate("${!bool}", Boolean.class));

    addVariable("bool", true);
    assertTrue(evaluate("${bool}", Boolean.class));
    assertFalse(evaluate("${!bool}", Boolean.class));
  }

  @Test
  public void booleanCoercionOfNumbers() throws Exception{
    // Negation tests have been moved to EL subdir
    addVariable("bool", 0);
    assertFalse(evaluate("${bool}", Boolean.class));

    addVariable("bool", 1);
    assertTrue(evaluate("${bool}", Boolean.class));
  }

  @Test
  public void booleanCoercionOfNull() throws Exception{
    addVariable("bool", null);
    assertFalse(evaluate("${bool}", Boolean.class));
    assertTrue(evaluate("${!bool}", Boolean.class));
  }

  @Test
  public void booleanCoercionOfStrings() throws Exception{
    // Negation tests for FALSE and any String have been moved El subdir
    addVariable("bool", "");
    assertFalse(evaluate("${bool}", Boolean.class));
    assertTrue(evaluate("${!bool}", Boolean.class));

    addVariable("bool", "false");
    assertFalse(evaluate("${bool}", Boolean.class));
    assertTrue(evaluate("${!bool}", Boolean.class));

    // Case-sensitive coercion:  FALSE is true
    addVariable("bool", "FALSE");
    assertTrue(evaluate("${bool}", Boolean.class));

    addVariable("bool", "true");
    assertTrue(evaluate("${bool}", Boolean.class));
    assertFalse(evaluate("${!bool}", Boolean.class));

    addVariable("bool", "booga");
    assertTrue(evaluate("${bool}", Boolean.class));
  }


  @Test
  public void iterableCoercionOfScalar() throws Exception {
    addVariable("iter", "foo");
    assertEquals(ImmutableList.of("foo"),
        evaluate("${iter}", Iterable.class));
  }

  @Test
  public void iterableCoercionOfNull() throws Exception {
    addVariable("iter", null);
    assertEquals(ImmutableList.of(),
        evaluate("${iter}", Iterable.class));
  }

  @Test
  public void iterableCoercionOfCollection() throws Exception {
    addVariable("iter", ImmutableList.of(1, 2, 3));
    assertEquals(ImmutableList.of(1, 2, 3),
        evaluate("${iter}", Iterable.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void iterableCoercionOfJSONArray() throws Exception {
    addVariable("iter", new JSONArray("[1,2,3]"));
    assertEquals(ImmutableList.of(1, 2, 3),
        ImmutableList.copyOf(evaluate("${iter}", Iterable.class)));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void iterableCoercionOfJSONObjectWithListProperty() throws Exception {
    addVariable("iter", new JSONObject("{list: [1,2,3]}"));
    assertEquals(ImmutableList.of(1, 2, 3),
        ImmutableList.copyOf(evaluate("${iter}", Iterable.class)));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void iterableCoercionOfJSONObjectWithoutListProperty() throws Exception {
    JSONObject json = new JSONObject("{foo: [1,2,3]}");
    addVariable("iter", json);
    assertEquals(ImmutableList.of(json),
        ImmutableList.copyOf(evaluate("${iter}", Iterable.class)));
  }

  public <T> T evaluate(String expression, Class<T> type) {
    ValueExpression expr = expressions.parse(expression, type);
    return type.cast(expr.getValue(context));
  }

  public void addVariable(String key, Object value) {
    variables.put(key, value);
  }
}
