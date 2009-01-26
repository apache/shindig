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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.expressions.ExpressionContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.junit.Test;

import java.util.Map;

public class ContainerConfigExpressionContextTest {
  private static final String CHILD_CONTAINER = "child";
  private static final String PARENT_CONTAINER = "parent";

  private final FakeContainerConfig config = new FakeContainerConfig();
  private final ExpressionContext context
      = new ContainerConfigExpressionContext(CHILD_CONTAINER, config);

  @Test
  public void testGetProperty() {
    config.data.put(CHILD_CONTAINER, ImmutableMap.of("foo", "bar"));

    assertEquals("bar", context.getVariable("foo").toString());
  }

  @Test
  public void testGetParentProperty() {
    Map<String, String> data = ImmutableMap.of("foo", "bar");
    config.data.put(PARENT_CONTAINER, data);
    config.data.put(CHILD_CONTAINER,
        ImmutableMap.of(JsonContainerConfig.PARENT_KEY, PARENT_CONTAINER));

    Object obj = context.getVariable(JsonContainerConfig.PARENT_KEY);

    assertTrue("Did not create a nested context.", obj instanceof ExpressionContext);

    assertEquals("bar", ((ExpressionContext) obj).getVariable("foo"));
  }

  private static class FakeContainerConfig extends AbstractContainerConfig {
    protected final Map<String, Map<String, String>> data = Maps.newHashMap();

    @Override
    public Object getProperty(String container, String name) {
      return data.get(container).get(name);
    }

  }
}
