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
import org.apache.shindig.gadgets.spec.View;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * Basic implementation of a gadget spec factory.
 */
@Singleton
public class BasicGadgetSpecFactory implements GadgetSpecFactory {
  public static final String RAW_GADGETSPEC_XML_PARAM_NAME = "rawxml";
  public static final URI RAW_GADGET_URI = getRawGadgetUri();
  
  static final Logger logger = Logger.getLogger(BasicGadgetSpecFactory.class.getName());

  private final HttpFetcher fetcher;
  private final ExecutorService executor;
  private final TtlCache<URI, GadgetSpec> ttlCache;

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
    if (ignoreCache) {
      return fetchObjectAndCache(gadgetUri, ignoreCache);
    }
    
    TtlCache.CachedObject<GadgetSpec> cached = null;
    synchronized(ttlCache) {
      cached = ttlCache.getElementWithExpiration(gadgetUri);
    }
    
    if (cached.obj == null || cached.isExpired) {
      try {
        return fetchObjectAndCache(gadgetUri, ignoreCache);
      } catch (GadgetException e) {
        // Failed to re-fetch raw object. Use cached object if it exists.
        if (cached.obj == null) {
          throw e;
        } else {
          logger.info("GadgetSpec fetch failed for " + gadgetUri + " -  using cached.");
        }
      }
    }
    
    return cached.obj;
  }

  /**
   * Retrieves a gadget specification from the Internet, processes its views and
   * adds it to the cache.
   */
  private GadgetSpec fetchObjectAndCache(URI url, boolean ignoreCache) throws GadgetException {
    HttpRequest request = new HttpRequest(Uri.fromJavaUri(url)).setIgnoreCache(ignoreCache);
    HttpResponse response = fetcher.fetch(request);
    if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
      throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
                                "Unable to retrieve gadget xml. HTTP error " +
                                response.getHttpStatusCode());
    }
    GadgetSpec spec = new GadgetSpec(url, response.getResponseAsString());

    // Find the type=HTML views that link to their content externally.
    List<View> hrefViewList = new ArrayList<View>();
    for (View v : spec.getViews().values()) {
      if (v.getType() != View.ContentType.URL && v.getHref() != null) {
        hrefViewList.add(v);
      }
    }

    // Retrieve all external view contents simultaneously.
    CountDownLatch latch = new CountDownLatch(hrefViewList.size());
    for (View v : hrefViewList) {
      executor.execute(new ViewContentFetcher(v, latch, fetcher, ignoreCache));
    }
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    for (View v : spec.getViews().values()) {
      if (v.getType() != View.ContentType.URL) {
        // A non-null href at this point indicates that the retrieval of remote
        // content has failed.
        if (v.getHref() != null) {
          throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
                                    "Unable to retrieve remote gadget content.");
        }
      }
    }

    ttlCache.addElement(url, spec, response.getCacheExpiration());
    
    return spec;
  }
  
  private static URI getRawGadgetUri() {
    try {
      return new URI("http", "localhost", "/raw.xml", null);
    } catch (URISyntaxException e) {
      // Never happens
    }
    return null;
  }

  @Inject
  public BasicGadgetSpecFactory(HttpFetcher fetcher,
      CacheProvider cacheProvider,
      ExecutorService executor,
      @Named("shindig.gadget-spec.cache.capacity")int gadgetSpecCacheCapacity,
      @Named("shindig.gadget-spec.cache.minTTL")long minTtl,
      @Named("shindig.gadget-spec.cache.maxTTL")long maxTtl) {
    this.fetcher = fetcher;
    this.executor = executor;
    this.ttlCache =
        new TtlCache<URI, GadgetSpec>(cacheProvider, gadgetSpecCacheCapacity, minTtl, maxTtl);
  }
}
