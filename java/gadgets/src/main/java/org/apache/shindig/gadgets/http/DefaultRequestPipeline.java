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

import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.OAuthRequest;
import org.apache.shindig.gadgets.rewrite.image.ImageRewriter;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * A standard implementation of a request pipeline. Performs request caching and
 * signing on top of standard HTTP requests.
 */
@Singleton
public class DefaultRequestPipeline implements RequestPipeline {
  private final HttpFetcher httpFetcher;
  private final HttpCache httpCache;
  private final Provider<OAuthRequest> oauthRequestProvider;
  private final ImageRewriter imageRewriter;
  private final InvalidationService invalidationService;

  @Inject
  public DefaultRequestPipeline(HttpFetcher httpFetcher,
                                HttpCache httpCache,
                                Provider<OAuthRequest> oauthRequestProvider,
                                ImageRewriter imageRewriter,
                                InvalidationService invalidationService) {
    this.httpFetcher = httpFetcher;
    this.httpCache = httpCache;
    this.oauthRequestProvider = oauthRequestProvider;
    this.imageRewriter = imageRewriter;
    this.invalidationService = invalidationService;
  }

  public HttpResponse execute(HttpRequest request) throws GadgetException {
    normalizeProtocol(request);
    HttpResponse invalidatedResponse = null;

    if (!request.getIgnoreCache()) {
      HttpResponse cachedResponse = httpCache.getResponse(request);
      // Note that we don't remove invalidated entries from the cache as we want them to be
      // available in the event of a backend fetch failure
      if (cachedResponse != null) {
        if (!cachedResponse.isStale()) {
          if(invalidationService.isValid(request, cachedResponse)) {
            return cachedResponse;
          } else {
            invalidatedResponse = cachedResponse;
          }
        }
      }
    }

    HttpResponse fetchedResponse = null;
    switch (request.getAuthType()) {
      case NONE:
        fetchedResponse = httpFetcher.fetch(request);
        break;
      case SIGNED:
      case OAUTH:
        fetchedResponse = oauthRequestProvider.get().fetch(request);
        break;
      default:
        return HttpResponse.error();
    }

    if (fetchedResponse.isError() && invalidatedResponse != null) {
      // Use the invalidated cached response if it is not stale. We don't update its
      // mark so it remains invalidated
      return invalidatedResponse;
    }

    if (!fetchedResponse.isError() && !request.getIgnoreCache() && request.getCacheTtl() != 0) {
      fetchedResponse = imageRewriter.rewrite(request, fetchedResponse);
    }

    if (!request.getIgnoreCache() ) {
      // Mark the response with invalidation information prior to caching
      if (fetchedResponse.getCacheTtl() > 0) {
        fetchedResponse = invalidationService.markResponse(request, fetchedResponse);
      }
      httpCache.addResponse(request, fetchedResponse);
    }
    return fetchedResponse;
  }

  public void normalizeProtocol(HttpRequest request) throws GadgetException {
    // Normalize the protocol part of the URI
    if (request.getUri().getScheme()== null) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "Url " + request.getUri().toString() + " does not include scheme");
    } else if (!"http".equals(request.getUri().getScheme()) &&
        !"https".equals(request.getUri().getScheme())) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "Invalid request url scheme in url: " + Utf8UrlCoder.encode(request.getUri().toString()) +
            "; only \"http\" and \"https\" supported.");
    }
  }
}
