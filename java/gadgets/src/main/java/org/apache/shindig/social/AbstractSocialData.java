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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * Autoconvert a pojo in a JSONObject If a value is set to null will not be
 * added in the JsonObject expect if the annotation {@link Mandatory} has been
 *
 * @author Vincent Demay
 *
 */
public abstract class AbstractSocialData {

  private static final Class[] EMPTY_PARAM = {};
  private static final Object[] EMPTY_OBJECT = {};

  /**
   * Convert this object to {@link JSONObject} reading Pojo properties
   *
   * @return A JSONObject representing this pojo
   */
  public JSONObject toJson() {
    JSONObject toReturn = new JSONObject();
    Field[] fields = this.getClass().getDeclaredFields();
    for (Field field : fields) {
      String errorMessage = "Could not encode the " + field + " field.";
      try {
        putAttribute(toReturn, field,
            field.getAnnotation(Mandatory.class) != null);
      } catch (JSONException e) {
        throw new RuntimeException(errorMessage, e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(errorMessage, e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(errorMessage, e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(errorMessage, e);
      }
    }
    return toReturn;
  }

  /**
   * Convert java declared attritiue and its value to an entry in the given
   * {@link JSONObject}
   *
   * @param object the json object to put the field value in
   * @param field the field to encode
   * @param mandatory true if the field is mandatory
   * @throws IllegalArgumentException
   * @throws JSONException
   * @throws IllegalAccessException
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws SecurityException
   */
  private void putAttribute(JSONObject object, Field field, boolean mandatory)
      throws IllegalArgumentException, JSONException, IllegalAccessException,
      SecurityException, InvocationTargetException, NoSuchMethodException {
    String getterName = field.getName();
    getterName = "get" + getterName.substring(0, 1).toUpperCase()
        + getterName.substring(1, getterName.length());
    Object val = this.getClass().getMethod(getterName, EMPTY_PARAM).invoke(
        this, EMPTY_OBJECT);
    String name = field.getName();
    if (val != null) {
      if (val instanceof AbstractSocialData[]) {
        JSONArray array = new JSONArray();
        for (AbstractSocialData asd : (AbstractSocialData[]) val) {
          array.put(asd.toJson());
        }
        val = array;
      } else if (val instanceof AbstractSocialData) {
        // get json object
        val = ((AbstractSocialData) val).toJson();
      }
      object.put(name, val);
    } else {
      if (mandatory) {
        throw new RuntimeException(name
            + " is a mandory value, it should not be null");
      }
    }
  }
}
