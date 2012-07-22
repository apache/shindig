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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;

import org.apache.shindig.expressions.Expressions;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ValueExpression;

/**
 * Represents a container configuration that uses expressions in values.
 *
 * We use a cascading model, so you only have to specify attributes in your
 * config that you actually want to change.
 *
 * String values may use expressions. The variable context defaults to the
 * 'current' container, but parent values may be accessed through the special
 * "parent" property.
 *
 * get* can take either a simple property name (foo), or an EL expression
 * (${foo.bar}).
 */
@Singleton
public class ExpressionContainerConfig extends BasicContainerConfig {

  protected Map<String, Map<String, Object>> rawConfig;
  private final Expressions expressions;

  public ExpressionContainerConfig(Expressions expressions) {
    this.expressions = expressions;
    this.rawConfig = Maps.newHashMap();
  }

  /**
   * Creates a new transaction to create, modify or remove containers.
   *
   * @return The new transaction object.
   */
  @Override
  public Transaction newTransaction() {
    return new ExpressionTransaction();
  }

  @Override
  public Object getProperty(String container, String property) {
    if (property.startsWith("${")) {
      // An expression!
      try {
        ValueExpression expression = expressions.parse(property, Object.class);
        return expression.getValue(createExpressionContext(container));
      } catch (ELException e) {
        return null;
      }
    }

    return super.getProperty(container, property);
  }

  protected Expressions getExpressions() {
    return expressions;
  }

  protected ELContext createExpressionContext(String container) {
    return getExpressions().newELContext(new ContainerConfigELResolver(this, container));
  }

  public class ExpressionTransaction extends BasicTransaction {

    @Override
    protected BasicContainerConfig getTemporaryConfig(boolean copyValues) {
      ExpressionContainerConfig tmp = new ExpressionContainerConfig(getExpressions());
      if (copyValues) {
        tmp.rawConfig = deepCopyConfig(rawConfig);
        tmp.config = deepCopyConfig(config);
      }
      return tmp;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void changeContainersInConfig(BasicContainerConfig config,
        Map<String, Map<String, Object>> setContainers, Set<String> removeContainers)
        throws ContainerConfigException {
      ExpressionContainerConfig tmp = (ExpressionContainerConfig) config;
      tmp.rawConfig.putAll(setContainers);
      for (String container : removeContainers) {
        tmp.rawConfig.remove(container);
      }
      tmp.config.clear();
      for (String container : tmp.rawConfig.keySet()) {
        Map<String, Object> merged = mergeParents(container, tmp.rawConfig);
        tmp.rawConfig.put(container, merged);
        Map<String, Object> value =
            (Map<String, Object>) parseAll(merged, tmp.createExpressionContext(container));
        tmp.config.put(container, value);
      }
      for (String container : tmp.config.keySet()) {
        Map<String, Object> value = (Map<String, Object>) evaluateAll(tmp.config.get(container));
        tmp.config.put(container, value);
      }
    }

    @Override
    protected void setNewConfig(BasicContainerConfig newConfig) {
      ExpressionContainerConfig tmp = (ExpressionContainerConfig) newConfig;
      rawConfig = tmp.rawConfig;
      config = tmp.config;
    }

    private Object parseAll(Object value, ELContext context) {
      if (value instanceof String) {
        return new DynamicConfigProperty((String) value, expressions, context);
      } else if (value instanceof Map<?, ?>) {
        Map<?, ?> mapValue = (Map<?, ?>) value;
        Map<Object, Object> newMap = Maps.newHashMap();
        for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
          newMap.put(entry.getKey(), parseAll(entry.getValue(), context));
        }
        return Collections.unmodifiableMap(newMap);
      } else if (value instanceof List<?>) {
        List<Object> newList = Lists.newArrayList();
        for (Object entry : (List<?>) value) {
          newList.add(parseAll(entry, context));
        }
        return Collections.unmodifiableList(newList);
      } else {
        return value;
      }
    }

    private Object evaluateAll(Object value) {
      if (value instanceof CharSequence) {
        return value.toString();
      } else if (value instanceof Map<?, ?>) {
        Map<?, ?> mapValue = (Map<?, ?>) value;
        Map<Object, Object> newMap = Maps.newHashMap();
        for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
          newMap.put(entry.getKey(), evaluateAll(entry.getValue()));
        }
        return Collections.unmodifiableMap(newMap);
      } else if (value instanceof List<?>) {
        List<Object> newList = Lists.newArrayList();
        for (Object entry : (List<?>) value) {
          newList.add(evaluateAll(entry));
        }
        return Collections.unmodifiableList(newList);
      } else {
        return value;
      }
    }
  }
}
