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
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.SigningFetcherFactory;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.oauth.OAuthFetcherFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * A factory of the supported HttpFetcher types.
 *
 * TODO: Remove this.
 */
public class ContentFetcherFactory implements Provider<HttpFetcher> {

  private final RemoteContentFetcherFactory remoteContentFetcherFactory;
  private final SigningFetcherFactory signingFetcherFactory;
  private final OAuthFetcherFactory oauthFetcherFactory;

  @Inject
  public ContentFetcherFactory(RemoteContentFetcherFactory remoteContentFetcherFactory,
      SigningFetcherFactory signingFetcherFactory,
      OAuthFetcherFactory oauthFetcherFactory) {
    this.signingFetcherFactory = signingFetcherFactory;
    this.remoteContentFetcherFactory = remoteContentFetcherFactory;
    this.oauthFetcherFactory = oauthFetcherFactory;
  }

  /**
   * @param token
   * @return A signing content fetcher
   * @throws GadgetException
   */
  public HttpFetcher getSigningFetcher(SecurityToken token)
      throws GadgetException {
    return signingFetcherFactory.getSigningFetcher(
            remoteContentFetcherFactory.get(), token);
  }

  /**
   * @param token
   * @param params
   * @return an OAuth fetcher
   * @throws GadgetException
   */
  public HttpFetcher getOAuthFetcher(
      SecurityToken token,
      OAuthArguments params)
      throws GadgetException {
    return oauthFetcherFactory.getOAuthFetcher(
        remoteContentFetcherFactory.get(), token, params);
  }

  /**
   * @return a standard fetcher
   */
  public HttpFetcher get() {
    return remoteContentFetcherFactory.get();
  }
}
