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
package org.apache.shindig.gadgets.config;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

/**
 * Registers base config contribution bindings.
 */
public class DefaultConfigContributorModule extends AbstractModule {

  @Override
  protected void configure() {
    registerConfigContributors();
    bind(ConfigProcessor.class).to(DefaultConfigProcessor.class);
  }

  protected void registerConfigContributors() {
    MapBinder<String, ConfigContributor> configBinder = MapBinder.newMapBinder(binder(), String.class, ConfigContributor.class);
    configBinder.addBinding("core.util").to(CoreUtilConfigContributor.class);
    configBinder.addBinding("osapi").to(OsapiServicesConfigContributor.class);
    configBinder.addBinding("shindig.auth").to(ShindigAuthConfigContributor.class);
    configBinder.addBinding("shindig.xhrwrapper").to(XhrwrapperConfigContributor.class);
  }

}
