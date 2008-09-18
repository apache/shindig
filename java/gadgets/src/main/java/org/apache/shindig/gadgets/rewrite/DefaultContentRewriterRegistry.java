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

import com.google.inject.Inject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.MutableContent;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;

/**
 * Registry into which is injected a single rewriter, which
 * bootstraps the rewriters list. This enables modularization
 * of {@code ContentRewriter} instances without changing
 * Guice injection bindings. The class also provides a method
 * for manipulating a simple list of rewriters. It does not
 * support caching of rewritten contents in any way.
 */
public class DefaultContentRewriterRegistry implements ContentRewriterRegistry {
  private final List<ContentRewriter> rewriters;
  private final GadgetHtmlParser htmlParser;
  
  @Inject
  public DefaultContentRewriterRegistry(ContentRewriter firstRewriter,
      GadgetHtmlParser htmlParser) {
    this.rewriters = new LinkedList<ContentRewriter>();
    this.htmlParser = htmlParser;
    appendRewriter(firstRewriter);
  }
  
  /** {@inheritDoc} */
  public List<ContentRewriter> getRewriters() {
    return Collections.unmodifiableList(rewriters);
  }
  
  public void appendRewriter(ContentRewriter rewriter) {
    if (rewriter != null) {
      rewriters.add(rewriter);
    }
  }
  
  /** {@inheritDoc} */
  public boolean rewriteGadget(Gadget gadget) throws GadgetException {
    String originalContent = gadget.getContent();
    
    if (originalContent == null) {
      // Nothing to rewrite.
      return false;
    }

    for (ContentRewriter rewriter : getRewriters()) {
      rewriter.rewrite(gadget);
    }
    
    return !originalContent.equals(gadget.getContent());
  }
  
  /** {@inheritDoc} */
  public HttpResponse rewriteHttpResponse(HttpRequest req, HttpResponse resp) {
    MutableContent mc = new MutableContent(htmlParser);
    String originalContent = resp.getResponseAsString();
    mc.setContent(originalContent);
    
    for (ContentRewriter rewriter : getRewriters()) {
      rewriter.rewrite(req, resp, mc);
    }
    
    String rewrittenContent = mc.getContent();
    if (rewrittenContent.equals(originalContent)) {
      return resp;
    }
    
    return new HttpResponseBuilder(resp).setResponseString(rewrittenContent).create();
  }

}
