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

import org.apache.shindig.protocol.model.Exportablebean;

import com.google.inject.Injector;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.Collection;

/**
 * Bean converter that uses Guice bindings to correctly convert
 */
public class GuiceBeanConverter implements Converter {
  private Mapper mapper;
  private GuiceBeanProvider beanProvider;

  public GuiceBeanConverter(Mapper mapper, Injector injector) {
    this(mapper, new GuiceBeanProvider(injector));
  }

  public GuiceBeanConverter(Mapper mapper, GuiceBeanProvider beanProvider) {
    this.mapper = mapper;
    this.beanProvider = beanProvider;
  }

  /**
   * Only checks for the availability of a public default constructor. If you
   * need stricter checks, subclass JavaBeanConverter
   */
  // Base API is inherently unchecked

  public boolean canConvert(Class type) {
    while (true) {
      if (type == null) {
        return false;
      }
      if (Object.class.equals(type)) {
        return false;
      }
      if (type.isInterface()) {
        return true;
      }
      for (Class<?> iff : type.getInterfaces()) {
        if (iff.isAnnotationPresent(Exportablebean.class)) {
          return true;
        }
      }
      type = type.getSuperclass();
    }
  }

  public void marshal(final Object source,
      final HierarchicalStreamWriter writer, final MarshallingContext context) {
    beanProvider.visitSerializableProperties(source,
        new GuiceBeanProvider.Visitor() {
          public boolean shouldVisit(String name, Class<?> definedIn) {
            return mapper.shouldSerializeMember(definedIn, name);
          }

          public void visit(String propertyName, Class<?> fieldType,
              Class<?> definedIn, Object newObj) {
            if (newObj != null) {
              Mapper.ImplicitCollectionMapping mapping = mapper
                  .getImplicitCollectionDefForFieldName(source.getClass(),
                      propertyName);
              if (mapping != null) {
                if (mapping.getItemFieldName() != null) {
                  Collection<?> list = (Collection<?>) newObj;
                  for (Object obj : list) {
                    writeField(propertyName, mapping.getItemFieldName(),
                        mapping.getItemType(), definedIn, obj);
                  }
                } else {
                  context.convertAnother(newObj);
                }
              } else {
                writeField(propertyName, propertyName, fieldType, definedIn,
                    newObj);
              }
            }
          }

          private void writeField(String propertyName, String aliasName,
              Class<?> fieldType, Class<?> definedIn, Object newObj) {
            ExtendedHierarchicalStreamWriterHelper.startNode(writer, mapper
                .serializedMember(source.getClass(), aliasName), fieldType);
            context.convertAnother(newObj);
            writer.endNode();

          }
        });
  }

  public Object unmarshal(final HierarchicalStreamReader reader,
      final UnmarshallingContext context) {
    final Object result = instantiateNewInstance(context);

    while (reader.hasMoreChildren()) {
      reader.moveDown();

      String propertyName = mapper.realMember(result.getClass(), reader
          .getNodeName());

      boolean propertyExistsInClass = beanProvider.propertyDefinedInClass(
          propertyName, result.getClass());

      if (propertyExistsInClass) {
        Class<?> type = determineType(reader, result, propertyName);
        Object value = context.convertAnother(result, type);
        beanProvider.writeProperty(result, propertyName, value);
      } else if (mapper.shouldSerializeMember(result.getClass(), propertyName)) {
        throw new ConversionException("Property '" + propertyName
            + "' not defined in class " + result.getClass().getName());
      }

      reader.moveUp();
    }

    return result;
  }

  private Object instantiateNewInstance(UnmarshallingContext context) {
    Object result = context.currentObject();
    if (result == null) {
      result = beanProvider.newInstance(context.getRequiredType());
    }
    return result;
  }

  private Class<?> determineType(HierarchicalStreamReader reader,
      Object result, String fieldName) {
    final String classAttributeName = mapper.attributeForAlias("class");
    String classAttribute = reader.getAttribute(classAttributeName);
    if (classAttribute != null) {
      return mapper.realClass(classAttribute);
    } else {
      return mapper.defaultImplementationOf(beanProvider.getPropertyType(
          result, fieldName));
    }
  }
}
