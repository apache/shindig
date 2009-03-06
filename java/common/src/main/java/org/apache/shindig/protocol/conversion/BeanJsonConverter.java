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
package org.apache.shindig.protocol.conversion;

import org.apache.shindig.common.JsonProperty;
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.protocol.model.Enum;
import org.apache.shindig.protocol.model.EnumImpl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Injector;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Converts pojos to json objects.
 * TODO: Replace with standard library
 */
public class BeanJsonConverter implements BeanConverter {

  // Only compute the filtered setters once per-class
  private static final Map<Class<?>, Map<String, Method>> setters = new MapMaker().makeMap();

  private final Injector injector;

  @Inject
  public BeanJsonConverter(Injector injector) {
    this.injector = injector;
  }

  public String getContentType() {
    return "application/json";
  }

  /**
   * Convert the passed in object to a string.
   *
   * @param pojo The object to convert
   * @return An object whos toString method will return json
   */
  public String convertToString(final Object pojo) {
    return JsonSerializer.serialize(pojo);
  }

  public void append(Appendable buf, Object pojo) throws IOException {
    JsonSerializer.append(buf, pojo);
  }

  private static Map<String, Method> getSetters(Object pojo) {
    Class<?> clazz = pojo.getClass();

    Map<String, Method> methods = setters.get(clazz);
    if (methods != null) {
      return methods;
    }
    // Ensure consistent method ordering by using a linked hash map.
    methods = Maps.newHashMap();

    for (Method method : clazz.getMethods()) {
      String name = getPropertyName(method);
      if (name != null) {
        methods.put(name, method);
      }
    }

    setters.put(clazz, methods);
    return methods;
  }

  private static String getPropertyName(Method method) {
    JsonProperty property = method.getAnnotation(JsonProperty.class);
    if (property == null) {
      String name = method.getName();
      if (name.startsWith("set")) {
        return name.substring(3, 4).toLowerCase() + name.substring(4);
      }
      return null;
    } else {
      return property.value();
    }
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
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(errorMessage, e);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T convertToObject(String json, T pojo)
      throws JSONException, InvocationTargetException, IllegalAccessException,
      NoSuchFieldException {

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

    } else if (pojo instanceof Collection) {
      JSONArray array = new JSONArray(json);
      for (int i = 0; i < array.length(); i++) {
        ((Collection<Object>) pojo).add(array.get(i));
      }
    } else {
      JSONObject jsonObject = new JSONObject(json);
      for (Map.Entry<String, Method> setter : getSetters(pojo).entrySet()) {
        if (jsonObject.has(setter.getKey())) {
          callSetterWithValue(pojo, setter.getValue(), jsonObject, setter.getKey());
        }
      }
    }
    return pojo;
  }

  @SuppressWarnings("boxing")
  private <T> void callSetterWithValue(T pojo, Method method,
      JSONObject jsonObject, String fieldName)
      throws IllegalAccessException, InvocationTargetException, NoSuchFieldException,
      JSONException {

    Class<?> expectedType = method.getParameterTypes()[0];
    Object value = null;

    if (!jsonObject.has(fieldName)) {
      // Skip
    } else if (expectedType.equals(List.class)) {
      ParameterizedType genericListType
          = (ParameterizedType) method.getGenericParameterTypes()[0];
      Type type = genericListType.getActualTypeArguments()[0];
      Class<?> rawType;
      Class<?> listElementClass;
      if (type instanceof ParameterizedType) {
        listElementClass = (Class<?>)((ParameterizedType)type).getActualTypeArguments()[0];
        rawType = (Class<?>)((ParameterizedType)type).getRawType();
      } else {
        listElementClass = (Class<?>) type;
        rawType = listElementClass;
      }

      List<Object> list = Lists.newArrayList();
      JSONArray jsonArray = jsonObject.getJSONArray(fieldName);
      for (int i = 0; i < jsonArray.length(); i++) {
        if (org.apache.shindig.protocol.model.Enum.class
            .isAssignableFrom(rawType)) {
          list.add(convertEnum(listElementClass, jsonArray.getJSONObject(i)));
        } else {
          list.add(convertToObject(jsonArray.getString(i), listElementClass));
        }
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

      Iterator<?> keys = jsonMap.keys();
      while (keys.hasNext()) {
        String keyName = (String) keys.next();
        map.put(keyName, convertToObject(jsonMap.getString(keyName),
            valueClass));
      }

      value = map;

    } else if (org.apache.shindig.protocol.model.Enum.class
        .isAssignableFrom(expectedType)) {
      // TODO Need to stop using Enum as a class name :(
      value = convertEnum(
          (Class<?>)((ParameterizedType) method.getGenericParameterTypes()[0]).
              getActualTypeArguments()[0],
          jsonObject.getJSONObject(fieldName));
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
    } else if (expectedType.equals(Long.class) || expectedType.equals(Long.TYPE)) {
      value = jsonObject.getLong(fieldName);
    } else if (expectedType.equals(Integer.class) || expectedType.equals(Integer.TYPE)) {
      value = jsonObject.getInt(fieldName);
    } else if (expectedType.equals(Boolean.class) || expectedType.equals(Boolean.TYPE)) {
      value = jsonObject.getBoolean(fieldName);
    } else if (expectedType.equals(Float.class) || expectedType.equals(Float.TYPE)) {
      value = ((Double) jsonObject.getDouble(fieldName)).floatValue();
    } else if (expectedType.equals(Double.class) || expectedType.equals(Double.TYPE)) {
      value = jsonObject.getDouble(fieldName);
    } else {
      // Assume its an injected type
      value = convertToObject(jsonObject.getJSONObject(fieldName).toString(), expectedType);
    }

    if (value != null) {
      method.invoke(pojo, value);
    }
  }

  private Object convertEnum(Class<?> enumKeyType, JSONObject jsonEnum)
      throws JSONException, IllegalAccessException, NoSuchFieldException {
    // TODO This isnt injector friendly but perhaps implementors dont need it. If they do a
    // refactoring of the Enum handling in general is needed.
    Object value;
    if (jsonEnum.has(Enum.Field.VALUE.toString())) {
      Enum.EnumKey enumKey = (Enum.EnumKey) enumKeyType
          .getField(jsonEnum.getString(Enum.Field.VALUE.toString())).get(null);
      String displayValue = null;
      if (jsonEnum.has(Enum.Field.DISPLAY_VALUE.toString())) {
        displayValue = jsonEnum.getString(Enum.Field.DISPLAY_VALUE.toString());
      }
      value = new EnumImpl<Enum.EnumKey>(enumKey,displayValue);
    } else {
      value = new EnumImpl<Enum.EnumKey>(null,
          jsonEnum.getString(Enum.Field.DISPLAY_VALUE.toString()));
    }
    return value;
  }
}
