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

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.LruCache;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/**
 * Basic implementation of a gadget spec factory
 */
@Singleton
public class BasicGadgetSpecFactory implements GadgetSpecFactory {

  private static final Logger logger = Logger.getLogger(BasicGadgetSpecFactory.class.getName());

  private final HttpFetcher specFetcher;
  private final ContentRewriter rewriter;
  private final Executor executor;
  private final long specMinTTL;

  // A cache of GadgetSpecs with expirations
  private final Cache<URI, SpecTimeoutPair> inMemorySpecCache;

  public GadgetSpec getGadgetSpec(GadgetContext context) throws GadgetException {
    return getGadgetSpec(context.getUrl(), context.getIgnoreCache());
  }

  /**
   * Retrieves a gadget specification from the cache or from the Internet.
   */
  public GadgetSpec getGadgetSpec(URI gadgetUri, boolean ignoreCache) throws GadgetException {
    if (ignoreCache) {
      return fetchGadgetSpecFromWeb(gadgetUri, true);
    }

    GadgetSpec spec = null;
    long expiration = -1;

    // Attempt to retrieve the gadget spec from the cache.
    synchronized (inMemorySpecCache) {
      SpecTimeoutPair gadgetSpecEntry = inMemorySpecCache.getElement(gadgetUri);
      if (gadgetSpecEntry != null) {
        spec = gadgetSpecEntry.spec;
        expiration = gadgetSpecEntry.timeout;
      }
    }

    // If the gadget spec is not in the cache or has expired, fetch it from its URI.
    if (spec == null || expiration < System.currentTimeMillis()) {
      try {
        return fetchGadgetSpecFromWeb(gadgetUri, false);
      } catch (GadgetException e) {
        if (spec == null) {
          throw e;
        } else {
          logger.info("Gadget spec fetch failed for " + gadgetUri + " -  using cached ");
        }
      }
    }

    return spec;
  }

  /**
   * Retrieves a gadget specification from the Internet, processes its views and
   * adds it to the cache.
   */
  private GadgetSpec fetchGadgetSpecFromWeb(URI gadgetUri, boolean ignoreCache)
      throws GadgetException {
    HttpRequest request = HttpRequest.getRequest(gadgetUri, ignoreCache);
    HttpResponse response = specFetcher.fetch(request);
    if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
      throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
                                "Unable to retrieve gadget xml. HTTP error " +
                                response.getHttpStatusCode());
    }
    GadgetSpec spec = new GadgetSpec(gadgetUri, response.getResponseAsString());

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
      executor.execute(new ViewContentFetcher(v, latch, specFetcher, ignoreCache));
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
        if (rewriter != null) {
          v.setRewrittenContent(rewriter.rewriteGadgetView(spec, v.getContent(), "text/html"));
        }
      }
    }

    // Add the updated spec back to the cache and force the min TTL
    long expiration = Math.max(
        response.getCacheExpiration(), System.currentTimeMillis() + specMinTTL);
    synchronized (inMemorySpecCache) {
      inMemorySpecCache.addElement(gadgetUri, new SpecTimeoutPair(spec, expiration));
    }

    return spec;
  }

  @Inject
  public BasicGadgetSpecFactory(HttpFetcher specFetcher,
                                ContentRewriter rewriter,
                                Executor executor,
                                @Named("gadget-spec.cache.capacity")int gadgetSpecCacheCapacity,
                                @Named("gadget-spec.cache.minTTL")long minTTL) {
    this.specFetcher = specFetcher;
    this.rewriter = rewriter;
    this.executor = executor;
    this.inMemorySpecCache = new LruCache<URI, SpecTimeoutPair>(gadgetSpecCacheCapacity);
    this.specMinTTL = minTTL;
  }

  private static class SpecTimeoutPair {
    private final GadgetSpec spec;
    private final long timeout;

    private SpecTimeoutPair(GadgetSpec spec, long timeout) {
      this.spec = spec;
      this.timeout = timeout;
    }
  }
}
