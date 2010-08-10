/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.http;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.uri.UriCommon;

/**
 * Base class for content caches. Defines cache expiration rules and
 * and restrictions on allowed content.
 *
 * Implementations that override this are discouraged from using custom cache keys unless there is
 * actually customization in the request object itself. It is highly recommended that you still
 * use {@link #createKey} in the base class and append any custom data to the end of the key instead
 * of building your own keys from scratch.
 */
public abstract class AbstractHttpCache implements HttpCache {
  private static final String RESIZE_HEIGHT = UriCommon.Param.RESIZE_HEIGHT.getKey();
  private static final String RESIZE_WIDTH = UriCommon.Param.RESIZE_WIDTH.getKey();
  private static final String RESIZE_QUALITY = UriCommon.Param.RESIZE_QUALITY.getKey();

  // Implement these methods to create a concrete HttpCache class.
  protected abstract HttpResponse getResponseImpl(String key);
  protected abstract void addResponseImpl(String key, HttpResponse response);
  protected abstract HttpResponse removeResponseImpl(String key);

  public final HttpResponse getResponse(HttpRequest request) {
    if (isCacheable(request)) {
      String keyString = createKey(request);
      HttpResponse cached = getResponseImpl(keyString);
      if (responseStillUsable(cached)) {
        return cached;
      }
    }
    return null;
  }

  public boolean addResponse(HttpRequest request, HttpResponse response) {
    if (isCacheable(request, response)) {
      // Both are cacheable. Check for forced cache TTL overrides.
      HttpResponseBuilder responseBuilder = new HttpResponseBuilder(response);
      int forcedTtl = request.getCacheTtl();
      if (forcedTtl != -1) {
        responseBuilder.setCacheTtl(forcedTtl);
      }

      response = responseBuilder.create();
      String keyString = createKey(request);
      addResponseImpl(keyString, response);
      return true;
    }

    return false;
  }

  public HttpResponse removeResponse(HttpRequest request) {
    String keyString = createKey(request);
    HttpResponse response = getResponseImpl(keyString);
    removeResponseImpl(keyString);
    if (responseStillUsable(response)) {
      return response;
    }
    return null;
  }

  protected boolean isCacheable(HttpRequest request) {
    if (request.getIgnoreCache()) {
      return false;
    }
    return ("GET".equals(request.getMethod()) ||
            "GET".equals(request.getHeader("X-Method-Override")));
  }

  protected boolean isCacheable(HttpRequest request, HttpResponse response) {
    if (!isCacheable(request)) {
      return false;
    }

    if (request.getCacheTtl() != -1) {
      // Caching was forced. Ignore what the response wants.
      return true;
    }
    
    if (response.getHttpStatusCode() == HttpResponse.SC_NOT_MODIFIED) {
      // Shindig server will serve 304s. Do not cache 304s from the origin server.
      return false;
    }

    // If the HTTP response allows for it, we can cache.
    return !response.isStrictNoCache();
  }

  /**
   * Produce a key from the given request.
   *
   * Relevant pieces of the cache key:
   *
   * - request URI
   * - authentication type
   * - owner id
   * - viewer id
   * - owner of the token
   * - gadget url (from security token; we don't trust what's on the URI itself)
   * - instance id
   * - oauth service name
   * - oauth token name
   * - the resize height parameter
   * - the resize width parameter
   * - the resize quality parameter
   *
   * Except for the first two, all of these may be unset or <code>null</code>,
   * depending on authentication rules. See individual methods for details.  New cache key items
   * should always be inserted using {@code CacheKeyBuilder#setParam(String, Object)}.
   */
  public String createKey(HttpRequest request) {
    if ((request.getAuthType() != AuthType.NONE) &&
        (request.getSecurityToken() == null)) {
      throw new IllegalArgumentException(
          "Cannot sign request without security token: [" + request + ']');
    }

    CacheKeyBuilder keyBuilder = new CacheKeyBuilder()
        .setLegacyParam(0, request.getUri())
        .setLegacyParam(1, request.getAuthType())
        .setLegacyParam(2, getOwnerId(request))
        .setLegacyParam(3, getViewerId(request))
        .setLegacyParam(4, getTokenOwner(request))
        .setLegacyParam(5, getAppUrl(request))
        .setLegacyParam(6, getInstanceId(request))
        .setLegacyParam(7, getServiceName(request))
        .setLegacyParam(8, getTokenName(request))
        .setParam("rh", request.getParam(RESIZE_HEIGHT))
        .setParam("rw", request.getParam(RESIZE_WIDTH))
        .setParam("rq", request.getParam(RESIZE_QUALITY));

    return keyBuilder.build();
  }

  protected static String getOwnerId(HttpRequest request) {
    if (request.getAuthType() != AuthType.NONE &&
        request.getOAuthArguments().getSignOwner()) {
      Preconditions.checkState(request.getSecurityToken() != null, "No Security Token set for request");
      String ownerId = request.getSecurityToken().getOwnerId();
      return Objects.firstNonNull(ownerId, "");
    }
    // Requests that don't use authentication can share the result.
    return null;
  }

  protected static String getViewerId(HttpRequest request) {
    if (request.getAuthType() != AuthType.NONE &&
        request.getOAuthArguments().getSignViewer()) {
      Preconditions.checkState(request.getSecurityToken() != null, "No Security Token set for request");
      String viewerId = request.getSecurityToken().getViewerId();
      return Objects.firstNonNull(viewerId, "");
    }
    // Requests that don't use authentication can share the result.
    return null;
  }

  protected static String getTokenOwner(HttpRequest request) {
    SecurityToken st = request.getSecurityToken();
    if (request.getAuthType() != AuthType.NONE &&
        st.getOwnerId() != null
        && st.getOwnerId().equals(st.getViewerId())
        && request.getOAuthArguments().mayUseToken()) {
      return st.getOwnerId();
    }
    // Requests that don't use authentication can share the result.
    return null;
  }

  protected static String getAppUrl(HttpRequest request) {
    if (request.getAuthType() != AuthType.NONE) {
      return request.getSecurityToken().getAppUrl();
    }
    // Requests that don't use authentication can share the result.
    return null;
  }

  protected static String getInstanceId(HttpRequest request) {
    if (request.getAuthType() != AuthType.NONE) {
      return Long.toString(request.getSecurityToken().getModuleId());
    }
    // Requests that don't use authentication can share the result.
    return null;
  }

  protected static String getServiceName(HttpRequest request) {
    if (request.getAuthType() != AuthType.NONE) {
      return request.getOAuthArguments().getServiceName();
    }
    // Requests that don't use authentication can share the result.
    return null;
  }

  protected static String getTokenName(HttpRequest request) {
    if (request.getAuthType() != AuthType.NONE) {
      return request.getOAuthArguments().getTokenName();
    }
    // Requests that don't use authentication can share the result.
    return null;
  }

  /**
   * Utility function to verify that an entry is usable
   * The cache still serve staled data, it is the responsible of the user
   * to decide if to use it or not (use isStale).
   * @return true If the response can be used.
   */
  protected boolean responseStillUsable(HttpResponse response) {
    return response != null;
  }
}
