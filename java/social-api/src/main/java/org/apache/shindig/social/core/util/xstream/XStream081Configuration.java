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
package org.apache.shindig.social.core.util.xstream;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.core.DefaultConverterLookup;
import org.apache.shindig.protocol.DataCollection;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.protocol.conversion.xstream.ClassFieldMapping;
import org.apache.shindig.protocol.conversion.xstream.DataCollectionConverter;
import org.apache.shindig.protocol.conversion.xstream.ExtendableBeanConverter;
import org.apache.shindig.protocol.conversion.xstream.GuiceBeanConverter;
import org.apache.shindig.protocol.conversion.xstream.ImplicitCollectionFieldMapping;
import org.apache.shindig.protocol.conversion.xstream.InterfaceClassMapper;
import org.apache.shindig.protocol.conversion.xstream.InterfaceFieldAliasMapping;
import org.apache.shindig.protocol.conversion.xstream.InterfaceFieldAliasingMapper;
import org.apache.shindig.protocol.conversion.xstream.MapConverter;
import org.apache.shindig.protocol.conversion.xstream.NamespaceSet;
import org.apache.shindig.protocol.conversion.xstream.RestfullCollectionConverter;
import org.apache.shindig.protocol.conversion.xstream.WriterStack;
import org.apache.shindig.protocol.conversion.xstream.XStreamConfiguration;
import org.apache.shindig.protocol.model.EnumImpl;
import org.apache.shindig.protocol.model.ExtendableBean;
import org.apache.shindig.social.core.util.atom.AtomAttribute;
import org.apache.shindig.social.core.util.atom.AtomAttributeConverter;
import org.apache.shindig.social.core.util.atom.AtomContent;
import org.apache.shindig.social.core.util.atom.AtomEntry;
import org.apache.shindig.social.core.util.atom.AtomFeed;
import org.apache.shindig.social.core.util.atom.AtomKeyValue;
import org.apache.shindig.social.core.util.atom.AtomLinkConverter;
import org.apache.shindig.social.core.util.atom.AtomSummaryConverter;
import org.apache.shindig.social.opensocial.model.Account;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.ActivityEntry;
import org.apache.shindig.social.opensocial.model.ActivityObject;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.BodyType;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.MediaLink;
import org.apache.shindig.social.opensocial.model.Message;
import org.apache.shindig.social.opensocial.model.MessageCollection;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Url;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.extended.ISO8601DateConverter;
import com.thoughtworks.xstream.converters.extended.ISO8601GregorianCalendarConverter;
import com.thoughtworks.xstream.converters.extended.ISO8601SqlTimestampConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.AttributeMapper;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * Opensocial 0.81 compliant Xstream binding
 */
public class XStream081Configuration implements XStreamConfiguration {

  /**
   * Defines the type of the list container when at the top level where there
   * are no methods to specify the name of the list.
   */
  private static final Map<ConverterSet, List<ClassFieldMapping>> listElementMappingList =
      DefaultedEnumMap.init(ConverterSet.class,  ConverterSet.DEFAULT);

  /**
   * Specifies a priority sorted list of Class to Element Name mappings.
   */
  private static final Map<ConverterSet, List<ClassFieldMapping>> elementMappingList =
      DefaultedEnumMap.init(ConverterSet.class, ConverterSet.DEFAULT);

  /**
   * A list of omits, the potential field is the key, and if the class which the
   * field is in, is also in the list, the field is supressed.
   */
  private static final Map<ConverterSet, ImmutableMultimap<String, Class<?>>> omitMap = ImmutableMap.of(ConverterSet.DEFAULT,
      ImmutableMultimap.<String,Class<?>>builder()
          .put("isOwner", Person.class)
          .put("isViewer", Person.class).build());

  /**
   * Maps elements names to classes.
   */
  private static final Map<ConverterSet, Map<String, Class<?>>> elementClassMap = DefaultedEnumMap.init(ConverterSet.class,  ConverterSet.DEFAULT);
  private static final Map<ConverterSet, List<ImplicitCollectionFieldMapping>> itemFieldMappings = DefaultedEnumMap.init(ConverterSet.class, ConverterSet.DEFAULT);
  private static final Map<ConverterSet, List<InterfaceFieldAliasMapping>> fieldAliasMappingList = DefaultedEnumMap.init(ConverterSet.class, ConverterSet.DEFAULT);

  private static final String ATOM_NS = "http://www.w3.org/2005/Atom";
  private static final String OS_NS = "http://ns.opensocial.org/2008/opensocial";
  private static final String OSEARCH_NS = "http://a9.com/-/spec/opensearch/1.1";


