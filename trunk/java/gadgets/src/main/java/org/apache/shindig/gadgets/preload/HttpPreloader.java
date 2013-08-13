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
package org.apache.shindig.gadgets.preload;

import org.apache.shindig.gadgets.FetchResponseUtils;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.oauth2.OAuth2Arguments;
import org.apache.shindig.gadgets.spec.Preload;
import org.apache.shindig.gadgets.spec.RequestAuthenticationInfo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Handles HTTP Preloading (/ModulePrefs/Preload elements).
 *
 * @see org.apache.shindig.gadgets.spec.Preload
 */
public class HttpPreloader implements Preloader {
  private final RequestPipeline requestPipeline;

  @Inject
  public HttpPreloader(RequestPipeline requestPipeline) {
    this.requestPipeline = requestPipeline;
  }

  public Collection<Callable<PreloadedData>> createPreloadTasks(Gadget gadget) {
    List<Callable<PreloadedData>> preloads = Lists.newArrayList();

    GadgetContext context = gadget.getContext();

    for (Preload preload : gadget.getSpec().getModulePrefs().getPreloads()) {
      Set<String> preloadViews = preload.getViews();
      if (preloadViews.isEmpty() || preloadViews.contains(context.getView())) {
        preloads.add(new PreloadTask(context, preload, preload.getHref().toString()));
      }
    }

    return preloads;
  }

  // TODO: move somewhere more sensible
  public static HttpRequest newHttpRequest(GadgetContext context,
      RequestAuthenticationInfo authenticationInfo) throws GadgetException {
    return new HttpRequest(authenticationInfo.getHref())
        .setSecurityToken(context.getToken())
        .setOAuthArguments(new OAuthArguments(authenticationInfo))
        .setOAuth2Arguments(new OAuth2Arguments(authenticationInfo))
        .setAuthType(authenticationInfo.getAuthType())
        .setContainer(context.getContainer())
        .setGadget(context.getUrl())
        .setIgnoreCache(context.getIgnoreCache());
  }

  class PreloadTask implements Callable<PreloadedData> {
    private final GadgetContext context;
    private final Preload preload;
    private final String key;

    public PreloadTask(GadgetContext context, Preload preload, String key) {
      this.context = context;
      this.preload = preload;
      this.key = key;
    }

    public PreloadedData call() throws Exception {
      HttpRequest request = newHttpRequest(context, preload);

      return new HttpPreloadData(requestPipeline.execute(request), key);
    }
  }

  /**
   * Implements PreloadData by returning a Map that matches the output format used by makeRequest.
   */
  private static class HttpPreloadData implements PreloadedData {
    private final Map<String, Object> data;

    public HttpPreloadData(HttpResponse response, String key) {
      this.data = FetchResponseUtils.getResponseAsJson(response, key,
          response.getResponseAsString(), false);
    }

    public Collection<Object> toJson() {
      return ImmutableList.of((Object) data);
    }
  }
}
