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

package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetServer;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;

/**
 * Validates and wraps a JSON input object into a
 *
 */
public class JsonRpcRequest {
  private final List<GadgetContext> gadgets;

  /**
   * Processes the request and returns a JSON object
   * That can be emitted as output.
   */
  public JSONObject process(CrossServletState servletState)
      throws RpcException {
    GadgetServer server = servletState.getGadgetServer();

    JSONObject out = new JSONObject();

    // Dispatch a separate thread for each gadget that we wish to render.
    CompletionService<Gadget> processor =
      new ExecutorCompletionService<Gadget>(server.getConfig().getExecutor());

    for (GadgetContext gadget : gadgets) {
      processor.submit(new JsonRpcGadgetJob(server, gadget));
    }

    int numJobs = gadgets.size();
    do {
      try {
        Gadget gadget = processor.take().get();
        JSONObject gadgetJson = new JSONObject();

        GadgetSpec spec = gadget.getSpec();
        ModulePrefs prefs = spec.getModulePrefs();

        JSONObject views = new JSONObject();
        for (View view : spec.getViews().values()) {
          views.put(view.getName(), new JSONObject()
               // .put("content", view.getContent())
               .put("type", view.getType().toString().toLowerCase())
               .put("quirks", view.getQuirks()));
        }

        // Features.
        Set<String> feats = prefs.getFeatures().keySet();
        String[] features = feats.toArray(new String[feats.size()]);

        JSONObject userPrefs = new JSONObject();

        // User pref specs
        for (UserPref pref : spec.getUserPrefs()) {
          JSONObject up = new JSONObject()
              .put("displayName", pref.getDisplayName())
              .put("type", pref.getDataType().toString().toLowerCase())
              .put("default", pref.getDefaultValue())
              .put("enumValues", pref.getEnumValues());
          userPrefs.put(pref.getName(), up);
        }

        gadgetJson.put("iframeUrl", servletState.getIframeUrl(gadget))
                  .put("url", gadget.getContext().getUrl().toString())
                  .put("moduleId", gadget.getContext().getModuleId())
                  .put("title", prefs.getTitle())
                  .put("titleUrl", prefs.getTitleUrl().toString())
                  .put("views", views)
                  .put("features", features)
                  .put("userPrefs", userPrefs)
                  // extended meta data
                  .put("directoryTitle", prefs.getDirectoryTitle())
                  .put("thumbnail", prefs.getThumbnail().toString())
                  .put("screenshot", prefs.getScreenshot().toString())
                  .put("author", prefs.getAuthor())
                  .put("authorEmail", prefs.getAuthorEmail())
                  .put("categories", prefs.getCategories())
                  .put("screenshot", prefs.getScreenshot().toString());
        out.append("gadgets", gadgetJson);
      } catch (InterruptedException e) {
        throw new RpcException("Incomplete processing", e);
      } catch (ExecutionException ee) {
        if (!(ee.getCause() instanceof RpcException)) {
          throw new RpcException("Incomplete processing", ee);
        }
        RpcException e = (RpcException)ee.getCause();
        // Just one gadget failed; mark it as such.
        try {
          GadgetContext context = e.getContext();

          if (context == null) {
            throw e;
          }

          JSONObject errorObj = new JSONObject();
          errorObj.put("url", context.getUrl())
                  .put("moduleId", context.getModuleId());
          if (e.getCause() instanceof GadgetException) {
            GadgetException gpe = (GadgetException)e.getCause();
            errorObj.append("errors", gpe.getMessage());
          } else {
            errorObj.append("errors", e.getMessage());
          }
          out.append("gadgets", errorObj);
        } catch (JSONException je) {
          throw new RpcException("Unable to write JSON", je);
        }
      } catch (JSONException e) {
        throw new RpcException("Unable to write JSON", e);
      } finally {
        numJobs--;
      }
    } while (numJobs > 0);

    return out;
  }

  public JsonRpcRequest(String content) throws RpcException {
    try {
      JSONObject json = new JSONObject(content);
      JSONObject context = json.getJSONObject("context");
      JSONArray gadgets = json.getJSONArray("gadgets");
      if (gadgets.length() == 0) {
        throw new RpcException("No gadgets requested.");
      }

      List<GadgetContext> gadgetList = new LinkedList<GadgetContext>();
      for (int i = 0, j = gadgets.length(); i < j; ++i) {
        gadgetList.add(new JsonRpcGadgetContext(context, gadgets.getJSONObject(i)));
      }
      this.gadgets = Collections.unmodifiableList(gadgetList);
    } catch (JSONException e) {
      throw new RpcException("Malformed JSON input.", e);
    }
  }
}
