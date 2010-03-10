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
package org.apache.shindig.gadgets.uri;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.internal.Nullable;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Generates URIs for use by the Shindig proxy service.
 * 
 * URIs are generated on the host specified in ContainerConfig at key
 * "gadgets.uri.proxy.host".
 * 
 * The remainder of the URL may reference either chained or query-style
 * proxy syntax. The former is used when "gadgets.uri.proxy.path" has token
 * "%chained_params%" in it.
 * 
 * Chained: Returned URI contains query params in its path, with the proxied
 * resource's URI appended verbatim to the end. This enables proxied SWFs
 * to perform proxied, relative-URI resource loads. Example:
 * http://www.example.com/gadgets/proxy/&s&refresh=1&...&e/http://www.foo.com/img.gif
 * 
 * Query param: All params are provided on the query string. Example:
 * http://www.example.com/gadgets/proxy?refresh=1&url=http://www.foo.com/img.gif&...
 * 
 * This implementation supports batched versioning as well. The old-style "fp"
 * (fingerprint) parameter is not supported any longer; its functionality is assumed
 * to be subsumed into the version param.
 */
public class DefaultProxyUriManager implements ProxyUriManager {
  public static final String PROXY_HOST_PARAM = "gadgets.uri.proxy.host";
  public static final String PROXY_PATH_PARAM = "gadgets.uri.proxy.path";
  static final String CHAINED_PARAMS_START_BEACON = "&s&";
  static final String CHAINED_PARAMS_END_BEACON = "&e";
  static final String CHAINED_PARAMS_TOKEN = "%chained_params%";

  private final ContainerConfig config;
  private final Versioner versioner;
  
  @Inject
  public DefaultProxyUriManager(ContainerConfig config,
                                @Nullable Versioner versioner) {
    this.config = config;
    this.versioner = versioner;
  }
  
  public List<Uri> make(List<ProxyUri> resources, Integer forcedRefresh) {
    List<Uri> result = Lists.newArrayListWithCapacity(resources.size());
    
    if (resources.size() == 0) {
      return result;
    }
    
    List<Uri> resourceUris = Lists.newArrayListWithCapacity(resources.size());
    
    for (ProxyUri puc : resources) {
      resourceUris.add(puc.getResource());
    }
    
    Map<Uri, String> versions = Maps.newHashMap();
    if (versioner != null) {
      List<String> versionList = versioner.version(resourceUris, resources.get(0).getContainer());
      if (versionList.size() == resources.size()) {
        // This should always be the case.
        // Should we error if not, or just WARNING?
        Iterator<String> versionIt = versionList.iterator();
        for (ProxyUri puc : resources) {
          versions.put(puc.getResource(), versionIt.next());
        }
      }
    }
    
    for (ProxyUri puc : resources) {
      result.add(makeProxiedUri(puc, forcedRefresh, versions.get(puc.getResource())));
    }
    
    return result;
  }

  private Uri makeProxiedUri(ProxyUri puc, Integer forcedRefresh, String version) {
    UriBuilder queryBuilder = new UriBuilder();
    
    // Add all params common to both chained and query syntax.
    String container = puc.getContainer();
    queryBuilder.addQueryParameter(Param.CONTAINER.getKey(), container);
    queryBuilder.addQueryParameter(Param.GADGET.getKey(), puc.getGadget());
    queryBuilder.addQueryParameter(Param.DEBUG.getKey(), puc.isDebug() ? "1" : "0");
    queryBuilder.addQueryParameter(Param.NO_CACHE.getKey(), puc.isNoCache() ? "1" : "0");
    if (forcedRefresh != null) {
      queryBuilder.addQueryParameter(Param.REFRESH.getKey(), forcedRefresh.toString());
    }
    if (version != null) {
      queryBuilder.addQueryParameter(Param.VERSION.getKey(), version);
    }
    
    UriBuilder uri = new UriBuilder();
    uri.setAuthority(getReqConfig(container, PROXY_HOST_PARAM));
    
    // Chained vs. query-style syntax is determined by the presence of CHAINED_PARAMS_TOKEN
    String path = getReqConfig(container, PROXY_PATH_PARAM);
    if (path.contains(CHAINED_PARAMS_TOKEN)) {
      // Chained proxy syntax. Stuff query params into the path and append URI verbatim at the end
      path = path.replace(CHAINED_PARAMS_TOKEN,
          CHAINED_PARAMS_START_BEACON + queryBuilder.getQuery() + CHAINED_PARAMS_END_BEACON);
      uri.setPath(path);
      String uriStr = uri.toString();
      String curUri = uriStr + (!uriStr.endsWith("/") ? "/" : "") + puc.getResource().toString();
      return Uri.parse(curUri);
    }
    
    // Query-style syntax. Use path as normal and append query params at the end.
    queryBuilder.addQueryParameter(Param.URL.getKey(), puc.getResource().toString());
    uri.setQuery(queryBuilder.getQuery());
    uri.setPath(path);
    
    return uri.toUri();
  }
  
