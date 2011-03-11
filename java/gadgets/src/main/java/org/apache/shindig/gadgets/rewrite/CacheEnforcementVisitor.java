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
package org.apache.shindig.gadgets.rewrite;

import com.google.common.collect.ImmutableList;

import org.apache.shindig.common.Pair;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpCache;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Visitor that walks over html tags as specified by {@code resourceTags} and
 * reserves html tag nodes whose uri attributes are either not in cache, or are
 * in cache, but the response in cache is either stale or an error response. In
 * all the above mentioned cases except for the error case, we trigger a
 * background fetch for the resource. This visitor should be used by a rewriter
 * in conjuction with other visitors which depend on the uri of the html node
 * being in cache.
 *
 * Note that in order to use the CacheEnforcementVisitor effectively, the
 * shindig property shindig.cache.http.strict-no-cache-resource.max-age should
 * be set to a positive value, so that strict no-cache resources are stored in
 * cache with this ttl, and unnecessary fetches are not triggered each time for
 * such resources.
 *
 */
public class CacheEnforcementVisitor extends ResourceMutateVisitor {

  private static final Logger logger = Logger.getLogger(CacheEnforcementVisitor.class.getName());
  private final HttpCache cache;
  private final RequestPipeline requestPipeline;
  private final Executor executor;


  /**
   * Constructs the cache enforcement visitor.
   */
  public CacheEnforcementVisitor(ContentRewriterFeature.Config featureConfig,
                                 Executor executor,
                                 HttpCache cache,
                                 RequestPipeline requestPipeline,
                                 Tags... resourceTags) {
    super(featureConfig, resourceTags);
    this.executor = executor;
    this.cache = cache;
    this.requestPipeline = requestPipeline;
  }

  @Override
  public VisitStatus visit(Gadget gadget, Node node) throws RewritingException {
    if (super.visit(gadget, node).equals(VisitStatus.RESERVE_NODE)) {
      Element element = (Element) node;
      String nodeName = node.getNodeName().toLowerCase();
      String uriStr = element.getAttribute(resourceTags.get(nodeName)).trim();
      // TODO: Construct other attributes of the HttpRequest using the passed in
      // gadget.
      HttpResponse response = cache.getResponse(new HttpRequest(Uri.parse(uriStr)));
      if (response == null) {
        return handleResponseNotInCache(uriStr);
      } else {
        return handleResponseInCache(uriStr, response);
      }
    }
    return VisitStatus.BYPASS;
  }

  /**
   * The action to be performed if the response is in cache.
   *
   * @param uri The uri of the node.
   * @param response The HttpResponse retrieved from cache.
   * @return The visit status of the node.
   */
  protected VisitStatus handleResponseInCache(String uri, HttpResponse response) {
    if (response.shouldRefetch()) {
      // Reserve the node if the response should be refetched.
      if (response.getCacheControlMaxAge() != 0) {
        // If the cache-control max-age of the original response is zero, it doesn't make sense to
        // trigger a pre-fetch for it, since by the time the request for it comes in, it will
        // already be stale. Such resources will continuously be prefetched, but the fetched
        // response will never be used.
        // TODO: While we definitely should not be pre-fetching resources with a max-age of 0, other
        // cases with a very small max-age(say 1s) should also probably not be pre-fetched either
        // since the response might not be usable by the time the actual request comes in. Also
        // we should consider the cases with no max-age, but instead an Expires header which is
        // close to, or the same as the Date header.
        triggerFetch(uri);        
      }
      return VisitStatus.RESERVE_NODE;
    } else if (response.isStrictNoCache() || response.getHeader("Set-Cookie") != null ||
               response.isError()) {
      // If the response is strict no-cache, or has a Set-Cookie header or is an error response,
      // reserve the node. Do not trigger a fetch, since pre-fetching the resource doesn't help as
      // the response will not be cached. Also, for the error case, since we already set the
      // ttl for such resources to 30 seconds, we should not keep pre-fetching these till they
      // become stale.
      return VisitStatus.RESERVE_NODE;
    } else {
      // Otherwise, we assume the cached response is valid and bypass the node.
      return VisitStatus.BYPASS;
    }
  }

  /**
   * The action to be performed if the response is not in cache.
   *
   * @param uri The uri of the node.
   * @return The visit status of the node.
   */
  protected VisitStatus handleResponseNotInCache(String uri) {
    triggerFetch(uri);
    return VisitStatus.RESERVE_NODE;
  }

  /**
   * Triggers a background fetch for a resource.
   *
   * @param uri The uri
   */
  protected void triggerFetch(final String uri) {

    executor.execute(new Runnable() {

      public void run() {
        try {
          requestPipeline.execute(new HttpRequest(Uri.parse(uri)));
        } catch (GadgetException e) {
          logger.log(Level.WARNING, "Triggered fetch failed for " + uri, e);
        }
      }
    });
  }

  @Override
  protected Collection<Pair<Node, Uri>> mutateUris(Gadget gadget, Collection<Node> nodes) {
    return ImmutableList.of();
  }
}
