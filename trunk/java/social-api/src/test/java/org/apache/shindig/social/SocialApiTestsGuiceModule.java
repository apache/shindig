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
package org.apache.shindig.social;

import java.util.Set;

import org.apache.shindig.common.servlet.ParameterFetcher;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.config.JsonContainerConfig;
import org.apache.shindig.protocol.DataServiceServletFetcher;
import org.apache.shindig.protocol.conversion.BeanConverter;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.protocol.conversion.BeanXStreamConverter;
import org.apache.shindig.protocol.conversion.xstream.XStreamConfiguration;
import org.apache.shindig.social.core.oauth2.OAuth2DataService;
import org.apache.shindig.social.core.oauth2.OAuth2DataServiceImpl;
import org.apache.shindig.social.core.oauth2.OAuth2Service;
import org.apache.shindig.social.core.oauth2.OAuth2ServiceImpl;
import org.apache.shindig.social.core.util.xstream.XStream081Configuration;
import org.apache.shindig.social.opensocial.service.ActivityHandler;
import org.apache.shindig.social.opensocial.service.ActivityStreamHandler;
import org.apache.shindig.social.opensocial.service.AppDataHandler;
import org.apache.shindig.social.opensocial.service.MessageHandler;
import org.apache.shindig.social.opensocial.service.PersonHandler;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.ActivityStreamService;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.MessageService;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.sample.spi.JsonDbOpensocialService;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * Provides social api component injection for all large tests
 */
public class SocialApiTestsGuiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ParameterFetcher.class).annotatedWith(Names.named("DataServiceServlet"))
        .to(DataServiceServletFetcher.class);

    bind(ActivityService.class).to(JsonDbOpensocialService.class);
    bind(ActivityStreamService.class).to(JsonDbOpensocialService.class);
    bind(AppDataService.class).to(JsonDbOpensocialService.class);
    bind(MessageService.class).to(JsonDbOpensocialService.class);
    bind(PersonService.class).to(JsonDbOpensocialService.class);

    bind(String.class).annotatedWith(Names.named("shindig.canonical.json.db"))
        .toInstance("sampledata/canonicaldb.json");

    bind(XStreamConfiguration.class).to(XStream081Configuration.class);
    bind(BeanConverter.class).annotatedWith(Names.named("shindig.bean.converter.xml")).to(
        BeanXStreamConverter.class);
    bind(BeanConverter.class).annotatedWith(Names.named("shindig.bean.converter.json")).to(
        BeanJsonConverter.class);

    bind(new TypeLiteral<Set<Object>>(){}).annotatedWith(
        Names.named("org.apache.shindig.handlers"))
        .toInstance(ImmutableSet.<Object>of(ActivityHandler.class, AppDataHandler.class,
            PersonHandler.class, MessageHandler.class, ActivityStreamHandler.class));

    bindConstant().annotatedWith(Names.named("shindig.containers.default"))
        .to("res://containers/default/container.js");
    bindConstant().annotatedWith(Names.named("shindig.port")).to("8080");
    bindConstant().annotatedWith(Names.named("shindig.host")).to("localhost");
    bindConstant().annotatedWith(Names.named("shindig.contextroot")).to("");
    bind(ContainerConfig.class).to(JsonContainerConfig.class);

    bind(Integer.class).annotatedWith(
        Names.named("shindig.cache.lru.default.capacity"))
        .toInstance(10);
    bind(OAuth2Service.class).to(OAuth2ServiceImpl.class);
    bind(OAuth2DataService.class).to(OAuth2DataServiceImpl.class);
  }
}
