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
package org.apache.shindig.gadgets;

import static org.junit.Assert.assertEquals;

import org.apache.shindig.expressions.Expressions;
import org.junit.Before;
import org.junit.Test;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ValueExpression;

import com.google.common.collect.ImmutableMap;

/**
 * Test of GadgetELResolver.
 */
public class GadgetELResolverTest {
  private UserPrefs userPrefs;
  private String viewParams;
  private ELResolver resolver;
  private Expressions expressions;
  private ELContext context;

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

    resolver = new GadgetELResolver(gadgetContext);
    expressions = Expressions.forTesting();

    context = expressions.newELContext(resolver);
  }

  @Test
  public void getPrefs() {
    userPrefs = new UserPrefs(ImmutableMap.of("foo", "bar"));
    ValueExpression expression = expressions.parse("${UserPrefs.foo}", String.class);

    assertEquals("bar", expression.getValue(context));

    expression = expressions.parse("${UserPrefs.wrongKey}", String.class);
    assertEquals("", expression.getValue(context));
  }

  @Test
  public void getPrefsEmpty() {
    userPrefs = UserPrefs.EMPTY;
    ValueExpression expression = expressions.parse("${UserPrefs.foo}", String.class);
    assertEquals("", expression.getValue(context));
  }

  @Test
  public void testViewParams() {
    viewParams = "{foo: 'bar'}";

    ValueExpression expression = expressions.parse("${ViewParams.foo}", String.class);
    assertEquals("bar", expression.getValue(context));

    expression = expressions.parse("${ViewParams.wrongKey}", String.class);
    assertEquals("", expression.getValue(context));
  }

  @Test
  public void testViewParamsEmpty() {
    ValueExpression expression = expressions.parse("${ViewParams.foo}", String.class);
    assertEquals("", expression.getValue(context));
  }
}
