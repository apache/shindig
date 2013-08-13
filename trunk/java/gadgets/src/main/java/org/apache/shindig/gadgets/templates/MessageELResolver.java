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

import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.gadgets.spec.MessageBundle;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.List;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotWritableException;

import com.google.common.collect.Lists;

/**
 * ELResolver for the Msg property in templates.
 */
public class MessageELResolver extends ELResolver {
  public static final String PROPERTY_MSG = "Msg";
  private final MessageBundle bundle;
  private final Expressions expressions;

  public MessageELResolver(Expressions expressions, MessageBundle bundle) {
    this.expressions = expressions;
    this.bundle = bundle;
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
    if ((base == null) && PROPERTY_MSG.equals(property)) {
      context.setPropertyResolved(true);
      return bundle;
    } else if (base instanceof MessageBundle) {
      String text = bundle.getMessages().get(property.toString());
      if (text == null) {
        context.setPropertyResolved(true);
        return null;
      }

      List<Object> properties = null;
      try {
        properties = pushCurrentProperty(context, property);
        context.setPropertyResolved(false);
        return expressions.parse(text, Object.class).getValue(context);
      } finally {
        popProperty(properties);
        context.setPropertyResolved(true);
      }
    }

    return null;
  }

  /**
   * Track the set of message bundle properties being evaluated.  We allow
   * recursion, but don't want to allow infinite self-recursion (though the
   * stack overflows quickly).
   */
  private List<Object> pushCurrentProperty(ELContext context, Object property) {
    @SuppressWarnings("unchecked")
    List<Object> propertyList = (List<Object>) context.getContext(MessageELResolver.class);
    if (propertyList == null) {
      propertyList = Lists.newArrayList();
      context.putContext(MessageELResolver.class, propertyList);
    } else {
      if (propertyList.contains(property)) {
        throw new ELException("Recursive invocation of message bundle properties");
      }
    }

    propertyList.add(property);
    return propertyList;
  }

  private void popProperty(List<Object> properties) {
    if (properties != null) {
      properties.remove(properties.size() - 1);
    }
  }

  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property) {
    if ((base == null) && PROPERTY_MSG.equals(property)) {
      context.setPropertyResolved(true);
      return true;
    }

    return false;
  }

  @Override
  public void setValue(ELContext context, Object base, Object property, Object value) {
    if ((base == null) && PROPERTY_MSG.equals(property)) {
      throw new PropertyNotWritableException();
    }
  }
}
