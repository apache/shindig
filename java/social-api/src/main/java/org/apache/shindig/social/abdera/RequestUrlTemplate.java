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
  PROFILES_OF_CONNECTIONS_OF_USER("Profiles of Connections of User",
      "people/:uid/@all", "/people/{guid}/@all"),
  PROFILES_OF_FRIENDS_OF_USER("Profiles of Friends of User",
      "people/:uid/@friends", "/people/{guid}/@friends"),
  PROFILES_IN_GROUP_OF_USER("Profiles in Group of User",
      "people/:uid/:gid", "/people/{guid}/{groupid}"),
  PROFILE_OF_CONNECTION_OF_USER("Profile of Connection of User",
      "people/:uid/@all/:pid", "/people/{guid}/@all/{pid}"),
  PROFILE_OF_USER("Profile of User",
      "people/:uid/@self", "/people/{guid}/@self"),
  PROFILE_OF_REQUESTER("Profile of Requester",
          "people/@me/@self", "/people/@me/@self"),
  // Activities
  ACTIVITIES_OF_USER("Activities of User",
      "activities/:uid/@self", "/activities/{uid}/@self"),
  ACTIVITIES_OF_FRIENDS_OF_USER("Activities of Friends of User",
      "activities/:uid/@friends", "/activities/{uid}/@friends"),
  ACTIVITIES_OF_GROUP_OF_USER("Activities of Group of User",
      "activities/:uid/:gid", "/activities/{uid}/{gid}"),
  ACTIVITY_OF_USER("Activity of User",
      "activities/:uid/@self/:aid", "/activities/{uid}/@self/{aid}"),
  // AppData
  APPDATA_OF_APP_OF_USER("AppData of App of User",
      "appdata/:uid/@self/:aid", "/appdata/{uid}/@self/{aid}"),
  APPDATA_OF_FRIENDS_OF_USER("AppData of Friends of User",
      "appdata/:uid/@friends/:aid", "/appdata/{uid}/@friends/{aid}");

  private String description;
  private String routePattern;
  private String urlTemplate;

  private RequestUrlTemplate(String description, String routePattern,
      String urlTemplate) {
    this.description = description;
    this.routePattern = routePattern;
    this.urlTemplate = urlTemplate;
  }

  @Override
  public String toString() {
    return description;
  }

  public String getRoutePattern() {
    return routePattern;
  }

  public String getUrlTemplate() {
    return urlTemplate;
  }

  public static RequestUrlTemplate getValue(String value) {
    return valueOf(value.replaceAll(" ", "_").toUpperCase());
  }
}
