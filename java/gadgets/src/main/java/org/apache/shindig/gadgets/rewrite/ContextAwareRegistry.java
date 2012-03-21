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
import com.google.inject.Provider;

import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterList.RewriteFlow;

import java.util.List;
import java.util.Map;

/**
 * An implementation of ResponseRewriterRegistry which applies the list of
 * rewriters based on the container and the rewrite flow id.
 */
public class ContextAwareRegistry implements ResponseRewriterRegistry {
  protected final GadgetHtmlParser htmlParser;
  protected final RewriteFlow rewriteFlow;
  protected final Provider<Map<RewritePath, Provider<List<ResponseRewriter>>>>
      rewritePathToRewriterList;

  public ContextAwareRegistry(GadgetHtmlParser htmlParser,
                              RewriteFlow rewriteFlow,
                              Provider<Map<RewritePath, Provider<List<ResponseRewriter>>>>
                                  rewritePathToRewriterList) {
    this.rewriteFlow = rewriteFlow;
    this.rewritePathToRewriterList = rewritePathToRewriterList;
    this.htmlParser = htmlParser;
  }

  /**
   * Returns the list of response rewriters for the given container. Falls back
   * to the default container if no rewriters are present for the given
   * container.
   * @param container The container to return rewriters for.
   * @return List of response rewriters for given container and rewrite flow.
   */
  List<ResponseRewriter> getResponseRewriters(String container) {
    RewritePath rewritePath = new RewritePath(container,  rewriteFlow);
    Provider<List<ResponseRewriter>> rewriterListProvider =
        rewritePathToRewriterList.get().get(rewritePath);

    if (rewriterListProvider == null) {
      // Try default container if there are no rewriters provided for current container.
      rewritePath = new RewritePath(ContainerConfig.DEFAULT_CONTAINER,  rewriteFlow);
      rewriterListProvider = rewritePathToRewriterList.get().get(rewritePath);
    }

    return rewriterListProvider != null ? rewriterListProvider.get() :
                                          ImmutableList.<ResponseRewriter>of();
  }

  public HttpResponse rewriteHttpResponse(HttpRequest req, HttpResponse resp,
          Gadget gadget) throws RewritingException {
    HttpResponseBuilder builder = new HttpResponseBuilder(htmlParser, resp);
    for (ResponseRewriter rewriter : getResponseRewriters(req.getContainer())) {
      rewriter.rewrite(req, builder, gadget);
    }

    // Returns the original HttpResponse if no changes have been made.
    return builder.create();
  }
}
