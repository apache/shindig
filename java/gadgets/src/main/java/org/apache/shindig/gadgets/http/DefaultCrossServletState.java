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

import org.apache.shindig.gadgets.BasicGadgetBlacklist;
import org.apache.shindig.gadgets.BasicGadgetSigner;
import org.apache.shindig.gadgets.BasicRemoteContentFetcher;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetFeature;
import org.apache.shindig.gadgets.GadgetFeatureFactory;
import org.apache.shindig.gadgets.GadgetFeatureRegistry;
import org.apache.shindig.gadgets.GadgetServer;
import org.apache.shindig.gadgets.GadgetServerConfig;
import org.apache.shindig.gadgets.GadgetSigner;
import org.apache.shindig.gadgets.GadgetSpecFetcher;
import org.apache.shindig.gadgets.GadgetToken;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.MessageBundleFetcher;
import org.apache.shindig.gadgets.RemoteContentFetcher;
import org.apache.shindig.gadgets.RequestSigner;
import org.apache.shindig.gadgets.SignedFetchRequestSigner;
import org.apache.shindig.gadgets.SyndicatorConfig;
import org.apache.shindig.gadgets.UserPrefs;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.util.HashUtil;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

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
  public GadgetSigner getGadgetSigner() {
    return gadgetSigner;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getIframeUrl(Gadget gadget) {
    GadgetContext context = gadget.getContext();
    StringBuilder buf = new StringBuilder();
    try {
      GadgetSpec spec = gadget.getSpec();
      String url = context.getUrl().toString();
      View view = spec.getView(context.getView());
      if (view.getType().equals(View.ContentType.HTML)) {
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

      buf.append("mid=").append(context.getModuleId());
      buf.append("&synd=").append(context.getSyndicator());
      buf.append("&v=").append(gadget.getSpec().getChecksum());

      UserPrefs prefs = context.getUserPrefs();
      for (Map.Entry<String, String> entry : prefs.getPrefs().entrySet()) {
        buf.append("&up_")
           .append(entry.getKey())
           .append("=")
           .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
      }
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 Not supported!", e);
    }

    return buf.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getJsUrl(Set<String> features, GadgetContext context) {
    StringBuilder buf = new StringBuilder();
    buf.append(jsPath);
    if (features == null || features.size() == 0) {
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
    buf.append(".js?v=")
       .append(jsCacheParam)
       .append("&synd=")
       .append(context.getSyndicator())
       .append("&debug=")
       .append(context.getDebug() ? "1" : "0");
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

    try {
      String features = context.getInitParameter("features");
      String syndicators = context.getInitParameter("syndicators");
      String blacklist = context.getInitParameter("blacklist");
      gadgetSigner = new BasicGadgetSigner();
      RemoteContentFetcher fetcher = new BasicRemoteContentFetcher(1024 * 1024);
      SyndicatorConfig syndicatorConfig = new SyndicatorConfig(syndicators);
      GadgetFeatureRegistry registry
          = new GadgetFeatureRegistry(features, fetcher);

      GadgetServerConfig config =  new GadgetServerConfig()
          .setExecutor(Executors.newCachedThreadPool())
          .setMessageBundleFetcher(new MessageBundleFetcher(fetcher))
          .setGadgetSpecFetcher(new GadgetSpecFetcher(fetcher))
          .setContentFetcher(fetcher)
          .setFeatureRegistry(registry)
          .setSyndicatorConfig(syndicatorConfig);

      if (blacklist != null) {
        File file = new File(blacklist);
        try {
          config.setGadgetBlacklist(new BasicGadgetBlacklist(file));
        } catch (IOException e) {
          throw new GadgetException(GadgetException.Code.INVALID_CONFIG,
              "Unable to load blacklist file: " + blacklist);
        }
      }
      gadgetServer = new GadgetServer(config);

      // Grab all static javascript, concatenate it together, and generate
      // an md5. This becomes the cache busting suffix for javascript files.
      StringBuilder jsBuf = new StringBuilder();

      for (Map.Entry<String, GadgetFeatureRegistry.Entry> entry :
          registry.getAllFeatures().entrySet()) {
        GadgetFeatureFactory factory = entry.getValue().getFeature();
        GadgetFeature feature = factory.create();
        for (JsLibrary library : feature.getJsLibraries(null)) {
          jsBuf.append(library.getContent());
        }
      }

      jsCacheParam = HashUtil.checksum(jsBuf.toString().getBytes());
    } catch (GadgetException e) {
      throw new ServletException(e);
    }
  }

  @Override
  public RequestSigner makeOAuthRequestSigner(GadgetToken token) {
    return null;
  }

  @Override
  public RequestSigner makeSignedFetchRequestSigner(GadgetToken token) {
    // Real implementations should use their own key, probably pulled from
    // disk rather than hardcoded in the source.
    final String PRIVATE_KEY_TEXT =
      "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBALRiMLAh9iimur8V" +
      "A7qVvdqxevEuUkW4K+2KdMXmnQbG9Aa7k7eBjK1S+0LYmVjPKlJGNXHDGuy5Fw/d" +
      "7rjVJ0BLB+ubPK8iA/Tw3hLQgXMRRGRXXCn8ikfuQfjUS1uZSatdLB81mydBETlJ" +
      "hI6GH4twrbDJCR2Bwy/XWXgqgGRzAgMBAAECgYBYWVtleUzavkbrPjy0T5FMou8H" +
      "X9u2AC2ry8vD/l7cqedtwMPp9k7TubgNFo+NGvKsl2ynyprOZR1xjQ7WgrgVB+mm" +
      "uScOM/5HVceFuGRDhYTCObE+y1kxRloNYXnx3ei1zbeYLPCHdhxRYW7T0qcynNmw" +
      "rn05/KO2RLjgQNalsQJBANeA3Q4Nugqy4QBUCEC09SqylT2K9FrrItqL2QKc9v0Z" +
      "zO2uwllCbg0dwpVuYPYXYvikNHHg+aCWF+VXsb9rpPsCQQDWR9TT4ORdzoj+Nccn" +
      "qkMsDmzt0EfNaAOwHOmVJ2RVBspPcxt5iN4HI7HNeG6U5YsFBb+/GZbgfBT3kpNG" +
      "WPTpAkBI+gFhjfJvRw38n3g/+UeAkwMI2TJQS4n8+hid0uus3/zOjDySH3XHCUno" +
      "cn1xOJAyZODBo47E+67R4jV1/gzbAkEAklJaspRPXP877NssM5nAZMU0/O/NGCZ+" +
      "3jPgDUno6WbJn5cqm8MqWhW1xGkImgRk+fkDBquiq4gPiT898jusgQJAd5Zrr6Q8" +
      "AO/0isr/3aa6O6NLQxISLKcPDk2NOccAfS/xOtfOz4sJYM3+Bs4Io9+dZGSDCA54" +
      "Lw03eHTNQghS0A==";
    final String PRIVATE_KEY_NAME = "shindig-insecure-key";
    return new SignedFetchRequestSigner(token, PRIVATE_KEY_NAME,
        PRIVATE_KEY_TEXT);
  }
}
