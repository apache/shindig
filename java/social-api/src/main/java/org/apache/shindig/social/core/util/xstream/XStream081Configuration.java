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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.Mapper;

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
import org.apache.shindig.social.opensocial.spi.RestfulCollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class XStream081Configuration implements XStreamConfiguration {

  /**
   * Defines the type of the list container when at the top level where there
   * are no methods to specify the name of the list.
   */
  private static final Map<ConverterSet, List<ClassFieldMapping>> listElementMappingList = new HashMap<ConverterSet, List<ClassFieldMapping>>();
  /**
   * Specifies a priority sorted list of Class to Element Name mappings.
   */
  private static final Map<ConverterSet, List<ClassFieldMapping>> elementMappingList = new HashMap<ConverterSet, List<ClassFieldMapping>>();
  /**
   * A list of omits, the potential field is the key, and if the class which the
   * field is in, is also in the list, the field is supressed.
   */
  private static final Map<ConverterSet, Map<String, Class<?>[]>> omitMap = new HashMap<ConverterSet, Map<String, Class<?>[]>>();
  /**
   * Maps elements names to classes.
   */
  private static final Map<ConverterSet, Map<String, Class<?>>> elementClassMap = new HashMap<ConverterSet, Map<String, Class<?>>>();
  private static final Map<ConverterSet, List<ItemFieldMapping>> itemFieldMappings = new HashMap<ConverterSet, List<ItemFieldMapping>>();
  static {
    List<ClassFieldMapping> defaultElementMappingList = new ArrayList<ClassFieldMapping>();
    // this is order specific, so put the more specified interfaces at the top.
    defaultElementMappingList.add(new ClassFieldMapping("activity",
        Activity.class));
    defaultElementMappingList.add(new ClassFieldMapping("account",
        Account.class));
    defaultElementMappingList.add(new ClassFieldMapping("address",
        Address.class));
    defaultElementMappingList.add(new ClassFieldMapping("bodyType",
        BodyType.class));
    defaultElementMappingList.add(new ClassFieldMapping("message",
        Message.class));
    defaultElementMappingList.add(new ClassFieldMapping("mediaItem",
        MediaItem.class));
    defaultElementMappingList.add(new ClassFieldMapping("name", Name.class));
    defaultElementMappingList.add(new ClassFieldMapping("organization",
        Organization.class));
    defaultElementMappingList
        .add(new ClassFieldMapping("person", Person.class));
    defaultElementMappingList.add(new ClassFieldMapping("url", Url.class));
    // this is an example of a class field mapping with context. If
    // ListField is mapped inside an element named emails, replace the element
    // name
    // that would have been defiend as fqcn ListField with email

    defaultElementMappingList.add(new ClassFieldMapping("emails", "email",
        ListField.class));
    defaultElementMappingList.add(new ClassFieldMapping("phoneNumbers",
        "phone", ListField.class));
    defaultElementMappingList.add(new ClassFieldMapping("ListField",
        ListField.class));

    // some standard mappings not needed for runtime, but used in test, at the
    // bottom so as not
    // to conflict with other mappings.

    defaultElementMappingList.add(new ClassFieldMapping("response",
        RestfulCollection.class));
    defaultElementMappingList.add(new ClassFieldMapping("list", List.class));
    defaultElementMappingList.add(new ClassFieldMapping("map", Map.class));

    elementMappingList.put(ConverterSet.DEFAULT, defaultElementMappingList);

    // element setup for RestfullCollection Responses

    List<ClassFieldMapping> collectionElementMappingList = new ArrayList<ClassFieldMapping>();

    collectionElementMappingList.add(new ClassFieldMapping("activity",
        Activity.class));
    collectionElementMappingList.add(new ClassFieldMapping("account",
        Account.class));
    collectionElementMappingList.add(new ClassFieldMapping("address",
        Address.class));
    collectionElementMappingList.add(new ClassFieldMapping("bodyType",
        BodyType.class));
    collectionElementMappingList.add(new ClassFieldMapping("message",
        Message.class));
    collectionElementMappingList.add(new ClassFieldMapping("mediaItem",
        MediaItem.class));
    collectionElementMappingList.add(new ClassFieldMapping("name", Name.class));
    collectionElementMappingList.add(new ClassFieldMapping("organization",
        Organization.class));
    collectionElementMappingList.add(new ClassFieldMapping("person",
        Person.class));
    collectionElementMappingList.add(new ClassFieldMapping("url", Url.class));
    // this is an example of a class field mapping with context. If
    // ListField is mapped inside an element named emails, replace the element
    // name
    // that would have been defiend as fqcn ListField with email

//    collectionElementMappingList.add(new ClassFieldMapping("emails", "email",
//        ListField.class));
//    collectionElementMappingList.add(new ClassFieldMapping("phoneNumbers",
//        "phone", ListField.class));
    collectionElementMappingList.add(new ClassFieldMapping("ListField",
        ListField.class));

    // some standard mappings not needed for runtime, but used in test, at the
    // bottom so as not
    // to conflict with other mappings.

    collectionElementMappingList.add(new ClassFieldMapping("response",
        RestfulCollection.class));
    collectionElementMappingList.add(new ClassFieldMapping("list", List.class));
    collectionElementMappingList.add(new ClassFieldMapping("map", Map.class));

    elementMappingList.put(ConverterSet.COLLECTION,
        collectionElementMappingList);

    // ignore these fields in the specified classes.
    // Entries here might indicate a hole in the spec.
    // This not in the XSD for 81, but is in the wording of the spec.
    // omitMap.put("addresses", new Class[] { Person.class });
    Map<String, Class<?>[]> defaultOmitMap = new HashMap<String, Class<?>[]>();
    defaultOmitMap.put("isOwner", new Class[] { Person.class });
    defaultOmitMap.put("isViewer", new Class[] { Person.class });
    omitMap.put(ConverterSet.DEFAULT, defaultOmitMap);

    Map<String, Class<?>> defaultElementClassMap = new HashMap<String, Class<?>>();
    defaultElementClassMap.put("person", Person.class);
    defaultElementClassMap.put("email", ListField.class);
    defaultElementClassMap.put("phone", ListField.class);
    defaultElementClassMap.put("list", ArrayList.class);
    defaultElementClassMap.put("map", ConcurrentHashMap.class);
    defaultElementClassMap.put("appdata", ConcurrentHashMap.class);
    defaultElementClassMap.put("activity", Activity.class);
    defaultElementClassMap.put("account", Account.class);
    defaultElementClassMap.put("address", Address.class);
    defaultElementClassMap.put("bodyType", BodyType.class);
    defaultElementClassMap.put("message", Message.class);
    defaultElementClassMap.put("mediaItem", MediaItem.class);
    defaultElementClassMap.put("name", Name.class);
    defaultElementClassMap.put("organization", Organization.class);
    defaultElementClassMap.put("person", Person.class);
    defaultElementClassMap.put("url", Url.class);
    defaultElementClassMap.put("listField", ListField.class);
    elementClassMap.put(ConverterSet.DEFAULT, defaultElementClassMap);

    List<ItemFieldMapping> defaultItemFieldMappings = new ArrayList<ItemFieldMapping>();
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "books",
        String.class, "books"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "cars",
        String.class, "cars"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "heroes",
        String.class, "heroes"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "food",
        String.class, "food"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "interests",
        String.class, "interests"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "languagesSpoken",
        String.class, "languagesSpoken"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "movies",
        String.class, "movies"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "music",
        String.class, "music"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "quotes",
        String.class, "quotes"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "sports",
        String.class, "sports"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "tags",
        String.class, "tags"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "turnOns",
        String.class, "turnOns"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "turnOffs",
        String.class, "turnOffs"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "tvShows",
        String.class, "tvShows"));
    
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "emails",
        ListField.class, "emails"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "phoneNumbers",
        ListField.class, "phoneNumbers"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "ims",
        ListField.class, "ims"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "photos",
        ListField.class, "photos"));
    
    
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "activities",
        Activity.class, "activities"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "addresses",
        Address.class, "addresses"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "organizations",
        Organization.class, "organizations"));
    defaultItemFieldMappings.add(new ItemFieldMapping(Person.class, "urls",
        Url.class, "urls"));
    
    
    defaultItemFieldMappings.add(new ItemFieldMapping(Activity.class, "mediaItems",
        MediaItem.class, "mediaItems"));
    
    
    itemFieldMappings.put(ConverterSet.DEFAULT, defaultItemFieldMappings);
    
    
    
    List<ClassFieldMapping> defaultListElementMappingList = new ArrayList<ClassFieldMapping>();
    listElementMappingList.put(ConverterSet.DEFAULT, defaultListElementMappingList);
  }

  private Map<String, Class<?>> getElementClassMap(ConverterSet c) {
    Map<String, Class<?>> ecm = elementClassMap.get(c);
    if (ecm == null) {
      ecm = elementClassMap.get(ConverterSet.DEFAULT);
    }
    return ecm;
  }

  private List<ClassFieldMapping> getElementMappingList(ConverterSet c) {
    List<ClassFieldMapping> eml = elementMappingList.get(c);
    if (eml == null) {
      eml = elementMappingList.get(ConverterSet.DEFAULT);
    }
    return eml;
  }

  private List<ClassFieldMapping> getListElementMappingList(ConverterSet c) {
    List<ClassFieldMapping> leml = listElementMappingList.get(c);
    if (leml == null) {
      leml = listElementMappingList.get(ConverterSet.DEFAULT);
    }
    return leml;
  }

  private Map<String, Class<?>[]> getOmitMap(ConverterSet c) {
    Map<String, Class<?>[]> om = omitMap.get(c);
    if (om == null) {
      om = omitMap.get(ConverterSet.DEFAULT);
    }
    return om;
  }

  private Converter[] getConverters(Mapper mapper, ConverterSet c) {
    return new Converter[] { new MapConverter(mapper),
        new RestfullCollectionConverter(mapper) };
  }

  /**
   * @param c
   * @return
   */
  private List<ItemFieldMapping> getItemFieldMappings(ConverterSet c) {
    List<ItemFieldMapping> om = itemFieldMappings.get(c);
    if (om == null) {
      om = itemFieldMappings.get(ConverterSet.DEFAULT);
    }
    return om;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.shindig.social.core.util.xstream.XStreamConfiguration#getMapper(org.apache.shindig.social.core.util.xstream.XStreamConfiguration.ConverterSet,
   *      org.apache.shindig.social.core.util.xstream.WriterStack)
   */
  public InterfaceClassMapper getMapper(ConverterSet c, Mapper dmapper,
      WriterStack writerStack) {
    return new InterfaceClassMapper(writerStack, dmapper,
        getElementMappingList(c), getListElementMappingList(c),
        getItemFieldMappings(c), getOmitMap(c), getElementClassMap(c));
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.shindig.social.core.util.xstream.XStreamConfiguration#getXStream(org.apache.shindig.social.core.util.xstream.XStreamConfiguration.ConverterSet,
   *      com.thoughtworks.xstream.converters.reflection.ReflectionProvider,
   *      com.thoughtworks.xstream.mapper.Mapper,
   *      com.thoughtworks.xstream.io.HierarchicalStreamDriver)
   */
  public XStream getXStream(ConverterSet c, ReflectionProvider rp,
      Mapper mapper, HierarchicalStreamDriver driver) {
    XStream xstream = new XStream(rp, mapper, driver);
    for (Converter converter : getConverters(mapper, c)) {
      xstream.registerConverter(converter);
    }

    xstream.setMode(XStream.NO_REFERENCES);
    return xstream;
  }
}
