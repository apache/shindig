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
package org.apache.shindig.social.sample;

import org.apache.shindig.social.core.oauth2.OAuth2DataService;
import org.apache.shindig.social.core.oauth2.OAuth2DataServiceImpl;
import org.apache.shindig.social.core.oauth2.OAuth2Service;
import org.apache.shindig.social.core.oauth2.OAuth2ServiceImpl;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.ActivityStreamService;
import org.apache.shindig.social.opensocial.spi.AlbumService;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.GroupService;
import org.apache.shindig.social.opensocial.spi.MediaItemService;
import org.apache.shindig.social.opensocial.spi.MessageService;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.sample.oauth.SampleOAuthDataStore;
import org.apache.shindig.social.sample.spi.JsonDbOpensocialService;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * Provides bindings for sample-only implementations of social API
 * classes.  This class should never be used in production deployments,
 * but does provide a good overview of the pieces of Shindig that require
 * custom container implementations.
 */
public class SampleModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(String.class).annotatedWith(Names.named("shindig.canonical.json.db"))
        .toInstance("sampledata/canonicaldb.json");
    bind(ActivityService.class).to(JsonDbOpensocialService.class);
    bind(ActivityStreamService.class).to(JsonDbOpensocialService.class);
    bind(AlbumService.class).to(JsonDbOpensocialService.class);
    bind(MediaItemService.class).to(JsonDbOpensocialService.class);
    bind(AppDataService.class).to(JsonDbOpensocialService.class);
    bind(PersonService.class).to(JsonDbOpensocialService.class);
    bind(MessageService.class).to(JsonDbOpensocialService.class);
    bind(GroupService.class).to(JsonDbOpensocialService.class);
    bind(OAuthDataStore.class).to(SampleOAuthDataStore.class);
    bind(OAuth2Service.class).to(OAuth2ServiceImpl.class);
    bind(OAuth2DataService.class).to(OAuth2DataServiceImpl.class);
  }
}
