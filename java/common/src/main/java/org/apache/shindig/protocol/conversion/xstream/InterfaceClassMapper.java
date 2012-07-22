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

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The InterfaceClassMapper provides the central mapping of the XStream bean
 * converter. It is used by XStream to determine the element names and classes
 * being mapped. This is driven by a number of read only data structures that
 * are injected on creation. The resolution of classes follow the inheritance
 * model. To map all collections to an element we would use the Collection.class
 * as the reference class and then ArrayList and Set would both be mapped to the
 * same element.
 */
public class InterfaceClassMapper extends MapperWrapper {

  /**
   * A logger.
   */
  private static final Logger LOG = Logger.getLogger(InterfaceClassMapper.class.getName());
  /**
   * A map of element names to classes.
   */
  private Map<String, Class<?>> elementClassMap = Maps.newHashMap();
  /**
   * The first child of the root object. If the root object is not a collection,
   * this is null. If the root object is a collection and all the elements are
   * the same this is set to the class of those elements. Once the first call
   * has been made to the mapper, this is set back to null. Note its a thread
   * local enabling this class to remain multi threaded.
   */
  private ThreadLocal<Class<?>> firstChild = new ThreadLocal<Class<?>>();
  /**
   * A map of classed to ommit, the key is the field name, and the value is an
   * array of classes where that field is ommitted from the output.
   */
  private Multimap<String, Class<?>> omitMMap;
  /**
   * A map of elements, where the ClassMapping object defines how the classes
   * are mapped to elements.
   */
  private List<ClassFieldMapping> elementMappingList;
  /**
   * A map of parent elements used where there is a collection as the root
   * object being serialized. This ensures that the root obejct gets the right
   * element name rather than list a generic &gt;list&lt;
   */
  private List<ClassFieldMapping> listElementMappingList;
  /**
   * An implementation of a tracking stack for the writer. If this class is to
   * be thread safe, the implementation of this field must also be thread safe
   * as it is shared over multiple threads.
   */
  private WriterStack writerStack;

  /**
   * A list of explicit mapping specifications.
   */
  private List<ImplicitCollectionFieldMapping> itemFieldMappings;

  /**
   * Create an Interface Class Mapper with a configuration.
   *
   * @param writerStack
   *          A thread safe WriterStack implementation connected to the XStream
   *          driver and hence all the writers.
   * @param wrapped
   *          the base mapper to be wrapped by this wrapper. All mappers must
   *          wrap the default mapper.
   * @param elementMappingList
   *          A list of element to class mappings specified by ClassFieldMapping
   *          Object. This list is priority ordered with the highest priority
   *          mappings coming first.
   * @param listElementMappingList
   *          A list of element names to use as the base element where there is
   *          a collection of the same type objects being serialized.
   * @param omitMMap
   *          A Multimap of fields in classes to omit from serialization.
   * @param elementClassMap
   *          a map of element names to class types.
   */
  public InterfaceClassMapper(WriterStack writerStack,
      Mapper wrapped,
      List<ClassFieldMapping> elementMappingList,
      List<ClassFieldMapping> listElementMappingList,
      List<ImplicitCollectionFieldMapping> itemFieldMappings,
      Multimap<String, Class<?>> omitMMap,
      Map<String, Class<?>> elementClassMap) {
    super(wrapped);
    this.elementClassMap = elementClassMap;
    this.elementMappingList = elementMappingList;
    this.listElementMappingList = listElementMappingList;
    this.omitMMap = omitMMap;
    this.writerStack = writerStack;
    this.itemFieldMappings = itemFieldMappings;
  }

