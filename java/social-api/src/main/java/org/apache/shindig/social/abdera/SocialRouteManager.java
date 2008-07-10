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

import org.apache.shindig.social.abdera.atom.ActivityAdapter;
import org.apache.shindig.social.abdera.atom.DataAdapter;
import org.apache.shindig.social.abdera.atom.PersonAdapter;
import org.apache.shindig.social.abdera.json.PersonJsonAdapter;
import org.apache.shindig.social.abdera.util.ValidRequestFilter;
import org.apache.shindig.social.abdera.util.ValidRequestFilter.Format;

import org.apache.abdera.i18n.templates.Route;
import org.apache.abdera.protocol.Request;
import org.apache.abdera.protocol.server.CollectionAdapter;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.Target;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.abdera.protocol.server.impl.DefaultWorkspaceManager;
import org.apache.abdera.protocol.server.impl.RouteManager;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.ImplementedBy;

public class SocialRouteManager extends RouteManager {
  private final PersonJsonAdapter personJsonAdapter;
  private final PersonAdapter personAtomAdapter;
  private final DataAdapter dataAtomAdapter;
  private final ActivityAdapter activityAtomAdapter;
  private static final String BASE = "/social/rest/";
  protected final String base;

  public SocialRouteManager(PersonJsonAdapter personJsonAdapter, PersonAdapter personAtomAdapter,
      DataAdapter dataAtomAdapter, ActivityAdapter activityAtomAdapter) {
    this.base = BASE;
    this.personJsonAdapter = personJsonAdapter;
    this.personAtomAdapter = personAtomAdapter;
    this.dataAtomAdapter = dataAtomAdapter;
    this.activityAtomAdapter = activityAtomAdapter;
  }

  public void setRoutes() {
        // People
    this.addRoute(RequestUrlTemplate.JSON_PROFILES_OF_CONNECTIONS_OF_USER,
            TargetType.TYPE_COLLECTION, personJsonAdapter)
        .addRoute(RequestUrlTemplate.ATOM_PROFILES_OF_CONNECTIONS_OF_USER,
            TargetType.TYPE_COLLECTION, personAtomAdapter)
        .addRoute(RequestUrlTemplate.JSON_PROFILES_OF_FRIENDS_OF_USER,
            TargetType.TYPE_COLLECTION, personJsonAdapter)
        .addRoute(RequestUrlTemplate.ATOM_PROFILES_OF_FRIENDS_OF_USER,
            TargetType.TYPE_COLLECTION, personAtomAdapter)
        .addRoute(RequestUrlTemplate.JSON_PROFILES_IN_GROUP_OF_USER,
            TargetType.TYPE_COLLECTION, personJsonAdapter)
        .addRoute(RequestUrlTemplate.ATOM_PROFILES_IN_GROUP_OF_USER,
            TargetType.TYPE_COLLECTION, personAtomAdapter)
        .addRoute(RequestUrlTemplate.JSON_PROFILE_OF_CONNECTION_OF_USER,
            TargetType.TYPE_ENTRY, personJsonAdapter)
        .addRoute(RequestUrlTemplate.ATOM_PROFILE_OF_CONNECTION_OF_USER,
            TargetType.TYPE_ENTRY, personAtomAdapter)
        .addRoute(RequestUrlTemplate.JSON_PROFILE_OF_USER,
            TargetType.TYPE_ENTRY, personJsonAdapter)
        .addRoute(RequestUrlTemplate.ATOM_PROFILE_OF_USER,
            TargetType.TYPE_ENTRY, personAtomAdapter)

        // Activities
        .addRoute(RequestUrlTemplate.ACTIVITIES_OF_USER,
            TargetType.TYPE_COLLECTION, activityAtomAdapter)
        .addRoute(RequestUrlTemplate.ACTIVITIES_OF_FRIENDS_OF_USER,
            TargetType.TYPE_COLLECTION, activityAtomAdapter)
        .addRoute(RequestUrlTemplate.ACTIVITIES_OF_GROUP_OF_USER,
            TargetType.TYPE_COLLECTION, null)
        .addRoute(RequestUrlTemplate.ACTIVITY_OF_USER,
            TargetType.TYPE_ENTRY, activityAtomAdapter)

        // AppData
        .addRoute(RequestUrlTemplate.APPDATA_OF_APP_OF_USER,
            TargetType.TYPE_COLLECTION, dataAtomAdapter)
        .addRoute(RequestUrlTemplate.APPDATA_OF_FRIENDS_OF_USER,
            TargetType.TYPE_COLLECTION, dataAtomAdapter)
        ;
  }

