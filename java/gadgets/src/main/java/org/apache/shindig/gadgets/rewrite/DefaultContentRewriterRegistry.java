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

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.spec.View;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Basic registry -- just iterates over rewriters and invokes them sequentially.
 */
public class DefaultContentRewriterRegistry implements ContentRewriterRegistry {
  protected final List<ContentRewriter> rewriters;
  protected final GadgetHtmlParser htmlParser;

  @Inject
  public DefaultContentRewriterRegistry(List<ContentRewriter> rewriters,
      GadgetHtmlParser htmlParser) {
    if (rewriters == null) {
      rewriters = Collections.emptyList();
    }
    this.rewriters = Lists.newLinkedList(rewriters);
    this.htmlParser = htmlParser;
  }

  /** {@inheritDoc} */
  public String rewriteGadget(Gadget gadget, View currentView) throws GadgetException {
    if (currentView == null) {
      // Nothing to rewrite.
      return null;
    }
    MutableContent mc = new MutableContent(htmlParser, currentView.getContent());

    for (ContentRewriter rewriter : rewriters) {
      rewriter.rewrite(gadget, mc);
    }
    return mc.getContent();
  }

  /** {@inheritDoc} */
  public String rewriteGadget(Gadget gadget, String content) {
    if (content == null) {
      // Nothing to rewrite.
      return null;
    }
    MutableContent mc = new MutableContent(htmlParser, content);

    for (ContentRewriter rewriter : rewriters) {
      rewriter.rewrite(gadget, mc);
    }

    return mc.getContent();
  }

  /** {@inheritDoc} */
  public HttpResponse rewriteHttpResponse(HttpRequest req, HttpResponse resp) {
    MutableContent mc = new MutableContent(htmlParser, resp);

    boolean wasRewritten = false;
    for (ContentRewriter rewriter : rewriters) {
      wasRewritten |= (rewriter.rewrite(req, resp, mc) != null);
    }

    if (wasRewritten) {
      return new HttpResponseBuilder(resp).setResponseString(mc.getContent()).create();
    }
    return resp;
  }

  protected List<ContentRewriter> getRewriters() {
    return rewriters;
  }
}
