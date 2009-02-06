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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Base class for container configurations.
 */
public abstract class AbstractContainerConfig implements ContainerConfig {
  public String getString(String container, String property) {
    Object value = getProperty(container, property);
    if (value == null) {
      return null;
    }
    return value.toString();
  }

  public int getInt(String container, String property) {
    Object value = getProperty(container, property);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return 0;
  }

  public boolean getBool(String container, String property) {
    Object value = getProperty(container, property);
    if (value instanceof Boolean) {
      return ((Boolean) value).booleanValue();
    }
    return false;
  }

  public List<Object> getList(String container, String property) {
    Object value = getProperty(container, property);
    if (value instanceof List) {
      @SuppressWarnings("unchecked")
      List<Object> listValue = (List<Object>) value;
      return listValue;
    }
    return Collections.emptyList();
  }

  public Map<String, Object> getMap(String container, String property) {
    Object value = getProperty(container, property);
    if (value instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> mapValue = (Map<String, Object>) value;
      return mapValue;
    }
    return Collections.emptyMap();
  }

  public Collection<String> getContainers() {
    throw new UnsupportedOperationException();
  }

  public Map<String, Object> getProperties(String container) {
    throw new UnsupportedOperationException();
  }

  public abstract Object getProperty(String container, String name);
}
