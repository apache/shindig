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

import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.TtlCache;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.net.URI;
import java.util.logging.Logger;

/**
 * Basic implementation of a gadget spec factory.
 */
@Singleton
public class BasicGadgetSpecFactory implements GadgetSpecFactory {
  static final String RAW_GADGETSPEC_XML_PARAM_NAME = "rawxml";
  static final Uri RAW_GADGET_URI = Uri.parse("http://localhost/raw.xml");
  static final String ERROR_SPEC = "<Module><ModulePrefs title='Error'/><Content/></Module>";
  static final String ERROR_KEY = "parse.exception";
  static final Logger logger = Logger.getLogger(BasicGadgetSpecFactory.class.getName());

  private final HttpFetcher fetcher;
  private final TtlCache<Uri, GadgetSpec> ttlCache;
  private final long minTtl;
  private final long maxTtl;

  @Inject
  public BasicGadgetSpecFactory(HttpFetcher fetcher,
                                CacheProvider cacheProvider,
                                @Named("shindig.gadget-spec.cache.capacity") int capacity,
                                @Named("shindig.gadget-spec.cache.minTTL") long minTtl,
                                @Named("shindig.gadget-spec.cache.maxTTL") long maxTtl) {
    this.fetcher = fetcher;
    this.minTtl = minTtl;
    this.maxTtl = maxTtl;
    this.ttlCache = new TtlCache<Uri, GadgetSpec>(cacheProvider, capacity, minTtl, maxTtl);
  }

  public GadgetSpec getGadgetSpec(GadgetContext context) throws GadgetException {
    String rawxml = context.getParameter(RAW_GADGETSPEC_XML_PARAM_NAME);
    if (rawxml != null) {
      // Set URI to a fixed, safe value (localhost), preventing a gadget rendered
      // via raw XML (eg. via POST) to be rendered on a locked domain of any other
      // gadget whose spec is hosted non-locally.
      return new GadgetSpec(RAW_GADGET_URI, rawxml);
    }
    return getGadgetSpec(context.getUrl(), context.getIgnoreCache());
  }

  /**
   * Retrieves a gadget specification from the cache or from the Internet.
   */
  public GadgetSpec getGadgetSpec(URI gadgetUri, boolean ignoreCache) throws GadgetException {
    Uri uri = Uri.fromJavaUri(gadgetUri);
    if (ignoreCache) {
      return fetchObjectAndCache(uri, ignoreCache);
    }

    TtlCache.CachedObject<GadgetSpec> cached = null;
    synchronized(ttlCache) {
      cached = ttlCache.getElementWithExpiration(uri);
    }

    if (cached.obj == null || cached.isExpired) {
      try {
        return fetchObjectAndCache(uri, ignoreCache);
      } catch (GadgetException e) {
        // Enforce negative caching.
        GadgetSpec spec;
        if (cached.obj != null) {
          spec = cached.obj;
        } else {
          // We create this dummy spec to avoid the cost of re-parsing when a remote site is out.
          spec = new GadgetSpec(uri, ERROR_SPEC);
          spec.setAttribute(ERROR_KEY, e);
          cached.obj = spec;
        }
        logger.info("GadgetSpec fetch failed for " + uri + " - using cached for " + minTtl + " ms");
        ttlCache.addElementWithTtl(uri, spec, minTtl);
      }
    }

    GadgetException exception = (GadgetException) cached.obj.getAttribute(ERROR_KEY);
    if (exception != null) {
      throw exception;
    }
    return cached.obj;
  }

  /**
   * Retrieves a gadget specification from the Internet, processes its views and
   * adds it to the cache.
   */
  private GadgetSpec fetchObjectAndCache(Uri url, boolean ignoreCache) throws GadgetException {
    HttpRequest request = new HttpRequest(url).setIgnoreCache(ignoreCache);
    if (minTtl == maxTtl) {
      // Since we don't allow any variance in cache time, we should just force the cache time
      // globally. This ensures propagation to shared caches when this is set.
      request.setCacheTtl((int) (maxTtl / 1000));
    }

    HttpResponse response = fetcher.fetch(request);
    if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
      throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
                                "Unable to retrieve gadget xml. HTTP error " +
                                response.getHttpStatusCode());
    }

    GadgetSpec spec = new GadgetSpec(url, response.getResponseAsString());

    ttlCache.addElement(url, spec, response.getCacheExpiration());

    return spec;
  }
}
