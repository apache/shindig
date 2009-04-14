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

import org.json.JSONObject;

import java.lang.reflect.Method;

import javax.el.ELContext;
import javax.el.ValueExpression;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.TestCase;

public class FunctionsTest extends TestCase {
  private Functions functions;

  @Override
  protected void setUp() throws Exception {
    functions = new Functions(FunctionsTest.class);
  }

  public void testExpose() throws Exception {
    Method hi = functions.resolveFunction("test", "hi");
    assertEquals("hi", hi.invoke(null));

    Method hiAlternate = functions.resolveFunction("test", "hola");
    assertEquals("hi", hiAlternate.invoke(null));

    Method bonjour = functions.resolveFunction("other", "bonjour");
    assertEquals("French hello", bonjour.invoke(null));
  }
  
  public void testNonStaticNotExposed() {
    assertNull(functions.resolveFunction("test", "goodbye"));
  }
  
  public void testDefaultBinding() throws Exception {
    Injector injector = Guice.createInjector();
    functions = injector.getInstance(Functions.class);
    
    Method toJson = functions.resolveFunction("osx", "parseJson");
    Object o = toJson.invoke(null, "{a : 1}");
    assertTrue(o instanceof JSONObject);
    assertEquals(1, ((JSONObject) o).getInt("a"));
  }
  
  public void testExpressionEvaluation() {
    Expressions expressions = new Expressions(functions);
    ELContext context = expressions.newELContext();
    ValueExpression expression = expressions.parse("${other:bonjour()}", String.class);
    
    assertEquals("French hello", expression.getValue(context));
    
    expression = expressions.parse("${test:add(1, 2)}", Integer.class);
    assertEquals(3, expression.getValue(context));
  }
  
  /**
   * Static function, should be exposed under two names.
   */
  @Functions.Expose(prefix="test", names={"hi", "hola"})
  public static String sayHi() {
    return "hi";
  }

  /**
   * Test with some arguments.
   */
  @Functions.Expose(prefix="test", names={"add"})
  public static int add(int i, int j) {
    return i + j;
  }

  /**
   * Static function, should be exposed under two names.
   */
  @Functions.Expose(prefix="other", names={"bonjour"})
  public static String sayHi2() {
    return "French hello";
  }

  /**
   * Non-static: shouldn't be exposed.
   */
  @Functions.Expose(prefix="test", names={"goodbye"})
  public String sayGoodbye() {
    return "goodbye";
  }
}
