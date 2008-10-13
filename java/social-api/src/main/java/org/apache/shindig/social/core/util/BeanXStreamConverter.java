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
package org.apache.shindig.social.core.util;

import com.google.inject.Inject;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.mapper.DefaultMapper;
import com.thoughtworks.xstream.mapper.Mapper;

import org.apache.shindig.social.core.util.xstream.ClassFieldMapping;
import org.apache.shindig.social.core.util.xstream.InterfaceClassMapper;
import org.apache.shindig.social.core.util.xstream.MapConverter;
import org.apache.shindig.social.core.util.xstream.StackDriver;
import org.apache.shindig.social.core.util.xstream.ThreadSafeWriterStack;
import org.apache.shindig.social.core.util.xstream.WriterStack;
import org.apache.shindig.social.opensocial.model.Account;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.BodyType;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.Message;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Url;
import org.apache.shindig.social.opensocial.service.BeanConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeanXStreamConverter implements BeanConverter {
  /**
   * Defines the type of the list container when at the top level where there
   * are no methods to specify the name of the list.
   */
  private static final List<ClassFieldMapping> listElementMappingList = new ArrayList<ClassFieldMapping>();
  /**
   * Specifies a priority sorted list of Class to Element Name mappings.
   */
  private static final List<ClassFieldMapping> elementMappingList = new ArrayList<ClassFieldMapping>();
  /**
   * A list of omits, the potential field is the key, and if the class which the
   * field is in, is also in the list, the field is supressed.
   */
  private static final Map<String, Class<?>[]> omitMap = new HashMap<String, Class<?>[]>();
  /**
   * Maps elements names to classes.
   */
  private static final Map<String, Class<?>> elementClassMap = new HashMap<String, Class<?>>();
  static {

    // this is order specific, so put the more specified interfaces at the top.
    elementMappingList.add(new ClassFieldMapping("Activity", Activity.class));
    elementMappingList.add(new ClassFieldMapping("Account", Account.class));
    elementMappingList.add(new ClassFieldMapping("Address", Address.class));
    elementMappingList.add(new ClassFieldMapping("BodyType", BodyType.class));
    elementMappingList.add(new ClassFieldMapping("Message", Message.class));
    elementMappingList.add(new ClassFieldMapping("MediaItem", MediaItem.class));
    elementMappingList.add(new ClassFieldMapping("Name", Name.class));
    elementMappingList.add(new ClassFieldMapping("Organization",
        Organization.class));
    elementMappingList.add(new ClassFieldMapping("person", Person.class));
    elementMappingList.add(new ClassFieldMapping("Url", Url.class));
    // this is an example of a class field mapping with context. If
    // ListField is mapped inside an element named emails, replace the element
    // name
    // that would have been defiend as fqcn ListField with email
    elementMappingList.add(new ClassFieldMapping("emails", "email",
        ListField.class));
    elementMappingList.add(new ClassFieldMapping("phoneNumbers", "phone",
        ListField.class));
    elementMappingList.add(new ClassFieldMapping("ListField", ListField.class));

    // some standard mappings not needed for runtime, but used in test, at the
    // bottom so as not
    // to conflict with other mappings.
    elementMappingList.add(new ClassFieldMapping("list", List.class));
    elementMappingList.add(new ClassFieldMapping("map", Map.class));

    // ignore these fields in the specified classes.
    // Entries here might indicate a hole in the spec.
    omitMap.put("addresses", new Class[] { Person.class });
    omitMap.put("isOwner", new Class[] { Person.class });
    omitMap.put("isViewer", new Class[] { Person.class });

    elementClassMap.put("person", Person.class);
    elementClassMap.put("email", ListField.class);
    elementClassMap.put("phone", ListField.class);
    elementClassMap.put("list", ArrayList.class);
    elementClassMap.put("map", ConcurrentHashMap.class);
    elementClassMap.put("appdata", ConcurrentHashMap.class);
    elementClassMap.put("Activity", Activity.class);
    elementClassMap.put("Account", Account.class);
    elementClassMap.put("Address", Address.class);
    elementClassMap.put("BodyType", BodyType.class);
    elementClassMap.put("Message", Message.class);
    elementClassMap.put("MediaItem", MediaItem.class);
    elementClassMap.put("Name", Name.class);
    elementClassMap.put("Organization", Organization.class);
    elementClassMap.put("person", Person.class);
    elementClassMap.put("Url", Url.class);
    elementClassMap.put("ListField", ListField.class);

  }
  private static Log log = LogFactory.getLog(BeanXStreamConverter.class);
  private Converter mapConverter;
  private InterfaceClassMapper icmapper;
  private ReflectionProvider rp;
  private HierarchicalStreamDriver driver;
  private WriterStack writerStack;

  @Inject
  public BeanXStreamConverter() {
    rp = new PureJavaReflectionProvider();
    Mapper dmapper = new DefaultMapper(this.getClass().getClassLoader());
    /*
     * Putting this here means only one conversion per thread may be active at
     * any one time, but since the conversion process is atomic this will not
     * matter unless the class is extended.
     */
    writerStack = new ThreadSafeWriterStack();
    /*
     * create a driver that wires into a standard driver, and updates the stack
     * position.
     */
    driver = new StackDriver(new XppDriver(), writerStack);
    /*
     * Create an interface class mapper that understands class hierarchy
     */
    icmapper = new InterfaceClassMapper(writerStack, dmapper,
        elementMappingList, listElementMappingList, omitMap, elementClassMap);
    /*
     * Create a map converter to ensure that maps are converted in the
     * compressed form.
     */
    mapConverter = new MapConverter(icmapper);
  }

  public String convertToString(Object pojo) {
    return convertToXml(pojo);
  }

  /**
   * convert an Object to XML, but make certain that only one of these is run on
   * a thread at any one time. This only matters if this class is extended.
   * 
   * @param obj
   * @return
   */
  public String convertToXml(Object obj) {

    XStream xstream = new XStream(rp, icmapper, driver);
    xstream.registerConverter(mapConverter);
    xstream.setMode(XStream.NO_REFERENCES);
    writerStack.reset();
    icmapper.setBaseObject(obj); // thread safe method

    String result = xstream.toXML(obj);
    log.debug("Result is " + result);
    return "<response>" + result + "</response>";
  }

  public <T> T convertToObject(String xml, Class<T> className) {
    XStream xstream = new XStream(rp, icmapper, driver);
    xstream.registerConverter(mapConverter);
    xstream.setMode(XStream.NO_REFERENCES);
    return (T) xstream.fromXML(xml);
  }

}
