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

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.List;

import javax.el.ELContext;
import javax.el.ELResolver;

import com.google.common.collect.Lists;

/**
 * ELResolver that processes DOM elements.
 */
public class ElementELResolver extends ELResolver {
  /**
   * A wrapper for a DOM Element that overrides toString().
   * TODO: remove with JUEL 2.1.1.
   */
  public static class ElementWrapper {
    public final Element element;

    public ElementWrapper(Element element) {
      this.element = element;
    }

    @Override
    public String toString() {
      return element.getTextContent();
    }
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext context, Object base) {
    if (base instanceof ElementWrapper) {
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
    Object value = getValue(context, base, property);
    return value == null ? null : value.getClass();
  }

  @Override
  public Object getValue(ELContext context, Object base, Object property) {
    if (!(base instanceof ElementWrapper)) {
      return null;
    }

    context.setPropertyResolved(true);
    Element element = ((ElementWrapper) base).element;
    String propertyString = property.toString();

    // See if there is an Object property.
    Object data = element.getUserData(propertyString);
    if (data != null) {
      return data;
    }

    // Next, check for an attribute.
    Attr attribute = element.getAttributeNode(propertyString);
    if (attribute != null) {
      return attribute.getValue();
    }

    // Finally, look for child nodes with matching local names.
    List<ElementWrapper> childElements = null;
    for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (!(child instanceof Element)) {
        continue;
      }

      Element childElement = (Element) child;
      if (!propertyString.equals(childElement.getLocalName())) {
        continue;
      }

      if (childElements == null) {
        childElements = Lists.newArrayListWithCapacity(2);
      }

      childElements.add(new ElementWrapper(childElement));
    }

    if (childElements == null) {
      return null;
    } else if (childElements.size() == 1) {
      return childElements.get(0);
    } else {
      return childElements;
    }
  }

  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property) {
    if (base instanceof ElementWrapper) {
      context.setPropertyResolved(true);
    }
    return true;
  }

  @Override
  public void setValue(ELContext context, Object base, Object property,
      Object value) {
  }

}
