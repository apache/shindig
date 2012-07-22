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

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableSet;

import org.apache.shindig.expressions.Expressions;
import org.junit.Test;

import java.util.Map;

/**
 * Tests for ExpressionContainerConfig.
 */
public class ExpressionContainerConfigTest extends BasicContainerConfigTest {

  private static final Map<String, Object> DEFAULT_EXPR_CONTAINER =
      makeContainer("default", "expr", "${inherited}", "inherited", "yes");
  private static final Map<String, Object> MODIFIED_DEFAULT_EXPR_CONTAINER =
      makeContainer("default", "expr", "${inherited}", "inherited", "si");

  @Override
  public void setUp() throws Exception {
    config = new ExpressionContainerConfig(Expressions.forTesting());
    config.newTransaction().addContainer(DEFAULT_EXPR_CONTAINER).commit();
  }

  @Override
  public void testGetProperties() throws Exception {
    assertEquals(ImmutableSet.of("gadgets.container", "inherited", "expr"),
        config.getProperties("default").keySet());
  }

  @Test
  public void testExpressionValues() throws Exception {
    assertEquals("yes", config.getString("default", "expr"));
  }

  @Test
  public void testExpressionInheritance() throws Exception {
    config.newTransaction().addContainer(EXTRA_CONTAINER).commit();
    assertEquals("yes", config.getString("default", "expr"));
    assertEquals("yes", config.getString("extra", "expr"));
    config.newTransaction().addContainer(MODIFIED_EXTRA_CONTAINER).commit();
    assertEquals("no", config.getString("extra", "expr"));
    config.newTransaction().addContainer(MODIFIED_DEFAULT_EXPR_CONTAINER).commit();
    config.newTransaction().addContainer(EXTRA_CONTAINER).commit();
    assertEquals("si", config.getString("extra", "expr"));
    assertEquals("si", config.getString("extra", "expr"));
  }
}
