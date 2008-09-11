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
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.rewrite.DefaultContentRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.List;

/**
 * Standard implementation of a content rewriter with caching logic layered atop it.
 * Uses {@code BasicContentRewriterRegistry} for actual rewriting, and a
 * {@code TtlCache}, whose underlying persistence is provided by {@code CacheProvider},
 * as the cache.
 */
public class CachingContentRewriterRegistry extends DefaultContentRewriterRegistry {
  
  private final TtlCache<String, String> rewrittenCache;
  private String rewritersKey;
  
  @Inject
  public CachingContentRewriterRegistry(ContentRewriter firstRewriter,
      GadgetHtmlParser htmlParser,
      CacheProvider cacheProvider,
      @Named("shindig.rewritten-content.cache.capacity")int capacity,
      @Named("shindig.rewritten-content.cache.minTTL")long minTtl,
      @Named("shindig.rewritten-content.cache.maxTTL")long maxTtl) {
    super(firstRewriter, htmlParser);
    rewrittenCache = new TtlCache<String, String>(cacheProvider, capacity, minTtl, maxTtl);
  }

  protected String getGadgetCacheKey(Gadget gadget) {
    return getRewritersKey() + ":" + HashUtil.checksum(gadget.getContent().getBytes());
  }
  
  protected String getHttpResponseCacheKey(HttpRequest req, HttpResponse response) {
    return getRewritersKey() + ":" + req.getUri().toString() + ":" + 
        HashUtil.checksum(response.getResponseAsString().getBytes());
  }
  
  private String getRewritersKey() {
    if (rewritersKey == null) {
      // No need for lock: "rewriter key" generation is idempotent
      StringBuilder keyBuilder = new StringBuilder();
      List<ContentRewriter> rewriters = getRewriters();
      for (ContentRewriter rewriter : rewriters) {
        keyBuilder.append(rewriter.getClass().getCanonicalName())
            .append("-").append(rewriter.getClass().hashCode());
      }
      rewritersKey = keyBuilder.toString();
    }
    return rewritersKey;
  }

  /** {@inheritDoc} */
  public boolean rewriteGadget(Gadget gadget)
      throws GadgetException {
    if (gadget.getContext().getIgnoreCache()) {
      return super.rewriteGadget(gadget);
    }
    
    String cacheKey = getGadgetCacheKey(gadget);
    String cached = rewrittenCache.getElement(cacheKey);
    
    if (cached != null) {
      gadget.setContent(cached);
      return true;
    }
    
    // Do a fresh rewrite and cache the results.
    boolean rewritten = super.rewriteGadget(gadget);
    if (rewritten) {
      // Only cache if the rewriters did something.
      long expiration = 0;
      Object expirationObj = gadget.getSpec().getAttribute(GadgetSpec.EXPIRATION_ATTRIB);
      if (expirationObj instanceof Long) {
        expiration = (Long)expirationObj;
      }
      rewrittenCache.addElement(cacheKey, gadget.getContent(), expiration);
    }

    return rewritten;
  }
  
  /** {@inheritDoc} */
  public HttpResponse rewriteHttpResponse(HttpRequest req, HttpResponse resp) {
    if (req.getIgnoreCache()) {
      return super.rewriteHttpResponse(req, resp);
    }
    
    String cacheKey = getHttpResponseCacheKey(req, resp);
    String cached = rewrittenCache.getElement(cacheKey);
    
    if (cached != null) {
      return new HttpResponseBuilder(resp).setResponseString(cached).create();
    }
    
    HttpResponse rewritten = super.rewriteHttpResponse(req, resp);
    if (rewritten != null) {
      // Favor forced cache TTL from request first, then
      // the response's cache expiration, then the response's cache TTL
      long forceTtl = req.getCacheTtl();
      long expiration = 0;
      if (forceTtl > 0) {
        expiration = System.currentTimeMillis() + forceTtl;
      } else {
        expiration = resp.getCacheExpiration();
      }
      if (expiration == -1) {
        expiration = System.currentTimeMillis() + resp.getCacheTtl();
      }
      
      // All these are bounded by the provided TTLs
      rewrittenCache.addElement(cacheKey, rewritten.getResponseAsString(), expiration);
    }
    
    return rewritten;
  }
}
