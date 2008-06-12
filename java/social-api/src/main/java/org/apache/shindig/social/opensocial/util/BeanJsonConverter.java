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
package org.apache.shindig.social.opensocial.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts pojos to json objects
 * TODO: Replace with standard library
 */
public class BeanJsonConverter {

  private static final Object[] EMPTY_OBJECT = {};
  private static final String EXCLUDED_FIELDS = "class";
  private static final Pattern GETTER = Pattern.compile("^get([a-zA-Z]+)$");
  private static final Pattern SETTER = Pattern.compile("^set([a-zA-Z]+)$");

  /**
   * Convert the passed in object to a json object
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
      for (Object item : (List) val) {
        list.put(translateObjectToJson(item));
      }
      return list;

    } else if (val instanceof Map) {
      JSONObject map = new JSONObject();
      Map originalMap = (Map) val;

      for (Object item : originalMap.keySet()) {
        map.put(item.toString(), translateObjectToJson(originalMap.get(item)));
      }
      return map;

    } else if (val instanceof String
        || val instanceof Boolean
        || val instanceof Integer
        || val instanceof Date
        || val instanceof Long
        || val instanceof Enum
        || val instanceof Float
        || val instanceof JSONObject
        || val instanceof JSONArray) {
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
      String errorMessage = "Could not encode the " + getter.method + " method.";
      try {
        Object val = getter.method.invoke(pojo, EMPTY_OBJECT);
        if (val != null) {
          toReturn.put(getter.fieldName, translateObjectToJson(val));
        }
      } catch(JSONException e) {
        throw new RuntimeException(errorMessage, e);
      } catch(IllegalAccessException e) {
        throw new RuntimeException(errorMessage, e);
      } catch(InvocationTargetException e) {
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
      if (fieldName.equalsIgnoreCase(EXCLUDED_FIELDS)) {
        continue;
      }
      availableGetters.add(new MethodPair(method, fieldName));
    }
    return availableGetters;
  }

  public <T> T convertToObject(String json, Class<T> className) {
    String errorMessage = "Could not convert " + json + " to " + className;

    try {
      T pojo = className.newInstance();
      return convertToObjectPrivate(json, pojo);
    } catch (JSONException e) {
      throw new RuntimeException(errorMessage, e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(errorMessage, e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(errorMessage, e);
    } catch (InstantiationException e) {
      throw new RuntimeException(errorMessage, e);
    }
  }

  private <T> T convertToObjectPrivate(String json, T pojo)
      throws JSONException, InvocationTargetException, IllegalAccessException,
      InstantiationException {

    if (pojo instanceof String) {
      pojo = (T) json; // This is a weird cast...

    } else if (pojo instanceof Map) {
      Type[] types = pojo.getClass().getTypeParameters();
      // TODO: Figure out how to get the actual generic type for the
      // second Map parameter. Right now we are hardcoding to String
      Class<?> mapValueClass = String.class;

      JSONObject jsonObject = new JSONObject(json);
      Iterator iterator = jsonObject.keys();
      while (iterator.hasNext()) {
        String key = (String) iterator.next();
        Object value = convertToObject(jsonObject.getString(key), mapValueClass);
        ((Map<String, Object>) pojo).put(key, value);
      }

    } else if (pojo instanceof List) {
      // TODO: process as a JSONArray
      throw new UnsupportedOperationException("We don't support lists as a " +
          "base json type yet. You can put it inside a pojo for now.");

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
      throws IllegalAccessException, InvocationTargetException, JSONException {

    Class<?> expectedType = method.getParameterTypes()[0];
    Object value = null;

    if (expectedType.equals(List.class)) {
      ParameterizedType genericListType
          = (ParameterizedType) method.getGenericParameterTypes()[0];
      Class<?> listElementClass
          = (Class) genericListType.getActualTypeArguments()[0];

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
      Class<?> valueClass = (Class) types[1];

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

    } else if (expectedType.getSuperclass().equals(Enum.class)) {
      String enumString = jsonObject.getString(fieldName);
      value = Enum.valueOf((Class<? extends Enum>) expectedType, enumString);

    } else if (expectedType.equals(String.class)) {
      value = jsonObject.getString(fieldName);

    } else if (expectedType.equals(Date.class)) {
      Long time = jsonObject.getLong(fieldName);
      value = new Date(time);

    } else if (expectedType.equals(Long.class)) {
      value = jsonObject.getLong(fieldName);

    } else if (expectedType.equals(Float.class)) {
      String stringFloat = jsonObject.getString(fieldName);
      value = new Float(stringFloat);
    }

    if (value != null) {
      method.invoke(pojo, value);
    }
  }
}