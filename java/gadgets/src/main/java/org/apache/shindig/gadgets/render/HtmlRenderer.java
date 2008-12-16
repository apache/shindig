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
package org.apache.shindig.gadgets.render;

import com.google.inject.Inject;
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.preload.PreloadException;
import org.apache.shindig.gadgets.preload.PreloadedData;
import org.apache.shindig.gadgets.preload.PreloaderService;
import org.apache.shindig.gadgets.preload.Preloads;
import org.apache.shindig.gadgets.rewrite.ContentRewriterRegistry;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;
import org.json.JSONArray;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles producing output markup for a gadget based on the provided context.
 */
public class HtmlRenderer {
  private final ContentFetcherFactory fetcher;
  private final PreloaderService preloader;
  private final ContentRewriterRegistry rewriter;

  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final Logger logger = Logger.getLogger(HtmlRenderer.class.getName());

  @Inject
  public HtmlRenderer(ContentFetcherFactory fetcher,
                      PreloaderService preloader,
                      ContentRewriterRegistry rewriter) {
    this.fetcher = fetcher;
    this.preloader = preloader;
    this.rewriter = rewriter;
  }

  /**
   * Render the gadget into a string by performing the following steps:
   *
   * - Retrieve gadget specification information (GadgetSpec, MessageBundle, etc.)
   *
   * - Fetch any preloaded data needed to handle the request, as handled by Preloader.
   *
   * - Perform rewriting operations on the output content, handled by Rewriter.
   *
   * @param gadget The gadget for the rendering operation.
   * @return The rendered gadget content
   * @throws RenderingException if any issues arise that prevent rendering.
   */
  public String render(Gadget gadget) throws RenderingException {
    try {
      View view = gadget.getCurrentView();
      GadgetContext context = gadget.getContext();
      GadgetSpec spec = gadget.getSpec();

      Preloads preloads = preloader.preload(context, spec,
          PreloaderService.PreloadPhase.HTML_RENDER);
      gadget.setPreloads(preloads);

      String content;

      if (view.getHref() == null) {
        content = view.getContent();
      } else {
        Preloads proxyPreloads = preloader.preload(context, spec,
            PreloaderService.PreloadPhase.PROXY_FETCH);

        // TODO: Add current url to GadgetContext to support transitive proxying.
        UriBuilder uri = new UriBuilder(view.getHref());
        uri.addQueryParameter("lang", context.getLocale().getLanguage());
        uri.addQueryParameter("country", context.getLocale().getCountry());

        HttpRequest request = new HttpRequest(uri.toUri())
            .setIgnoreCache(context.getIgnoreCache())
            .setOAuthArguments(new OAuthArguments(view))
            .setAuthType(view.getAuthType())
            .setSecurityToken(context.getToken())
            .setContainer(context.getContainer())
            .setGadget(spec.getUrl());

        // POST any preloaded content
        if ((proxyPreloads != null) && !proxyPreloads.getData().isEmpty()) {
          JSONArray array = new JSONArray();

          for (PreloadedData preload : proxyPreloads.getData()) {
            try {
              Map<String, Object> dataMap = preload.toJson();
              for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                // TODO: the existing, supported content is JSONObjects that contain the
                // key already.  Discarding the key is odd.
                array.put(entry.getValue());
              }
            } catch (PreloadException pe) {
              logger.log(Level.WARNING, "Unexpected error when preloading", pe);
            }
          }

          String postContent = JsonSerializer.serialize(array);
          // POST the preloaded content, with a method override of GET
          // to enable caching
          request.setMethod("POST")
              .addHeader("X-Method-Override", "GET")
              .setPostBody(UTF8.encode(postContent).array());
        }

        HttpResponse response = fetcher.fetch(request);
        if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
          throw new RenderingException("Unable to reach remote host. HTTP status " +
              response.getHttpStatusCode());
        }

        content = response.getResponseAsString();
      }

      return rewriter.rewriteGadget(gadget, content);
    } catch (GadgetException e) {
      throw new RenderingException(e.getMessage(), e);
    }
  }
}
