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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.OAuthRequest;

/**
 * A standard implementation of a request pipeline. Performs request caching and
 * signing on top of standard HTTP requests.
 */
@Singleton
public class DefaultRequestPipeline implements RequestPipeline {
  private final HttpFetcher httpFetcher;
  private final HttpCache httpCache;
  private final Provider<OAuthRequest> oauthRequestProvider;

  @Inject
  public DefaultRequestPipeline(HttpFetcher httpFetcher,
                                HttpCache httpCache,
                                Provider<OAuthRequest> oauthRequestProvider) {
    this.httpFetcher = httpFetcher;
    this.httpCache = httpCache;
    this.oauthRequestProvider = oauthRequestProvider;
  }

  public HttpResponse execute(HttpRequest request) throws GadgetException {
    HttpResponse response = null;

    if (!request.getIgnoreCache()) {
      response = httpCache.getResponse(request);
      if (response != null && !response.isStale()) {
        return response;
      }
    }

    switch (request.getAuthType()) {
      case NONE:
        response = httpFetcher.fetch(request);
        break;
      case SIGNED:
      case OAUTH:
        // TODO: Why do we have to pass the request twice? This doesn't make sense...
        response = oauthRequestProvider.get().fetch(request);
        break;
      default:
        return HttpResponse.error();
    }

    if (!request.getIgnoreCache()) {
      httpCache.addResponse(request, response);
    }
    return response;
  }
}
