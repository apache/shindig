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

import com.google.common.base.Strings;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;

import com.google.inject.Inject;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of the invalidation service. No security checks are applied when
 * invalidating Urls. Invalidation marks are added to HttpResponse objects which are then cached.
 * We do an exact equality check on the invalidation marks rather than trying to do something
 * timestamp based.
 *
 * This implementation uses an invalidate-on-read technique. HttpResponses are 'marked' with a
 * globally unique sequence value assigned to the users in who's context the signed/oauth request
 * was made. When that same request is repeated later we ensure that the mark on the cached response
 * is consistent with the current mark for the request.
 *
 * When the content for a user is invalidated a new unique sequence value is assigned to them and
 * all the marks on cached content associated with that user will become invalid.
 *
 * This technique is reliable if the lifetime of the HttpCache is tied to the invalidation cache
 * and when the invalidation cache is canonical. A non-canonical invalidation cache can be used
 * but cached responses must become invalid if an invalidation entry is missing.
 */
public class DefaultInvalidationService implements InvalidationService {

  public static final String CACHE_NAME = "invalidatedUsers";

  private final HttpCache httpCache;
  protected final Cache<String,Long> invalidationEntries;
  private final AtomicLong marker;

  private static final String TOKEN_PREFIX = "INV_TOK:";

  @Inject
  public DefaultInvalidationService(HttpCache httpCache, CacheProvider cacheProvider) {
    // Initialize to current time to mimimize conflict with persistent caches
    this(httpCache, cacheProvider, new AtomicLong(System.currentTimeMillis()));
  }

  DefaultInvalidationService(HttpCache httpCache, CacheProvider cacheProvider, AtomicLong marker) {
    this.httpCache = httpCache;
    invalidationEntries = cacheProvider.createCache(CACHE_NAME);
    this.marker = marker;
  }

  public void invalidateApplicationResources(Set<Uri> uris, SecurityToken token) {
    // TODO Add checks on content
    for (Uri uri : uris) {
      httpCache.removeResponse(new HttpRequest(uri));
    }
  }

  /**
   * Invalidate all fetched content that was signed on behalf of the specified set of users.
   *
   * @param opensocialIds
   * @param token
   */
  public void invalidateUserResources(Set<String> opensocialIds, SecurityToken token) {
    for (String userId : opensocialIds) {
      // Allocate a new mark for each user
      invalidationEntries.addElement(getKey(userId, token), marker.incrementAndGet());
    }
  }

  public boolean isValid(HttpRequest request, HttpResponse response) {
    if (request.getAuthType() == AuthType.NONE) {
      // Always valid for unauthenticated requests
      return true;
    }
    String invalidationHeader = response.getHeader(INVALIDATION_HEADER);
    if (invalidationHeader == null) {
      invalidationHeader = "";
    }
    return invalidationHeader.equals(getInvalidationMark(request));
  }

  public HttpResponse markResponse(HttpRequest request, HttpResponse response) {
    if (request.getAuthType() == AuthType.NONE) {
      return response;
    }
    String mark = getInvalidationMark(request);
    if (mark.length() > 0) {
      return new HttpResponseBuilder(response).setHeader(INVALIDATION_HEADER, mark).create();
    }
    return response;
  }

  /**
   * Get the invalidation entry key for a user in the scope of a given
   * application
   */
  private String getKey(String userId, SecurityToken token) {
    // Convert the id to the container relative form
    int colonIndex = userId.lastIndexOf(':');
    if (colonIndex != -1) {
      userId = userId.substring(colonIndex + 1);
    }

    // Assume the container is consistent in its use of either appId or appUrl.
    // Use appId
    if (!Strings.isNullOrEmpty(token.getAppId())) {
      return TOKEN_PREFIX + token.getAppId() + ':' + userId;
    }
    return TOKEN_PREFIX + token.getAppUrl() + ':' + userId;
  }

  /**
   * Calculate the invalidation mark for a request
   */
  private String getInvalidationMark(HttpRequest request) {
    StringBuilder currentInvalidation = new StringBuilder();

    Long ownerStamp = null;
    if (request.getOAuthArguments() != null && request.getOAuthArguments().getSignOwner()) {
      String ownerKey = getKey(request.getSecurityToken().getOwnerId(), request.getSecurityToken());
      ownerStamp = invalidationEntries.getElement(ownerKey);
    }
    Long viewerStamp = null;
    if (request.getOAuthArguments() != null && request.getOAuthArguments().getSignViewer()) {
      if (ownerStamp != null &&
          request.getSecurityToken().getOwnerId().equals(
              request.getSecurityToken().getViewerId())) {
        viewerStamp = ownerStamp;
      } else {
        String viewerKey = getKey(request.getSecurityToken().getViewerId(),
            request.getSecurityToken());
        viewerStamp = invalidationEntries.getElement(viewerKey);
      }
    }
    if (ownerStamp != null) {
      currentInvalidation.append("o=").append(ownerStamp).append(';');
    }
    if (viewerStamp != null) {
      currentInvalidation.append("v=").append(viewerStamp).append(';');
    }
    return currentInvalidation.toString();
  }

}
