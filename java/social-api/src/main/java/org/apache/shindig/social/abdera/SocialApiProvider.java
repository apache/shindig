/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.shindig.social.abdera;

import org.apache.shindig.social.abdera.json.JSONFilter;
import org.apache.shindig.social.abdera.util.ValidRequestFilter;

import com.google.inject.Inject;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.abdera.protocol.server.impl.DefaultProvider;

public class SocialApiProvider extends DefaultProvider {
  //TODO why is this hardcoded here. can't this be from servletContext?
  private static final String BASE = "/social/rest/";

  private PersonAdapter personAdapter;
  private DataAdapter dataAdapter;
  private ActivityAdapter activityAdapter;

  @Inject
  public void setAdapters(
      PersonAdapter personAdapter,
      DataAdapter dataAdapter,
      ActivityAdapter activityAdapter) {
    this.personAdapter = personAdapter;
    this.dataAdapter = dataAdapter;
    this.activityAdapter = activityAdapter;
  }

  /**
   * CollectionAdapters are provided via Guice and the RouteManager wires
   * together the Routes, their TargetTypes and CollectionAdapters.
   *
   * TODO: Implement the group urls.
   */
  public void initialize() {
    routeManager = new SocialRouteManager(BASE)
        // People
        .addRoute(RequestUrlTemplate.PROFILES_OF_CONNECTIONS_OF_USER,
            TargetType.TYPE_COLLECTION, personAdapter)
        .addRoute(RequestUrlTemplate.PROFILES_OF_FRIENDS_OF_USER,
            TargetType.TYPE_COLLECTION, personAdapter)
        .addRoute(RequestUrlTemplate.PROFILES_IN_GROUP_OF_USER,
            TargetType.TYPE_COLLECTION, personAdapter)
        .addRoute(RequestUrlTemplate.PROFILE_OF_CONNECTION_OF_USER,
            TargetType.TYPE_ENTRY, personAdapter)
        .addRoute(RequestUrlTemplate.PROFILE_OF_USER,
            TargetType.TYPE_ENTRY, personAdapter)

         // Activities
        .addRoute(RequestUrlTemplate.ACTIVITIES_OF_USER,
            TargetType.TYPE_COLLECTION, activityAdapter)
        .addRoute(RequestUrlTemplate.ACTIVITIES_OF_FRIENDS_OF_USER,
            TargetType.TYPE_COLLECTION, activityAdapter)
        .addRoute(RequestUrlTemplate.ACTIVITIES_OF_GROUP_OF_USER,
            TargetType.TYPE_COLLECTION, null)
        .addRoute(RequestUrlTemplate.ACTIVITY_OF_USER,
            TargetType.TYPE_ENTRY, activityAdapter)

         // AppData
        .addRoute(RequestUrlTemplate.APPDATA_OF_APP_OF_USER,
            TargetType.TYPE_COLLECTION, dataAdapter)
        .addRoute(RequestUrlTemplate.APPDATA_OF_FRIENDS_OF_USER,
            TargetType.TYPE_COLLECTION, dataAdapter)
        ;

    addFilter(new ValidRequestFilter());
    // JsonFilter should always be the last class in the chain of filters
    addFilter(new JSONFilter());
    targetBuilder = routeManager;
    targetResolver = routeManager;
  }
}
