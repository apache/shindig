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
package org.apache.shindig.common;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Serializes a JSONObject.
 *
 * The methods here are designed to be substantially more CPU and memory efficient than those found
 * in org.json or net.sf.json. In profiling, the performance of both of these libraries has been
 * found to be woefully inadequate for large scale deployments.
 *
 * The append*() methods can be used to serialize directly into an Appendable, such as an output
 * stream. This avoids unnecessary copies to intermediate objects.
 */
public final class JsonSerializer {
  // Multiplier to use for allocating the buffer.
  private static final int BASE_MULTIPLIER = 256;

  private static final char[] HEX_DIGITS = {
    '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
  };

  private static final Set<String> EXCLUDE_METHODS
      = ImmutableSet.of("getClass", "getDeclaringClass");

  private static final Map<Class<?>, Map<String, Method>> getters = Maps.newConcurrentHashMap();

  private JsonSerializer() {}

  public static String serialize(Object object) {
    StringBuilder buf = new StringBuilder(1024);
    try {
      append(buf, object);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return buf.toString();
  }

  /**
   * Serialize a JSONObject. Does not guard against cyclical references.
   */
  public static String serialize(JSONObject object) {
    StringBuilder buf = new StringBuilder(object.length() * BASE_MULTIPLIER);
    try {
      appendJsonObject(buf, object);
    } catch (IOException e) {
      // Shouldn't ever happen unless someone adds something to append*.
      throw new RuntimeException(e);
    }
    return buf.toString();
  }

  /**
   * Serializes a Map as a JSON object. Does not guard against cyclical references.
   */
  public static String serialize(Map<String, ?> map) {
    StringBuilder buf = new StringBuilder(map.size() * BASE_MULTIPLIER);
    try {
      appendMap(buf, map);
    } catch (IOException e) {
      // Shouldn't ever happen unless someone adds something to append*.
      throw new RuntimeException(e);
    }
    return buf.toString();
  }

  /**
   * Serializes a Collection as a JSON array. Does not guard against cyclical references.
   */
  public static String serialize(Collection<?> collection) {
    StringBuilder buf = new StringBuilder(collection.size() * BASE_MULTIPLIER);
    try {
      appendCollection(buf, collection);
    } catch (IOException e) {
      // Shouldn't ever happen unless someone adds something to append*.
      throw new RuntimeException(e);
    }
    return buf.toString();
  }

  /**
   * Serializes an array as a JSON array. Does not guard against cyclical references
   */
  public static String serialize(Object[] array) {
    StringBuilder buf = new StringBuilder(array.length * BASE_MULTIPLIER);
    try {
      appendArray(buf, array);
    } catch (IOException e) {
      // Shouldn't ever happen unless someone adds something to append*.
      throw new RuntimeException(e);
    }
    return buf.toString();
  }

  /**
   * Serializes a JSON array. Does not guard against cyclical references
   */
  public static String serialize(JSONArray array) {
    StringBuilder buf = new StringBuilder(array.length() * BASE_MULTIPLIER);
    try {
      appendJsonArray(buf, array);
    } catch (IOException e) {
      // Shouldn't ever happen unless someone adds something to append*.
      throw new RuntimeException(e);
    }
    return buf.toString();
  }

  /**
   * Appends a value to the buffer.
   *
   * @throws IOException If {@link Appendable#append(char)} throws an exception.
   */
  @SuppressWarnings("unchecked")
  public static void append(Appendable buf, Object value) throws IOException {
    if (value == null) {
      buf.append("null");
    } else if (value instanceof Number ||
               value instanceof Boolean) {
      // Primitives
      buf.append(value.toString());
    } else if (value instanceof CharSequence ||
               value instanceof DateTime ||
               value instanceof Date ||
               value.getClass().isEnum()) {
      // String-like Primitives
      appendString(buf, value.toString());
    } else if (value instanceof JSONObject) {
      appendJsonObject(buf, (JSONObject) value);
    } else if (value instanceof JSONArray) {
      buf.append(value.toString());
    } else if (value instanceof Map) {
      appendMap(buf, (Map<String, Object>) value);
    } else if (value instanceof Collection) {
      appendCollection(buf, (Collection<Object>) value);
    } else if (value.getClass().isArray()) {
      appendArray(buf, (Object[]) value);
    } else {
      // Try getter conversion
      appendPojo(buf, value);
    }
  }

  /**
   * Appends a java object using getters
   *
   * @throws IOException If {@link Appendable#append(char)} throws an exception.
   */
  public static void appendPojo(Appendable buf, Object pojo) throws IOException {
    Map<String, Method> methods = getGetters(pojo);
    buf.append('{');
    boolean firstDone = false;
    for (Map.Entry<String, Method> entry : methods.entrySet()) {
      if (firstDone) {
        buf.append(',');
      } else {
        firstDone = true;
      }
      appendString(buf, entry.getKey());
      buf.append(':');
      try {
        append(buf, entry.getValue().invoke(pojo));
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
    buf.append('}');
  }

  /**
   * Appends an array to the buffer.
   *
   * @throws IOException If {@link Appendable#append(char)} throws an exception.
   */
  public static void appendArray(Appendable buf, Object[] array) throws IOException {
    buf.append('[');
    boolean firstDone = false;
    for (Object o : array) {
      if (firstDone) {
        buf.append(',');
      } else {
        firstDone = true;
      }
      append(buf, o);
    }
    buf.append(']');
  }

  /**
   * Append a JSONArray to the buffer.
   * @throws IOException If {@link Appendable#append(char)} throws an exception.
   */
  public static void appendJsonArray(Appendable buf, JSONArray array) throws IOException {
    buf.append('[');
    boolean firstDone = false;
    for (int i = 0, j = array.length(); i < j; ++i) {
      if (firstDone) {
        buf.append(',');
      } else {
        firstDone = true;
      }
      append(buf, array.opt(i));
    }
    buf.append(']');
  }

  /**
   * Appends a Collection to the buffer.
   *
   * @throws IOException If {@link Appendable#append(char)} throws an exception.
   */
  public static void appendCollection(Appendable buf, Collection<?> collection)
      throws IOException {
    buf.append('[');
    boolean firstDone = false;
    for (Object o : collection) {
      if (firstDone) {
        buf.append(',');
      } else {
        firstDone = true;
      }
      append(buf, o);
    }
    buf.append(']');
  }

  /**
   * Appends a Map to the buffer.
   *
   * @throws IOException If {@link Appendable#append(char)} throws an exception.
   */
  public static void appendMap(Appendable buf, Map<String, ?> map)
      throws IOException {
    buf.append('{');
    boolean firstDone = false;
    for (Map.Entry<String, ?> entry : map.entrySet()) {
      if (firstDone) {
        buf.append(',');
      } else {
        firstDone = true;
      }
      appendString(buf, entry.getKey());
      buf.append(':');
      append(buf, entry.getValue());
    }
    buf.append('}');
  }

  /**
   * Appends a JSONObject to the buffer.
   *
   * @throws IOException If {@link Appendable#append(char)} throws an exception.
   */
  @SuppressWarnings("unchecked")
  public static void appendJsonObject(Appendable buf, JSONObject object) throws IOException {
    buf.append('{');
    Iterator<String> keys = object.keys();
    boolean firstDone = false;
    while (keys.hasNext()) {
      if (firstDone) {
        buf.append(',');
      } else {
        firstDone = true;
      }
      String key = keys.next();
      appendString(buf, key);
      buf.append(':');
      append(buf, object.opt(key));
    }
    buf.append('}');
  }

  /**
   * Appends a string to the buffer. The string will be JSON encoded and enclosed in quotes.
   *
   * @throws IOException If {@link Appendable#append(char)} throws an exception.
   */
  public static void appendString(Appendable buf, CharSequence string) throws IOException {
    if (string == null || string.length() == 0) {
      buf.append("\"\"");
      return;
    }

    char previous, current = 0;
    buf.append('"');
    for (int i = 0, j = string.length(); i < j; ++i) {
      previous = current;
      current = string.charAt(i);
      switch (current) {
        case '\\':
        case '"':
          buf.append('\\');
          buf.append(current);
          break;
        case '/':
          if (previous == '<') {
            buf.append('\\');
          }
          buf.append(current);
          break;
        default:
          if (current < ' ' || (current >= '\u0080' && current < '\u00a0') ||
              (current >= '\u2000' && current < '\u2100')) {
            buf.append('\\');
            switch (current) {
              case '\b':
                buf.append('b');
                break;
              case '\t':
                buf.append('t');
                break;
              case '\n':
                buf.append('n');
                break;
              case '\f':
                buf.append('f');
                break;
              case '\r':
                buf.append('r');
                break;
              default:
                // The three possible alternative approaches for dealing with unicode characters are
                // as follows:
                // Method 1 (from json.org.JSONObject)
                // 1. Append "000" + Integer.toHexString(current)
                // 2. Truncate this value to 4 digits by using value.substring(value.length() - 4)
                //
                // Method 2 (from net.sf.json.JSONObject)
                // This method is fairly unique because the entire thing uses an intermediate fixed
                // size buffer of 1KB. It's an interesting approach, but overall performs worse than
                // org.json
                // 1. Append "000" + Integer.toHexString(current)
                // 2. Append value.charAt(value.length() - 4)
                // 2. Append value.charAt(value.length() - 3)
                // 2. Append value.charAt(value.length() - 2)
                // 2. Append value.charAt(value.length() - 1)
                //
                // Method 3 (previous experiment)
                // 1. Calculate Integer.hexString(current)
                // 2. for (int i = 0; i < 4 - value.length(); ++i) { buf.append('0'); }
                // 3. buf.append(value)
                //
                // Method 4 (Sun conversion from java.util.Properties)
                // 1. Append '\'
                // 2. Append 'u'
                // 3. Append each of 4 octets by indexing into a hex array.
                //
                // Method 5
                // Index into a single lookup table of all relevant lookup values.
                buf.append('u');
                buf.append(HEX_DIGITS[(current >> 12) & 0xF]);
                buf.append(HEX_DIGITS[(current >>  8) & 0xF]);
                buf.append(HEX_DIGITS[(current >>  4) & 0xF]);
                buf.append(HEX_DIGITS[current & 0xF]);
             }
          } else {
            buf.append(current);
          }
      }
    }
    buf.append('"');
  }

  private static Map<String, Method> getGetters(Object pojo) {
    Class<?> clazz = pojo.getClass();

    Map<String, Method> methods = getters.get(clazz);
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

    getters.put(clazz, methods);
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
