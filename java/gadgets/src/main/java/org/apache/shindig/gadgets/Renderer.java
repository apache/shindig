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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;

import com.google.inject.Inject;

/**
 * Handles producing output markup for a gadget based on the provided context.
 */
public class Renderer {
  private final GadgetSpecFactory gadgetSpecFactory;
  private final HttpFetcher httpFetcher;

  @Inject
  public Renderer(GadgetSpecFactory gadgetSpecFactory,
                  HttpFetcher httpFetcher) {
    this.gadgetSpecFactory = gadgetSpecFactory;
    this.httpFetcher = httpFetcher;
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
   * @param context The context for the gadget rendering operation.
   * @return The rendered gadget content
   * @throws RenderingException if any issues arise that prevent rendering.
   */
  public String render(GadgetContext context) throws RenderingException {
    try {
      GadgetSpec spec = gadgetSpecFactory.getGadgetSpec(context);
      View view = spec.getView(context.getView());
      if (view.getType() == View.ContentType.URL) {
        throw new RenderingException("Attempted to render a url-type gadget.");
      }

      // TODO: Add current url to GadgetContext to support transitive proxying.

      if (view.getHref() == null) {
        return view.getContent();
      } else {
        HttpRequest request = new HttpRequest(Uri.fromJavaUri(view.getHref()));
        HttpResponse response = httpFetcher.fetch(request);
        return response.getResponseAsString();
      }
    } catch (GadgetException e) {
      throw new RenderingException(e);
    }
  }
}
