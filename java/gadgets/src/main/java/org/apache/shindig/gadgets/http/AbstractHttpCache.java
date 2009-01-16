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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.TimeSource;
import org.apache.shindig.gadgets.AuthType;

import com.google.inject.Inject;

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
  public static final char KEY_SEPARATOR = ':';
  public static final String DEFAULT_KEY_VALUE = "0";

  private TimeSource clock = new TimeSource();

  /**
   * Subclasses should call this directly or be injected themselves to override.
   */
  @Inject
  public void setClock(TimeSource clock) {
    this.clock = clock;
  }

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
    if (isCacheable(request) && isCacheable(response)) {
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

  protected boolean isCacheable(HttpResponse response) {
    return !response.isStrictNoCache();
  }

  protected boolean isCacheable(HttpRequest request) {
    if (request.getIgnoreCache()) {
      return false;
    }
    return !(!"GET".equals(request.getMethod()) &&
        !"GET".equals(request.getHeader("X-Method-Override")));
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
   *
   * Except for the first two, all of these may be "0" depending on authentication rules. See
   * individual methods for details.
   */
  public String createKey(HttpRequest request) {
    String uri = request.getUri().toString();
    StringBuilder key = new StringBuilder(uri.length() * 2);
    key.append(request.getUri());
    key.append(KEY_SEPARATOR);
    key.append(request.getAuthType());
    key.append(KEY_SEPARATOR);
    key.append(getOwnerId(request));
    key.append(KEY_SEPARATOR);
    key.append(getViewerId(request));
    key.append(KEY_SEPARATOR);
    key.append(getTokenOwner(request));
    key.append(KEY_SEPARATOR);
    key.append(getAppUrl(request));
    key.append(KEY_SEPARATOR);
    key.append(getInstanceId(request));
    key.append(KEY_SEPARATOR);
    key.append(getServiceName(request));
    key.append(KEY_SEPARATOR);
    key.append(getTokenName(request));
    return key.toString();
  }

  protected static String getOwnerId(HttpRequest request) {
    if (request.getAuthType() != AuthType.NONE &&
        request.getOAuthArguments().getSignOwner()) {
      return request.getSecurityToken().getOwnerId();
    }
    // Requests that don't use authentication can share the result.
    return DEFAULT_KEY_VALUE;
  }

  protected static String getViewerId(HttpRequest request) {
    if (request.getAuthType() != AuthType.NONE &&
        request.getOAuthArguments().getSignViewer()) {
      return request.getSecurityToken().getViewerId();
    }
    // Requests that don't use authentication can share the result.
    return DEFAULT_KEY_VALUE;
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
    return DEFAULT_KEY_VALUE;
  }

  protected static String getAppUrl(HttpRequest request) {
    if (request.getAuthType() != AuthType.NONE) {
      return request.getSecurityToken().getAppUrl();
    }
    // Requests that don't use authentication can share the result.
    return DEFAULT_KEY_VALUE;
  }

  protected static String getInstanceId(HttpRequest request) {
    if (request.getAuthType() != AuthType.NONE) {
      return Long.toString(request.getSecurityToken().getModuleId());
    }
    // Requests that don't use authentication can share the result.
    return DEFAULT_KEY_VALUE;
  }

  protected static String getServiceName(HttpRequest request) {
    if (request.getAuthType() != AuthType.NONE) {
      return request.getOAuthArguments().getServiceName();
    }
    // Requests that don't use authentication can share the result.
    return DEFAULT_KEY_VALUE;
  }

  protected static String getTokenName(HttpRequest request) {
    if (request.getAuthType() != AuthType.NONE) {
      return request.getOAuthArguments().getTokenName();
    }
    // Requests that don't use authentication can share the result.
    return DEFAULT_KEY_VALUE;
  }

  /**
   * Utility function to verify that an entry is cacheable and not expired
   * @return true If the response can be used.
   */
  protected boolean responseStillUsable(HttpResponse response) {
    if (response == null) {
      return false;
    }
    return response.getCacheExpiration() > clock.currentTimeMillis();
  }
}
