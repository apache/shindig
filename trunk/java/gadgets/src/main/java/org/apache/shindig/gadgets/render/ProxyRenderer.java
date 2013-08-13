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
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpCache;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.oauth2.OAuth2Arguments;
import org.apache.shindig.gadgets.preload.PipelineExecutor;
import org.apache.shindig.gadgets.spec.PipelinedData;
import org.apache.shindig.gadgets.spec.View;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletResponse;

/**
 * Implements proxied rendering.
 */
public class ProxyRenderer {
  public static final String PATH_PARAM = "path";
  public static final String UA_IDENT = "Shindig";

  private final RequestPipeline requestPipeline;
  private final HttpCache httpCache;
  private final PipelineExecutor pipelineExecutor;

  /**
   * @param requestPipeline Used for performing the proxy request. Always ignores caching because
   *                        we want to skip preloading when the object is in the cache.
   * @param httpCache The shared http cache. Used before checking the request pipeline to determine
   *                  whether to perform the preload / fetch cycle.
   */
  @Inject
  public ProxyRenderer(RequestPipeline requestPipeline,
      HttpCache httpCache, PipelineExecutor pipelineExecutor) {
    this.requestPipeline = requestPipeline;
    this.httpCache = httpCache;
    this.pipelineExecutor = pipelineExecutor;
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

    OAuthArguments oauthArgs = new OAuthArguments(view);
    OAuth2Arguments oauth2Args = new OAuth2Arguments(view);
    oauthArgs.setProxiedContentRequest(true);

    HttpRequest request = new HttpRequest(uri.toUri())
        .setIgnoreCache(context.getIgnoreCache())
        .setOAuthArguments(oauthArgs)
        .setOAuth2Arguments(oauth2Args)
        .setAuthType(view.getAuthType())
        .setSecurityToken(context.getToken())
        .setContainer(context.getContainer())
        .setGadget(gadget.getSpec().getUrl());
    setUserAgent(request, context);

    HttpResponse response = httpCache.getResponse(request);

    if (response == null || response.isStale()) {
      HttpRequest proxyRequest = createPipelinedProxyRequest(gadget, request);
      response = requestPipeline.execute(proxyRequest);
      httpCache.addResponse(request, response);
    }

    if (response.isError()) {
      throw new RenderingException("Unable to reach remote host. HTTP status " +
        response.getHttpStatusCode(), HttpServletResponse.SC_NOT_FOUND);
    }

    return response.getResponseAsString();
  }

  /**
   * Creates a proxy request by fetching pipelined data and adding it to an existing request.
   *
   */
  protected HttpRequest createPipelinedProxyRequest(Gadget gadget, HttpRequest original) {
    HttpRequest request = new HttpRequest(original);
    request.setIgnoreCache(true);

    PipelinedData data = gadget.getCurrentView().getPipelinedData();
    if (data != null) {
      PipelineExecutor.Results results =
        pipelineExecutor.execute(gadget.getContext(), ImmutableList.of(data));

      if (results != null && !results.results.isEmpty()) {
        String postContent = JsonSerializer.serialize(results.results);
        // POST the preloaded content, with a method override of GET
        // to enable caching
        request.setMethod("POST")
            .setPostBody(CharsetUtil.getUtf8Bytes(postContent))
            .setHeader("Content-Type", "application/json;charset=utf-8");
      }
    }

    return request;
  }

  /**
   * Sets the User-Agent header in the new request to a variant of the original
   * request's User-Agent, plus a small ident string for the gadget server.
   */
  private void setUserAgent(HttpRequest request, GadgetContext context) {
    String userAgent = context.getUserAgent();
    if (userAgent != null) {
      String myIdent = getUAIdent();
      if (myIdent != null) {
        userAgent = userAgent + ' ' + myIdent;
      }
      request.setHeader("User-Agent", userAgent);
    }
  }

  /**
   * Returns the program name which will be added at the end of the User-Agent
   * string, to identify the gadget server.
   */
  protected String getUAIdent() {
    return UA_IDENT;
  }
}
