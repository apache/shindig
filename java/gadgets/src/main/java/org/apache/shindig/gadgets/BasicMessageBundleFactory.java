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

import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.TtlCache;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.net.URI;
import java.util.logging.Logger;

/**
 * Basic implementation of a message bundle factory.
 */
@Singleton
public class BasicMessageBundleFactory extends AbstractMessageBundleFactory {
  static final Logger logger = Logger.getLogger(BasicMessageBundleFactory.class.getName());
  private final HttpFetcher fetcher;
  private final TtlCache<URI, MessageBundle> ttlCache;
  
  @Override
  protected MessageBundle fetchBundle(LocaleSpec locale, boolean ignoreCache)
      throws GadgetException {
    if (ignoreCache) {
      return fetchAndCacheBundle(locale, ignoreCache);
    }
    
    TtlCache.CachedObject<MessageBundle> cached = null;
    
    synchronized(ttlCache) {
      cached = ttlCache.getElementWithExpiration(locale.getMessages());
    }
    
    if (cached.obj == null || cached.isExpired) {
      try {
        return fetchAndCacheBundle(locale, ignoreCache);
      } catch (GadgetException e) {
        if (cached.obj == null) {
          throw e;
        } else {
          logger.info("Message bundle fetch failed for " + locale + " -  using cached.");
        }
      }
    }
    
    return cached.obj;
  }

  private MessageBundle fetchAndCacheBundle(LocaleSpec locale,
      boolean ignoreCache) throws GadgetException {
    URI url = locale.getMessages();
    HttpRequest request = new HttpRequest(Uri.fromJavaUri(url)).setIgnoreCache(ignoreCache);
    HttpResponse response = fetcher.fetch(request);
    if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
      throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
          "Unable to retrieve message bundle xml. HTTP error " +
          response.getHttpStatusCode());
    }

    MessageBundle bundle  = new MessageBundle(locale, response.getResponseAsString());
    ttlCache.addElement(url, bundle, response.getCacheExpiration());
    return bundle;
  }

  @Inject
  public BasicMessageBundleFactory(HttpFetcher fetcher,
                                   CacheProvider cacheProvider,
                                   @Named("shindig.message-bundle.cache.capacity")int capacity,
                                   @Named("shindig.message-bundle.cache.minTTL")long minTtl,
                                   @Named("shindig.message-bundle.cache.maxTTL")long maxTtl) {
    this.fetcher = fetcher;
    this.ttlCache = new TtlCache<URI, MessageBundle>(cacheProvider, capacity, minTtl, maxTtl);
  }
}
