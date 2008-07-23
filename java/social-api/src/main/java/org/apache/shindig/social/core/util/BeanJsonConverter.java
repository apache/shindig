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
package org.apache.shindig.social.core.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;

import org.apache.shindig.social.core.model.EnumImpl;
import org.apache.shindig.social.opensocial.model.Enum;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts pojos to json objects.
 * TODO: Replace with standard library
 */
public class BeanJsonConverter implements BeanConverter {

  private static final Object[] EMPTY_OBJECT = {};
  private static final Set<String> EXCLUDED_FIELDS = Sets.newHashSet("class", "declaringclass");
  private static final Pattern GETTER = Pattern.compile("^get([a-zA-Z]+)$");
  private static final Pattern SETTER = Pattern.compile("^set([a-zA-Z]+)$");
  private Injector injector;

  @Inject
  public BeanJsonConverter(Injector injector) {
    this.injector = injector;
  }

  /**
   * Convert the passed in object to a string.
   *
   * @param pojo The object to convert
   * @return An object whos toString method will return json
   */
  public String convertToString(Object pojo) {
    return convertToJson(pojo).toString();
  }

  /**
   * Convert the passed in object to a json object.
   *
   * @param pojo The object to convert
   * @return An object whos toString method will return json
   */
  public Object convertToJson(Object pojo) {
    try {
      return translateObjectToJson(pojo);
    } catch (JSONException e) {
      throw new RuntimeException("Could not translate " + pojo + " to json", e);
    }
  }

  private Object translateObjectToJson(Object val) throws JSONException {
    if (val instanceof Object[]) {
      JSONArray array = new JSONArray();
      for (Object asd : (Object[]) val) {
        array.put(translateObjectToJson(asd));
      }
      return array;

    } else if (val instanceof List) {
      JSONArray list = new JSONArray();
      for (Object item : (List<?>) val) {
        list.put(translateObjectToJson(item));
      }
      return list;

    } else if (val instanceof Map) {
      JSONObject map = new JSONObject();
      Map<?, ?> originalMap = (Map<?,?>) val;

      for (Entry<?, ?> item : originalMap.entrySet()) {
        map.put(item.getKey().toString(), translateObjectToJson(item.getValue()));
      }
      return map;

    } else if (val != null && val.getClass().isEnum()) {
      return val.toString();
    } else if (val instanceof String
        || val instanceof Boolean
        || val instanceof Integer
        || val instanceof Date
        || val instanceof Long
        || val instanceof Float
        || val instanceof JSONObject
        || val instanceof JSONArray
        || val == null) {
      return val;
    }

    return convertMethodsToJson(val);
  }