  /**
   * Set the base object at the start of a serialization, this ensures that the
   * base element type is appropriate for the elements that are contained within
   * the object. This method only has any effect if the base object is a
   * Collection of some form. other wise this method has no effect on the state.
   * The method is thread safe attaching state to the thread for later
   * retrieval.
   *
   * @param base
   *          the base object being serialized.
   */
  public void setBaseObject(Object base) {
    firstChild.set(null);
    if (Collection.class.isAssignableFrom(base.getClass())) {
      Collection<?> c = (Collection<?>) base;
      Class<?> clazz = null;

      for (Object o : c) {
        if (clazz == null) {
          clazz = o.getClass();
        } else {
          if (!clazz.equals(o.getClass())) {
            clazz = null;
            break;
          }
        }
      }
      firstChild.set(clazz);
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("First Child set to " + clazz);
      }
    }
  }

  /**
   * <p>
   * Get the serialized element name for a specific class. If this is the first
   * object to be serialized, and it is a collection, then the elements of the
   * collection will have been inspected to determine if the container should
   * have a special name. These names are specified in the
   * listElementMappingList which is specified on construction. If the first
   * element is not found a standard list.container element name is used to
   * contain all the others, this list type is only ever used in the unit tests.
   * </p>
   * <p>
   * For subsequent elements, the class is mapped directly to a element name at
   * the same level, specified via the elementMappingList which is injected in
   * the constructor. This mapping looks to see if the class in question
   * inherits of extends the classes in the list and uses the element name
   * associated wit the first match.
   * </p>
   *
   * @see com.thoughtworks.xstream.mapper.MapperWrapper#serializedClass(java.lang.Class)
   * @param type
   *          the type of the class to the serialized
   * @return the name of the element that that should be used to contain the
   *         class when serialized.
   */
  @SuppressWarnings("unchecked")
  // the API is not generic
  @Override
  public String serializedClass(Class type) {
    String parentElementName = writerStack.peek();
    if (Collection.class.isAssignableFrom(type) && firstChild.get() != null) {
      // empty list, if this is the first one, then we need to look at the
      // first child setup on startup.
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Converting Child to " + firstChild.get());
      }
      type = firstChild.get();
      firstChild.set(null);
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("serializedClass(" + type + ") is a collection member "
            + Collection.class.isAssignableFrom(type));
      }
      for (ClassFieldMapping cfm : listElementMappingList) {
        if (cfm.matches(parentElementName, type)) {
          return cfm.getElementName();
        }
      }
      return "list.container";
    } else {
      // but after we have been asked once, then clear
      firstChild.set(null);
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("serializedClass(" + type + ')');
      }
      for (ClassFieldMapping cfm : elementMappingList) {
        if (cfm.matches(parentElementName, type)) {
          if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("From MAP serializedClass(" + type + ")  =="
                + cfm.getElementName());
          }
          return cfm.getElementName();
        }
      }

    }

    String fieldName = super.serializedClass(type);
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("--- From Super serializedClass(" + type + ")  ==" + fieldName);
    }
    return fieldName;

  }

  /**
   * Checks to see if the field in a class should be serialized. This is
   * controlled buy the omitMMap Multimap which is keyed by the field name. Each entry
   * in the map contains a list of classes where the field name should be
   * excluded from the output.
   *
   * @param definedIn
   *          the class the field is defined in
   * @param fieldName
   *          the field being considered
   * @return true of the field should be serialized false if it should be
   *         ignored.
   * @see com.thoughtworks.xstream.mapper.MapperWrapper#shouldSerializeMember(java.lang.Class,
   *      java.lang.String)
   *
   */
  @SuppressWarnings("unchecked")
  // API is not generic
  @Override
  public boolean shouldSerializeMember(Class definedIn, String fieldName) {
    for (Class<?> omit : omitMMap.get(fieldName)) {
      if (omit.isAssignableFrom(definedIn)) {
        return false;
      }
    }
    return super.shouldSerializeMember(definedIn, fieldName);
  }

  /**
   * Get the real class associated with an element name from the
   * elementMappingList.
   *
   * @param elementName
   *          the name of the element being read.
   * @see com.thoughtworks.xstream.mapper.MapperWrapper#realClass(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public Class realClass(String elementName) {
    Class<?> clazz = elementClassMap.get(elementName);
    if (clazz == null) {
      clazz = super.realClass(elementName);
    }
    return clazz;
  }



  /**
   * {@inheritDoc}
   * @see com.thoughtworks.xstream.mapper.MapperWrapper#getImplicitCollectionDefForFieldName(java.lang.Class, java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public ImplicitCollectionMapping getImplicitCollectionDefForFieldName(Class itemType, String fieldName) {
    for ( ImplicitCollectionFieldMapping ifm : itemFieldMappings) {
      if ( ifm.matches(itemType, fieldName) ) {
        return ifm;
      }
    }
    return super.getImplicitCollectionDefForFieldName(itemType, fieldName);
  }
}
