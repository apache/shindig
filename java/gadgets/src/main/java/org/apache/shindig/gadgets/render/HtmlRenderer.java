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

import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpCache;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.preload.PreloadException;
import org.apache.shindig.gadgets.preload.PreloadedData;
import org.apache.shindig.gadgets.preload.PreloaderService;
import org.apache.shindig.gadgets.preload.Preloads;
import org.apache.shindig.gadgets.rewrite.ContentRewriterRegistry;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;

import com.google.inject.Inject;

import org.json.JSONArray;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles producing output markup for a gadget based on the provided context.
 */
public class HtmlRenderer {
  private final RequestPipeline requestPipeline;
  private final HttpCache httpCache;
  private final PreloaderService preloader;
  private final ContentRewriterRegistry rewriter;

  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final Logger logger = Logger.getLogger(HtmlRenderer.class.getName());

  /**
   * @param requestPipeline Used for performing the proxy request. Always ignores caching because
   *                        we want to skip preloading when the object is in the cache.
   * @param httpCache The shared http cache. Used before checking the request pipeline to determine
   *                  whether to perform the preload / fetch cycle.
   */
  @Inject
  public HtmlRenderer(RequestPipeline requestPipeline,
                      HttpCache httpCache,
                      PreloaderService preloader,
                      ContentRewriterRegistry rewriter) {
    this.requestPipeline = requestPipeline;
    this.httpCache = httpCache;
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

      // We always execute these preloads, they have nothing to do with the cache output.
      Preloads preloads = preloader.preload(context, spec,
          PreloaderService.PreloadPhase.HTML_RENDER);
      gadget.setPreloads(preloads);

      String content;

      if (view.getHref() == null) {
        content = view.getContent();
      } else {
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

        HttpResponse response = httpCache.getResponse(request);

        if (response == null || response.isStale()) {
          HttpRequest proxyRequest = createPipelinedProxyRequest(gadget, request);
          response = requestPipeline.execute(proxyRequest);
          httpCache.addResponse(request, response);
        }

        if (response.isError()) {
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

  /**
   * Creates a proxy request by fetching pipelined data and adding it to an existing request.
   *
   */
  private HttpRequest createPipelinedProxyRequest(Gadget gadget, HttpRequest original) {
    HttpRequest request = new HttpRequest(original);
    request.setIgnoreCache(true);
    GadgetSpec spec = gadget.getSpec();
    GadgetContext context = gadget.getContext();
    Preloads proxyPreloads = preloader.preload(context, spec,
          PreloaderService.PreloadPhase.PROXY_FETCH);
    // TODO: Add current url to GadgetContext to support transitive proxying.

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
          // TODO: Determine whether this is a terminal path for the request. The spec is not
          // clear.
          logger.log(Level.WARNING, "Unexpected error when preloading", pe);
        }
      }

      String postContent = JsonSerializer.serialize(array);
      // POST the preloaded content, with a method override of GET
      // to enable caching
      request.setMethod("POST")
          .setPostBody(UTF8.encode(postContent).array())
          .setHeader("Content-Type", "text/json;charset=utf-8");
    }
    return request;
  }
}
