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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.google.inject.Inject;
import com.google.inject.Injector;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.extended.ISO8601DateConverter;
import com.thoughtworks.xstream.converters.extended.ISO8601GregorianCalendarConverter;
import com.thoughtworks.xstream.converters.extended.ISO8601SqlTimestampConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.AttributeMapper;
import com.thoughtworks.xstream.mapper.Mapper;

import org.apache.shindig.social.core.model.EnumImpl;
import org.apache.shindig.social.core.util.atom.AtomAttribute;
import org.apache.shindig.social.core.util.atom.AtomAttributeConverter;
import org.apache.shindig.social.core.util.atom.AtomContent;
import org.apache.shindig.social.core.util.atom.AtomEntry;
import org.apache.shindig.social.core.util.atom.AtomFeed;
import org.apache.shindig.social.core.util.atom.AtomKeyValue;
import org.apache.shindig.social.core.util.atom.AtomLinkConverter;
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
import org.apache.shindig.social.opensocial.spi.DataCollection;
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
  private static final Map<ConverterSet, List<ClassFieldMapping>> listElementMappingList = Maps.newHashMap();
  /**
   * Specifies a priority sorted list of Class to Element Name mappings.
   */
  private static final Map<ConverterSet, List<ClassFieldMapping>> elementMappingList = Maps.newHashMap();
  /**
   * A list of omits, the potential field is the key, and if the class which the
   * field is in, is also in the list, the field is supressed.
   */
  private static final Map<ConverterSet, Map<String, Class<?>[]>> omitMap = Maps.newHashMap();
  /**
   * Maps elements names to classes.
   */
  private static final Map<ConverterSet, Map<String, Class<?>>> elementClassMap = Maps.newHashMap();
  private static final Map<ConverterSet, List<ImplicitCollectionFieldMapping>> itemFieldMappings = Maps.newHashMap();
  private static final Map<ConverterSet, List<InterfaceFieldAliasMapping>> fieldAliasMappingList = Maps.newHashMap();
  private static final Map<String, NamespaceSet> namepaces = Maps.newHashMap();
  private static final String ATOM_NS = "http://www.w3.org/2005/Atom";
  private static final String OS_NS = "http://ns.opensocial.org/2008/opensocial";
  private static final String OSEARCH_NS = "http://a9.com/-/spec/opensearch/1.1";
  
  static {
    // configure the name space mapping. This does not need to be all the elments in the
    // namespace, just the point of translation from one namespace to another.
    // It would have been good to use a standard parser/serializer approach, but 
    // the xstream namespace implementation does not work exactly how we need it to.
    NamespaceSet atom = new NamespaceSet();
    atom.addNamespace("xmlns", ATOM_NS);
    atom.addNamespace("xmlns:osearch",OSEARCH_NS);
    atom.addPrefixedElement("totalResults","osearch:totalResults");
    atom.addPrefixedElement("startIndex","osearch:startIndex");
    atom.addPrefixedElement("itemsPerPage","osearch:itemsPerPage");
    namepaces.put("feed", atom);
    NamespaceSet os = new NamespaceSet();
    atom.addNamespace("xmlns", OS_NS);
    namepaces.put("person", os);
    namepaces.put("activity", os);
    namepaces.put("account", os);
    namepaces.put("address", os);
    namepaces.put("bodyType", os);
    namepaces.put("message", os);
    namepaces.put("mediaItem", os);
    namepaces.put("name", os);
    namepaces.put("url", os);
    namepaces.put("reponse", os);
    namepaces.put("appdata", os);

    List<ClassFieldMapping> defaultElementMappingList = Lists.newArrayList();
    // this is order specific, so put the more specified interfaces at the top.
    defaultElementMappingList.add(new ClassFieldMapping("feed",
        AtomFeed.class));
    defaultElementMappingList.add(new ClassFieldMapping("content",
        AtomContent.class));

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

    defaultElementMappingList.add(new ClassFieldMapping("ListField",
        ListField.class));

    // some standard mappings not needed for runtime, but used in test, at the
    // bottom so as not
    // to conflict with other mappings.

    defaultElementMappingList.add(new ClassFieldMapping("response",
        RestfulCollection.class));
    defaultElementMappingList.add(new ClassFieldMapping("appdata",
        DataCollection.class));
    defaultElementMappingList.add(new ClassFieldMapping("list", List.class));
    defaultElementMappingList.add(new ClassFieldMapping("map", Map.class));

    elementMappingList.put(ConverterSet.DEFAULT, defaultElementMappingList);

    // element setup for RestfullCollection Responses

    List<ClassFieldMapping> collectionElementMappingList = Lists.newArrayList();

    collectionElementMappingList.add(new ClassFieldMapping("feed",
        AtomFeed.class));
    collectionElementMappingList.add(new ClassFieldMapping("content",
        AtomContent.class));

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

    // collectionElementMappingList.add(new ClassFieldMapping("emails", "email",
    // ListField.class));
    // collectionElementMappingList.add(new ClassFieldMapping("phoneNumbers",
    // "phone", ListField.class));
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
    Map<String, Class<?>[]> defaultOmitMap = Maps.newHashMap();
    defaultOmitMap.put("isOwner", new Class[] { Person.class });
    defaultOmitMap.put("isViewer", new Class[] { Person.class });
    omitMap.put(ConverterSet.DEFAULT, defaultOmitMap);

    Map<String, Class<?>> defaultElementClassMap = Maps.newHashMap();
    defaultElementClassMap.put("feed", AtomFeed.class);
    defaultElementClassMap.put("content", AtomContent.class);
    defaultElementClassMap.put("person", Person.class);
    defaultElementClassMap.put("email", ListField.class);
    defaultElementClassMap.put("phone", ListField.class);
    defaultElementClassMap.put("list", ArrayList.class);
    defaultElementClassMap.put("map", ConcurrentHashMap.class);
    defaultElementClassMap.put("appdata", DataCollection.class);
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

    List<ImplicitCollectionFieldMapping> defaultItemFieldMappings = Lists.newArrayList();
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        AtomFeed.class, "entry", AtomEntry.class, "entry"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        AtomContent.class, "entry", AtomKeyValue.class, "entry"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "books", String.class, "books"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "cars", String.class, "cars"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "heroes", String.class, "heroes"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "food", String.class, "food"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "interests", String.class, "interests"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "languagesSpoken", String.class, "languagesSpoken"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "movies", String.class, "movies"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "music", String.class, "music"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "quotes", String.class, "quotes"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "sports", String.class, "sports"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "tags", String.class, "tags"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "turnOns", String.class, "turnOns"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "turnOffs", String.class, "turnOffs"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "tvShows", String.class, "tvShows"));

    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "emails", ListField.class, "emails"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "phoneNumbers", ListField.class, "phoneNumbers"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "ims", ListField.class, "ims"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "photos", ListField.class, "photos"));

    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "activities", Activity.class, "activities"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "addresses", Address.class, "addresses"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "organizations", Organization.class, "organizations"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "urls", Url.class, "urls"));
    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Person.class, "lookingFor", EnumImpl.class, "lookingFor"));

    defaultItemFieldMappings.add(new ImplicitCollectionFieldMapping(
        Activity.class, "mediaItems", MediaItem.class, "mediaItems"));

    itemFieldMappings.put(ConverterSet.DEFAULT, defaultItemFieldMappings);

    List<ClassFieldMapping> defaultListElementMappingList = Lists.newArrayList();
    listElementMappingList.put(ConverterSet.DEFAULT,
        defaultListElementMappingList);

    List<InterfaceFieldAliasMapping> defaultFieldAliasMappingList = Lists.newArrayList();
    // defaultFieldAliasMappingList.add(new
    // InterfaceFieldAliasMapping("address",ListField.class,"value","urls"));
    // defaultFieldAliasMappingList.add(new
    // InterfaceFieldAliasMapping("address",
    // ListField.class,"value","profileSong"));
    // defaultFieldAliasMappingList.add(new
    // InterfaceFieldAliasMapping("address",
    // ListField.class,"value","profileVideo"));


    fieldAliasMappingList.put(ConverterSet.DEFAULT,
        defaultFieldAliasMappingList);
  }

  /**
   * The Guice injector, used for creating new instances of the model.
   */
  private Injector injector;

  /**
   *
   */
  @Inject
  public XStream081Configuration(Injector injector) {
    this.injector = injector;
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
        new RestfullCollectionConverter(mapper),
        new DataCollectionConverter(mapper),
        new AtomLinkConverter()};
  }

  /**
   * @param c
   * @return
   */
  private List<ImplicitCollectionFieldMapping> getItemFieldMappings(
      ConverterSet c) {
    List<ImplicitCollectionFieldMapping> om = itemFieldMappings.get(c);
    if (om == null) {
      om = itemFieldMappings.get(ConverterSet.DEFAULT);
    }
    return om;
  }

  /**
   * @param c
   * @return
   */
  private List<InterfaceFieldAliasMapping> getFieldAliasMappingList(
      ConverterSet c) {
    List<InterfaceFieldAliasMapping> om = fieldAliasMappingList.get(c);
    if (om == null) {
      om = fieldAliasMappingList.get(ConverterSet.DEFAULT);
    }
    return om;
  }

  /**
   * {@inheritDoc}
   *
   * @param writerStack
   *
   * @see org.apache.shindig.social.core.util.xstream.XStreamConfiguration#getXStream(org.apache.shindig.social.core.util.xstream.XStreamConfiguration.ConverterSet,
   *      com.thoughtworks.xstream.converters.reflection.ReflectionProvider,
   *      com.thoughtworks.xstream.mapper.Mapper,
   *      com.thoughtworks.xstream.io.HierarchicalStreamDriver)
   */
  public ConverterConfig getConverterConfig(ConverterSet c, ReflectionProvider rp,
      Mapper dmapper, HierarchicalStreamDriver driver, WriterStack writerStack) {

    InterfaceFieldAliasingMapper emapper = new InterfaceFieldAliasingMapper(
        dmapper, writerStack, getFieldAliasMappingList(c));
    InterfaceClassMapper fmapper = new InterfaceClassMapper(writerStack, emapper,
        getElementMappingList(c), getListElementMappingList(c),
        getItemFieldMappings(c), getOmitMap(c), getElementClassMap(c));
    AttributeMapper amapper = new AttributeMapper(fmapper);

    XStream xstream = new XStream(rp, amapper, driver);
    amapper.addAttributeFor(AtomAttribute.class);
    for (Converter converter : getConverters(fmapper, c)) {
      xstream.registerConverter(converter);
    }
    xstream.registerConverter(new ISO8601DateConverter());
    xstream.registerConverter(new ISO8601GregorianCalendarConverter());
    xstream.registerConverter(new ISO8601SqlTimestampConverter());
    xstream.registerConverter(new GuiceBeanConverter(fmapper, injector));
    xstream.registerConverter(new AtomAttributeConverter());
    xstream.setMode(XStream.NO_REFERENCES);

    return new ConverterConfig(fmapper,xstream);

  }


  /**
   * {@inheritDoc}
   * @see org.apache.shindig.social.core.util.xstream.XStreamConfiguration#getNameSpaces()
   */
  public Map<String, NamespaceSet> getNameSpaces() {
    return namepaces;
  }
}
