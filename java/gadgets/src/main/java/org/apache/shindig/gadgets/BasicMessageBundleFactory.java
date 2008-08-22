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
 * Basic implementation of a message bundle factory
 */
@Singleton
public class BasicMessageBundleFactory extends AbstractMessageBundleFactory {

  private static final Logger LOG = Logger.getLogger(BasicMessageBundleFactory.class.getName());

  private final HttpFetcher fetcher;

  private final Cache<URI, TimeoutPair> cache;

  private final long minTtl;
  private final long maxTtl;

  protected MessageBundle fetchBundle(LocaleSpec locale, boolean ignoreCache)
      throws GadgetException {
    if (ignoreCache) {
      return fetchFromWeb(locale, true);
    }

    URI messages = locale.getMessages();

    MessageBundle bundle = null;
    long expiration = -1;
    synchronized (cache) {
      TimeoutPair entry = cache.getElement(messages);
      if (entry != null) {
        bundle = entry.bundle;
        expiration = entry.timeout;
      }
    }

    long now = System.currentTimeMillis();
    if (bundle == null || expiration < now) {
      try {
        return fetchFromWeb(locale, false);
      } catch (GadgetException e) {
        if (bundle == null) {
          throw e;
        } else {
          LOG.info("Message bundle fetch failed for " + messages + " -  using cached ");
          // Try again later...
          synchronized (cache) {
            cache.addElement(messages, new TimeoutPair(bundle, now + minTtl));
          }
        }
      }
    }
    return bundle;
  }

  private MessageBundle fetchFromWeb(LocaleSpec locale, boolean ignoreCache) throws GadgetException {
    URI url = locale.getMessages();
    HttpRequest request = new HttpRequest(Uri.fromJavaUri(url)).setIgnoreCache(ignoreCache);
    HttpResponse response = fetcher.fetch(request);
    if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
      throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
          "Unable to retrieve message bundle xml. HTTP error " +
          response.getHttpStatusCode());
    }

    MessageBundle bundle  = new MessageBundle(locale, response.getResponseAsString());

    // We enforce the lower bound limit here for situations where a remote server temporarily serves
    // the wrong cache control headers. This allows any distributed caches to be updated and for the
    // updates to eventually cascade back into the factory.
    long now = System.currentTimeMillis();
    long expiration = response.getCacheExpiration();
    expiration = Math.max(now + minTtl, Math.min(now + maxTtl, expiration));
    synchronized (cache) {
      cache.addElement(url, new TimeoutPair(bundle, expiration));
    }
    return bundle;
  }

  @Inject
  public BasicMessageBundleFactory(HttpFetcher fetcher,
                                   CacheProvider cacheProvider,
                                   @Named("shindig.message-bundle.cache.capacity")int capacity,
                                   @Named("shindig.message-bundle.cache.minTTL")long minTtl,
                                   @Named("shindig.message-bundle.cache.maxTTL")long maxTtl) {
    this.fetcher = fetcher;
    this.cache = cacheProvider.createCache(capacity);
    this.minTtl = minTtl;
    this.maxTtl = maxTtl;
  }

  private static class TimeoutPair {
    private MessageBundle bundle;
    private long timeout;

    private TimeoutPair(MessageBundle bundle, long timeout) {
      this.bundle = bundle;
      this.timeout = timeout;
    }
  }
}
