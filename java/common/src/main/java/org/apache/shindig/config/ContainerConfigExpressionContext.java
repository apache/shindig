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

import org.apache.shindig.expressions.ExpressionContext;

/**
 * An expression context that allows a container configuration to be self-referencing.
 *
 * Also supports a magical 'parent' parameter that automatically evaluates to the parent of the
 * current container, allowing expressions such as:
 *
 * ${parent.parent.foo} to work for derived configurations.
 */
public class ContainerConfigExpressionContext implements ExpressionContext {
  private final String currentContainer;
  private final ContainerConfig config;

  public ContainerConfigExpressionContext(String currentContainer, ContainerConfig config) {
    this.currentContainer = currentContainer;
    this.config = config;
  }

  public Object getVariable(String name) {
    if (name.equals(JsonContainerConfig.PARENT_KEY)) {
      String parent = config.getString(currentContainer, name);
      return new ContainerConfigExpressionContext(parent, config);
    }

    return config.getProperty(currentContainer, name);
  }

}