  private static final Map<String, NamespaceSet> NAMESPACES = initNameSpace();

  private static Map<String, NamespaceSet> initNameSpace() {
    // configure the name space mapping. This does not need to be all the elments in the
    // namespace, just the point of translation from one namespace to another.
    // It would have been good to use a standard parser/serializer approach, but
    // the xstream namespace implementation does not work exactly how we need it to.
    NamespaceSet atom = new NamespaceSet();
    atom.addNamespace("xmlns", ATOM_NS);
    atom.addNamespace("xmlns:osearch", OSEARCH_NS);
    atom.addPrefixedElement("totalResults", "osearch:totalResults");
    atom.addPrefixedElement("startIndex", "osearch:startIndex");
    atom.addPrefixedElement("itemsPerPage", "osearch:itemsPerPage");

    NamespaceSet os = new NamespaceSet();
    os.addNamespace("xmlns", OS_NS);

    return ImmutableMap.<String, NamespaceSet>builder()
        .put("feed", atom)
        .put("person", os)
        .put("activity", os)
        .put("activityEntry", os)
        .put("account", os)
        .put("address", os)
        .put("bodyType", os)
        .put("message", os)
        .put("mediaItem", os)
        .put("url", os)
        .put("response", os)
        .put("appdata", os)
        .build();
  }

