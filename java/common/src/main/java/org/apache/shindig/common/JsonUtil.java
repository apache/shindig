/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.common;

import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;

/**
 * JSON utilities that are not specific to either serialization or conversion.
 */
public final class JsonUtil {
  private JsonUtil() {}

  private static final Set<String> EXCLUDE_METHODS
      = ImmutableSet.of("getClass", "getDeclaringClass");

  private static final Map<Class<?>, Map<String, Method>> GETTERS = new MapMaker().makeMap();

  /**
   * Gets a property of an Object.  Will return a property value if
   * serializing the value would produce a JSON object containing that 
   * property, otherwise returns null.
   */
  public static Object getProperty(Object value, String propertyName) {
    Preconditions.checkNotNull(value);
    Preconditions.checkNotNull(propertyName);

    if (value instanceof JSONObject) {
      return ((JSONObject) value).opt(propertyName);
    } else if (value instanceof Map<?, ?>) {
      return ((Map<?, ?>) value).get(propertyName);
    } else {
      // Try getter conversion
      Method method = getGetters(value).get(propertyName);
      if (method != null) {
        try {
          return method.invoke(value);
        } catch (IllegalArgumentException e) {
          // Shouldn't be possible.
          throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
          // Bad class.
          throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
          // Bad class.
          throw new RuntimeException(e);
        }
      }
    }
    
    return null;
  }
  
  static Map<String, Method> getGetters(Object pojo) {
    Class<?> clazz = pojo.getClass();

    Map<String, Method> methods = GETTERS.get(clazz);
    if (methods != null) {
      return methods;
    }
    // Ensure consistent method ordering by using a linked hash map.
    methods = Maps.newHashMap();

    for (Method method : clazz.getMethods()) {
      if (method.getParameterTypes().length == 0) {
        String name = getPropertyName(method);
        if (name != null) {
          methods.put(name, method);
        }
      }
    }

    GETTERS.put(clazz, methods);
    return methods;
  }

  private static String getPropertyName(Method method) {
    JsonProperty property = method.getAnnotation(JsonProperty.class);
    if (property == null) {
      String name = method.getName();
      if (name.startsWith("get") && (!EXCLUDE_METHODS.contains(name))) {
        return name.substring(3, 4).toLowerCase() + name.substring(4);
      }
      return null;
    } else {
      return property.value();
    }
  }
}
