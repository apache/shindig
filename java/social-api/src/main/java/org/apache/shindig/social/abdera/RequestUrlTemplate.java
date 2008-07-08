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

import org.apache.shindig.social.abdera.util.ValidRequestFilter;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * The RequestUrlTemplate enum standardizes the names and descriptions of the
 * URL templates as defined in the RESTful API spec.
 *
 * For example, "/people/{uid}/@all" is roughly translated in English to read
 * "Profiles of Connections of User" and is the referred to in the code as
 * PROFILES_OF_CONNECTIONS_OF_USER. The descriptions can be customized by an
 * implementer and used as the titles for feeds.
 *
 * TODO: Add ResourceBundle functions.
 */

public enum RequestUrlTemplate {
  // People
  JSON_PROFILES_OF_CONNECTIONS_OF_USER("Profiles of Connections of User in JSON format",
      "people/:uid/@all", ValidRequestFilter.Format.JSON),
  ATOM_PROFILES_OF_CONNECTIONS_OF_USER("Profiles of Connections of User in ATOM format",
      "people/:uid/@all", ValidRequestFilter.Format.ATOM),

  JSON_PROFILES_OF_FRIENDS_OF_USER("Profiles of Friends of User in JSON format",
      "people/:uid/@friends", ValidRequestFilter.Format.JSON),
  ATOM_PROFILES_OF_FRIENDS_OF_USER("Profiles of Friends of User in ATOM format",
      "people/:uid/@friends", ValidRequestFilter.Format.ATOM),

  JSON_PROFILES_IN_GROUP_OF_USER("Profiles in Group of User in JSON format",
      "people/:uid/:gid", ValidRequestFilter.Format.JSON),
  ATOM_PROFILES_IN_GROUP_OF_USER("Profiles in Group of User in ATOM format",
      "people/:uid/:gid", ValidRequestFilter.Format.ATOM),

  JSON_PROFILE_OF_CONNECTION_OF_USER("Profile of Connection of User in the JSON format",
      "people/:uid/@all/:pid", ValidRequestFilter.Format.JSON),
  ATOM_PROFILE_OF_CONNECTION_OF_USER("Profile of Connection of User in the ATOM format",
      "people/:uid/@all/:pid", ValidRequestFilter.Format.ATOM),

  JSON_PROFILE_OF_USER("Profile of User in JSON format",
      "people/:uid/@self", ValidRequestFilter.Format.JSON),
  ATOM_PROFILE_OF_USER("Profile of User in ATOM format",
      "people/:uid/@self", ValidRequestFilter.Format.ATOM),

  JSON_PROFILE_OF_REQUESTER("Profile of Requester in JSON format",
      "people/@me/@self", ValidRequestFilter.Format.JSON),
  ATOM_PROFILE_OF_REQUESTER("Profile of Requester in ATOM format",
      "people/@me/@self", ValidRequestFilter.Format.ATOM),

  // Activities
  ACTIVITIES_OF_USER("Activities of User",
      "activities/:uid/@self", null),
  ACTIVITIES_OF_FRIENDS_OF_USER("Activities of Friends of User",
      "activities/:uid/@friends", null),
  ACTIVITIES_OF_GROUP_OF_USER("Activities of Group of User",
      "activities/:uid/:gid", null),
  ACTIVITY_OF_USER("Activity of User",
      "activities/:uid/@self/:aid", null),
  // AppData
  APPDATA_OF_APP_OF_USER("AppData of App of User",
      "appdata/:uid/@self/:aid", null),
  APPDATA_OF_FRIENDS_OF_USER("AppData of Friends of User",
      "appdata/:uid/@friends/:aid", null);

  private final String description;
  private final String routePattern;
  private final ValidRequestFilter.Format formatRestriction;

  private RequestUrlTemplate(String description, String routePattern,
      ValidRequestFilter.Format format) {
    this.description = description;
    this.routePattern = routePattern;
    this.formatRestriction = format;
  }

  @Override
  public String toString() {
    return getDescription();
  }
  
  public String getDescription() {
    return description;
  }

  public String getRoutePattern() {
    return routePattern;
  }

  public ValidRequestFilter.Format getFormatRestriction() {
    return formatRestriction;
  }

  //Reverse Lookup for the description Field
  private static final Map<String, RequestUrlTemplate> lookupByDescription = 
    new HashMap<String, RequestUrlTemplate>();

  static {
    for (RequestUrlTemplate t : EnumSet.allOf(RequestUrlTemplate.class)) {
      lookupByDescription.put(t.getDescription(), t);
    }
  }

  public static RequestUrlTemplate getByDescription(String description) {
    return lookupByDescription.get(description);
  }

  //Reverse Lookup for the routePattern Field
  private static final Map<String, RequestUrlTemplate> lookupByRoutePattern = 
    new HashMap<String, RequestUrlTemplate>();

  static {
    for (RequestUrlTemplate t : EnumSet.allOf(RequestUrlTemplate.class))
      lookupByRoutePattern.put(t.getRoutePattern(), t);
  }

  public static RequestUrlTemplate getByRoutePattern(String pattern) {
    return lookupByRoutePattern.get(pattern);
  }

}
