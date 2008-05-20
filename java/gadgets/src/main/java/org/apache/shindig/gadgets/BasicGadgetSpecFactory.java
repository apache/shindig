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
package org.apache.shindig.gadgets;

import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.net.URI;

/**
 * Basic implementation of a gadget spec factory
 */
@Singleton
public class BasicGadgetSpecFactory implements GadgetSpecFactory {

  private final HttpFetcher specFetcher;
  private final ContentRewriter rewriter;
  private final boolean enableRewrite;

  public GadgetSpec getGadgetSpec(GadgetContext context)
      throws GadgetException {
    return getGadgetSpec(context.getUrl(), context.getIgnoreCache());
  }

  public GadgetSpec getGadgetSpec(URI gadgetUri, boolean ignoreCache)
      throws GadgetException {
    HttpRequest request = HttpRequest.getRequest(
        gadgetUri, ignoreCache);
    HttpResponse response = specFetcher.fetch(request);
    if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
      throw new GadgetException(
          GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
          "Unable to retrieve gadget xml. HTTP error " +
              response.getHttpStatusCode());
    }
    GadgetSpec spec
        = new GadgetSpec(gadgetUri, response.getResponseAsString());
    if (new ContentRewriterFeature(spec, enableRewrite).isRewriteEnabled()) {
      for (View v : spec.getViews().values()) {
        if (v.getType() == View.ContentType.HTML && rewriter != null) {
          v.setRewrittenContent(
              rewriter.rewrite(gadgetUri, v.getContent(), "text/html"));
        }
      }
    }
    return spec;
  }

  @Inject
  public BasicGadgetSpecFactory(HttpFetcher specFetcher,
      ContentRewriter rewriter,
      @Named("content-rewrite.enabled") boolean defaultEnableRewrite) {
    this.specFetcher = specFetcher;
    this.rewriter = rewriter;
    this.enableRewrite = defaultEnableRewrite;
  }
}