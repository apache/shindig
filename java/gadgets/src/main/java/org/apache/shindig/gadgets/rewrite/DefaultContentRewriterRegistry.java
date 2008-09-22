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
import org.apache.shindig.gadgets.MutableContent;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;

import com.google.inject.Inject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Basic registry -- just iterates over rewriters and invokes them sequentially.
 *
 * TODO: Make abstract and bind CachingContentRewriterRegistry as the default.
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
    this.rewriters = new LinkedList<ContentRewriter>(rewriters);
    this.htmlParser = htmlParser;
  }

  /** {@inheritDoc} */
  public boolean rewriteGadget(Gadget gadget) throws GadgetException {
    String originalContent = gadget.getContent();

    if (originalContent == null) {
      // Nothing to rewrite.
      return false;
    }

    for (ContentRewriter rewriter : rewriters) {
      rewriter.rewrite(gadget);
    }

    return !originalContent.equals(gadget.getContent());
  }

  /** {@inheritDoc} */
  public HttpResponse rewriteHttpResponse(HttpRequest req, HttpResponse resp) {
    MutableContent mc = new MutableContent(htmlParser);
    String originalContent = resp.getResponseAsString();
    mc.setContent(originalContent);

    for (ContentRewriter rewriter : rewriters) {
      rewriter.rewrite(req, resp, mc);
    }

    String rewrittenContent = mc.getContent();
    if (rewrittenContent.equals(originalContent)) {
      return resp;
    }

    return new HttpResponseBuilder(resp).setResponseString(rewrittenContent).create();
  }

}
