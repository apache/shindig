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
package org.apache.shindig.gadgets;

import org.json.JSONException;
import org.json.JSONObject;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotWritableException;

import com.google.common.collect.ImmutableMap;

/**
 * ELResolver for the built-in gadget properties:
 * - UserPrefs: the user preferences
 * - ViewParams: view params (as a JSON object)
 */
public class GadgetELResolver extends ELResolver {
  public static final String USER_PREFS_PROPERTY = "UserPrefs";
  public static final String VIEW_PARAMS_PROPERTY = "ViewParams";

  private final GadgetContext gadgetContext;

  public GadgetELResolver(GadgetContext context) {
    this.gadgetContext = context;
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext context, Object base) {
    if (base == null) {
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
    if (base == null) {
      if (VIEW_PARAMS_PROPERTY.equals(property)) {
        context.setPropertyResolved(true);
        return Object.class;
      } else if (USER_PREFS_PROPERTY.equals(property)) {
        context.setPropertyResolved(true);
        return Map.class;
      }
    }

    return null;
  }

  @Override
  public Object getValue(ELContext context, Object base, Object property) {
    if (base == null) {
      if (VIEW_PARAMS_PROPERTY.equals(property)) {
        context.setPropertyResolved(true);
        String params = gadgetContext.getParameter("view-params");
        if (params != null && !"".equals(params)) {
          try {
            // TODO: immutable?
            return new JSONObject(params);
          } catch (JSONException e) {
            throw new ELException(e);
          }
        }

        // Return an empty map - this doesn't allocate anything, whereas an
        // empty JSONObject would
        return ImmutableMap.of();
      } else if (USER_PREFS_PROPERTY.equals(property)) {
        context.setPropertyResolved(true);
        // TODO: immutable?
        return gadgetContext.getUserPrefs().getPrefs();
      }
    }

    return null;
  }

  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property) {
    if ((base == null) &&
        (VIEW_PARAMS_PROPERTY.equals(property)
        || USER_PREFS_PROPERTY.equals(property))) {
      context.setPropertyResolved(true);
    }

    return true;
  }

  @Override
  public void setValue(ELContext context, Object base, Object property, Object value) {
    if ((base == null) &&
        (VIEW_PARAMS_PROPERTY.equals(property)
        || USER_PREFS_PROPERTY.equals(property))) {
      throw new PropertyNotWritableException("Cannot override " + property);
    }
  }
}
