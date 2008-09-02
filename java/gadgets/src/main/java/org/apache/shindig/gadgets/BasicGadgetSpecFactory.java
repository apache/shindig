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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * Basic implementation of a gadget spec factory.
 */
@Singleton
public class BasicGadgetSpecFactory extends CachingWebRetrievalFactory<GadgetSpec, URI, URI> 
    implements GadgetSpecFactory {
  static final Logger logger = Logger.getLogger(BasicGadgetSpecFactory.class.getName());

  private final HttpFetcher fetcher;
  private final ExecutorService executor;

  @Override
  protected URI getCacheKeyFromQueryObj(URI queryObj) {
    return queryObj;
  }
  
  @Override
  protected Logger getLogger() {
    return logger;
  }

  public GadgetSpec getGadgetSpec(GadgetContext context) throws GadgetException {
    return getGadgetSpec(context.getUrl(), context.getIgnoreCache());
  }

  /**
   * Retrieves a gadget specification from the cache or from the Internet.
   */
  public GadgetSpec getGadgetSpec(URI url, boolean ignoreCache) throws GadgetException {
    return doCachedFetch(url, ignoreCache);
  }

  /**
   * Retrieves a gadget specification from the Internet, processes its views and
   * adds it to the cache.
   */
  protected FetchedObject<GadgetSpec> retrieveRawObject(URI url, boolean ignoreCache)
      throws GadgetException {
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

    // Annotate this spec instance with the expiration time (as a Long) associated
    // with its retrieval. This enables CachingContentRewriterRegistry to properly
    // cache rewritten content generated from Gadgets based on the spec.
    spec.setAttribute(GadgetSpec.EXPIRATION_ATTRIB, new Long(response.getCacheExpiration()));
    
    return new FetchedObject<GadgetSpec>(spec, response.getCacheExpiration());
  }

  @Inject
  public BasicGadgetSpecFactory(HttpFetcher fetcher,
                                CacheProvider cacheProvider,
                                ExecutorService executor,
                                @Named("shindig.gadget-spec.cache.capacity")int gadgetSpecCacheCapacity,
                                @Named("shindig.gadget-spec.cache.minTTL")long minTtl,
                                @Named("shindig.gadget-spec.cache.maxTTL")long maxTtl) {
    super(cacheProvider, gadgetSpecCacheCapacity, minTtl, maxTtl);
    this.fetcher = fetcher;
    this.executor = executor;
  }
}
