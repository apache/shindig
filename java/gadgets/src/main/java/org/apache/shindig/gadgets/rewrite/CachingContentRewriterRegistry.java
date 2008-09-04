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

import java.util.List;
import java.util.logging.Logger;

import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.gadgets.CachingWebRetrievalFactory;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.rewrite.BasicContentRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Implementation of a content rewriter registry that delegates to
 * {@code BasicContentRewriterRegistry} for base operations, but also
 * provides a layer of caching atop that.
 */
public class CachingContentRewriterRegistry
    extends CachingWebRetrievalFactory<String, Gadget, String>
    implements ContentRewriterRegistry {
  
  static final Logger logger = Logger.getLogger(CachingContentRewriterRegistry.class.getName());
  private final BasicContentRewriterRegistry baseRegistry;
  
  @Inject
  public CachingContentRewriterRegistry(ContentRewriter firstRewriter,
      CacheProvider cacheProvider,
      @Named("shindig.gadget-spec.cache.capacity")int capacity,
      @Named("shindig.gadget-spec.cache.minTTL")long minTtl,
      @Named("shindig.gadget-spec.cache.maxTTL")long maxTtl) {
    // Cache configuration values are currently identical to those for the spec
    // cache in BasicGadgetSpecFactory for backward compatibility (ensuring that if
    // caching is set up for specs, it's set up for rewritten content as well)
    // TODO: create separate rewritten-content config values.
    super(cacheProvider, capacity, minTtl, maxTtl);
    baseRegistry = new BasicContentRewriterRegistry(firstRewriter);
  }

  @Override
  protected String getCacheKeyFromQueryObj(Gadget gadget) {
    // Cache by URI + View. Since we always append a view, there should be no
    // key conflicts associated with this operation.
    return gadget.getSpec().getUrl().toString() + "#v=" + gadget.getCurrentView().getName();
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected FetchedObject<String> retrieveRawObject(Gadget gadget,
      boolean ignoreCache) throws GadgetException {
    // Always attempt to rewrite the inbound gadget object.
    // Even if that fails, the non-rewritten Gadget should be cached,
    // to avoid superfluous rewrites later.
    baseRegistry.rewriteGadget(gadget);
    
    // Expiration time of 30 minutes by default. This number is arbitrary.
    // TODO: Make this value configurable.
    long expiration = System.currentTimeMillis() + (1000 * 60 * 30);
    Object expirationObj = gadget.getSpec().getAttribute(GadgetSpec.EXPIRATION_ATTRIB);
    if (expirationObj instanceof Long) {
      expiration = (Long)expirationObj;
    }
    
    return new FetchedObject<String>(gadget.getContent(), expiration);
  }
  
  /** {@inheritDoc} */
  public List<ContentRewriter> getRewriters() {
    return baseRegistry.getRewriters();
  }

  /** {@inheritDoc} */
  public boolean rewriteGadget(Gadget gadget)
      throws GadgetException {
    String cached = doCachedFetch(gadget, gadget.getContext().getIgnoreCache());
    // At present, the output of rewriting is just the string contained within
    // the Gadget object. Thus, a successful cache hit results in copying over the
    // rewritten value to the input gadget object.
    // TODO: Clean up the ContentRewriter interface so rewriting "output" is clearer.
    // TODO: If necessary later, copy other modified contents to Gadget object.
    if (cached != null) {
      gadget.setContent(cached);
      return true;
    }
    return baseRegistry.rewriteGadget(gadget);
  }
  
  public void appendRewriter(ContentRewriter rewriter) {
    baseRegistry.appendRewriter(rewriter);
  }
}