  static {
    elementMappingList.put(ConverterSet.DEFAULT, ImmutableList.of(
        // this is order specific, so put the more specified interfaces at the top.
        new ClassFieldMapping("feed", AtomFeed.class),
        new ClassFieldMapping("content", AtomContent.class),
        new ClassFieldMapping("entry", AtomEntry.class),

        new ClassFieldMapping("activity", Activity.class),
        new ClassFieldMapping("activityEntry", ActivityEntry.class),
        new ClassFieldMapping("object", ActivityObject.class),
        new ClassFieldMapping("mediaLink", MediaLink.class),
        new ClassFieldMapping("account", Account.class),
        new ClassFieldMapping("address", Address.class),
        new ClassFieldMapping("bodyType", BodyType.class),
        new ClassFieldMapping("message", Message.class),
        new ClassFieldMapping("messageCollection", MessageCollection.class),
        new ClassFieldMapping("mediaItem", MediaItem.class),
        new ClassFieldMapping("name", Name.class),
        new ClassFieldMapping("organization", Organization.class),
        new ClassFieldMapping("person", Person.class),
        new ClassFieldMapping("url", Url.class),
        new ClassFieldMapping("openSocial", ExtendableBean.class),
        // this is an example of a class field mapping with context. If
        // ListField is mapped inside an element named emails, replace the element
        // name
        // that would have been defiend as fqcn ListField with email

        new ClassFieldMapping("ListField", ListField.class),

        // some standard mappings not needed for runtime, but used in test, at the
        // bottom so as not
        // to conflict with other mappings.

        new ClassFieldMapping("response", RestfulCollection.class),
        new ClassFieldMapping("appdata", DataCollection.class),
        new ClassFieldMapping("list", List.class),
        new ClassFieldMapping("map", Map.class))
    );

    // element setup for RestfullCollection Responses

    elementMappingList.put(ConverterSet.COLLECTION, ImmutableList.of(
        new ClassFieldMapping("feed", AtomFeed.class),
        new ClassFieldMapping("content", AtomContent.class),
        new ClassFieldMapping("entry", AtomEntry.class),

        new ClassFieldMapping("activity", Activity.class),
        new ClassFieldMapping("activityEntry", ActivityEntry.class),
        new ClassFieldMapping("object", ActivityObject.class),
        new ClassFieldMapping("mediaLink", MediaLink.class),
        new ClassFieldMapping("account", Account.class),
        new ClassFieldMapping("address", Address.class),
        new ClassFieldMapping("bodyType", BodyType.class),
        new ClassFieldMapping("message", Message.class),
        new ClassFieldMapping("messageCollection", MessageCollection.class),
        new ClassFieldMapping("mediaItem", MediaItem.class),
        new ClassFieldMapping("name", Name.class),
        new ClassFieldMapping("organization", Organization.class),
        new ClassFieldMapping("person", Person.class),
        new ClassFieldMapping("url", Url.class),
        new ClassFieldMapping("openSocial", ExtendableBean.class),
        // this is an example of a class field mapping with context. If
        // ListField is mapped inside an element named emails, replace the element
        // name that would have been defiend as fqcn ListField with email

        //     new ClassFieldMapping("emails", "email", ListField.class),
        //     new ClassFieldMapping("phoneNumbers","phone", ListField.class),
        new ClassFieldMapping("ListField", ListField.class),

        // some standard mappings not needed for runtime, but used in test, at the
        // bottom so as not to conflict with other mappings.

        new ClassFieldMapping("response", RestfulCollection.class),
        new ClassFieldMapping("list", List.class),
        new ClassFieldMapping("map", Map.class))
    );

    elementClassMap.put(ConverterSet.DEFAULT, new ImmutableMap.Builder<String, Class<?>>()
        .put("feed", AtomFeed.class)
        .put("content", AtomContent.class)
        .put("entry", AtomEntry.class)
        .put("email", ListField.class)
        .put("phone", ListField.class)
        .put("list", ArrayList.class)
        .put("map", ConcurrentHashMap.class)
        .put("appdata", DataCollection.class)
        .put("activity", Activity.class)
        .put("activityEntry", ActivityEntry.class)
        .put("object", ActivityObject.class)
        .put("openSocial", ExtendableBean.class)
        .put("mediaLink", MediaLink.class)
        .put("account", Account.class)
        .put("address", Address.class)
        .put("bodyType", BodyType.class)
        .put("message", Message.class)
        .put("messageCollection", MessageCollection.class)
        .put("mediaItem", MediaItem.class)
        .put("name", Name.class)
        .put("organization", Organization.class)
        .put("person", Person.class)
        .put("url", Url.class)
        .put("listField", ListField.class).build()
    );


    itemFieldMappings.put(ConverterSet.DEFAULT, ImmutableList.of(
        new ImplicitCollectionFieldMapping(AtomFeed.class, "entry", AtomEntry.class, "entry"),
        new ImplicitCollectionFieldMapping(AtomContent.class, "entry", AtomKeyValue.class, "entry"),
        new ImplicitCollectionFieldMapping(Person.class, "books", String.class, "books"),
        new ImplicitCollectionFieldMapping(Person.class, "cars", String.class, "cars"),
        new ImplicitCollectionFieldMapping(Person.class, "heroes", String.class, "heroes"),
        new ImplicitCollectionFieldMapping(Person.class, "food", String.class, "food"),
        new ImplicitCollectionFieldMapping(Person.class, "interests", String.class, "interests"),
        new ImplicitCollectionFieldMapping(Person.class, "languagesSpoken", String.class, "languagesSpoken"),
        new ImplicitCollectionFieldMapping(Person.class, "movies", String.class, "movies"),
        new ImplicitCollectionFieldMapping(Person.class, "music", String.class, "music"),
        new ImplicitCollectionFieldMapping(Person.class, "quotes", String.class, "quotes"),
        new ImplicitCollectionFieldMapping(Person.class, "sports", String.class, "sports"),
        new ImplicitCollectionFieldMapping(Person.class, "tags", String.class, "tags"),
        new ImplicitCollectionFieldMapping(Person.class, "turnOns", String.class, "turnOns"),
        new ImplicitCollectionFieldMapping(Person.class, "turnOffs", String.class, "turnOffs"),
        new ImplicitCollectionFieldMapping(Person.class, "tvShows", String.class, "tvShows"),

        new ImplicitCollectionFieldMapping(Person.class, "emails", ListField.class, "emails"),
        new ImplicitCollectionFieldMapping(Person.class, "phoneNumbers", ListField.class, "phoneNumbers"),
        new ImplicitCollectionFieldMapping(Person.class, "ims", ListField.class, "ims"),
        new ImplicitCollectionFieldMapping(Person.class, "photos", ListField.class, "photos"),

        new ImplicitCollectionFieldMapping(Person.class, "activities", Activity.class, "activities"),
        new ImplicitCollectionFieldMapping(Person.class, "addresses", Address.class, "addresses"),
        new ImplicitCollectionFieldMapping(Person.class, "organizations", Organization.class, "organizations"),
        new ImplicitCollectionFieldMapping(Person.class, "urls", Url.class, "urls"),
        new ImplicitCollectionFieldMapping(Person.class, "lookingFor", EnumImpl.class, "lookingFor"),

        new ImplicitCollectionFieldMapping(Message.class, "recipients", String.class, "recipients"),
        new ImplicitCollectionFieldMapping(Message.class, "collectionIds", String.class, "collectionsIds"),
        new ImplicitCollectionFieldMapping(Message.class, "replies", String.class, "replies"),

        new ImplicitCollectionFieldMapping(ActivityObject.class, "downstreamDuplicates", String.class, "downstreamDuplicate"),
        new ImplicitCollectionFieldMapping(ActivityObject.class, "upstreamDuplicates", String.class, "upstreamDuplicate"),

        new ImplicitCollectionFieldMapping(Activity.class, "mediaItems", MediaItem.class, "mediaItems"))
    );

    listElementMappingList.put(ConverterSet.DEFAULT, ImmutableList.<ClassFieldMapping>of());
    fieldAliasMappingList.put(ConverterSet.DEFAULT, ImmutableList.<InterfaceFieldAliasMapping>of());
  }