  /**
   * Convert the object to {@link JSONObject} reading Pojo properties
   *
   * @param pojo The object to convert
   * @return A JSONObject representing this pojo
   */
  private JSONObject convertMethodsToJson(Object pojo) {
    List<MethodPair> availableGetters = getMatchingMethods(pojo, GETTER);

    JSONObject toReturn = new JSONObject();
    for (MethodPair getter : availableGetters) {
      String errorMessage = "Could not encode the " + getter.method + " method on "
        + pojo.getClass().getName();
      try {
        Object val = getter.method.invoke(pojo, EMPTY_OBJECT);
        if (val != null) {
          toReturn.put(getter.fieldName, translateObjectToJson(val));
        }
      } catch (JSONException e) {
        throw new RuntimeException(errorMessage, e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(errorMessage, e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(errorMessage, e);
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(errorMessage, e);
      }
    }
    return toReturn;
  }

  private static class MethodPair {
    public Method method;
    public String fieldName;

    private MethodPair(Method method, String fieldName) {
      this.method = method;
      this.fieldName = fieldName;
    }
  }

  private List<MethodPair> getMatchingMethods(Object pojo, Pattern pattern) {
    List<MethodPair> availableGetters = Lists.newArrayList();

    Method[] methods = pojo.getClass().getMethods();
    for (Method method : methods) {
      Matcher matcher = pattern.matcher(method.getName());
      if (!matcher.matches()) {
        continue;
      }

      String name = matcher.group();
      String fieldName = name.substring(3, 4).toLowerCase() + name.substring(4);
      if (EXCLUDED_FIELDS.contains(fieldName.toLowerCase())) {
        continue;
      }
      availableGetters.add(new MethodPair(method, fieldName));
    }
    return availableGetters;
  }

  public <T> T convertToObject(String json, Class<T> className) {
    String errorMessage = "Could not convert " + json + " to " + className;

    try {
      T pojo = injector.getInstance(className);
      return convertToObject(json, pojo);
    } catch (JSONException e) {
      throw new RuntimeException(errorMessage, e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(errorMessage, e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(errorMessage, e);
    } catch (InstantiationException e) {
      throw new RuntimeException(errorMessage, e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(errorMessage, e);
    }
  }

  private <T> T convertToObject(String json, T pojo)
      throws JSONException, InvocationTargetException, IllegalAccessException,
      InstantiationException, NoSuchFieldException {

    if (pojo instanceof String) {
      pojo = (T) json; // This is a weird cast...

    } else if (pojo instanceof Map) {
      // TODO: Figure out how to get the actual generic type for the
      // second Map parameter. Right now we are hardcoding to String
      Class<?> mapValueClass = String.class;

      JSONObject jsonObject = new JSONObject(json);
      Iterator<?> iterator = jsonObject.keys();
      while (iterator.hasNext()) {
        String key = (String) iterator.next();
        Object value = convertToObject(jsonObject.getString(key), mapValueClass);
        ((Map<String, Object>) pojo).put(key, value);
      }

    } else if (pojo instanceof List) {
      // TODO: process as a JSONArray
      throw new UnsupportedOperationException("We don't support lists as a "
          + "base json type yet. You can put it inside a pojo for now.");

    } else {
      JSONObject jsonObject = new JSONObject(json);
      List<MethodPair> methods = getMatchingMethods(pojo, SETTER);
      for (MethodPair setter : methods) {
        if (jsonObject.has(setter.fieldName)) {
          callSetterWithValue(pojo, setter.method, jsonObject, setter.fieldName);
        }
      }
    }
    return pojo;
  }

  private <T> void callSetterWithValue(T pojo, Method method,
      JSONObject jsonObject, String fieldName)
      throws IllegalAccessException, InvocationTargetException, NoSuchFieldException,
      JSONException {

    Class<?> expectedType = method.getParameterTypes()[0];
    Object value = null;

    if (expectedType.equals(List.class)) {
      ParameterizedType genericListType
          = (ParameterizedType) method.getGenericParameterTypes()[0];
      Class<?> listElementClass
          = (Class<?>) genericListType.getActualTypeArguments()[0];

      List<Object> list = Lists.newArrayList();
      JSONArray jsonArray = jsonObject.getJSONArray(fieldName);
      for (int i = 0; i < jsonArray.length(); i++) {
        list.add(convertToObject(jsonArray.getString(i), listElementClass));
      }

      value = list;

    } else if (expectedType.equals(Map.class)) {
      ParameterizedType genericListType
          = (ParameterizedType) method.getGenericParameterTypes()[0];
      Type[] types = genericListType.getActualTypeArguments();
      Class<?> valueClass = (Class<?>) types[1];

      // We only support keys being typed as Strings.
      // Nothing else really makes sense in json.
      Map<String, Object> map = Maps.newHashMap();
      JSONObject jsonMap = jsonObject.getJSONObject(fieldName);

      Iterator keys = jsonMap.keys();
      while (keys.hasNext()) {
        String keyName = (String) keys.next();
        map.put(keyName, convertToObject(jsonMap.getString(keyName),
            valueClass));
      }

      value = map;

    } else if (org.apache.shindig.social.opensocial.model.Enum.class
        .isAssignableFrom(expectedType)) {
      // TODO Need to stop using Enum as a class name :(
      Class<?> enumType = (Class<?>) ((ParameterizedType) method.getGenericParameterTypes()[0])
          .getActualTypeArguments()[0];
      // TODO This isnt injector friendly but perhaps implementors dont need it. If they do a
      // refactoring of the Enum handling in general is needed.
      if (jsonObject.has(fieldName)) {
        JSONObject jsonEnum = jsonObject.getJSONObject(fieldName);
        if (jsonEnum.has(Enum.Field.KEY.toString())) {
          Enum.EnumKey enumKey = (Enum.EnumKey) enumType
              .getField(jsonEnum.getString(Enum.Field.KEY.toString())).get(null);
          value = new EnumImpl<Enum.EnumKey>(enumKey,
              jsonEnum.getString(Enum.Field.DISPLAY_VALUE.toString()));
        } else {
          value = new EnumImpl<Enum.EnumKey>(null,
              jsonEnum.getString(Enum.Field.DISPLAY_VALUE.toString()));
        }
      }
    } else if (expectedType.isEnum()) {
      if (jsonObject.has(fieldName)) {
        for (Object v : expectedType.getEnumConstants()) {
          if (v.toString().equals(jsonObject.getString(fieldName))) {
            value = v;
            break;
          }
        }
        if (value == null) {
          throw new IllegalArgumentException(
              "No enum value  '" + jsonObject.getString(fieldName)
                  + "' in " + expectedType.getName());
        }
      }
    } else if (expectedType.equals(String.class)) {
      value = jsonObject.getString(fieldName);
    } else if (expectedType.equals(Date.class)) {
      // Use JODA ISO parsing for the conversion
      value = new DateTime(jsonObject.getString(fieldName)).toDate();
    } else if (expectedType.equals(Long.class)) {
      value = jsonObject.getLong(fieldName);
    } else if (expectedType.equals(Integer.class)) {
      value = jsonObject.getInt(fieldName);
    } else if (expectedType.equals(Boolean.class)) {
      value = jsonObject.getBoolean(fieldName);
    } else if (expectedType.equals(Float.class)) {
      String stringFloat = jsonObject.getString(fieldName);
      value = new Float(stringFloat);
    } else {
      // Assume its an injected type
      value = convertToObject(jsonObject.getJSONObject(fieldName).toString(), expectedType);
    }

    if (value != null) {
      method.invoke(pojo, value);
    }
  }
}
