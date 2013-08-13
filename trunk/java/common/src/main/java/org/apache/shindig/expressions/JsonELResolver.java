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
import org.json.JSONException;
import org.json.JSONObject;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;

/**
 * ELResolver implementation for JSONArray and JSONObject.
 */
class JsonELResolver extends ELResolver {

  @Override
  public Class<?> getCommonPropertyType(ELContext context, Object base) {
    if (base instanceof JSONArray) {
      return Integer.class;
    }

    if (base instanceof JSONObject) {
      return String.class;
    }

    return null;
  }

  @Override
  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context,
      Object base) {
    return null;
  }

  @Override
  public Class<?> getType(ELContext context, Object base, Object property) {
    if (isJson(base)) {
      context.setPropertyResolved(true);
      Object value = getValue(context, base, property);
      return value == null ? null : value.getClass();
    }

    return null;
  }

  @Override
  public Object getValue(ELContext context, Object base, Object property) {
    if (base instanceof JSONObject) {
      context.setPropertyResolved(true);
      return ((JSONObject) base).opt(String.valueOf(property));
    }

    if (base instanceof JSONArray) {
      context.setPropertyResolved(true);
      int index = toInt(property);
      return ((JSONArray) base).opt(index);
    }

    return null;
  }

  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property) {
    if (isJson(base)) {
      context.setPropertyResolved(true);
    }

    return false;
  }

  @Override
  public void setValue(ELContext context, Object base, Object property, Object value) {
    if (base instanceof JSONObject) {
      context.setPropertyResolved(true);
      try {
        ((JSONObject) base).put(String.valueOf(property), value);
      } catch (JSONException e) {
        throw new ELException(e);
      }
      context.setPropertyResolved(true);
    }

    if (base instanceof JSONArray) {
      context.setPropertyResolved(true);
      int index = toInt(property);
      try {
        ((JSONArray) base).put(index, value);
      } catch (JSONException e) {
        throw new ELException(e);
      }
      context.setPropertyResolved(true);
    }
  }

  private int toInt(Object property) {
    if (property instanceof Number) {
      return ((Number) property).intValue();
    }

    try {
      return Integer.parseInt(String.valueOf(property));
    } catch (NumberFormatException nfe) {
      throw new ELException(nfe);
    }
  }

  private boolean isJson(Object base) {
    return (base instanceof JSONObject || base instanceof JSONArray);
  }
}
