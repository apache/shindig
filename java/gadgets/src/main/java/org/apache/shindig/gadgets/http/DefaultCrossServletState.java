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

import org.apache.shindig.gadgets.BasicGadgetDataCache;
import org.apache.shindig.gadgets.BasicGadgetSigner;
import org.apache.shindig.gadgets.BasicRemoteContentFetcher;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetFeatureFactory;
import org.apache.shindig.gadgets.GadgetFeatureRegistry;
import org.apache.shindig.gadgets.GadgetServer;
import org.apache.shindig.gadgets.GadgetServerConfig;
import org.apache.shindig.gadgets.GadgetSigner;
import org.apache.shindig.gadgets.GadgetSpec;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.JsLibraryFeatureFactory;
import org.apache.shindig.gadgets.MessageBundle;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.UserPrefs;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * A handler which uses all of the basic versions of classes.
 */
public class DefaultCrossServletState extends CrossServletState {

  private GadgetServer gadgetServer;
  private GadgetSigner gadgetSigner;
  private String jsPath;
  private String iframePath;
  private String jsCacheParam;

  private static final String DEFAULT_JS_PREFIX = "/gadgets/js/";
  private static final String DEFAULT_IFRAME_PREFIX  = "/gadgets/ifr?";

  /**
   * {@inheritDoc}
   */
  @Override
  public GadgetServer getGadgetServer() {
    return gadgetServer;
  }

  /**
   * {@inheritDoc}
   * Just returns the same gadget signer no matter the request.
   */
  @Override
  public GadgetSigner getGadgetSigner(HttpServletRequest req) {
    return gadgetSigner;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getIframeUrl(Gadget gadget, HttpServletRequest req) {
    // We don't have any meaningful data in the current request anyway, so
    // we'll just do this statically.
    StringBuilder buf = new StringBuilder();

    try {
      String url = gadget.getId().getURI().toString();
      if (gadget.getContentType().equals(GadgetSpec.ContentType.HTML)) {
        buf.append(iframePath)
           .append("url=")
           .append(URLEncoder.encode(url, "UTF-8"))
           .append("&");
      } else {
        // type = url
        buf.append(url);
        if (url.indexOf('?') == -1) {
          buf.append('?');
        } else {
          buf.append('&');
        }
      }

      buf.append("mid=").append(gadget.getId().getModuleId());

      UserPrefs prefs = gadget.getUserPrefValues();
      for (Map.Entry<String, String> entry : prefs.getPrefs().entrySet()) {
        buf.append("&up_")
           .append(entry.getKey())
           .append("=")
           .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
      }
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 Not supported!", e);
    }

    // TODO: extract user prefs, current view, etc. from <req>. Currently
    // consumers of the response are on their own for this.
    return buf.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getJsUrl(String[] features) {
    StringBuilder buf = new StringBuilder();
    buf.append(jsPath);
    if (features == null || features.length == 0) {
      buf.append("core");
    } else {
      boolean firstDone = false;
      for (String feature : features) {
        if (firstDone) {
          buf.append(":");
        } else {
          firstDone = true;
        }
        buf.append(feature);
      }
    }
    buf.append(".js?v=").append(jsCacheParam);
    return buf.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void init(ServletContext context) throws ServletException {
    jsPath = context.getInitParameter("js-service-path");
    if (jsPath == null) {
      jsPath = DEFAULT_JS_PREFIX;
    }

    iframePath = context.getInitParameter("iframe-path");
    if (iframePath == null) {
      iframePath = DEFAULT_IFRAME_PREFIX;
    }

    // features could be null, but that would probably be a bad idea.
    String features = context.getInitParameter("features");
    try {
      gadgetSigner = new BasicGadgetSigner();
      GadgetFeatureRegistry registry = new GadgetFeatureRegistry(features);
      GadgetServerConfig config =  new GadgetServerConfig()
          .setExecutor(Executors.newCachedThreadPool())
          .setMessageBundleCache(new BasicGadgetDataCache<MessageBundle>())
          .setSpecCache(new BasicGadgetDataCache<GadgetSpec>())
          .setContentFetcher(new BasicRemoteContentFetcher(1024 * 1024))
          .setFeatureRegistry(registry);
      gadgetServer = new GadgetServer(config);

      // Grab all static javascript, concatenate it together, and generate
      // an md5. This becomes the cache busting suffix for javascript files.
      StringBuilder jsBuf = new StringBuilder();

      for (Map.Entry<String, GadgetFeatureRegistry.Entry> entry :
          registry.getAllFeatures().entrySet()) {
        GadgetFeatureFactory factory = entry.getValue().getFeature();
        if (factory instanceof JsLibraryFeatureFactory) {
          JsLibraryFeatureFactory lib = (JsLibraryFeatureFactory)factory;
          for (RenderingContext rc : RenderingContext.values()) {
            for (JsLibrary library : lib.getLibraries(rc)) {
              jsBuf.append(library.getContent());
            }
          }
        }
      }
      MessageDigest md;
      try {
        md = MessageDigest.getInstance("MD5");
      } catch (NoSuchAlgorithmException noMD5) {
        try {
          md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException noSha) {
          throw new ServletException("No suitable MessageDigest found!");
        }
      }
      byte[] hash = md.digest(jsBuf.toString().getBytes());
      // Convert to hex. This might be a waste of bytes (32) -- could be
      // replaced with a base64 implementation.
      StringBuffer hexString = new StringBuffer();
      for (byte b : hash) {
        hexString.append(Integer.toHexString(0xFF & b));
      }
      jsCacheParam = hexString.toString();
    } catch (GadgetException e) {
      throw new ServletException(e);
    }
  }
}
