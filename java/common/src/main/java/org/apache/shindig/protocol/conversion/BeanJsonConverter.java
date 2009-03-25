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

import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Injector;

import org.apache.shindig.common.JsonProperty;
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.protocol.ContentTypes;
import org.apache.shindig.protocol.model.Enum;
import org.apache.shindig.protocol.model.EnumImpl;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts between JSON and java objects.
 *
 * TODO: Eliminate BeanConverter interface.
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
    return ContentTypes.OUTPUT_JSON_CONTENT_TYPE;
  }

  /**
   * Convert the passed in object to a string.
   *
   * @param pojo The object to convert
   * @return An object whose toString method will return json
   */
  public String convertToString(final Object pojo) {
    return JsonSerializer.serialize(pojo);
  }

  public void append(Appendable buf, Object pojo) throws IOException {
    JsonSerializer.append(buf, pojo);
  }

  private static Map<String, Method> getSetters(Class<?> type) {
    Map<String, Method> methods = setters.get(type);

    if (methods != null) {
      return methods;
    }

    methods = new HashMap<String, Method>();

    for (Method method : type.getMethods()) {
      String name = getPropertyName(method);
      if (name != null) {
        methods.put(name, method);
      }
    }

    setters.put(type, methods);
    return methods;
  }

  private static String getPropertyName(Method setter) {
    JsonProperty property = setter.getAnnotation(JsonProperty.class);
    if (property == null) {
      String name = setter.getName();
      if (name.startsWith("set") && !Modifier.isStatic(setter.getModifiers())) {
        return name.substring(3, 4).toLowerCase() + name.substring(4);
      }
      return null;
    } else {
      return property.value();
    }
  }

  public <T> T convertToObject(String string, Class<T> clazz) {
    return (T)convertToObject(string, (Type) clazz);
  }

  @SuppressWarnings("unchecked")
  public <T> T convertToObject(String json, Type type) {
    try {
      return (T) convertToObject(new JSONObject(json), type);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  public Object convertToObject(Object value, Type type) {
    if (type == null || type.equals(Object.class)) {
      // Use the source type instead.
      if (value instanceof JSONObject) {
        return convertToMap((JSONObject) value, null);
      } else if (value instanceof JSONArray) {
        return convertToCollection((JSONArray) value, new ArrayList<Object>(), null);
      }
      return value;
    } else if (type instanceof ParameterizedType) {
      return convertGeneric(value, (ParameterizedType) type);
    } else if (type.equals(String.class)) {
      return String.valueOf(value);
    } else if (type.equals(Boolean.class) || type.equals(Boolean.TYPE)) {
      return value instanceof String ? Boolean.valueOf((String) value) : Boolean.TRUE.equals(value);
    } else if (type.equals(Integer.class) || type.equals(Integer.TYPE)) {
      return value instanceof String ? Integer.valueOf((String) value) : ((Number) value).intValue();
    } else if (type.equals(Long.class) || type.equals(Long.TYPE)) {
      return value instanceof String ? Long.valueOf((String) value) : ((Number) value).longValue();
    } else if (type.equals(Double.class) || type.equals(Double.TYPE)) {
      return value instanceof String ? Double.valueOf((String) value) : ((Number) value).doubleValue();
    } else if (type.equals(Float.class) || type.equals(Float.TYPE)) {
      return value instanceof String ? Float.valueOf((String) value) : ((Number) value).floatValue();
    } else if (type.equals(Date.class)) {
      return new DateTime(String.valueOf(value)).toDate();
    } else if (type.equals(Uri.class)) {
      return Uri.parse(String.valueOf(value));
    } else if (type.equals(Map.class)) {
      return convertToMap((JSONObject) value, null);
    } else if (type.equals(List.class) || type.equals(Collection.class)) {
      return convertToCollection((JSONArray) value, new ArrayList<Object>(), null);
    } else if (type.equals(Set.class)) {
      return convertToCollection((JSONArray) value, new HashSet<Object>(), null);
    }

    Class<?> clazz = (Class<?>) type;

    if (clazz.isEnum()) {
      return convertToEnum((String) value, clazz);
    }

    return convertToClass((JSONObject) value, clazz);
  }

  private Object convertGeneric(Object value, ParameterizedType type) {
    Type[] typeArgs = type.getActualTypeArguments();
    Class<?> clazz = (Class<?>) type.getRawType();

    if (Set.class.isAssignableFrom(clazz)) {
      return convertToCollection((JSONArray) value, new HashSet<Object>(), typeArgs[0]);
    } else if (Collection.class.isAssignableFrom(clazz)) {
      return convertToCollection((JSONArray) value, new ArrayList<Object>(), typeArgs[0]);
    } else if (Map.class.isAssignableFrom(clazz)) {
      return convertToMap((JSONObject) value, typeArgs[1]);
    } else if (org.apache.shindig.protocol.model.Enum.class.isAssignableFrom(clazz)) {
      // Special case for opensocial Enum objects. These really need to be refactored to not require
      // this handling.
      return convertToOsEnum((JSONObject) value, (Class<?>) typeArgs[0]);
    }
    return convertToClass((JSONObject) value, clazz);
  }

  private Enum<Enum.EnumKey> convertToOsEnum(JSONObject json, Class<?> enumKeyType) {
    Enum<Enum.EnumKey> value;
    String val = Enum.Field.VALUE.toString();
    String display = Enum.Field.DISPLAY_VALUE.toString();
    if (json.has(val)) {
      Enum.EnumKey enumKey;
      try {
        enumKey = (Enum.EnumKey) enumKeyType.getField(json.optString(val)).get(null);
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
      } catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
      String displayValue = null;
      if (json.has(display)) {
        displayValue = json.optString(display);
      }
      value = new EnumImpl<Enum.EnumKey>(enumKey,displayValue);
    } else {
      value = new EnumImpl<Enum.EnumKey>(null, json.optString(display));
    }
    return value;
  }

  private Object convertToEnum(String value, Class<?> type) {
    for (Object o : type.getEnumConstants()) {
      if (o.toString().equals(value)) {
        return o;
      }
    }
    throw new IllegalArgumentException("No enum value " + value + " in " + type.getName());
  }

  private Map<String, Object> convertToMap(JSONObject in, Type type) {
    Map<String, Object> out = new HashMap<String, Object>(in.length(), 1);
    for (String name : JSONObject.getNames(in)) {
      out.put(name, convertToObject(in.opt(name), type));
    }
    return out;
  }

  private Collection<Object> convertToCollection(JSONArray in, Collection<Object> out, Type type) {
    for (int i = 0, j = in.length(); i < j; ++i) {
      out.add(convertToObject(in.opt(i), type));
    }
    return out;
  }

  private Object convertToClass(JSONObject in, Class<?> type) {
    Object out = injector.getInstance(type);
    for (Map.Entry<String, Method> entry : getSetters(out.getClass()).entrySet()) {
      Object value = in.opt(entry.getKey());
      if (value != null) {
        Method method = entry.getValue();
        try {
          method.invoke(out, convertToObject(value, method.getGenericParameterTypes()[0]));
        } catch (IllegalArgumentException e) {
          throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return out;
  }
}
