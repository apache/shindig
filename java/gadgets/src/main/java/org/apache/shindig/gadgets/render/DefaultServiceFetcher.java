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
package org.apache.shindig.gadgets.render;

import static org.apache.shindig.auth.AbstractSecurityToken.Keys.APP_URL;
import static org.apache.shindig.auth.AbstractSecurityToken.Keys.OWNER;
import static org.apache.shindig.auth.AbstractSecurityToken.Keys.VIEWER;

import static org.apache.shindig.auth.AnonymousSecurityToken.ANONYMOUS_ID;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;

import org.apache.shindig.auth.BlobCrypterSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.servlet.Authority;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation for the ServiceFetcher the rpc services for a container by fetching
 * them from the container's system.listMethods endpoints as defined in the container config.
 */
public class DefaultServiceFetcher implements ServiceFetcher {
  public static final String JSON_RESPONSE_WRAPPER_ELEMENT = "result";

  public static final String OSAPI_FEATURE_CONFIG = "osapi";

  public static final String OSAPI_SERVICES = "osapi.services";

  public static final String GADGETS_FEATURES_CONFIG = "gadgets.features";

  public static final String SYSTEM_LIST_METHODS_METHOD = "system.listMethods";

  /** Key in container config that lists the endpoints offering services */
  public static final String OSAPI_BASE_ENDPOINTS = "endPoints";

  //class name for logging purpose
  private static final String classname = DefaultServiceFetcher.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  private final ContainerConfig containerConfig;

  private final HttpFetcher fetcher;

  private Authority authority;
  private SecurityTokenCodec codec;

  /** @param config Container Config for looking up endpoints */
  @Inject
  public DefaultServiceFetcher(ContainerConfig config, HttpFetcher fetcher) {
    this.containerConfig = config;
    this.fetcher = fetcher;
  }

  @Inject(optional = true)
  public void setAuthority(Authority authority) {
    this.authority = authority;
  }

  @Inject
  public void setSecurityTokenCodec(SecurityTokenCodec codec) {
    this.codec = codec;
  }

  /**
   * Returns the services, keyed by endpoint for the given container.
   *
   * @param container The particular container whose services we want.
   * @return Map endpoints and their serviceMethod list
   */
  public Multimap<String, String> getServicesForContainer(String container, String host) {
    if (containerConfig == null) {
      return ImmutableMultimap.<String, String>builder().build();
    }
    LinkedHashMultimap<String, String> endpointServices = LinkedHashMultimap.create();

    // First check services directly declared in container config
    @SuppressWarnings("unchecked")
    Map<String, Object> declaredServices = (Map<String, Object>) containerConfig.getMap(container,
        GADGETS_FEATURES_CONFIG).get(OSAPI_SERVICES);
    if (declaredServices != null) {
      for (Map.Entry<String, Object> entry : declaredServices.entrySet()) {
        @SuppressWarnings("unchecked")
        Iterable<String> entryValue = (Iterable<String>) entry.getValue();
        endpointServices.putAll(entry.getKey(), entryValue);
      }
    }

    // Merge services lazily loaded from the endpoints if any
    List<String> endpoints = getEndpointsFromContainerConfig(container, host);
    for (String endpoint : endpoints) {
      String endpointVal = endpoint;
      if ( endpoint.startsWith("//") && authority != null ){
        endpointVal = authority.getScheme() + ':' + endpoint;
      }
      endpointServices.putAll(endpoint, retrieveServices(container, endpointVal.replace("%host%", host)));
    }
    return ImmutableMultimap.copyOf(endpointServices);
  }

  @SuppressWarnings("unchecked")
  protected List<String> getEndpointsFromContainerConfig(String container, String host) {
    Map<String, Object> properties = (Map<String, Object>) containerConfig.getMap(container,
        GADGETS_FEATURES_CONFIG).get(OSAPI_FEATURE_CONFIG);

    if (properties != null) {
      return (List<String>) properties.get(OSAPI_BASE_ENDPOINTS);
    }
    return ImmutableList.of();
  }

  protected Set<String> retrieveServices(String container, String endpoint) {
    try {
      StringBuilder sb = new StringBuilder( 250 );
      sb.append(endpoint).append( "?method=" + SYSTEM_LIST_METHODS_METHOD );
      Map<String, String> parms = Maps.newHashMap();
      parms.put( OWNER.getKey(), ANONYMOUS_ID );
      parms.put( VIEWER.getKey(), ANONYMOUS_ID );
      parms.put( APP_URL.getKey(), "0" );
      SecurityToken token = new BlobCrypterSecurityToken(container, "*", "0", parms);
      sb.append( "&st=" ).append( codec.encodeToken( token ));
      Uri url = Uri.parse(sb.toString());
      HttpRequest request = new HttpRequest(url).setInternalRequest(true);

      HttpResponse response = fetcher.fetch(request);
      if (response.getHttpStatusCode() == HttpResponse.SC_OK) {
        return getServicesFromJsonResponse(response.getResponseAsString());
      } else {
        if (LOG.isLoggable(Level.SEVERE)) {
          LOG.logp(Level.SEVERE, classname, "retrieveServices", MessageKeys.HTTP_ERROR_FETCHING, new Object[] {response.getHttpStatusCode(),endpoint});
        }
      }
    } catch (SecurityTokenException se) {
      if (LOG.isLoggable(Level.SEVERE)) {
        LOG.logp(Level.SEVERE, classname, "retrieveServices", MessageKeys.FAILED_TO_FETCH_SERVICE, new Object[] {endpoint,se.getMessage()});
      }
    } catch (GadgetException ge) {
      if (LOG.isLoggable(Level.SEVERE)) {
        LOG.logp(Level.SEVERE, classname, "retrieveServices", MessageKeys.FAILED_TO_FETCH_SERVICE, new Object[] {endpoint,ge.getMessage()});
      }
    } catch (JSONException je) {
      if (LOG.isLoggable(Level.SEVERE)) {
        LOG.logp(Level.SEVERE, classname, "retrieveServices", MessageKeys.FAILED_TO_PARSE_SERVICE, new Object[] {endpoint,je.getMessage()});
      }
    }
    return ImmutableSet.of();
  }

  protected Set<String> getServicesFromJsonResponse(String content)
      throws JSONException {
    ImmutableSet.Builder<String> services = ImmutableSet.builder();
    JSONObject js = new JSONObject(content);
    JSONArray json = js.getJSONArray(JSON_RESPONSE_WRAPPER_ELEMENT);
    for (int i = 0; i < json.length(); i++) {
      String o = json.getString(i);
      services.add(o);
    }
    return services.build();
  }
}
