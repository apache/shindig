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

import com.google.inject.Inject;

import org.apache.abdera.protocol.server.CollectionAdapter;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.abdera.protocol.server.impl.RouteManager;

public class SocialRouteManager extends RouteManager {
  private PersonAdapter personAdapter;
  private DataAdapter dataAdapter;
  private ActivityAdapter activityAdapter;
  private static final String BASE = "/social/rest/";
  private String base;

  public SocialRouteManager() {
    this.base = BASE;
  }

  public void setRoutes() {
        // People
    this.addRoute(RequestUrlTemplate.PROFILES_OF_CONNECTIONS_OF_USER,
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
  }

  @Inject
  public void setAdapters(PersonAdapter personAdapter, DataAdapter dataAdapter,
      ActivityAdapter activityAdapter) {
    this.personAdapter = personAdapter;
    this.dataAdapter = dataAdapter;
    this.activityAdapter = activityAdapter;
  }

  /**
   * This extension of the addRoute from the parent allows a RequestUrlTemplate
   * to be passed in instead of a name and pattern. This is just a convenience
   * method to clean up the code. The parent method maps routes to types and
   * adapters.
   *
   * @param template RequestUrlTemplate enum should contain names and patterns.
   * @param type TargetType
   * @param collectionAdapter CollectionAdapter
   * @return addRoute from the parent RouteManager
   */
  public SocialRouteManager addRoute(RequestUrlTemplate template,
      TargetType type, CollectionAdapter collectionAdapter) {
    return (SocialRouteManager) addRoute(template.toString(),
        base + template.getRoutePattern(), type, collectionAdapter);
  }
}
