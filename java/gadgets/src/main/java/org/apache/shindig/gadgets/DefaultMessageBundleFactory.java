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
import org.apache.shindig.common.cache.SoftExpiringCache.CachedObject;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Default implementation of a message bundle factory.
 *
 * Containers wishing to implement custom bundle fetching behavior should override
 * {@link #fetchBundle}.
 */
@Singleton
public class DefaultMessageBundleFactory implements MessageBundleFactory {
  private static final Locale ALL_ALL = new Locale("all", "ALL");
  public static final String CACHE_NAME = "messageBundles";
  static final Logger LOG = Logger.getLogger(DefaultMessageBundleFactory.class.getName());
  private final HttpFetcher fetcher;
  final SoftExpiringCache<String, MessageBundle> cache;
  private final long refresh;

  @Inject
  public DefaultMessageBundleFactory(HttpFetcher fetcher,
                                     CacheProvider cacheProvider,
                                     @Named("shindig.cache.xml.refreshInterval") long refresh) {
    this.fetcher = fetcher;
    Cache<String, MessageBundle> baseCache = cacheProvider.createCache(CACHE_NAME);
    this.cache = new SoftExpiringCache<String, MessageBundle>(baseCache);
    this.refresh = refresh;
  }

  public MessageBundle getBundle(GadgetSpec spec, Locale locale, boolean ignoreCache)
      throws GadgetException {

    if (ignoreCache) {
      return getNestedBundle(spec, locale, true);
    }

    String key = spec.getUrl().toString() + '.' + locale.toString();
    CachedObject<MessageBundle> cached = cache.getElement(key);

    MessageBundle bundle;
    if (cached == null || cached.isExpired) {
      try {
        bundle = getNestedBundle(spec, locale, ignoreCache);
      } catch (GadgetException e) {
        // Enforce negative caching.
        if (cached != null) {
          LOG.info("MessageBundle fetch failed for " + key + " - using cached.");
          bundle = cached.obj;
        } else {
          // We create this dummy spec to avoid the cost of re-parsing when a remote site is out.
          LOG.info("MessageBundle fetch failed for " + key + " - using default.");
          bundle = MessageBundle.EMPTY;
        }
      }
      cache.addElement(key, bundle, refresh);
    } else {
      bundle = cached.obj;
    }

    return bundle;
  }

  private MessageBundle getNestedBundle(GadgetSpec spec, Locale locale, boolean ignoreCache)
      throws GadgetException {
    MessageBundle parent = getParentBundle(spec, locale, ignoreCache);
    MessageBundle child = null;
    LocaleSpec localeSpec = spec.getModulePrefs().getLocale(locale);
    if (localeSpec == null) {
      return parent == null ? MessageBundle.EMPTY : parent;
    }
    Uri messages = localeSpec.getMessages();
    if (messages == null || messages.toString().length() == 0) {
      child = localeSpec.getMessageBundle();
    } else {
      child = fetchBundle(localeSpec, ignoreCache);
    }
    return new MessageBundle(parent, child);
  }

  private MessageBundle getParentBundle(GadgetSpec spec, Locale locale, boolean ignoreCache)
      throws GadgetException {
    if (locale.getLanguage().equalsIgnoreCase("all")) {
      // Top most locale already.
      return null;
    }

    if (locale.getCountry().equalsIgnoreCase("ALL")) {
      return getBundle(spec, ALL_ALL, ignoreCache);
    }

    return getBundle(spec, new Locale(locale.getLanguage(), "ALL"), ignoreCache);
  }

  protected MessageBundle fetchBundle(LocaleSpec locale, boolean ignoreCache)
      throws GadgetException {
    Uri url = locale.getMessages();
    HttpRequest request = new HttpRequest(url).setIgnoreCache(ignoreCache);
    // Since we don't allow any variance in cache time, we should just force the cache time
    // globally. This ensures propagation to shared caches when this is set.
    request.setCacheTtl((int) (refresh / 1000));

    HttpResponse response = fetcher.fetch(request);
    if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
      throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
          "Unable to retrieve message bundle xml. HTTP error " +
          response.getHttpStatusCode());
    }

    return new MessageBundle(locale, response.getResponseAsString());
  }
}
