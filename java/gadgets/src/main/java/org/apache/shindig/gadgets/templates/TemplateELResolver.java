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
package org.apache.shindig.gadgets.templates;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;

import com.google.common.collect.ImmutableSet;

/**
 * ELResolver used to process OpenSocial templates.  Provides three variables:
 * <ul>
 * <li>"Top": Global values </li>
 * <li>"Cur": Current template variable</li>
 * <li>"Context": Miscellaneous contextual information</li>
 */
public class TemplateELResolver extends ELResolver {
  public static final String PROPERTY_TOP = "Top";
  public static final String PROPERTY_CONTEXT = "Context";
  public static final String PROPERTY_CUR = "Cur";
  public static final String PROPERTY_MY = "My";

  private static final Set<String> TOP_LEVEL_PROPERTIES =
    ImmutableSet.of(PROPERTY_TOP, PROPERTY_CONTEXT, PROPERTY_CUR, PROPERTY_MY);

  private final TemplateContext templateContext;

  public TemplateELResolver(TemplateContext templateContext) {
    this.templateContext = templateContext;
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
    // TODO: implement
    return null;
  }

  @Override
  public Object getValue(ELContext context, Object base, Object property) {
    if (base == null) {
      if (TOP_LEVEL_PROPERTIES.contains(property)) {
        context.setPropertyResolved(true);
        if (PROPERTY_TOP.equals(property)) {
          return templateContext.getTop();
        } else if (PROPERTY_CONTEXT.equals(property)) {
          return templateContext.getContext();
        } else if (PROPERTY_MY.equals(property)) {
          return templateContext.getMy();
        } else {
          return templateContext.getCur();
        }
      }

      // Check variables.
      if (property instanceof String) {
        // Workaround for inability of Jasper-EL resolvers to access VariableMapper
        ELContext elContext = (ELContext)context.getContext(TemplateContext.class);
        ValueExpression valueExp = elContext.getVariableMapper().resolveVariable((String) property);
        if (valueExp != null) {
          context.setPropertyResolved(true);
          return valueExp.getValue(context);
        }
      }

      // Check ${Cur} next.
      Object cur = templateContext.getCur();
      // Resolve through "cur" as if it were a value - if "isPropertyResolved()"
      // is true, it was handled
      if (cur != null) {
        Object value = context.getELResolver().getValue(context, cur, property);
        if (context.isPropertyResolved()) {
          if (value != null) {
            return value;
          } else {
            context.setPropertyResolved(false);
          }
        }
      }

      // Check ${My} next.
      Map<String, ? extends Object> scope = templateContext.getMy();
      if (scope != null && scope.containsKey(property)) {
        context.setPropertyResolved(true);
        return scope.get(property);
      }

      // Look at ${Top} context last.
      scope = templateContext.getTop();
      if (scope != null && scope.containsKey(property)) {
        context.setPropertyResolved(true);
        return scope.get(property);
      }
    }

    return null;
  }

  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property) {
    if (base == null && TOP_LEVEL_PROPERTIES.contains(property)) {
      context.setPropertyResolved(true);
      return true;
    }

    return false;
  }

  @Override
  public void setValue(ELContext context, Object base, Object property, Object value) {
    if (base == null && TOP_LEVEL_PROPERTIES.contains(property)) {
      throw new PropertyNotWritableException();
    }
  }

}
