/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.rewrite;

import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.TtlCache;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.MutableContent;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.List;

/**
 * Enhances {@code DefaultContentRewriterRegistry} by adding a caching layer.
 *
 * Entries are cached using a hash of their contents and all registered rewriters. This ensures
 * that changes to either the content or the rewriters will invalidate the cache, as well as
 * optimizing cache size by ensuring that duplicate inputs only have to be rewritten once.
 */
public class CachingContentRewriterRegistry extends DefaultContentRewriterRegistry {

  private final TtlCache<String, String> rewrittenCache;
  private long minCacheTtl;
  private String rewritersKey;

  /**
   * Creates a registry with underlying cache configured by the provided params.
   * @param htmlParser Parser used to generate parse tree versions of content.
   * @param cacheProvider Used to generate a cache instance.
   * @param capacity Maximum number of rewritten content entries to store in the cache.
   * @param minCacheTtl Minimum TTL value, in milliseconds, that it makes sense to cache an entry.
   */
  @Inject
  public CachingContentRewriterRegistry(List<ContentRewriter> rewriters,
      GadgetHtmlParser htmlParser,
      CacheProvider cacheProvider,
      @Named("shindig.rewritten-content.cache.capacity")int capacity,
      @Named("shindig.rewritten-content.cache.minTTL")long minCacheTtl) {
    super(rewriters, htmlParser);
    // minTtl = 0 and maxTtl = MAX_VALUE because the underlying cache is willing to store data
    // with any TTL value specified. Entries are added with a given TTL value per slightly
    // different logic by this class: if a rewrite pass has a cacheTtl lower than minCacheTtl,
    // it's simply not added.
    rewrittenCache = new TtlCache<String, String>(cacheProvider, capacity, 0, Long.MAX_VALUE);
    this.minCacheTtl = minCacheTtl;
  }

  protected String getGadgetCacheKey(Gadget gadget) {
    return getRewritersKey() + ':' + HashUtil.checksum(gadget.getContent().getBytes());
  }

  protected String getHttpResponseCacheKey(HttpRequest req, HttpResponse response) {
    return getRewritersKey() + ':' + req.getUri().toString() + ':' +
        HashUtil.checksum(response.getResponseAsString().getBytes());
  }

  private String getRewritersKey() {
    if (rewritersKey == null) {
      // No need for lock: "rewriter key" generation is idempotent
      StringBuilder keyBuilder = new StringBuilder();
      for (ContentRewriter rewriter : rewriters) {
        keyBuilder.append(rewriter.getClass().getCanonicalName())
            .append("-").append(rewriter.getClass().hashCode()).append(":");
      }
      rewritersKey = keyBuilder.toString();
    }
    return rewritersKey;
  }

  /** {@inheritDoc} */
  @Override
  public boolean rewriteGadget(Gadget gadget) {
    if (gadget.getContext().getIgnoreCache()) {
      return super.rewriteGadget(gadget);
    }

    String cacheKey = getGadgetCacheKey(gadget);
    String cached = rewrittenCache.getElement(cacheKey);

    if (cached != null) {
      gadget.setContent(cached);
      return true;
    }

    MutableContent mc = getMutableContent(gadget.getContent());

    long cacheTtl = Long.MAX_VALUE;
    String original = gadget.getContent();
    for (ContentRewriter rewriter : getRewriters()) {
      RewriterResults rr = rewriter.rewrite(gadget, mc);
      if (rr == null) {
        cacheTtl = 0;
      } else {
        cacheTtl = Math.min(cacheTtl, rr.getCacheTtl());
      }
    }

    gadget.setContent(mc.getContent());

    if (cacheTtl >= minCacheTtl) {
      // Only cache if the cacheTtl is greater than the minimum time configured for doing so.
      // This prevents cache churn and may be more efficient when rewriting is cheaper
      // than a cache lookup.
      rewrittenCache.addElementWithTtl(cacheKey, gadget.getContent(), cacheTtl);
    }

    return !original.equals(gadget.getContent());
  }

  /** {@inheritDoc} */
  @Override
  public HttpResponse rewriteHttpResponse(HttpRequest req, HttpResponse resp) {
    if (req.getIgnoreCache() || resp.getCacheTtl() <= 0) {
      return super.rewriteHttpResponse(req, resp);
    }

    String cacheKey = getHttpResponseCacheKey(req, resp);
    String cached = rewrittenCache.getElement(cacheKey);

    if (cached != null) {
      return new HttpResponseBuilder(resp).setResponseString(cached).create();
    }

    String original = resp.getResponseAsString();
    MutableContent mc = getMutableContent(original);
    long cacheTtl = Long.MAX_VALUE;
    for (ContentRewriter rewriter : getRewriters()) {
      RewriterResults rr = rewriter.rewrite(req, resp, mc);
      if (rr == null) {
        cacheTtl = 0;
      } else {
        cacheTtl = Math.min(cacheTtl, rr.getCacheTtl());
      }
    }

    if (cacheTtl >= minCacheTtl) {
      rewrittenCache.addElementWithTtl(cacheKey, mc.getContent(), cacheTtl);
    }

    if (!original.equals(mc.getContent())) {
      return new HttpResponseBuilder(resp).setResponseString(mc.getContent()).create();
    }

    // Not rewritten, just return original.
    return resp;
  }

  // Methods for testing purposes
  void setMinCacheTtl(long minCacheTtl) {
    this.minCacheTtl = minCacheTtl;
  }
}
