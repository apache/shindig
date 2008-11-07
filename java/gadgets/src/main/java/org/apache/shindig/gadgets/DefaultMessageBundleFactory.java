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

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.SoftExpiringCache;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.util.logging.Logger;

/**
 * Default implementation of a message bundle factory.
 */
@Singleton
public class DefaultMessageBundleFactory extends AbstractMessageBundleFactory {
  public static final String CACHE_NAME = "messageBundles";
  static final Logger LOG = Logger.getLogger(DefaultMessageBundleFactory.class.getName());
  private final HttpFetcher fetcher;
  private final SoftExpiringCache<Uri, MessageBundle> cache;
  private final long expiration;

  @Inject
  public DefaultMessageBundleFactory(HttpFetcher fetcher,
                                     CacheProvider cacheProvider,
                                     @Named("shindig.cache.expirationMs") long expiration) {
    this.fetcher = fetcher;
    Cache<Uri, MessageBundle> baseCache = cacheProvider.createCache(CACHE_NAME);
    this.cache = new SoftExpiringCache<Uri, MessageBundle>(baseCache);
    this.expiration = expiration;
  }

  @Override
  protected MessageBundle fetchBundle(LocaleSpec locale, boolean ignoreCache)
      throws GadgetException {
    if (ignoreCache) {
      return fetchAndCacheBundle(locale, ignoreCache);
    }

    Uri uri = locale.getMessages();

    SoftExpiringCache.CachedObject<MessageBundle> cached = cache.getElement(uri);

    MessageBundle bundle = null;
    if (cached == null || cached.isExpired) {
      try {
        bundle = fetchAndCacheBundle(locale, ignoreCache);
      } catch (GadgetException e) {
        // Enforce negative caching.
        if (cached != null) {
          bundle = cached.obj;
        } else {
          // We create this dummy spec to avoid the cost of re-parsing when a remote site is out.
          bundle = MessageBundle.EMPTY;
        }
        LOG.info("MessageBundle fetch failed for " + uri + " - using cached.");
        cache.addElement(uri, bundle, expiration);
      }
    } else {
      bundle = cached.obj;
    }

    return bundle;
  }

  private MessageBundle fetchAndCacheBundle(LocaleSpec locale, boolean ignoreCache)
      throws GadgetException {
    Uri url = locale.getMessages();
    HttpRequest request = new HttpRequest(url).setIgnoreCache(ignoreCache);
    // Since we don't allow any variance in cache time, we should just force the cache time
    // globally. This ensures propagation to shared caches when this is set.
    request.setCacheTtl((int) (expiration / 1000));

    HttpResponse response = fetcher.fetch(request);
    if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
      throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
          "Unable to retrieve message bundle xml. HTTP error " +
          response.getHttpStatusCode());
    }

    MessageBundle bundle  = new MessageBundle(locale, response.getResponseAsString());
    cache.addElement(url, bundle, expiration);
    return bundle;
  }
}
