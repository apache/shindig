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

import org.apache.shindig.protocol.conversion.jsonlib.BaseJsonLibConfig;
import org.apache.shindig.protocol.model.Enum;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.Drinker;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.NetworkPresence;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Smoker;
import org.apache.shindig.social.opensocial.model.Url;

import com.google.inject.Inject;
import com.google.inject.Injector;
import net.sf.ezmorph.MorpherRegistry;
import net.sf.json.util.EnumMorpher;
import net.sf.json.util.JSONUtils;

import java.util.Map;

/**
 * A Json Config class suitable for serializing Shindig json and pojos.
 */
public class BeanJsonLibConfig extends BaseJsonLibConfig {

  /**
   * Construct the config with a Guice injector.
   * @param injector the Guice injector
   */
  @Inject
  public BeanJsonLibConfig(Injector injector) {
    super(injector);
  }

  @Override
  protected void registerMorphers() {
    super.registerMorphers();
    MorpherRegistry morpherRegistry = JSONUtils.getMorpherRegistry();
    morpherRegistry.registerMorpher(new EnumMorpher(Address.Field.class));
    morpherRegistry.registerMorpher(new EnumMorpher(ListField.Field.class));
    morpherRegistry.registerMorpher(new EnumMorpher(ListField.Field.class));
    morpherRegistry.registerMorpher(new EnumMorpher(MediaItem.Field.class));
    morpherRegistry.registerMorpher(new EnumMorpher(MediaItem.Type.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Drinker.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Enum.Field.class));
    morpherRegistry.registerMorpher(new EnumMorpher(NetworkPresence.class));
    morpherRegistry.registerMorpher(new EnumMorpher(Smoker.class));
  }

  @Override
  protected Map<String, Class<?>> createClassMap() {
    Map<String, Class<?>> classMap = super.createClassMap();
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
    classMap.put("phoneNumbers", ListField.class);
    classMap.put("emails", ListField.class);
    classMap.put("mediaItems", MediaItem.class);
    classMap.put("jobs", Organization.class);
    classMap.put("schools", Organization.class);
    classMap.put("urls", Url.class);
    return classMap;
  }
}
