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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.shindig.common.JsonProperty;
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.protocol.ContentTypes;
import org.apache.shindig.protocol.model.Enum;
import org.apache.shindig.protocol.model.EnumImpl;
import org.apache.shindig.protocol.model.ExtendableBean;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Converts between JSON and java objects.
 *
 * TODO: Eliminate BeanConverter interface.
 */
public class BeanJsonConverter implements BeanConverter {

  // Only compute the filtered SETTERS once per-class
  private static final LoadingCache<Class<?>, Map<String, Method>> SETTERS = CacheBuilder
      .newBuilder()
      .build(new CacheLoader<Class<?>, Map<String, Method>>() {
        public Map<String, Method> load(Class<?> type) {
          ImmutableMap.Builder<String, Method> builder = ImmutableMap.builder();
          for (Method method : type.getMethods()) {
            if (method.getParameterTypes().length == 1) {
              String name = getPropertyName(method);
              if (name != null) {
                builder.put(name, method);
              }
            }
          }
          return builder.build();
        }
      });

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

  @VisibleForTesting
  protected static String getPropertyName(Method setter) {
    JsonProperty property = setter.getAnnotation(JsonProperty.class);
    if (property == null) {
      String name = setter.getName();
      if (name.startsWith("set") && !Modifier.isStatic(setter.getModifiers()) && !setter.isBridge()) {
        return name.substring(3, 4).toLowerCase() + name.substring(4);
      }
      return null;
    } else {
      return property.value();
    }
  }

  @SuppressWarnings("unchecked")
  // Class.cast() would be better - but the Class object may be null
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
        return convertToList((JSONArray) value, null);
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
      return convertToList((JSONArray) value, null);
    } else if (type.equals(Set.class)) {
      return convertToSet((JSONArray) value, null);
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
      return convertToSet((JSONArray) value, typeArgs[0]);
    } else if (Collection.class.isAssignableFrom(clazz)) {
      return convertToList((JSONArray) value, typeArgs[0]);
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
    if(in.length() == 0)
      return Collections.emptyMap();

    for (String name : JSONObject.getNames(in)) {
      out.put(name, convertToObject(in.opt(name), type));
    }
    return out;
  }

  private List<Object> convertToList(JSONArray in, Type type) {
    ArrayList<Object> out = Lists.newArrayListWithCapacity(in.length());

    for (int i = 0, j = in.length(); i < j; ++i) {
      out.add(convertToObject(in.opt(i), type));
    }
    return out;
  }

  private Set<Object> convertToSet(JSONArray in, Type type) {
    return ImmutableSet.copyOf(convertToList(in, type));
  }

  private Object convertToClass(JSONObject in, Class<?> type) {
    Object out = injector.getInstance(type);

    /*
     * Simple hack to add support for arbitrary extensions to Shindig's data
     * model.  It initializes keys/values of an ExtendableBean class, which is
     * a Map under the covers.  If a class implements ExtendableBean.java, it
     * will support arbitrary mappings to JSON & XML.
     */
    if (ExtendableBean.class.isAssignableFrom(type)) {
      String[] names = JSONObject.getNames(in);
      if (names != null) {
        for (String name : names) {
          ((ExtendableBean) out).put(name, convertToObject(in.opt(name), null));
        }
      }
    }

    for (Map.Entry<String, Method> entry : SETTERS.getUnchecked(out.getClass()).entrySet()) {
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
