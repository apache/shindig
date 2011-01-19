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
 * all the above mentioned cases, we trigger a background fetch for the
 * resource. This visitor should be used by a rewriter in conjuction with other
 * visitors which depend on the uri of the html node being in cache.
 *
 */
public class CacheEnforcementVisitor extends ResourceMutateVisitor {

  private static final Logger logger = Logger.getLogger(
      CacheEnforcementVisitor.class.getName());
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
    // TODO: If the response is strict no cache, then we should reserve the node.
    // Currently, we will keep triggering fetches for these resources.
    // Also, investigate the Cache-Control=no-cache case. Should we proxy resources in this case?
    // Also, investigate when Cache-Control is max-age=0. We are currently triggering a fetch in
    // this case. We should look at the TTL of the original response and if that is zero, we
    // shouldn't trigger a fetch for the resource.
    if (response.isStale() || response.isError()) {
      // Trigger a fetch if the response is stale or an error, and reserve the node.
      triggerFetch(uri);
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

      @Override
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
