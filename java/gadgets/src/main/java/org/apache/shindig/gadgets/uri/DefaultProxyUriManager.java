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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import org.apache.shindig.common.servlet.Authority;

// Temporary replacement of javax.annotation.Nullable
import org.apache.shindig.common.Nullable;
import java.util.Collections;
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
 * http://www.example.com/gadgets/proxy/refresh=1&.../http://www.foo.com/img.gif
 *
 * Query param: All params are provided on the query string. Example:
 * http://www.example.com/gadgets/proxy?refresh=1&url=http://www.foo.com/img.gif&...
 *
 * This implementation supports batched versioning as well. The old-style "fp"
 * (fingerprint) parameter is not supported any longer; its functionality is assumed
 * to be subsumed into the version param.
 *
 * @since 2.0.0
 */
public class DefaultProxyUriManager implements ProxyUriManager {
  public static final String PROXY_HOST_PARAM = "gadgets.uri.proxy.host";
  public static final String PROXY_PATH_PARAM = "gadgets.uri.proxy.path";
  static final String CHAINED_PARAMS_TOKEN = "%chained_params%";

  private final ContainerConfig config;
  private final Versioner versioner;
  private boolean strictParsing = false;
  private Authority authority;

  @Inject
  public DefaultProxyUriManager(ContainerConfig config,
                                @Nullable Versioner versioner) {
    this.config = config;
    this.versioner = versioner;
  }

  @Inject(optional = true)
  public void setUseStrictParsing(@Named("shindig.uri.proxy.use-strict-parsing") boolean useStrict) {
    this.strictParsing = useStrict;
  }

  @Inject(optional = true)
  public void setAuthority(Authority authority) {
    this.authority = authority;
  }

  public List<Uri> make(List<ProxyUri> resources, Integer forcedRefresh) {
    if (resources.isEmpty()) {
      return Collections.emptyList();
    }

    List<Uri> resourceUris = Lists.newArrayListWithCapacity(resources.size());
    List<String> resourceTags = Lists.newArrayListWithCapacity(resources.size());

    for (ProxyUri puc : resources) {
      resourceUris.add(puc.getResource());
      resourceTags.add(puc.getHtmlTagContext());
    }

    Map<Uri, String> versions;
    if (versioner == null) {
      versions = Collections.emptyMap();
    } else {
      versions = Maps.newHashMapWithExpectedSize(resources.size());
      List<String> versionList = versioner.version(resourceUris, resources.get(0).getContainer(),
          resourceTags);
      if (versionList != null && versionList.size() == resources.size()) {
        // This should always be the case.
        // Should we error if not, or just WARNING?
        Iterator<String> versionIt = versionList.iterator();
        for (ProxyUri puc : resources) {
          versions.put(puc.getResource(), versionIt.next());
        }
      }
    }

    List<Uri> result = Lists.newArrayListWithCapacity(resources.size());
    for (ProxyUri puc : resources) {
      result.add(makeProxiedUri(puc, forcedRefresh, versions.get(puc.getResource())));
    }

    return result;
  }