  /**
   * The Guice injector, used for creating new instances of the model.
   */
  private Injector injector;

  /**
   * @param injector the injector to initialize with
   */
  @Inject
  public XStream081Configuration(Injector injector) {
    this.injector = injector;
  }

  private static Multimap<String, Class<?>> getOmitMap(ConverterSet c) {
    return Objects.firstNonNull(omitMap.get(c), omitMap.get(ConverterSet.DEFAULT));
  }


  /**
   * {@inheritDoc}
   *
   * @param writerStack
   * @see XStreamConfiguration#getConverterConfig(org.apache.shindig.protocol.conversion.xstream.XStreamConfiguration.ConverterSet, com.thoughtworks.xstream.converters.reflection.ReflectionProvider, com.thoughtworks.xstream.mapper.Mapper, com.thoughtworks.xstream.io.HierarchicalStreamDriver, org.apache.shindig.protocol.conversion.xstream.WriterStack)
   */
  public ConverterConfig getConverterConfig(ConverterSet c, ReflectionProvider rp,
                                            Mapper dmapper, HierarchicalStreamDriver driver, WriterStack writerStack) {

    InterfaceFieldAliasingMapper emapper = new InterfaceFieldAliasingMapper(dmapper, writerStack, fieldAliasMappingList.get(c));

    InterfaceClassMapper fmapper = new InterfaceClassMapper(writerStack,
        emapper,
        elementMappingList.get(c),
        listElementMappingList.get(c),
        itemFieldMappings.get(c),
        getOmitMap(c),
        elementClassMap.get(c));

    AttributeMapper amapper = new AttributeMapper(fmapper, new DefaultConverterLookup(), rp);

    XStream xstream = new XStream(rp, driver, getClass().getClassLoader(), amapper);

    xstream.registerConverter(new MapConverter(fmapper));
    xstream.registerConverter(new RestfullCollectionConverter(fmapper));
    xstream.registerConverter(new DataCollectionConverter(fmapper));
    xstream.registerConverter(new AtomLinkConverter());
    xstream.registerConverter(new AtomSummaryConverter());

    xstream.registerConverter(new ISO8601DateConverter());
    xstream.registerConverter(new ISO8601GregorianCalendarConverter());
    xstream.registerConverter(new ISO8601SqlTimestampConverter());
    xstream.registerConverter(new GuiceBeanConverter(fmapper, injector));
    xstream.registerConverter(new AtomAttributeConverter());
    xstream.registerConverter(new ExtendableBeanConverter(), XStream.PRIORITY_VERY_HIGH);
    xstream.setMode(XStream.NO_REFERENCES);

    amapper.addAttributeFor(AtomAttribute.class);

    // prevent NPE on xstream 1.3.x
    amapper.setConverterLookup(xstream.getConverterLookup());
    return new ConverterConfig(fmapper, xstream);
  }


  /**
   * {@inheritDoc}
   *
   * @see org.apache.shindig.protocol.conversion.xstream.XStreamConfiguration#getNameSpaces()
   */
  public Map<String, NamespaceSet> getNameSpaces() {
    return NAMESPACES;
  }

  /**
   * Delegate for an EnumMap that returns the value of the defaultkey if the
   * designated key is not present.
   * @param <K>
   * @param <V>
   */

  private static final class DefaultedEnumMap<K extends Enum<K>,V> extends ForwardingMap<K,V> {
    private final EnumMap<K,V> backing;
    final K defaultval;

    public DefaultedEnumMap(Class<K> clz, K defaultkey) {
      super();
      this.backing = new EnumMap<K,V>(Preconditions.checkNotNull(clz));
      this.defaultval = Preconditions.checkNotNull(defaultkey);
    }

    public static <K extends Enum<K>,V> DefaultedEnumMap<K,V> init(Class<K> clz, K defaultkey) {
      return new DefaultedEnumMap<K,V>(clz, defaultkey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object o) {
      K key = (K)o;
      return Objects.firstNonNull(backing.get(key), backing.get(defaultval));
    }

    @Override
    protected Map<K,V> delegate() {
      return backing;
    }
  }
}
