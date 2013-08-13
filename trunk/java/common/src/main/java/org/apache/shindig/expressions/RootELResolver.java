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

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELResolver;

import com.google.common.collect.ImmutableMap;

/**
 * ELResolver implementation that adds a map of top-level variables.
 * New variables can be inserted after creation with:
 * {@code context.getELResolver().setValue(context, null, name, value);}
 *
 * TODO: should this be read-only?
 *
 * @see Expressions#newELContext(ELResolver...)
 */
public class RootELResolver extends ELResolver {
  private final Map<String, ? extends Object> map;

  public RootELResolver() {
    this(ImmutableMap.<String, Object>of());
  }

  public RootELResolver(Map<String, ? extends Object> base) {
    this.map = base;
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
    if (base == null && map.containsKey(property)) {
      context.setPropertyResolved(true);
      Object value = map.get(property);
      return value == null ? null : value.getClass();
    }

    return null;
  }

  @Override
  public Object getValue(ELContext context, Object base, Object property) {
    if (base == null && map.containsKey(property)) {
      context.setPropertyResolved(true);
      return map.get(property);
    }

    return null;
  }

  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property) {
    if (base == null && map.containsKey(property)) {
      context.setPropertyResolved(true);
      return true;
    }

    return false;
  }

  @Override
  public void setValue(ELContext context, Object base, Object property, Object value) {
  }
}
