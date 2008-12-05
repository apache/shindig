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

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.OAuthFetcherFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Implements HttpFetcher by delegating fetches to either plain or authenticated Http fetchers.
 *
 * TODO: Make this actually implement HttpFetcher to simplify the bindings. Currently we just
 * pretend that is implements HttpFetcher.
 *
 * TODO: Get rid of RemoteContentFetcherFactory by just injecting HttpFetcher.
 * TODO: Get rid of OAUthFetcherFactory by injecting OAuthFetcher (requires significant work.
 */
@Singleton
public class ContentFetcherFactory {
  private final RemoteContentFetcherFactory remoteContentFetcherFactory;
  private final OAuthFetcherFactory oauthFetcherFactory;

  @Inject
  public ContentFetcherFactory(RemoteContentFetcherFactory remoteContentFetcherFactory,
      OAuthFetcherFactory oauthFetcherFactory) {
    this.remoteContentFetcherFactory = remoteContentFetcherFactory;
    this.oauthFetcherFactory = oauthFetcherFactory;
  }

  public HttpResponse fetch(HttpRequest request) throws GadgetException {
    switch (request.getAuthType()) {
      case NONE:
        return remoteContentFetcherFactory.get().fetch(request);
      case SIGNED:
      case OAUTH:
        // TODO: Why do we have to pass the request twice? This doesn't make sense...
        return oauthFetcherFactory.getOAuthFetcher(remoteContentFetcherFactory.get(), request)
            .fetch(request);
      default:
        return HttpResponse.error();
    }
  }
}