  public ProxyUri process(Uri uriIn) throws GadgetException {
    UriStatus status = UriStatus.BAD_URI;
    Uri uri = null;
    
    // First determine if the URI is chained-syntax or query-style.
    String container = uriIn.getQueryParameter(Param.CONTAINER.getKey());
    String uriStr = null;
    Uri queryUri = null;
    if (container != null &&
        config.getString(container, PROXY_PATH_PARAM) != null &&
        config.getString(container, PROXY_PATH_PARAM).equalsIgnoreCase(uriIn.getPath())) {
      // Query-style. Has container param and path matches.
      uriStr = uriIn.getQueryParameter(Param.URL.getKey());
      queryUri = uriIn;
    } else {
      // Check for chained query string in the path.
      int start = uriIn.getPath().indexOf(CHAINED_PARAMS_START_BEACON);
      int end = uriIn.getPath().indexOf(CHAINED_PARAMS_END_BEACON, start);
      if (start >= 0 && end > start) {
        // Looks like chained proxy syntax. Pull out params.
        String queryStr =
            uriIn.getPath().substring(start + CHAINED_PARAMS_START_BEACON.length(), end);
        queryUri = new UriBuilder().setQuery(queryStr).toUri();
        container = queryUri.getQueryParameter(Param.CONTAINER.getKey());
        if (container != null) {
          String proxyPath = config.getString(container, PROXY_PATH_PARAM);
          if (proxyPath != null) {
            String[] chainedChunks = proxyPath.split(CHAINED_PARAMS_TOKEN);
            if (chainedChunks.length == 2) {
              // Pull URI out of original inUri's full representation.
              String fullProxyUri = uriIn.toString();
              int sfxIx = fullProxyUri.indexOf(chainedChunks[1]);
              if (sfxIx > 0) {
                uriStr = fullProxyUri.substring(sfxIx + chainedChunks[1].length());
                while (uriStr.startsWith("/")) {
                  uriStr = uriStr.substring(1);
                }
              }
            }
          }
        }
      }
    }

    // Parameter validation.
    if (StringUtils.isEmpty(uriStr) || StringUtils.isEmpty(container)) {
      throw new GadgetException(GadgetException.Code.MISSING_PARAMETER,
          "Missing required parameter(s):" +
          (StringUtils.isEmpty(uriStr) ? " " + Param.URL.getKey() : "") +
          (StringUtils.isEmpty(container) ? " " + Param.CONTAINER.getKey() : ""),
          HttpResponse.SC_BAD_REQUEST);
    }
    
    String queryHost = config.getString(container, PROXY_HOST_PARAM);
    if (queryHost == null ||
        !queryHost.equalsIgnoreCase(uriIn.getAuthority())) {
      throw new GadgetException(GadgetException.Code.INVALID_PATH, "Invalid proxy host",
          HttpResponse.SC_BAD_REQUEST);
    }
    
    try {
      uri = Uri.parse(uriStr);
    } catch (Exception e) {
      // NullPointerException or InvalidArgumentException.
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "Invalid " + Param.URL.getKey() + ": " + uriStr, HttpResponse.SC_BAD_REQUEST);
    }
    
    // URI is valid.
    status = UriStatus.VALID_UNVERSIONED;

    String version = queryUri.getQueryParameter(Param.VERSION.getKey());
    if (versioner != null && version != null) {
      status = versioner.validate(uri, container, version);
    }
    
    return new ProxyUri(status, uri, queryUri);
  }

  private String getReqConfig(String container, String key) {
    String val = config.getString(container, key);
    if (val == null) {
      throw new RuntimeException("Missing required container config key: " + key + " for " +
          "container: " + container);
    }
    return val;
  }
}
