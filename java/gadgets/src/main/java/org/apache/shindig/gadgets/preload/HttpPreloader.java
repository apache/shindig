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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.Preload;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Handles HTTP Preloading (/ModulePrefs/Preload elements).
 *
 * @see org.apache.shindig.gadgets.spec.Preload
 */
public class HttpPreloader implements Preloader {
  // TODO: This needs to be fixed.
  private final ContentFetcherFactory fetcher;

  @Inject
  public HttpPreloader(ContentFetcherFactory fetcherFactory) {
    this.fetcher = fetcherFactory;
  }

  public Map<String, Callable<PreloadedData>> createPreloadTasks(GadgetContext context,
      GadgetSpec gadget) {
    Map<String, Callable<PreloadedData>> preloads = Maps.newHashMap();

    for (Preload preload : gadget.getModulePrefs().getPreloads()) {
      preloads.put(preload.getHref().toString(), new PreloadTask(context, preload));
    }

    return preloads;
  }

  private class PreloadTask implements Callable<PreloadedData> {
    private final GadgetContext context;
    private final Preload preload;

    public PreloadTask(GadgetContext context, Preload preload) {
      this.context = context;
      this.preload = preload;
    }

    public PreloadedData call() throws Exception {
      // TODO: This should be extracted into a common helper that takes any
      // org.apache.shindig.gadgets.spec.RequestAuthenticationInfo.
      HttpRequest request = new HttpRequest(Uri.fromJavaUri(preload.getHref()))
            .setSignOwner(preload.isSignOwner())
            .setSignViewer(preload.isSignViewer())
            .setContainer(context.getContainer())
            .setSecurityToken(context.getToken())
            .setGadget(Uri.fromJavaUri(context.getUrl()));
      HttpResponse response = null;
      switch (preload.getAuthType()) {
        case NONE:
          response = fetcher.get().fetch(request);
          break;
        case SIGNED:
          response = fetcher.getSigningFetcher(context.getToken()).fetch(request);
          break;
        case OAUTH:
          response = fetcher.getOAuthFetcher(context.getToken(), new OAuthArguments(preload))
              .fetch(request);
          break;
      }
      return new HttpPreloadData(response);
    }
  }

  /**
   * Implements PreloadData by returning a Map that matches the output format used by makeRequest.
   */
  private static class HttpPreloadData implements PreloadedData {
    private final Map<String, String> data;

    public HttpPreloadData(HttpResponse response) {
      data = Maps.newHashMap(response.getMetadata());
      data.put("body", response.getResponseAsString());
      data.put("rc", Integer.toString(response.getHttpStatusCode()));
    }

    public Object toJson() {
      return data;
    }
  }
}
