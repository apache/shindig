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

import junit.framework.TestCase;

public class OpensocialFunctionsTest extends TestCase {
  private Expressions expressions;
  private ELContext context;
  private Map<String, Object> vars = Maps.newHashMap();
  
  @Override
  protected void setUp() {
    Functions functions = new Functions(OpensocialFunctions.class);
    expressions = new Expressions(functions);
    context = expressions.newELContext(new RootELResolver(vars));
  }
  
  public void testParseJsonObject() {
    ValueExpression testParseJsonObject =
      expressions.parse("${os:xParseJson('{a: 1}').a}", Integer.class);
    assertEquals(1, testParseJsonObject.getValue(context));
  }

  public void testParseJsonArray() {
    ValueExpression testParseJsonArray =
      expressions.parse("${os:xParseJson('[1, 2, 3]')[1]}", Integer.class);
    assertEquals(2, testParseJsonArray.getValue(context));
  }
  
  public void testDecodeBase64() throws Exception {
    String test = "12345";
    String encoded = new String(Base64.encodeBase64(test.getBytes("UTF-8")), "UTF-8");
    vars.put("encoded", encoded);
    
    ValueExpression testDecodeBase64 =
      expressions.parse("${os:xDecodeBase64(encoded)}", String.class);
    assertEquals("12345", testDecodeBase64.getValue(context));
  }
}
