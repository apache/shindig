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

import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.Enum;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Phone;
import org.apache.shindig.social.opensocial.model.Url;

import com.google.inject.Inject;
import com.google.inject.Injector;
import net.sf.ezmorph.MorpherRegistry;
import net.sf.json.JsonConfig;
import net.sf.json.util.EnumMorpher;
import net.sf.json.util.JSONUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * A Json Config class suitable for serializing Shindig json and pojos.
 */

public class BeanJsonLibConfig extends JsonConfig {

  /*
   * Register the Enum Morphers so that JSON -> Bean works correctly for enums.
   */
  static {
    MorpherRegistry morpherRegistry = JSONUtils.getMorpherRegistry();
    morpherRegistry.registerMorpher(new EnumMorpher(Address.Field.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Phone.Field.class));
    morpherRegistry.registerMorpher(new EnumMorpher(ListField.Field.class));
    morpherRegistry.registerMorpher(new EnumMorpher(MediaItem.Field.class));
    morpherRegistry.registerMorpher(new EnumMorpher(MediaItem.Type.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Enum.Drinker.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Enum.Field.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Enum.Gender.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Enum.NetworkPresence.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Enum.Smoker.class));
    morpherRegistry.registerMorpher(new JsonObjectToMapMorpher());
  }

  /**
   * Construct the config with a Guice injector.
   * @param injector the Guice injector
   */
  @Inject
  public BeanJsonLibConfig(Injector injector) {
    /*
     * This hook deals with the creation of new beans in the JSON -> Java Bean
     * conversion
     */
    setNewBeanInstanceStrategy(new InjectorBeanInstanceStrategy(injector));

    /*
     * We are expecting null for nulls
     */
    registerDefaultValueProcessor(String.class, new NullDefaultValueProcessor());

    setJsonPropertyFilter(new NullPropertyFilter());
    setJavaPropertyFilter(new NullPropertyFilter());
    // the classMap deals with the basic json string to bean conversion

    Map<String, Class<?>> classMap = new HashMap<String, Class<?>>();

    /*
     * mappings are required where there is a List of objects in the interface
     * with no indication of what type the list should contain. At the moment,
     * we are using 1 map for all json trees, as there is no conflict, but if
     * there is a map could be selected on the basis of the root object. It
     * would be better to do this with generics, but this is good enough and
     * compact enough for the moment.
     *
     */
    //
    // activity
    classMap.put("mediaItems", MediaItem.class);
    // this may not be necessary
    classMap.put("templateParams", Map.class);
    // BodyType needs no mappings
    // Message needs no mappings
    // Name needs no mappings
    // Organization needs no mappings
    // Url needs no mappings
    // Email needs no mappings
    // Phone Needs no mappings
    // Address Needs no mappings
    // MediaItem needs no mappings

    // Person map
    classMap.put("addresses", Address.class);
    classMap.put("phoneNumbers", Phone.class);
    classMap.put("emails", ListField.class);
    classMap.put("mediaItems", MediaItem.class);
    classMap.put("jobs", Organization.class);
    classMap.put("schools", Organization.class);
    classMap.put("urls", Url.class);
    setClassMap(classMap);

  }

}
