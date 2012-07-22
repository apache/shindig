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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.shindig.expressions.Expressions;
import org.json.JSONObject;

import java.util.Iterator;

// Temporary replacement of javax.annotation.Nullable
import org.apache.shindig.common.Nullable;

/**
 * Represents a container configuration using JSON notation.
 *
 * See config/container.js for an example configuration.
 *
 * We use a cascading model, so you only have to specify attributes in
 * your config that you actually want to change.
 *
 * String values may use expressions. The variable context defaults to the 'current' container,
 * but parent values may be accessed through the special "parent" property.
 */
@Singleton
public class JsonContainerConfig extends ExpressionContainerConfig {

  // Used by tests
  public JsonContainerConfig(String containers, Expressions expressions) throws ContainerConfigException {
    this(containers, "localhost", "8080", "",expressions);
  }
  /**
   * Creates a new configuration from files.
   * @throws ContainerConfigException
   */
  @Inject
  public JsonContainerConfig(@Named("shindig.containers.default") String containers,
                             @Nullable @Named("shindig.host") String host,
                             @Nullable @Named("shindig.port") String port,
                             @Nullable @Named("shindig.contextroot") String contextRoot,
                             Expressions expressions)
      throws ContainerConfigException {
    super(expressions);
    JsonContainerConfigLoader.getTransactionFromFile(containers, host, port, contextRoot, this).commit();
  }

  /**
   * Creates a new configuration from a JSON Object, for use in testing.
   * @throws ContainerConfigException
   */
  public JsonContainerConfig(JSONObject json, Expressions expressions) throws ContainerConfigException {
    super(expressions);
    Transaction transaction = newTransaction();
    Iterator<?> keys = json.keys();
    while (keys.hasNext()) {
      JSONObject optJSONObject = json.optJSONObject((String) keys.next());
      if (optJSONObject != null) {
        transaction.addContainer(JsonContainerConfigLoader.parseJsonContainer(optJSONObject));
      }
    }
    transaction.commit();
  }
}
