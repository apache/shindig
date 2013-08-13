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

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Basic registry -- just iterates over rewriters and invokes them sequentially.
 *
 * @since 2.0.0
 */
public class DefaultResponseRewriterRegistry implements ResponseRewriterRegistry {
  protected final List<ResponseRewriter> rewriters;
  protected final GadgetHtmlParser htmlParser;

  @Inject
  public DefaultResponseRewriterRegistry(List<ResponseRewriter> rewriters,
      GadgetHtmlParser htmlParser) {
    if (rewriters == null) {
      rewriters = Collections.emptyList();
    }
    this.rewriters = Lists.newLinkedList(rewriters);
    this.htmlParser = htmlParser;
  }

  /** {@inheritDoc} */
  public HttpResponse rewriteHttpResponse(HttpRequest req, HttpResponse resp, Gadget gadget)
          throws RewritingException {
    HttpResponseBuilder builder = new HttpResponseBuilder(htmlParser, resp);

    for (ResponseRewriter rewriter : rewriters) {
      rewriter.rewrite(req, builder, gadget);
    }

    // Returns the original HttpResponse if no changes have been made.
    return builder.create();
  }
}
