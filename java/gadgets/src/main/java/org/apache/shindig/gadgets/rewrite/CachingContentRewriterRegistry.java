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

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
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

  private final Cache<String, String> rewrittenCache;
  private String rewritersKey;

  @Inject
  public CachingContentRewriterRegistry(List<? extends ContentRewriter> rewriters,
      GadgetHtmlParser htmlParser,
      CacheProvider cacheProvider,
      @Named("shindig.rewritten-content.cache.capacity")int capacity) {
    super(rewriters, htmlParser);

    rewrittenCache = cacheProvider.createCache(capacity);
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
            .append('-').append(rewriter.getClass().hashCode());
      }
      rewritersKey = keyBuilder.toString();
    }
    return rewritersKey;
  }

  /** {@inheritDoc} */
  @Override
  public boolean rewriteGadget(Gadget gadget) throws GadgetException {
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
      rewrittenCache.addElement(cacheKey, gadget.getContent());
    }

    return rewritten;
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

    HttpResponse rewritten = super.rewriteHttpResponse(req, resp);
    if (rewritten != null) {
      // All these are bounded by the provided TTLs
      rewrittenCache.addElement(cacheKey, rewritten.getResponseAsString());
    }

    return rewritten;
  }
}
