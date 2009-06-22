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
package org.apache.shindig.protocol.conversion.jsonlib;

import org.apache.shindig.protocol.model.TestModel;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import net.sf.ezmorph.MorpherRegistry;
import net.sf.json.JsonConfig;
import net.sf.json.util.EnumMorpher;
import net.sf.json.util.JSONUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides social api component injection for all large tests
 */
public class JsonLibTestsGuiceModule extends AbstractModule {
  @Override
  protected void configure() {

    bind(Map.class).to(HashMap.class);
    bind(List.class).to(ArrayList.class);
    bind(JsonConfig.class).annotatedWith(Names.named("ShindigJsonConfig")).to(
        TestJsonLibConfig.class);
  }

  public static class TestJsonLibConfig extends BaseJsonLibConfig {
    @Inject
    public TestJsonLibConfig(Injector injector) {
      super(injector);
    }

    protected void registerMorphers() {
      super.registerMorphers();
      MorpherRegistry morpherRegistry = JSONUtils.getMorpherRegistry();
      morpherRegistry.registerMorpher(new EnumMorpher(TestModel.Engine.class));
    }

    protected Map<String, Class<?>> createClassMap() {
      Map<String, Class<?>> classMap = super.createClassMap();
      classMap.put("parkingTickets", Map.class);
      classMap.put("passengers", TestModel.Passenger.class);
      return classMap;
    }
  }
}
