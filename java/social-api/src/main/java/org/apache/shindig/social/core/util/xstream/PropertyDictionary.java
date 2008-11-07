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
package org.apache.shindig.social.core.util.xstream;

import com.thoughtworks.xstream.converters.javabean.BeanProperty;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds the serializable properties maps for each bean and caches them.
 */
public class PropertyDictionary {

  private final Map<String, Map<String, BeanProperty>> keyedByPropertyNameCache = new ConcurrentHashMap<String, Map<String, BeanProperty>>();

  public Iterator<BeanProperty> serializablePropertiesFor(Class<?> cls) {
    return buildMap(cls).values().iterator();
  }

  /**
   * Locates a serializable property
   * 
   * @param cls
   * @param name
   * @param definedIn
   * @return
   */
  public BeanProperty property(Class<?> cls, String name) {
    Map<String, BeanProperty> properties = buildMap(cls);
    BeanProperty property = (BeanProperty) properties.get(name);
    if (property == null) {
      throw new ObjectAccessException("No such property " + cls.getName() + "."
          + name);
    } else {
      return property;
    }
  }

  /**
   * Builds the map of all serializable properties for the the provided bean
   * 
   * @param cls
   * @param tupleKeyed
   * @return
   */
  private Map<String, BeanProperty> buildMap(Class<?> cls) {
    final String clsName = cls.getName();
    if (!keyedByPropertyNameCache.containsKey(clsName)) {
      synchronized (keyedByPropertyNameCache) {
        if (!keyedByPropertyNameCache.containsKey(clsName)) { // double check
          // Gather all the properties, using only the keyed map. It
          // is possible that a class have two writable only
          // properties that have the same name
          // but different types
          final Map<PropertyKey, BeanProperty> propertyMap = new HashMap<PropertyKey, BeanProperty>();
          Method[] methods = cls.getMethods();

          for (int i = 0; i < methods.length; i++) {
            if (!Modifier.isPublic(methods[i].getModifiers())
                || Modifier.isStatic(methods[i].getModifiers())) {
              continue;
            }

            String methodName = methods[i].getName();
            Class<?>[] parameters = methods[i].getParameterTypes();
            Class<?> returnType = methods[i].getReturnType();
            String propertyName = null;
            if ((methodName.startsWith("get") || methodName.startsWith("is"))
                && parameters.length == 0 && returnType != null) {
              if (methodName.startsWith("get"))
                propertyName = Introspector.decapitalize(methodName
                    .substring(3));
              else
                propertyName = Introspector.decapitalize(methodName
                    .substring(2));
              BeanProperty property = getBeanProperty(propertyMap, cls,
                  propertyName, returnType);
              property.setGetterMethod(methods[i]);
            } else if (methodName.startsWith("set") && parameters.length == 1
                && returnType.equals(void.class)) {
              propertyName = Introspector.decapitalize(methodName.substring(3));
              BeanProperty property = getBeanProperty(propertyMap, cls,
                  propertyName, parameters[0]);
              property.setSetterMethod(methods[i]);
            }
          }

          // retain only those that can be both read and written and
          // sort them by name
          List<BeanProperty> serializableProperties = new ArrayList<BeanProperty>();
          for (BeanProperty property : propertyMap.values()) {
            if (property.isReadable() || property.isWritable()) {
              serializableProperties.add(property);
            }
          }
          Collections
              .sort(serializableProperties, new BeanPropertyComparator());

          // build the maps and return
          final Map<String, BeanProperty> keyedByFieldName = new OrderRetainingMap();
          for (BeanProperty property : serializableProperties) {
            keyedByFieldName.put(property.getName(), property);
          }

          keyedByPropertyNameCache.put(clsName, keyedByFieldName);
        }
      }
    }
    return (Map<String, BeanProperty>) keyedByPropertyNameCache.get(clsName);
  }

  private BeanProperty getBeanProperty(
      Map<PropertyKey, BeanProperty> propertyMap, Class<?> cls,
      String propertyName, Class<?> type) {
    PropertyKey key = new PropertyKey(propertyName, type);
    BeanProperty property = (BeanProperty) propertyMap.get(key);
    if (property == null) {
      property = new BeanProperty(cls, propertyName, type);
      propertyMap.put(key, property);
    }
    return property;
  }

  /**
   * Needed to avoid problems with multiple setters with the same name, but
   * referred to different types 0
   */
  private static class PropertyKey {
    private String propertyName;

    private Class<?> propertyType;

    public PropertyKey(String propertyName, Class<?> propertyType) {
      this.propertyName = propertyName;
      this.propertyType = propertyType;
    }

    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof PropertyKey))
        return false;

      final PropertyKey propertyKey = (PropertyKey) o;

      if (propertyName != null ? !propertyName.equals(propertyKey.propertyName)
          : propertyKey.propertyName != null)
        return false;
      if (propertyType != null ? !propertyType.equals(propertyKey.propertyType)
          : propertyKey.propertyType != null)
        return false;

      return true;
    }

    public int hashCode() {
      int result;
      result = (propertyName != null ? propertyName.hashCode() : 0);
      result = 29 * result
          + (propertyType != null ? propertyType.hashCode() : 0);
      return result;
    }

    public String toString() {
      return "PropertyKey{propertyName='" + propertyName + "'"
          + ", propertyType=" + propertyType + "}";
    }

  }

  /**
   * Compares properties by name
   */
  private static class BeanPropertyComparator implements
      Comparator<BeanProperty> {

    public int compare(BeanProperty o1, BeanProperty o2) {
      return o1.getName().compareTo(o2.getName());
    }

  }

  private static class OrderRetainingMap<K, V> extends HashMap<K, V> {
    private static final long serialVersionUID = 1565370254073638221L;
    private List<V> valueOrder = new ArrayList<V>();

    public V put(K key, V value) {
      valueOrder.add(value);
      return super.put(key, value);
    }

    public Collection<V> values() {
      return Collections.unmodifiableList(valueOrder);
    }
  }
}
