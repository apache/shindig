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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.byteSources.ByteSourceInputStream;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.caja.CajaCssSanitizer;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.DomWalker;
import org.apache.shindig.gadgets.rewrite.ResponseRewriter;
import org.apache.shindig.gadgets.uri.ProxyUriManager;

import com.google.common.base.Strings;
import com.google.inject.Inject;

/**
 * Rewriter that sanitizes CSS and image content.
 *
 * @since 2.0.0
 */
public class SanitizingResponseRewriter implements ResponseRewriter {

  //class name for logging purpose
  private static final String classname = SanitizingResponseRewriter.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  private final ContentRewriterFeature.Factory featureConfigFactory;
  private final CajaCssSanitizer cssSanitizer;
  private final ProxyUriManager proxyUriManager;

  @Inject
  public SanitizingResponseRewriter(ContentRewriterFeature.Factory featureConfigFactory,
      CajaCssSanitizer cssSanitizer,
      ProxyUriManager proxyUriManager) {
    this.featureConfigFactory = featureConfigFactory;
    this.cssSanitizer = cssSanitizer;
    this.proxyUriManager = proxyUriManager;
  }

  public void rewrite(HttpRequest request, HttpResponseBuilder resp, Gadget gadget) {
    // Content fetched through the proxy can stipulate that it must be sanitized.
    if (request.isSanitizationRequested() &&
        featureConfigFactory.get(request).shouldRewriteURL(request.getUri().toString())) {
      if (Strings.isNullOrEmpty(request.getRewriteMimeType())) {
        if (LOG.isLoggable(Level.INFO)) {
          LOG.logp(Level.INFO, classname, "rewrite", MessageKeys.REQUEST_TO_SANITIZE_WITHOUT_CONTENT,new Object[] {request.getUri()});
        }
        resp.setContent("");
      } else if (request.getRewriteMimeType().equalsIgnoreCase("text/css")) {
        rewriteProxiedCss(request, resp);
      } else if (request.getRewriteMimeType().toLowerCase().startsWith("image/")) {
        rewriteProxiedImage(request, resp);
      } else {
        if (LOG.isLoggable(Level.WARNING)) {
          LOG.logp(Level.WARNING, classname, "rewrite",
            MessageKeys.REQUEST_TO_SANITIZE_UNKNOW_CONTENT,
            new Object[] {request.getRewriteMimeType(),request.getUri()});
        }
        resp.setContent("");
      }
    }
  }

  /**
   * We don't actually rewrite the image we just ensure that it is in fact a valid
   * and known image type.
   */
  private void rewriteProxiedImage(HttpRequest request, HttpResponseBuilder resp) {
    boolean imageIsSafe = false;
    try {
      String contentType = resp.getHeader("Content-Type");
      if (contentType == null || contentType.toLowerCase().startsWith("image/")) {
        // Unspecified or unknown image mime type.
        try {
          ImageFormat imageFormat = Sanselan
              .guessFormat(new ByteSourceInputStream(resp.getContentBytes(),
                  request.getUri().getPath()));
          if (imageFormat == ImageFormat.IMAGE_FORMAT_UNKNOWN) {
            if (LOG.isLoggable(Level.INFO)) {
              LOG.logp(Level.INFO, classname, "rewriteProxiedImage", MessageKeys.UNABLE_SANITIZE_UNKNOWN_IMG,new Object[] {request.getUri().toString()});
            }
            return;
          }
          imageIsSafe = true;
          // Return false to indicate that no rewriting occurred
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        } catch (ImageReadException ire) {
          // Unable to read the image so its not safe
          if (LOG.isLoggable(Level.INFO)) {
            LOG.logp(Level.INFO, classname, "rewriteProxiedImage", MessageKeys.UNABLE_DETECT_IMG_TYPE,new Object[] {request.getUri().toString()});
            LOG.log(Level.INFO, ire.getMessage(), ire);
          }
        }
      }
    } finally {
      if (!imageIsSafe) {
        resp.setContent("");
      }
    }
  }

  /**
   * Sanitize a CSS file.
   */
  private void rewriteProxiedCss(HttpRequest request, HttpResponseBuilder resp) {
    String sanitized = "";
    try {
      String contentType = resp.getHeader("Content-Type");
      if (contentType == null || contentType.toLowerCase().startsWith("text/")) {
        SanitizingProxyUriManager cssImageRewriter =
            new SanitizingProxyUriManager(proxyUriManager, "image/*");
        SanitizingProxyUriManager cssImportRewriter =
            new SanitizingProxyUriManager(proxyUriManager, "text/css");

        GadgetContext gadgetContext = DomWalker.makeGadget(request).getContext();
        sanitized = cssSanitizer.sanitize(resp.getContent(), request.getUri(),
            gadgetContext, cssImportRewriter, cssImageRewriter);
      }
    } finally {
      // Set sanitized content in finally to ensure it is always cleared in
      // the case of errors
      resp.setContent(sanitized);
    }
  }
}
