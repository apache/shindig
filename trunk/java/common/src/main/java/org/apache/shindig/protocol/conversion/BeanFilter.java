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
package org.apache.shindig.protocol.conversion;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Filter content of a bean according to fields list.
 * Fields list should be in lower case. And support sub objects using dot notation.
 * For example to get only the "name" field of the object in the "view" field,
 * specify "view.name" (and also specify "view" to get the view itself).
 * Use "*" to get all fields, or "view.*" all sub fields of view (see tests).
 * Note that specifying "view" does NOT imply "view.*" and that
 * specifying "view.*" require specifying "view" in order to get the view itself.
 * (Note that the processBeanFilter resolve the last limitation)
 *
 * Note this code create a new object for each filtered object.
 * Filtering can be done also using cglib.InterfaceMaker and reflect.Proxy.makeProxyInstance
 * That results with an object that have same finger print as source, but cannot be cast to it.
 *
 * @since 2.0.0
 */
public class BeanFilter {

  public static final String ALL_FIELDS = "*";
  public static final String DELIMITER = ".";

  /** Annotation for required field that should not be filtered */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Unfiltered {}

  /**
   * Create a proxy object that filter object fields according to set of fields.
   * If a field is not specified in the set, the get method will return null.
   * (Primitive returned type cannot be filtered)
   * The filter is done recursively on sub items.
   * @param data the object to filter
   * @param fields list of fields to pass through.
   */
  public Object createFilteredBean(Object data, Set<String> fields) {
    return createFilteredBean(data, fields, "");
  }

  @SuppressWarnings("unchecked")
  private Object createFilteredBean(Object data, Set<String> fields, String fieldName) {
    // For null, atomic object or for all fields just return original.
    if (data == null || fields == null
        || BeanDelegator.PRIMITIVE_TYPE_CLASSES.contains(data.getClass())
        || fields.contains(ALL_FIELDS)) {
      return data;
    }

    // For map, generate a new map with filtered objects
    if (data instanceof Map<? ,?>) {
      Map<Object, Object> oldMap = (Map<Object, Object>) data;
      Map<Object, Object> newMap = Maps.newHashMapWithExpectedSize(oldMap.size());
      for (Map.Entry<Object, Object> entry : oldMap.entrySet()) {
        newMap.put(entry.getKey(), createFilteredBean(entry.getValue(), fields, fieldName));
      }
      return newMap;
    }

    // For list, generate a new list of filtered objects
    if (data instanceof List<?>) {
      List<Object> oldList = (List<Object>) data;
      List<Object> newList = Lists.newArrayListWithCapacity(oldList.size());
      for (Object entry : oldList) {
        newList.add(createFilteredBean(entry, fields, fieldName));
      }
      return newList;
    }

    // Create a new intercepted object:
    return Proxy.newProxyInstance( data.getClass().getClassLoader(),
        data.getClass().getInterfaces(), new FilterInvocationHandler(data, fields, fieldName));
  }

  /**
   * Invocation handler to filter fields. It return null to fields that are not in the list.
   * It invokes method on original object. It does not filter primitive types.
   * And it create bean filter proxy for return objects
   */
  private class FilterInvocationHandler implements InvocationHandler {
    private final String prefix;
    private final Set<String> fields;
    private final Object origData;

    FilterInvocationHandler(Object origData, Set<String> fields, String fieldName) {
      this.fields = fields;
      this.prefix = Strings.isNullOrEmpty(fieldName) ? "" : fieldName + DELIMITER;
      this.origData = origData;
    }

    public Object invoke(Object data, Method method, Object[] args) {
      String fieldName = null;
      Object result;
      if (method.getName().startsWith("get")
          // Do not filter out primitive types, it will result in NPE
          && !method.getReturnType().isPrimitive()) {
        // Look for Required annotation
        boolean required = (method.getAnnotation(Unfiltered.class) != null);
        fieldName = prefix + method.getName().substring(3).toLowerCase();
        if (!required && !fields.contains(fieldName)) {
          return null;
        }
      }
      try {
        result = method.invoke(origData, args);
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      }
      if (result != null && fieldName != null
          // if the request ask for all fields, we don't need to filter them
          && !fields.contains(fieldName + DELIMITER + ALL_FIELDS)) {
        return createFilteredBean(result, fields, fieldName);
        // TODO: Consider improving the above by saving the filtered bean in a local map for reuse
        // for current use the get is called once, so it would actually create overhead
      }
      return result;
    }
  }

  public Set<String> processBeanFields(Collection<String> fields) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (String field : fields) {
      builder.add(field.toLowerCase());
      while (field.contains(DELIMITER)) {
        field = field.substring(0, field.lastIndexOf(DELIMITER));
        builder.add(field.toLowerCase());
      }
    }
    return builder.build();
  }

  /**
   * Provide list of all fields for a specific bean
   * @param bean the class to list fields for
   * @param depth maximum depth of recursive (mainly for infinite loop protection)
   */
  public List<String> getBeanFields(Class<?> bean, int depth) {
    List<String> fields = Lists.newLinkedList();
    for (Method method : bean.getMethods()) {
      if (method.getName().startsWith("get")) {
        String fieldName = method.getName().substring(3);
        fields.add(fieldName);
        Class<?> returnType = method.getReturnType();
        // Get the type of list:
        if (List.class.isAssignableFrom(returnType)) {
          ParameterizedType aType = (ParameterizedType) method.getGenericReturnType();
          Type[] parameterArgTypes = aType.getActualTypeArguments();
          if (parameterArgTypes.length > 0) {
            returnType = (Class<?>) parameterArgTypes[0];
          } else {
            returnType = null;
          }
        }
        // Get the type of map value
        if (Map.class.isAssignableFrom(returnType)) {
          ParameterizedType aType = (ParameterizedType) method.getGenericReturnType();
          Type[] parameterArgTypes = aType.getActualTypeArguments();
          if (parameterArgTypes.length > 1) {
            returnType = (Class<?>) parameterArgTypes[1];
          } else {
            returnType = null;
          }
        }
        // Get member fields and append fields using dot notation
        if (depth > 1 && returnType != null && !returnType.isPrimitive()
            && !returnType.isEnum()
            && !BeanDelegator.PRIMITIVE_TYPE_CLASSES.contains(returnType)) {
          List<String> subFields = getBeanFields(returnType, depth - 1);
          for (String field : subFields) {
            fields.add(fieldName + DELIMITER + field);
          }
        }
      }
    }
    return fields;
  }

}
