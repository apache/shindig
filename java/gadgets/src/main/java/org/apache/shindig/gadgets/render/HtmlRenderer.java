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

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.preload.PreloadedData;
import org.apache.shindig.gadgets.preload.PreloaderService;
import org.apache.shindig.gadgets.rewrite.GadgetRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.gadgets.spec.View;

import java.util.Collection;
import java.util.List;

import com.google.inject.Inject;

/**
 * Handles producing output markup for a gadget based on the provided context.
 */
public class HtmlRenderer {
  public static final String PATH_PARAM = "path";
  private final PreloaderService preloader;
  private final ProxyRenderer proxyRenderer;
  private final List<GadgetRewriter> gadgetRewriters;
  private final GadgetHtmlParser htmlParser;

  /**
   * @param requestPipeline Used for performing the proxy request. Always ignores caching because
   *                        we want to skip preloading when the object is in the cache.
   * @param httpCache The shared http cache. Used before checking the request pipeline to determine
   *                  whether to perform the preload / fetch cycle.
   */
  @Inject
  public HtmlRenderer(PreloaderService preloader,
                      ProxyRenderer proxyRenderer,
                      List<GadgetRewriter> gadgetRewriters,
                      GadgetHtmlParser htmlParser) {
    this.preloader = preloader;
    this.proxyRenderer = proxyRenderer;
    this.gadgetRewriters = gadgetRewriters;
    this.htmlParser = htmlParser;
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

      // We always execute these preloads, they have nothing to do with the cache output.
      Collection<PreloadedData> preloads = preloader.preload(gadget);
      gadget.setPreloads(preloads);

      String content;

      if (view.getHref() == null) {
        content = view.getContent();
      } else {
        content = proxyRenderer.render(gadget);
      }

      MutableContent mc = new MutableContent(htmlParser, content);
      for (GadgetRewriter rewriter : gadgetRewriters) {
        rewriter.rewrite(gadget, mc);
      }
      
      return mc.getContent();
    } catch (GadgetException e) {
      throw new RenderingException(e.getMessage(), e);
    } catch (RewritingException e) {
      throw new RenderingException(e.getMessage(), e);
    }
  }
}
