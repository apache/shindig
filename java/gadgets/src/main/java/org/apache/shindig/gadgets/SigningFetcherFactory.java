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
package org.apache.shindig.gadgets;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.gadgets.http.HttpCache;
import org.apache.shindig.gadgets.http.HttpFetcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

/**
 * Produces Signing content fetchers for input tokens.
 */
@Singleton
public class SigningFetcherFactory {
  private final HttpCache cache;
  private final String keyName;
  private final String privateKey;

  /**
   * Produces a signing fetcher that will sign requests and delegate actual
   * network retrieval to the {@code networkFetcher}
   *
   * @param networkFetcher The fetcher that will be doing actual work.
   * @param token The gadget token used for extracting signing parameters.
   * @return The signing fetcher.
   * @throws GadgetException
   */
  @SuppressWarnings("unused")
  public HttpFetcher getSigningFetcher(
      HttpFetcher networkFetcher, SecurityToken token)
  throws GadgetException {
    return SigningFetcher.makeFromB64PrivateKey(cache,
        networkFetcher, token, keyName, privateKey);
  }

  /**
   * Dummy ctor for implementations that produce custom fetchers.
   *
   */
  protected SigningFetcherFactory(HttpCache cache) {
    this.cache = cache;
    this.keyName = null;
    this.privateKey = null;
  }

  /**
   * @param keyName The key name (may be null) used in the signature.
   * @param keyFile The file containing your private key for signing requests.
   */
  @Inject
  public SigningFetcherFactory(HttpCache cache,
                               @Named("signing.key-name") String keyName,
                               @Named("signing.key-file") String keyFile) {
    this.cache = cache;
    if (keyName == null || keyName.length() == 0) {
      this.keyName = null;
    } else {
      this.keyName = keyName;
    }
    String privateKey = null;
    try {
      privateKey = IOUtils.toString(ResourceLoader.open(keyFile), "UTF-8");
    } catch (IOException e) {
      privateKey = "";
    }
    this.privateKey = privateKey;
  }
}
