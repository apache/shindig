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
import org.apache.shindig.common.uri.Uri;
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
import org.apache.shindig.gadgets.spec.View;
import org.json.JSONArray;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Implements proxied rendering.
 */
public class ProxyRenderer {
  public static final String PATH_PARAM = "path";
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final Logger logger = Logger.getLogger(ProxyRenderer.class.getName());

  private RequestPipeline requestPipeline;
  private HttpCache httpCache;
  private PreloaderService preloader;

  /**
   * @param requestPipeline Used for performing the proxy request. Always ignores caching because
   *                        we want to skip preloading when the object is in the cache.
   * @param httpCache The shared http cache. Used before checking the request pipeline to determine
   *                  whether to perform the preload / fetch cycle.
   */
  @Inject
  public ProxyRenderer(RequestPipeline requestPipeline,
      HttpCache httpCache, PreloaderService preloader) {
    this.requestPipeline = requestPipeline;
    this.httpCache = httpCache;
    this.preloader = preloader;
  }

  public String render(Gadget gadget) throws RenderingException, GadgetException {
    View view = gadget.getCurrentView();
    Uri href = view.getHref();
    Preconditions.checkArgument(href != null, "Gadget does not have href for the current view");
    
    GadgetContext context = gadget.getContext();
    String path = context.getParameter(PATH_PARAM);
    if (path != null) {
      try {
        Uri relative = Uri.parse(path);
        if (!relative.isAbsolute()) {
          href = href.resolve(relative);
        }
      } catch (IllegalArgumentException e) {
        // TODO: Spec does not say what to do for an invalid relative path.
        // Just ignoring for now.
      }
    }

    UriBuilder uri = new UriBuilder(href);
    uri.addQueryParameter("lang", context.getLocale().getLanguage());
    uri.addQueryParameter("country", context.getLocale().getCountry());

    HttpRequest request = new HttpRequest(uri.toUri())
        .setIgnoreCache(context.getIgnoreCache())
        .setOAuthArguments(new OAuthArguments(view))
        .setAuthType(view.getAuthType())
        .setSecurityToken(context.getToken())
        .setContainer(context.getContainer())
        .setGadget(gadget.getSpec().getUrl());

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

    return response.getResponseAsString();    
  }

  /**
   * Creates a proxy request by fetching pipelined data and adding it to an existing request.
   *
   */
  private HttpRequest createPipelinedProxyRequest(Gadget gadget, HttpRequest original) {
    HttpRequest request = new HttpRequest(original);
    request.setIgnoreCache(true);
    Collection<PreloadedData> proxyPreloads = preloader.preload(gadget,
          PreloaderService.PreloadPhase.PROXY_FETCH);
    // TODO: Add current url to GadgetContext to support transitive proxying.

    // POST any preloaded content
    if ((proxyPreloads != null) && !proxyPreloads.isEmpty()) {
      JSONArray array = new JSONArray();

      for (PreloadedData preload : proxyPreloads) {
        try {
          for (Object entry : preload.toJson()) {
            array.put(entry);
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
          .setHeader("Content-Type", "application/json;charset=utf-8");
    }
    return request;
  }
}
