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

import org.apache.shindig.social.abdera.SocialRouteManager;
import org.apache.shindig.social.dataservice.ActivityService;
import org.apache.shindig.social.dataservice.AppDataService;
import org.apache.shindig.social.dataservice.PersonService;
import org.apache.shindig.social.opensocial.ActivitiesService;
import org.apache.shindig.social.opensocial.DataService;
import org.apache.shindig.social.opensocial.OpenSocialDataHandler;
import org.apache.shindig.social.opensocial.PeopleService;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.ActivityImpl;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.MediaItemImpl;
import org.apache.shindig.social.samplecontainer.BasicActivitiesService;
import org.apache.shindig.social.samplecontainer.BasicDataService;
import org.apache.shindig.social.samplecontainer.BasicPeopleService;
import org.apache.shindig.social.samplecontainer.SampleContainerRouteManager;
import org.apache.shindig.social.samplecontainer.StateFileDataHandler;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

import java.util.List;

/**
 * Provides social api component injection
 */
public class SocialApiGuiceModule extends AbstractModule {

  /** {@inheritDoc} */
  @Override
  protected void configure() {
    bind(PeopleService.class).to(BasicPeopleService.class);
    bind(DataService.class).to(BasicDataService.class);
    bind(ActivitiesService.class).to(BasicActivitiesService.class);

    bind(new TypeLiteral<List<GadgetDataHandler>>() {})
        .toProvider(GadgetDataHandlersProvider.class);

    bind(PersonService.class).to(BasicPeopleService.class);
    bind(ActivityService.class).to(BasicActivitiesService.class);
    bind(AppDataService.class).to(BasicDataService.class);

    bind(SocialRouteManager.class).to(SampleContainerRouteManager.class);

    bind(Activity.class).to(ActivityImpl.class);
    bind(MediaItem.class).to(MediaItemImpl.class);
  }

  public static class GadgetDataHandlersProvider
      implements Provider<List<GadgetDataHandler>> {
    List<GadgetDataHandler> handlers;

    @Inject
    public GadgetDataHandlersProvider(OpenSocialDataHandler
        openSocialDataHandler, StateFileDataHandler stateFileHandler) {
      handlers = Lists.newArrayList(openSocialDataHandler, stateFileHandler);
    }

    public List<GadgetDataHandler> get() {
      return handlers;
    }
  }

}
