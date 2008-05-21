/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.LruCache;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic implementation of a message bundle factory
 */
public class BasicMessageBundleFactory implements MessageBundleFactory {

  private static final Logger logger
      = Logger.getLogger(BasicMessageBundleFactory.class.getName());

  private final HttpFetcher bundleFetcher;

  private final Cache<URI, BundleTimeoutPair> inMemoryBundleCache;

  private final long bundleMinTTL;

  public MessageBundle getBundle(LocaleSpec localeSpec, GadgetContext context)
      throws GadgetException {
    return getBundle(localeSpec.getMessages(), context.getIgnoreCache());
  }

  public MessageBundle getBundle(URI bundleUrl, boolean ignoreCache)
      throws GadgetException {
    MessageBundle bundle = null;
    long expiration = -1;
    synchronized (inMemoryBundleCache) {
      BundleTimeoutPair entry = inMemoryBundleCache.getElement(bundleUrl);
      if (entry != null) {
        bundle = entry.bundle;
        expiration = entry.timeout;
      }
    }
    if (ignoreCache || bundle == null || expiration < System.currentTimeMillis()) {
      try {
        HttpRequest request
            = HttpRequest.getRequest(bundleUrl, ignoreCache);
        HttpResponse response = bundleFetcher.fetch(request);
        if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
          throw new GadgetException(
              GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
              "Unable to retrieve message bundle xml. HTTP error " +
                  response.getHttpStatusCode());
        }
        bundle = new MessageBundle(bundleUrl, response.getResponseAsString());

        // Add the updated bundle back to the cache and force the min TTL
        expiration = Math
            .max(response.getCacheExpiration(), System.currentTimeMillis() + bundleMinTTL);
        synchronized (inMemoryBundleCache) {
          inMemoryBundleCache.addElement(bundleUrl, new BundleTimeoutPair(bundle, expiration));
        }
      } catch (GadgetException ge) {
        if (bundle == null) {
          throw ge;
        } else {
          logger.log(Level.WARNING,
              "Msg bundle fetch failed for " + bundleUrl + " -  using cached ", ge);
        }
      }
    }
    return bundle;
  }

  @Inject
  public BasicMessageBundleFactory(HttpFetcher bundleFetcher,
      @Named("message-bundle.cache.capacity")int messageBundleCacheCapacity,
      @Named("message-bundle.cache.minTTL")long minTTL) {
    this.bundleFetcher = bundleFetcher;
    this.inMemoryBundleCache =
        new LruCache<URI, BundleTimeoutPair>(messageBundleCacheCapacity);
    this.bundleMinTTL = minTTL;
  }

  private static class BundleTimeoutPair {
    private MessageBundle bundle;
    private long timeout;

    private BundleTimeoutPair(MessageBundle bundle, long timeout) {
      this.bundle = bundle;
      this.timeout = timeout;
    }
  }
}
