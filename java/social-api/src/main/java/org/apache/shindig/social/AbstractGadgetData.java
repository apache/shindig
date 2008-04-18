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
package org.apache.shindig.social;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Autoconvert a pojo in a JSONObject If a value is set to null will not be
 * added in the JsonObject expect if the annotation {@link Mandatory} has been
 */
// TODO: Move from an inheritance model to a class that can translate any java
// object based on its getters
public abstract class AbstractGadgetData {

  private static final Object[] EMPTY_OBJECT = {};
  private static final String EXCLUDED_GETTER = "class";
  private static final Pattern GETTER = Pattern.compile("^get([a-zA-Z]+)$");

  /**
   * Convert this object to {@link JSONObject} reading Pojo properties
   *
   * @return A JSONObject representing this pojo
   */
  public JSONObject toJson() {
    JSONObject toReturn = new JSONObject();
    Method[] methods = this.getClass().getMethods();
    for (Method method : methods) {
      String errorMessage = "Could not encode the " + method + " method.";
      try {
        putAttribute(toReturn, method,
            method.getAnnotation(Mandatory.class) != null);
      } catch (JSONException e) {
        throw new RuntimeException(errorMessage, e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(errorMessage, e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(errorMessage, e);
      }
    }
    return toReturn;
  }

  /**
   * Convert java declared method and its value to an entry in the given
   * {@link JSONObject}
   *
   * @param object the json object to put the field value in
   * @param method the method to encode
   * @param mandatory true if the field is mandatory
   * @throws JSONException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  private void putAttribute(JSONObject object, Method method, boolean mandatory)
      throws JSONException, IllegalAccessException, InvocationTargetException {
    Matcher matcher = GETTER.matcher(method.getName());
    if (!matcher.matches()) {
      return;
    }

    String name = matcher.group();
    String fieldName = name.substring(3, 4).toLowerCase() + name.substring(4);
    if (fieldName.equalsIgnoreCase(EXCLUDED_GETTER)) {
      return;
    }

    Object val = method.invoke(this, EMPTY_OBJECT);
    if (val != null) {
      object.put(fieldName, translateObjectToJson(val));
    } else {
      if (mandatory) {
        throw new RuntimeException(fieldName
            + " is a mandory value, it should not be null");
      }
    }
  }

  private Object translateObjectToJson(Object val) throws JSONException {
    if (val instanceof AbstractGadgetData[]) {
      JSONArray array = new JSONArray();
      for (Object asd : (AbstractGadgetData[]) val) {
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

    } else if (val instanceof AbstractGadgetData) {
      return ((AbstractGadgetData) val).toJson();
    }

    // Fallback to returning the original object. This works fine for primitive
    // types and JSONObject types.
    return val;
  }
}