  /**
   * Adds an additional addRoute constructor that allows a RequestUrlTemplate to be passed in
   * instead of a name and pattern. This is just a convenience method to clean up the code.
   * 
   * @param template RequestUrlTemplate enum should contain names and patterns.
   * @param type TargetType
   * @param collectionAdapter CollectionAdapter
   * @return addRoute from the parent RouteManager
   */
  public SocialRouteManager addRoute(RequestUrlTemplate template, TargetType type,
      CollectionAdapter collectionAdapter) {
    return addRoute(template.name(), base + template.getRoutePattern(), type, template
        .getFormatRestriction(), collectionAdapter);
  }

  /**
   * Adds an additional addRoute constructor beyond abdera.protocol.server.impl.RouteManager#resolve
   * so that a requirement can be added to each Route that includes the format that the Route
   * accepts
   * 
   * @param name The name of the Route
   * @param pattern The pattern to match and generate urls for requests
   * @param type The TargetType for the Route
   * @param format A Format enum of ATOM or JSON
   * @param collectionAdapter The CollectionAdapter to associate with the Route
   */
  public SocialRouteManager addRoute(
    String name, 
    String pattern, 
    TargetType type,
    ValidRequestFilter.Format format,
    CollectionAdapter collectionAdapter) {
    Map<String, String> requirements = new HashMap<String, String>();
    if (format !=null){
      requirements.put(ValidRequestFilter.FORMAT_FIELD, format.toString()); 
    }
    Route route = new Route(name, pattern, null, requirements);
    route2CA.put(route, collectionAdapter);
    return (SocialRouteManager) addRoute(route, type);
  }

  /**
   * Overrides abdera.protocol.server.impl.RouteManager#resolve to add a format restriction for
   * routes
   */
  public Target resolve(Request request) {
    RequestContext context = (RequestContext) request;
    String uri = context.getTargetPath();
    int idx = uri.indexOf('?');
    if (idx != -1) {
      uri = uri.substring(0, idx);
    }

    for(RouteTargetType routeTarget : targets) {
      // check if the formatRestriction requirement is in the route and if it is satisfied.
      if (matchRequestFormat(context, routeTarget.getRoute())) {
        // match the path portion of the url
        if (routeTarget.getRoute().match(uri)) {
          CollectionAdapter ca = route2CA.get(routeTarget.getRoute());
          if (ca != null) {
            context.setAttribute(DefaultWorkspaceManager.COLLECTION_ADAPTER_ATTRIBUTE, ca);
          }
          return getTarget(context, routeTarget.getRoute(), uri, routeTarget.getTargetType());
        }
      }
    }
    return null;
  }


  /**
   * The ValidRequestFilter runs after the initial resolving of the target so it cannot ensure that
   * a request will be stopped if the url does not have a proper format parameter. This method
   * returns false if there is no valid format parameter.
   * 
   * However, if the format requirement field in the route is null, we call it a match, since that
   * means the route does not have a format restriction
   * 
   * @param request Abdera's RequestContext
   * @param route Abdera's Route object
   */
  private boolean matchRequestFormat(RequestContext request, Route route) {
    Format requestFormat = ValidRequestFilter.getFormatTypeFromRequest(request);
    String routeFormatRestriction = route.getRequirement(ValidRequestFilter.FORMAT_FIELD);
    if (requestFormat == null) {
      return false;
    } else {
      if (routeFormatRestriction != null) {
        return routeFormatRestriction.toUpperCase().equals(
            requestFormat.getDisplayValue().toUpperCase());
      }
      return true;
    }
  }
  
  private Target getTarget(
    RequestContext context,
    Route route, 
    String uri, 
    TargetType type) {
      return new RouteTarget(type, context, route, uri);
    }
  
  // TODO: We should probably move the static methods here into a helper class
  public static RequestUrlTemplate getUrlTemplate(RequestContext request) {
    String routeName = getRoute(request).getName();
    return RequestUrlTemplate.valueOf(routeName);
  }

  /**
   * This assumes the target resolver was a RouteManager and returns a Route
   * object. If it does not, it throws a NPE for now. It could also deal with a
   * Regex resolver
   *
   * @param request Abdera's RequestContext
   * @return The Route object that matched the request.
   */
  public static Route getRoute(RequestContext request) {
    Object matcher = request.getTarget().getMatcher();
    if (matcher instanceof Route) {
      return (Route) matcher;
    } else {
      throw new NullPointerException();
    }
  }
}