  private Uri makeProxiedUri(ProxyUri puc, Integer forcedRefresh, String version) {
    UriBuilder queryBuilder = puc.makeQueryParams(forcedRefresh, version);

    String container = puc.getContainer();
    UriBuilder uri = new UriBuilder();
    uri.setAuthority(getReqConfig(container, PROXY_HOST_PARAM));

    // Chained vs. query-style syntax is determined by the presence of CHAINED_PARAMS_TOKEN
    String path = getReqConfig(container, PROXY_PATH_PARAM);
    if (path.contains(CHAINED_PARAMS_TOKEN)) {
      // Chained proxy syntax. Stuff query params into the path and append URI verbatim at the end
      path = path.replace(CHAINED_PARAMS_TOKEN, queryBuilder.getQuery());
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

  @SuppressWarnings("deprecation")
  public ProxyUri process(Uri uriIn) throws GadgetException {
    // First determine if the URI is chained-syntax or query-style.
    String container = uriIn.getQueryParameter(Param.CONTAINER.getKey());
    if (container == null) {
      container = uriIn.getQueryParameter(Param.SYND.getKey());
    }
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
      String containerStr = Param.CONTAINER.getKey() + '=';
      String path = uriIn.getPath();
      // It is possible to get decoded url ('=' converted to %3d)
      // for example from CssResponseRewriter, so we should support it
      boolean doDecode = (!path.contains(containerStr));
      if (doDecode) {
        path = Utf8UrlCoder.decode(path);
      }
      int start = path.indexOf(containerStr);
      if (start > 0) {
        start += containerStr.length();
        int end = path.indexOf('&', start);
        if (end < start) {
          end = path.indexOf('/', start);
        }
        if (end > start) {
          // Looks like chained proxy syntax. Pull out params.
          container = path.substring(start,end);
        }
        if (container != null) {
          String proxyPath = config.getString(container, PROXY_PATH_PARAM);
          if (proxyPath != null) {
            String[] chainedChunks = StringUtils.splitByWholeSeparatorPreserveAllTokens(
                proxyPath, CHAINED_PARAMS_TOKEN);

            // Parse out the URI of the actual resource. This URI is found as the
            // substring of the "full" URI, after the chained proxy prefix. We
            // first search for the pre- and post-fixes of the original /pre/%chained_params%/post
            // ContainerConfig value, and take the URI as everything beyond that point.
            String startToken = chainedChunks[0];
            String endToken = "/";
            if (chainedChunks.length == 2 && chainedChunks[1].length() > 0) {
              endToken = chainedChunks[1];
            }
            if (!endToken.endsWith("/")) {
              // add suffix '/' that was added by the creator
              endToken = endToken + '/';
            }

            // Pull URI out of original inUri's full representation.
            String fullProxyUri = uriIn.toString();
            int startIx = fullProxyUri.indexOf(startToken) + startToken.length();
            int endIx = fullProxyUri.indexOf(endToken, startIx);
            if (startIx > 0 && endIx > 0) {
              String chainedQuery = fullProxyUri.substring(startIx, endIx);
              if (doDecode) {
                chainedQuery = Utf8UrlCoder.decode(chainedQuery);
              }
              queryUri = new UriBuilder().setQuery(chainedQuery).toUri();
              uriStr = fullProxyUri.substring(endIx + endToken.length());
            }
          }
        }
      }
    }

    if (!strictParsing && container != null && Strings.isNullOrEmpty(uriStr)) {
      // Query-style despite the container being configured for chained style.
      uriStr = uriIn.getQueryParameter(Param.URL.getKey());
      queryUri = uriIn;
    }

    // Parameter validation.
    if (Strings.isNullOrEmpty(uriStr) || Strings.isNullOrEmpty(container)) {
      throw new GadgetException(GadgetException.Code.MISSING_PARAMETER,
          "Missing required parameter(s):" +
          (Strings.isNullOrEmpty(uriStr) ? ' ' + Param.URL.getKey() : "") +
          (Strings.isNullOrEmpty(container) ? ' ' + Param.CONTAINER.getKey() : ""),
          HttpResponse.SC_BAD_REQUEST);
    }

    String queryHost = config.getString(container, PROXY_HOST_PARAM);
    if (strictParsing) {
      if (queryHost == null || !queryHost.equalsIgnoreCase(uriIn.getAuthority())) {
        throw new GadgetException(GadgetException.Code.INVALID_PATH, "Invalid proxy host",
            HttpResponse.SC_BAD_REQUEST);
      }
    }


    Uri uri;
    try {
      uri = Uri.parse(uriStr);
      if (uri.getScheme() == null) {
        // For non schema url, use the proxy schema:
        uri = new UriBuilder(uri).setScheme(uriIn.getScheme()).toUri();
      }
    } catch (Exception e) {
      // NullPointerException or InvalidArgumentException.
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "Invalid " + Param.URL.getKey() + ": " + uriStr, HttpResponse.SC_BAD_REQUEST);
    }

    // URI is valid.
    UriStatus status = UriStatus.VALID_UNVERSIONED;

    String version = queryUri.getQueryParameter(Param.VERSION.getKey());
    if (versioner != null && version != null) {
      status = versioner.validate(uri, container, version);
    }

    ProxyUri proxied = new ProxyUri(status, uri, queryUri);
    proxied.setHtmlTagContext(uriIn.getQueryParameter(Param.HTML_TAG_CONTEXT.getKey()));
    return proxied;
  }

  private String getReqConfig(String container, String key) {
    String val = config.getString(container, key);
    if (val == null) {
      throw new RuntimeException("Missing required container config key: " + key + " for " +
          "container: " + container);
    }
    if (authority != null) {
      val = val.replace("%authority%", authority.getAuthority());
    } else{
      //require this for test purpose, %host% needs to be replaced with default value eg. StyleTagProxyEmbeddedUrlsVisitorTest
      if (val.contains("%authority%")) {
        val = val.replace("%authority%", "localhost:8080");
      }
    }
    return val;
  }
}
