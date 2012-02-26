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
package org.apache.shindig.config;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.PropertyNotWritableException;

/**
 * ELResolver that handles adds support for:
 *   - the "Cur" property, for explicit reference to the current container config
 *   - the "parent" property, for explicit and recursive reference to config parents
 *   - implicit reference to top-level properties in the current container config
 *     or inside any parents
 */
public class ContainerConfigELResolver extends ELResolver {
  /** Key for the current container. */
  public static final String CURRENT_CONFIG_KEY = "Cur";

  private final ContainerConfig config;
  private final String currentContainer;

  public ContainerConfigELResolver(ContainerConfig config, String currentContainer) {
    this.config = config;
    this.currentContainer = currentContainer;
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext context, Object base) {
    if ((base == null) || (base instanceof ContainerReference)) {
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
    if ((base == null) || (base instanceof ContainerReference)) {
      context.setPropertyResolved(true);
      Object value = getValue(context, base, property);
      return (value == null) ? null : value.getClass();
    }

    return null;
  }

  @Override
  public Object getValue(ELContext context, Object base, Object property) {
    // Handle all requests off the base, and anything that is a reference to
    // a container
    String container;
    if (base == null) {
      container = currentContainer;
    } else if (base instanceof ContainerReference) {
      container = ((ContainerReference) base).containerName;
    } else {
      // Not ours - return without setPropertyResolved(true)
      return null;
    }

    context.setPropertyResolved(true);
    if (JsonContainerConfig.PARENT_KEY.equals(property)) {
      // "parent": find the parent of the base, and return a ContainerReference
      String parent = config.getString(container, JsonContainerConfig.PARENT_KEY);
      if (parent == null) {
        return null;
      } else {
        ContainerReference reference = new ContainerReference();
        reference.containerName = parent;
        return reference;
      }
    } else if (CURRENT_CONFIG_KEY.equals(property) && base == null) {
      // "Cur": return a reference to the current container
      ContainerReference reference = new ContainerReference();
      reference.containerName = currentContainer;
      return reference;
    } else {
      // Referring to a property of an existing container
      return config.getProperty(container, property.toString());
    }
  }

  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property) {
    if ((base == null) || (base instanceof ContainerReference)) {
      context.setPropertyResolved(true);
      return true;
    }

    return false;
  }

  @Override
  public void setValue(ELContext context, Object base, Object property, Object value) {
    // No support for mutating container configs
    if ((base == null) || (base instanceof ContainerReference)) {
      throw new PropertyNotWritableException();
    }
  }

  /** A reference to the container, for later EL evaluation */
  private static class ContainerReference {
    public String containerName;
  }
}
