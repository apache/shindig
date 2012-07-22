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
package org.apache.shindig.protocol.conversion.xstream;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;

/**
 * GuiceBeanProvider class.
 */
public class GuiceBeanProvider {

  protected static final Object[] NO_PARAMS = new Object[0];
  private final Comparator<String> propertyNameComparator;

  private final transient LoadingCache<Class<?>, Map<String, PropertyDescriptor>> propertyNameCache = CacheBuilder
      .newBuilder().weakKeys().build(
          new CacheLoader<Class<?>, Map<String, PropertyDescriptor>>() {
            public Map<String, PropertyDescriptor> load(Class<?> type) {

              BeanInfo beanInfo;
              try {
                beanInfo = Introspector.getBeanInfo(type, Object.class);
              } catch (IntrospectionException e) {
                throw new ObjectAccessException("Cannot get BeanInfo of type " + type.getName(), e);
              }

              ImmutableMap.Builder<String, PropertyDescriptor> nameMapBuilder = ImmutableMap.builder();
              for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
                nameMapBuilder.put(descriptor.getName(), descriptor);
              }
              return nameMapBuilder.build();
            }
          }
      );
  private Injector injector;

  public GuiceBeanProvider(Injector injector) {
    this(injector, null);
  }

  public GuiceBeanProvider(Injector injector,
      final Comparator<String> propertyNameComparator) {
    this.propertyNameComparator = propertyNameComparator;
    this.injector = injector;
  }

  public Object newInstance(Class<?> type) {
    return injector.getInstance(type);
  }

  public void visitSerializableProperties(Object object, Visitor visitor) {
    for (PropertyDescriptor property : getSerializableProperties(object)) {
      try {
        Method readMethod = property.getReadMethod();
        String name = property.getName();
        Class<?> definedIn = readMethod.getDeclaringClass();
        if (visitor.shouldVisit(name, definedIn)) {
          Object value = readMethod.invoke(object);
          visitor.visit(name, property.getPropertyType(), definedIn, value);
        }
      } catch (IllegalArgumentException e) {
        throw new ObjectAccessException("Could not get property "
            + object.getClass() + '.' + property.getName(), e);
      } catch (IllegalAccessException e) {
        throw new ObjectAccessException("Could not get property "
            + object.getClass() + '.' + property.getName(), e);
      } catch (InvocationTargetException e) {
        throw new ObjectAccessException("Could not get property "
            + object.getClass() + '.' + property.getName(), e);
      }
    }
  }

  public void writeProperty(Object object, String propertyName, Object value) {
    PropertyDescriptor property = getProperty(propertyName, object.getClass());
    try {
      property.getWriteMethod().invoke(object, value);
    } catch (IllegalArgumentException e) {
      throw new ObjectAccessException("Could not set property "
          + object.getClass() + '.' + property.getName(), e);
    } catch (IllegalAccessException e) {
      throw new ObjectAccessException("Could not set property "
          + object.getClass() + '.' + property.getName(), e);
    } catch (InvocationTargetException e) {
      throw new ObjectAccessException("Could not set property "
          + object.getClass() + '.' + property.getName(), e);
    }
  }

  public Class<?> getPropertyType(Object object, String name) {
    return getProperty(name, object.getClass()).getPropertyType();
  }

  public boolean propertyDefinedInClass(String name, Class<?> type) {
    return getProperty(name, type) != null;
  }

  private List<PropertyDescriptor> getSerializableProperties(Object object) {
    Map<String, PropertyDescriptor> nameMap = propertyNameCache.getUnchecked(object.getClass());

    Set<String> names = (propertyNameComparator == null) ? nameMap.keySet() :
      ImmutableSortedSet.orderedBy(propertyNameComparator).addAll(nameMap.keySet()).build();

    List<PropertyDescriptor> result = Lists.newArrayListWithCapacity(nameMap.size());

    for (final String name : names) {
      final PropertyDescriptor descriptor = nameMap.get(name);
      if (canStreamProperty(descriptor)) {
        result.add(descriptor);
      }
    }
    return result;
  }

  protected boolean canStreamProperty(PropertyDescriptor descriptor) {
    return descriptor.getReadMethod() != null
        && descriptor.getWriteMethod() != null;
  }

  public boolean propertyWriteable(String name, Class<?> type) {
    PropertyDescriptor property = getProperty(name, type);
    return property.getWriteMethod() != null;
  }

  private PropertyDescriptor getProperty(String name, Class<?> type) {
    return propertyNameCache.getUnchecked(type).get(name);
  }

  interface Visitor {
    boolean shouldVisit(String name, Class<?> definedIn);

    void visit(String name, Class<?> type, Class<?> definedIn, Object value);
  }

}
