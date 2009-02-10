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
package org.apache.shindig.protocol.conversion.jsonlib;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import net.sf.ezmorph.MorpherRegistry;
import net.sf.json.JsonConfig;
import net.sf.json.util.EnumMorpher;
import net.sf.json.util.JSONUtils;

import java.util.Map;

/**
 * Base config for protocols
 */
public class BaseJsonLibConfig extends JsonConfig {

  /**
   * Construct the config with a Guice injector.
   * @param injector the Guice injector
   */
  @Inject
  public BaseJsonLibConfig(Injector injector) {

    registerMorphers();

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

    setClassMap(createClassMap());
  }

  /**
   * Register morphers. Override to add more.
   */
  protected void registerMorphers() {
    MorpherRegistry morpherRegistry = JSONUtils.getMorpherRegistry();
    morpherRegistry.registerMorpher(new EnumMorpher(org.apache.shindig.protocol.model.Enum.Field.class));
    morpherRegistry.registerMorpher(new JsonObjectToMapMorpher());
  }

  /**
   * Create a class map. Override to add
   * @return
   */
  protected Map<String, Class<?>> createClassMap() {
    return Maps.newHashMap();
  }

}