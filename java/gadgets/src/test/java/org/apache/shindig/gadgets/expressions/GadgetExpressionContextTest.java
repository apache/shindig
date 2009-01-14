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
package org.apache.shindig.gadgets.expressions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.UserPrefs;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test of GadgetExpressionContext.
 */
public class GadgetExpressionContextTest {

  private UserPrefs userPrefs;
  private String viewParams;
  private GadgetExpressionContext context;

  @Before
  public void setUp() throws Exception {
    GadgetContext gadgetContext = new GadgetContext() {
      @Override
      public String getParameter(String name) {
        if ("view-params".equals(name)) {
          return viewParams;
        }
        
        return null;
      }

      @Override
      public UserPrefs getUserPrefs() {
        return userPrefs;
      }
    };
    
    context = new GadgetExpressionContext(gadgetContext);
  }

  @Test
  public void getPrefs() throws ElException {
    userPrefs = new UserPrefs(ImmutableMap.of("foo", "bar"));
    Expression<String> expression = Expressions.parse("${UserPrefs.foo}", String.class);
    
    assertEquals("bar", expression.evaluate(context));

    expression = Expressions.parse("${UserPrefs.wrongKey}", String.class);
    assertNull(expression.evaluate(context));
  }

  @Test
  public void getPrefsEmpty() throws ElException {
    userPrefs = UserPrefs.EMPTY;
    Expression<String> expression = Expressions.parse("${UserPrefs.foo}", String.class);
    assertNull(expression.evaluate(context));
  }


  @Test
  public void testViewParams() throws ElException {
    viewParams = "{foo: 'bar'}";
    
    Expression<String> expression = Expressions.parse("${ViewParams.foo}", String.class);
    assertEquals("bar", expression.evaluate(context));

    expression = Expressions.parse("${ViewParams.wrongKey}", String.class);
    assertNull(expression.evaluate(context));
  }

  @Test
  public void testViewParamsEmpty() throws ElException {
    Expression<String> expression = Expressions.parse("${ViewParams.foo}", String.class);
    assertNull(expression.evaluate(context));
  }
}
