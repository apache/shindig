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
package org.apache.shindig.gadgets.oauth;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;

/**
 * Produces OAuth content fetchers for input tokens.
 */
@Singleton
public class OAuthFetcherFactory {

  protected OAuthFetcherConfig fetcherConfig;

  /**
   * Creates an OAuthFetcherFactory based on prepared crypter and token store.
   *
   * @param fetcherConfig configuration
   */
  @Inject
  protected OAuthFetcherFactory(OAuthFetcherConfig fetcherConfig) {
    this.fetcherConfig = fetcherConfig;
  }

  /**
   * Produces an OAuthFetcher that will sign requests and delegate actual
   * network retrieval to the {@code nextFetcher}
   *
   * @param nextFetcher The fetcher that will fetch real content
   * @param request request that will be sent through the fetcher
   * @return The oauth fetcher.
   * @throws GadgetException
   */
  @SuppressWarnings("unused")
  public OAuthFetcher getOAuthFetcher(HttpFetcher nextFetcher, HttpRequest request)
      throws GadgetException {
    OAuthFetcher fetcher = new OAuthFetcher(fetcherConfig, nextFetcher, request);
    return fetcher;
  }
  
  
}
