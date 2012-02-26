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
package org.apache.shindig.expressions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.el.ELException;

import com.google.common.collect.ImmutableList;


/**
 * Custom type converter class that overrides the default EL coercion rules
 * where necessary.  Specifically, Booleans are handled differently,
 * and JSONArray is supported.
 */
public class ShindigTypeConverter implements ELTypeConverter {


  public  boolean isPostConvertible(Class<?> type) {
    return false;
  }

  @SuppressWarnings("unchecked")
  public <T> T convert(Object obj, Class<T> type) throws ELException {
    // Handle boolean specially
    if (type == Boolean.class || type == Boolean.TYPE) {
      return (T) coerceToBoolean(obj);
    }

    if (type == JSONArray.class) {
      return (T) coerceToJsonArray(obj);
    }

    if (type == Iterable.class) {
      return (T) coerceToIterable(obj);
    }

    //  Nothing more we can do.
    return null;
  }

  /**
   * Coerce objects to iterables.  Iterables and JSONArrays have the obvious
   * coercion.  JSONObjects are coerced to single-element lists, unless
   * they have a "list" property that is in array, in which case that's used.
   */
  private Iterable<?> coerceToIterable(Object obj) {
    if (obj == null) {
      return ImmutableList.of();
    }

    if (obj instanceof Iterable<?>) {
      return ((Iterable<?>) obj);
    }

    if (obj instanceof JSONArray) {
      final JSONArray array = (JSONArray) obj;
      // TODO: Extract JSONArrayIterator class?
      return new Iterable<Object>() {
        public Iterator<Object> iterator() {
          return new Iterator<Object>() {
            private int i = 0;

            public boolean hasNext() {
              return i < array.length();
            }

            public Object next() {
              if (i >= array.length()) {
                throw new NoSuchElementException();
              }

              try {
                return array.get(i++);
              } catch (Exception e) {
                throw new ELException(e);
              }
            }

            public void remove() {
              throw new UnsupportedOperationException();
            }
          };
        }
      };
    }

    if (obj instanceof JSONObject) {
      JSONObject json = (JSONObject) obj;

      // Does this object have a "list" property that is an array?
      // TODO: add to specification
      Object childList = json.opt("list");
      if (childList instanceof JSONArray) {
        return coerceToIterable(childList);
      }

      // A scalar JSON value is treated as a single element list.
      return ImmutableList.of(json);
    }

    return ImmutableList.of(obj);
  }

  private JSONArray coerceToJsonArray(Object obj) {
    if (obj == null) {
      return null;
    }

    if (obj instanceof JSONArray) {
      return (JSONArray) obj;
    }

    if (obj instanceof String) {
      JSONArray array = new JSONArray();
      StringTokenizer tokenizer = new StringTokenizer(obj.toString(), ",");
      while (tokenizer.hasMoreTokens()) {
        array.put(tokenizer.nextToken());
      }

      return array;
    }

    throw new ELException("Could not coerce " + obj.getClass().getName() + " to JSONArray");
  }

  /**
   * Coerce the following booleans:
   *
   * null -> false
   * empty string, and "false" -> false
   * boolean false -> false
   * number 0 -> false
   *
   * All else is true.
   */
  private Boolean coerceToBoolean(Object obj) {
    if (obj == null) {
      return false;
    }

    if (obj instanceof String) {
      return !("".equals(obj) || "false".equals(obj));
    }

    if (obj instanceof Boolean) {
      return (Boolean) obj;
    }

    if (obj instanceof Number) {
      return 0 != ((Number) obj).intValue();
    }

    return true;
  }
}
