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
package org.apache.shindig.social;

import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.abdera.protocol.server.TargetType;
import org.apache.abdera.protocol.server.impl.DefaultProvider;
import org.apache.abdera.protocol.server.impl.RouteManager;
import org.apache.shindig.social.abdera.ActivitiesServiceAdapter;
import org.apache.shindig.social.abdera.PeopleServiceAdapter;

public class SocialApiProvider extends DefaultProvider {
  //TODO why is this hardcoded here. can't this be from servletContext?
  private static final String BASE = "/social/rest/";
  private static final String ROUTENAME_NOT_USED = "not_used_right_now";
  private PeopleServiceAdapter peopleAdapter;
  private ActivitiesServiceAdapter activitiesAdapter;

  private Provider<PeopleServiceAdapter> peopleAdapterProvider;
  @Inject
  public void setPeopleAdapter(
      Provider<PeopleServiceAdapter> peopleAdapterProvider) {
    this.peopleAdapterProvider = peopleAdapterProvider;
  }
  private Provider<ActivitiesServiceAdapter> activitiesAdapterProvider;
  @Inject
  public void setActivitiesAdapter(
      Provider<ActivitiesServiceAdapter> activitiesAdapterProvider) {
    this.activitiesAdapterProvider = activitiesAdapterProvider;
  }
  
  public SocialApiProvider() {
  }
  
  public void initialize() {
    peopleAdapter = peopleAdapterProvider.get();
    activitiesAdapter = activitiesAdapterProvider.get();
    
    // Add the RouteManager that parses incoming and builds outgoing URLs
    routeManager = new RouteManager()
    
      //Collection of all people connected to user {uid} 
      // /people/{uid}/@all
      .addRoute(ROUTENAME_NOT_USED,
            BASE + "people/:uid/@all", 
            TargetType.TYPE_COLLECTION, 
            peopleAdapter)

      //Individual person record. /people/{uid}/@all/{pid}
      .addRoute(ROUTENAME_NOT_USED, 
          BASE + "people/:uid/@all/:pid", 
          TargetType.TYPE_ENTRY, 
          peopleAdapter)
    
      //Self Profile record for user {uid} /people/{uid}/@self
      .addRoute(ROUTENAME_NOT_USED, 
          BASE + "people/:uid/@self", 
          TargetType.TYPE_ENTRY, 
          peopleAdapter)

      //Activities
      //Collection of activities for given user /activities/{uid}/@self  
      .addRoute(ROUTENAME_NOT_USED, 
          BASE + "activities/:uid/@self", 
          TargetType.TYPE_COLLECTION, 
          activitiesAdapter)
         
      //Individual activity resource; usually discovered from collection
      // /activities/{uid}/@self/{aid}
      .addRoute(ROUTENAME_NOT_USED, 
          BASE + "activities/:uid/@self/:aid", 
          TargetType.TYPE_ENTRY, 
          activitiesAdapter)
    ;    
      
    targetBuilder = routeManager;
    targetResolver = routeManager;
  }
}
