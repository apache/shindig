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
package org.apache.shindig.gadgets.http;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.uri.UriCommon;

/**
 * Base class for content caches. Defines cache expiration rules and
 * and restrictions on allowed content.
 *
 * Note that in the case where refetchStrictNoCacheAfterMs is non-negative, strict no-cache
 * resources are stored in the cache. In this case, only the Cache-Control/Pragma headers are stored
 * and not the original content or other headers.
 *
 * This is used primarily for automatic fetches internal to shindig from triggering lots of
 * back end fetches. Especially comes to play for fetch in CacheEnforcementVisitor. Now since this
 * response is not usable for serving the content, we need to explicitly check if the content is
 * strictNoCache. DefaultRequestPipeline does this correctly, and any implementation of
 * RequestPipeline should do this as well. To prevent breakages for existing implementations, we
 * are keeping the default value to -1.
 *
 * Example:
 * GET /private.html \r\n
 * Host: www.example.com \r\n
 * Cache-Control: private, max-age=1000 \r\n
 * \r\n
 * My credit card number is 1234-5678-1234-5678
 *
 * And with refetch-after=3000, then we store the response as:
 * GET /private.html \r\n
 * Host: www.example.com \r\n
 * Cache-Control: private, max-age=1000 \r\n
 * \r\n
 *
 * For non user facing requests, www.example.com/private.html is considered as private and will not
 * be refetched before 3000ms.
 *
 * For user facing requests, response.isStale() is always true, and will be fetched even before
 * 1000ms. The max-age=1000 is completely ignored by shindig, because that value is for
 * the client browser and not for proxies.
 *
 * isStale() = false always, for all time >= 0
 * shouldRefetch() = false when time < date + 3000ms
 * shouldRefetch() = true when time >= date + 3000ms
 *
 * Note that error cases are handled differently. (Even for strict no cache)
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
  private static final String NO_EXPAND = UriCommon.Param.NO_EXPAND.getKey();

  // Amount of time after which the entry in cache should be considered for a refetch for a
  // non-userfacing internal fetch when the response is strict-no-cache.
  @Inject(optional = true) @Named("shindig.cache.http.strict-no-cache-resource.refetch-after-ms")
  public static long REFETCH_STRICT_NO_CACHE_AFTER_MS_DEFAULT = -1L;

  private long refetchStrictNoCacheAfterMs = REFETCH_STRICT_NO_CACHE_AFTER_MS_DEFAULT;

  // Implement these methods to create a concrete HttpCache class.
  protected abstract HttpResponse getResponseImpl(String key);
  protected abstract void addResponseImpl(String key, HttpResponse response);
  protected abstract void removeResponseImpl(String key);

  public HttpResponse getResponse(HttpRequest request) {
    if (isCacheable(request)) {
      String keyString = createKey(request);
      HttpResponse cached = getResponseImpl(keyString);
      if (responseStillUsable(cached) &&
          (!cached.isStrictNoCache() || refetchStrictNoCacheAfterMs >= 0)) {
        return cached;
      }
    }
    return null;
  }

  public HttpResponse addResponse(HttpRequest request, HttpResponse response) {
    HttpResponseBuilder responseBuilder;
    boolean storeStrictNoCacheResources = (refetchStrictNoCacheAfterMs >= 0);
    if (isCacheable(request, response, storeStrictNoCacheResources)) {
      if (storeStrictNoCacheResources && response.isStrictNoCache()) {
        responseBuilder = buildStrictNoCacheHttpResponse(response);
      } else {
        responseBuilder = new HttpResponseBuilder(response);
      }
    } else {
      return null;
    }
    int forcedTtl = request.getCacheTtl();
    if (forcedTtl != -1 && !response.isError()) {
      responseBuilder.setCacheTtl(forcedTtl);
    }
    response = responseBuilder.create();
    String keyString = createKey(request);
    addResponseImpl(keyString, response);
    return response; // cached and possibly modified
  }

  @VisibleForTesting
  public void setRefetchStrictNoCacheAfterMs(long refetchStrictNoCacheAfterMs) {
    this.refetchStrictNoCacheAfterMs = refetchStrictNoCacheAfterMs;
  }

  @VisibleForTesting
  HttpResponseBuilder buildStrictNoCacheHttpResponse(HttpResponse response) {
    HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    copyHeaderIfPresent("Cache-Control", response, responseBuilder);
    copyHeaderIfPresent("Pragma", response, responseBuilder);
    responseBuilder.setRefetchStrictNoCacheAfterMs(refetchStrictNoCacheAfterMs);
    return responseBuilder;
  }

  /**
   * Copy the specified header from response into builder if it exists.
   */
  private void copyHeaderIfPresent(String header,
                                   HttpResponse response,
                                   HttpResponseBuilder builder) {
    String headerValue = response.getHeader(header);
    if (headerValue != null) {
      builder.setHeader(header, headerValue);
    }
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

  protected boolean isCacheable(HttpRequest request, HttpResponse response,
                                boolean allowStrictNoCacheResponses) {
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

    // If we allow strict no-cache responses or the HTTP response allows for it, we can cache.
    return allowStrictNoCacheResponses || !response.isStrictNoCache();
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
   * - the no_expand parameter
   * - the User-Agent request header
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
        .setParam("rq", request.getParam(RESIZE_QUALITY))
        .setParam("ne", request.getParam(NO_EXPAND))
        .setParam("rm", request.getRewriteMimeType())
        .setParam("ua", request.getHeader("User-Agent"));
    return keyBuilder.build();
  }

  protected static String getOwnerId(HttpRequest request) {
    if (request.getAuthType() != AuthType.NONE && request.getAuthType() != AuthType.OAUTH2
        && request.getOAuthArguments().getSignOwner()) {
      Preconditions.checkState(request.getSecurityToken() != null, "No Security Token set for request");
      String ownerId = request.getSecurityToken().getOwnerId();
      return Objects.firstNonNull(ownerId, "");
    }
    // Requests that don't use authentication can share the result.
    return null;
  }

  protected static String getViewerId(HttpRequest request) {
    if (request.getAuthType() != AuthType.NONE && request.getAuthType() != AuthType.OAUTH2
        && request.getOAuthArguments().getSignViewer()) {
      Preconditions.checkState(request.getSecurityToken() != null, "No Security Token set for request");
      String viewerId = request.getSecurityToken().getViewerId();
      return Objects.firstNonNull(viewerId, "");
    }
    // Requests that don't use authentication can share the result.
    return null;
  }

  protected static String getTokenOwner(HttpRequest request) {
    SecurityToken st = request.getSecurityToken();
    if (request.getAuthType() != AuthType.NONE && request.getAuthType() != AuthType.OAUTH2
        && st.getOwnerId() != null
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
    if ((request.getAuthType() != AuthType.NONE) && (request.getAuthType() != AuthType.OAUTH2)) {
      return request.getOAuthArguments().getServiceName();
    }
    // Requests that don't use authentication can share the result.
    return null;
  }

  protected static String getTokenName(HttpRequest request) {
    if ((request.getAuthType() != AuthType.NONE) && (request.getAuthType() != AuthType.OAUTH2)) {
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
