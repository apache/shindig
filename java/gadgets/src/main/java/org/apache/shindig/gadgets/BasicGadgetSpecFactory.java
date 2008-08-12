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
 * Basic implementation of a gadget spec factory.
 *
 * TODO: This needs to be unified with message bundle fetching. We've basically copied the class in
 * two places, which is horrible.
 */
@Singleton
public class BasicGadgetSpecFactory implements GadgetSpecFactory {

  private static final Logger logger = Logger.getLogger(BasicGadgetSpecFactory.class.getName());

  private final HttpFetcher fetcher;
  private final ContentRewriter rewriter;
  private final Executor executor;
  private final long minTtl;
  private final long maxTtl;

  // A cache of GadgetSpecs with expirations
  private final Cache<URI, TimeoutPair> cache;

  public GadgetSpec getGadgetSpec(GadgetContext context) throws GadgetException {
    return getGadgetSpec(context.getUrl(), context.getIgnoreCache());
  }

  /**
   * Retrieves a gadget specification from the cache or from the Internet.
   */
  public GadgetSpec getGadgetSpec(URI url, boolean ignoreCache) throws GadgetException {
    if (ignoreCache) {
      return fetchFromWeb(url, true);
    }

    GadgetSpec spec = null;
    long expiration = -1;

    // Attempt to retrieve the gadget spec from the cache.
    synchronized (cache) {
      TimeoutPair gadgetSpecEntry = cache.getElement(url);
      if (gadgetSpecEntry != null) {
        spec = gadgetSpecEntry.spec;
        expiration = gadgetSpecEntry.timeout;
      }
    }

    long now = System.currentTimeMillis();

    // If the gadget spec is not in the cache or has expired, fetch it from its URI.
    if (spec == null || expiration < now) {
      try {
        return fetchFromWeb(url, false);
      } catch (GadgetException e) {
        if (spec == null) {
          throw e;
        } else {
          logger.info("Gadget spec fetch failed for " + url + " -  using cached ");
          // Try again later...
          synchronized (cache) {
            cache.addElement(url, new TimeoutPair(spec, now + minTtl));
          }
        }
      }
    }

    return spec;
  }

  /**
   * Retrieves a gadget specification from the Internet, processes its views and
   * adds it to the cache.
   */
  private GadgetSpec fetchFromWeb(URI url, boolean ignoreCache) throws GadgetException {
    HttpRequest request = HttpRequest.getRequest(url, ignoreCache);
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
        if (rewriter != null) {
          v.setRewrittenContent(rewriter.rewriteGadgetView(spec, v.getContent(), "text/html"));
        }
      }
    }

    // We enforce the lower bound limit here for situations where a remote server temporarily serves
    // the wrong cache control headers. This allows any distributed caches to be updated and for the
    // updates to eventually cascade back into the factory.
    long now = System.currentTimeMillis();
    long expiration = response.getCacheExpiration();
    expiration = Math.max(now + minTtl, Math.min(now + maxTtl, expiration));
    synchronized (cache) {
      cache.addElement(url, new TimeoutPair(spec, expiration));
    }

    return spec;
  }

  @Inject
  public BasicGadgetSpecFactory(HttpFetcher fetcher,
                                ContentRewriter rewriter,
                                Executor executor,
                                @Named("shindig.gadget-spec.cache.capacity")int gadgetSpecCacheCapacity,
                                @Named("shindig.gadget-spec.cache.minTTL")long minTtl,
                                @Named("shindig.gadget-spec.cache.maxTTL")long maxTtl) {
    this.fetcher = fetcher;
    this.rewriter = rewriter;
    this.executor = executor;
    this.cache = new LruCache<URI, TimeoutPair>(gadgetSpecCacheCapacity);
    this.minTtl = minTtl;
    this.maxTtl = maxTtl;
  }

  private static class TimeoutPair {
    private final GadgetSpec spec;
    private final long timeout;

    private TimeoutPair(GadgetSpec spec, long timeout) {
      this.spec = spec;
      this.timeout = timeout;
    }
  }
}
