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

import org.apache.commons.codec.binary.Base64;

import java.util.Map;

import javax.el.ELContext;
import javax.el.ValueExpression;

import com.google.common.collect.Maps;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OpensocialFunctionsTest extends Assert {
  private Expressions expressions;
  private ELContext context;
  private Map<String, Object> vars = Maps.newHashMap();

  @Before
  public void setUp() {
    Functions functions = new Functions(OpensocialFunctions.class);
    expressions = Expressions.forTesting(functions);
    context = expressions.newELContext(new RootELResolver(vars));
  }

  @Test
  public void testParseJsonObject() {
    ValueExpression testParseJsonObject =
      expressions.parse("${osx:parseJson('{a: 1}').a}", Integer.class);
    assertEquals(1, testParseJsonObject.getValue(context));
  }

  @Test
  public void testParseJsonArray() {
    ValueExpression testParseJsonArray =
      expressions.parse("${osx:parseJson('[1, 2, 3]')[1]}", Integer.class);
    assertEquals(2, testParseJsonArray.getValue(context));
  }

  @Test
  public void testDecodeBase64() throws Exception {
    String test = "12345";
    String encoded = new String(Base64.encodeBase64(test.getBytes("UTF-8")), "UTF-8");
    vars.put("encoded", encoded);

    ValueExpression testDecodeBase64 =
      expressions.parse("${osx:decodeBase64(encoded)}", String.class);
    assertEquals("12345", testDecodeBase64.getValue(context));
  }

  @Test
  public void testUrlEncode() throws Exception {
    String test = "He He";
    vars.put("test", test);

    ValueExpression testUrlEncode =
      expressions.parse("${os:urlEncode(test)}", String.class);
    assertEquals("He+He", testUrlEncode.getValue(context));
  }

  @Test
  public void testUrlDecode() throws Exception {
    String test = "He+He";
    vars.put("encoded", test);

    ValueExpression testUrlDecode =
      expressions.parse("${os:urlDecode(encoded)}", String.class);
    assertEquals("He He", testUrlDecode.getValue(context));
  }

  @Test
  public void testHtmlEncode() throws Exception {
    String test = "<test>";
    vars.put("test", test);

    ValueExpression testHtmlEncode =
      expressions.parse("${os:htmlEncode(test)}", String.class);
    assertEquals("&lt;test&gt;", testHtmlEncode.getValue(context));
  }

  @Test
  public void testHtmlDecode() throws Exception {
    String test = "&lt;1+1>3&gt;";
    vars.put("encoded", test);

    ValueExpression testHtmlDecode =
      expressions.parse("${os:htmlDecode(encoded)}", String.class);
    assertEquals("<1+1>3>", testHtmlDecode.getValue(context));
  }

  @Test
  public void testParseJsonNull() throws Exception {
    ValueExpression testUrlEncode =
      expressions.parse("${osx:parseJson(null)}", String.class);
    assertEquals("", testUrlEncode.getValue(context));
  }

  @Test
  public void testDecodeBase64Null() throws Exception {
    ValueExpression testUrlEncode =
      expressions.parse("${osx:decodeBase64(null)}", String.class);
    assertEquals("", testUrlEncode.getValue(context));
  }

  @Test
  public void testUrlEncodeNull() throws Exception {
    ValueExpression testUrlEncode =
      expressions.parse("${os:urlEncode(null)}", String.class);
    assertEquals("", testUrlEncode.getValue(context));
  }

  @Test
  public void testUrlDecodeNull() throws Exception {
    ValueExpression testUrlDecode =
      expressions.parse("${os:urlDecode(null)}", String.class);
    assertEquals("", testUrlDecode.getValue(context));
  }
}
