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

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.Preload;
import org.apache.shindig.gadgets.spec.RequestAuthenticationInfo;
import org.apache.shindig.gadgets.spec.View;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Preloader for loading Data Pipelining Preload data.
 */
public class PipelinedDataPreloader implements Preloader {
  private final RequestPipeline requestPipeline;
  private final ContainerConfig config;

  private static final Charset UTF8 = Charset.forName("UTF-8");

  @Inject
  public PipelinedDataPreloader(RequestPipeline requestPipeline, ContainerConfig config) {
    this.requestPipeline = requestPipeline;
    this.config = config;
  }

  public Collection<Callable<PreloadedData>> createPreloadTasks(GadgetContext context,
      GadgetSpec gadget, PreloaderService.PreloadPhase phase) {
    View view = gadget.getView(context.getView());
    if (view != null
        && view.getPipelinedData() != null
        && phase == PreloaderService.PreloadPhase.PROXY_FETCH) {

      List<Callable<PreloadedData>> preloadList = Lists.newArrayList();
      Map<String, Object> socialPreloads = view.getPipelinedData().getSocialPreloads(context);

      // Load any social preloads into a JSONArray for delivery to
      // JsonRpcServlet
      if (!socialPreloads.isEmpty()) {
        JSONArray array = new JSONArray();
        for (Object socialRequest : socialPreloads.values()) {
          array.put(socialRequest);
        }

        Callable<PreloadedData> preloader = new SocialPreloadTask(context, array);
        preloadList.add(preloader);
      }

      Map<String, RequestAuthenticationInfo> httpPreloads =
          view.getPipelinedData().getHttpPreloads(context);
      if (!httpPreloads.isEmpty()) {
        for (Map.Entry<String, RequestAuthenticationInfo> httpPreloadEntry
            : httpPreloads.entrySet()) {
          preloadList.add(new HttpPreloadTask(context,  httpPreloadEntry.getValue(),
              httpPreloadEntry.getKey()));
        }

      }

      return preloadList;
    }

    return Collections.emptyList();
  }

  /**
   * Callable for issuing HttpRequests to JsonRpcServlet.
   */
  private class SocialPreloadTask implements Callable<PreloadedData> {

    private final GadgetContext context;
    private final JSONArray array;

    public SocialPreloadTask(GadgetContext context, JSONArray array) {
      this.context = context;
      this.array = array;
    }

    public PreloadedData call() throws Exception {
      Uri uri = getSocialUri(context);
      HttpRequest request = new HttpRequest(uri)
          .setIgnoreCache(context.getIgnoreCache())
          .setSecurityToken(context.getToken())
          .setMethod("POST")
          .setAuthType(AuthType.NONE)
          .setPostBody(UTF8.encode(array.toString()).array())
          .addHeader("Content-Type", "application/json; charset=UTF-8")
          .setContainer(context.getContainer())
          .setGadget(Uri.fromJavaUri(context.getUrl()));

      HttpResponse response = requestPipeline.execute(request);

      // Unpack the response into a map of PreloadedData responses
      final Map<String, Object> data = Maps.newHashMap();
      JSONArray array = new JSONArray(response.getResponseAsString());
      for (int i = 0; i < array.length(); i++) {
        JSONObject arrayElement = array.getJSONObject(i);

        // The posted form is just the returned data.
        String id = arrayElement.getString("id");
        data.put(id, arrayElement);
      }

      return new PreloadedData() {
        public Map<String, Object> toJson() {
          return data;
        }
      };
    }
  }

  // A task for preloading os:MakeRequest
  class HttpPreloadTask implements Callable<PreloadedData> {
    private final GadgetContext context;
    private final RequestAuthenticationInfo preload;
    private final String key;

    public HttpPreloadTask(GadgetContext context, RequestAuthenticationInfo preload, String key) {
      this.context = context;
      this.preload = preload;
      this.key = key;
    }

    public PreloadedData call() throws Exception {
      HttpRequest request = HttpPreloader.newHttpRequest(context, preload);
      String refreshIntervalStr = preload.getAttributes().get("refreshInterval");
      if (refreshIntervalStr != null) {
        try {
          int refreshInterval = Integer.parseInt(refreshIntervalStr);
          request.setCacheTtl(refreshInterval);
        } catch (NumberFormatException nfe) {
          // Ignore, and use the HTTP response interval
        }
      }
      
      // TODO: support non GET/POST methods
      String method = preload.getAttributes().get("method");
      if (method != null) {
        request.setMethod(method);
      }
      
      // TODO: support params
      return new Data(requestPipeline.execute(request));
    }

    // TODO: is this format correct?
    // TODO: change HttpPreloader to use this format?
    class Data implements PreloadedData {
      private final JSONObject data;

      public Data(HttpResponse response) {
        String format = preload.getAttributes().get("format");
        JSONObject data = new JSONObject();

        try {
          data.put("id", key);

          if (format == null || "json".equals(format)) {
            try {
              data.put("data", new JSONObject(response.getResponseAsString()));
            } catch (JSONException je) {
              data.put("code", 500);
              data.put("message", je.getMessage());
            }
          } else {
            if (response.isError()) {
              data.put("code", response.getHttpStatusCode());
              data.put("message", response.getResponseAsString());
            } else {
              data.put("data", response.getResponseAsString());
            }
          }
        } catch (JSONException outerJe) {
          throw new RuntimeException(outerJe);
        }

        this.data = data;
      }

      public Map<String, Object> toJson() {
        return ImmutableMap.of(key, (Object) data);
      }
    }
  }

  private Uri getSocialUri(GadgetContext context) {
    String jsonUri = config.get(context.getContainer(), "gadgets.osDataUri");
    if (jsonUri == null) {
      throw new NullPointerException("No JSON URI available for social preloads");
    }

    UriBuilder builder = UriBuilder.parse(
        jsonUri.replace("%host%", context.getHost()))
        //TODO: bogus?  find correct way.
        .addQueryParameter("st", context.getParameter("st"));
    return builder.toUri();
  }
}
