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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetFeature;
import org.apache.shindig.gadgets.GadgetFeatureRegistry;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.MessageBundleFactory;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.UnsupportedFeatureException;
import org.apache.shindig.gadgets.UrlGenerator;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.preload.PreloadException;
import org.apache.shindig.gadgets.preload.PreloadedData;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.RewriterResults;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;
import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.View;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * Produces a valid HTML document for the gadget output, automatically inserting appropriate HTML
 * document wrapper data as needed.
 *
 * Currently, this is only invoked directly since the rewriting infrastructure doesn't properly
 * deal with uncacheable rewrite operations.
 *
 * TODO: Break this up into multiple rewriters if and when rewriting infrastructure supports
 * parse tree manipulation without worrying about caching.
 *
 * Should be:
 *
 * - UserPrefs injection
 * - Javascript injection (including configuration)
 * - html document normalization
 */
public class RenderingContentRewriter implements ContentRewriter {
  private static final Logger LOG = Logger.getLogger(RenderingContentRewriter.class.getName());

  static final String DEFAULT_CSS =
      "body,td,div,span,p{font-family:arial,sans-serif;}" +
      "a {color:#0000cc;}a:visited {color:#551a8b;}" +
      "a:active {color:#ff0000;}" +
      "body{margin: 0px;padding: 0px;background-color:white;}";
  static final String INSERT_BASE_ELEMENT_KEY = "gadgets.insertBaseElement";
  static final String FEATURES_KEY = "gadgets.features";

  private final MessageBundleFactory messageBundleFactory;
  private final ContainerConfig containerConfig;
  private final GadgetFeatureRegistry featureRegistry;
  private final UrlGenerator urlGenerator;

  /**
   * @param messageBundleFactory Used for injecting message bundles into gadget output.
   */
  @Inject
  public RenderingContentRewriter(MessageBundleFactory messageBundleFactory,
                                  ContainerConfig containerConfig,
                                  GadgetFeatureRegistry featureRegistry,
                                  UrlGenerator urlGenerator) {
    this.messageBundleFactory = messageBundleFactory;
    this.containerConfig = containerConfig;
    this.featureRegistry = featureRegistry;
    this.urlGenerator = urlGenerator;
  }

  public RewriterResults rewrite(HttpRequest req, HttpResponse resp, MutableContent content) {
    // Rendering does not rewrite arbitrary HTTP responses currently
    return null;
  }

  public RewriterResults rewrite(Gadget gadget, MutableContent mutableContent) {
    // Don't touch sanitized gadgets.
    if ("1".equals(gadget.getContext().getParameter("sanitize"))) {
      return RewriterResults.notCacheable();
    }

    try {
      Document document = mutableContent.getDocument();

      Element head = (Element)DomUtil.getFirstNamedChildNode(document.getDocumentElement(), "head");

      // Remove all the elements currently in head and add them back after we inject content
      NodeList children = head.getChildNodes();
      List<Node> existingHeadContent = Lists.newArrayListWithCapacity(children.getLength());
      for (int i = 0; i < children.getLength(); i++) {
        existingHeadContent.add(children.item(i));
      }

      for (Node n : existingHeadContent) {
        head.removeChild(n);
      }


      // Only inject default styles if no doctype was specified.
      if (document.getDoctype() == null) {
        Element defaultStyle = document.createElement("style");
        defaultStyle.setAttribute("type", "text/css");
        head.appendChild(defaultStyle);
        defaultStyle.appendChild(defaultStyle.getOwnerDocument().
            createTextNode(DEFAULT_CSS));
      }

      injectBaseTag(gadget, head);
      injectFeatureLibraries(gadget, head);

      // This can be one script block.
      Element mainScriptTag = document.createElement("script");
      injectMessageBundles(gadget, mainScriptTag);
      injectDefaultPrefs(gadget, mainScriptTag);
      injectPreloads(gadget, mainScriptTag);

      // We need to inject our script before any developer scripts.
      head.appendChild(mainScriptTag);

      Element body = (Element)DomUtil.getFirstNamedChildNode(document.getDocumentElement(), "body");

      LocaleSpec localeSpec = gadget.getLocale();
      if (localeSpec != null) {
        body.setAttribute("dir", localeSpec.getLanguageDirection());
      }

      // re append head content
      for (Node node : existingHeadContent) {
        head.appendChild(node);
      }

      injectOnLoadHandlers(body);

      mutableContent.documentChanged();
      return RewriterResults.notCacheable();
    } catch (GadgetException e) {
      // TODO: Rewriter interface needs to be modified to handle GadgetException or
      // RewriterException or something along those lines.
      throw new RuntimeException(e);
    }
  }

  private void injectBaseTag(Gadget gadget, Node headTag) {
    GadgetContext context = gadget.getContext();
    if (containerConfig.getBool(context.getContainer(), INSERT_BASE_ELEMENT_KEY)) {
      Uri base = gadget.getSpec().getUrl();
      View view = gadget.getCurrentView();
      if (view != null && view.getHref() != null) {
        base = view.getHref();
      }
      Element baseTag = headTag.getOwnerDocument().createElement("base");
      baseTag.setAttribute("href", base.toString());
      headTag.insertBefore(baseTag, headTag.getFirstChild());
    }
  }

  private void injectOnLoadHandlers(Node bodyTag) {
    Element onloadScript = bodyTag.getOwnerDocument().createElement("script");
    bodyTag.appendChild(onloadScript);
    onloadScript.appendChild(bodyTag.getOwnerDocument().createTextNode(
        "gadgets.util.runOnLoadHandlers();"));
  }

  /**
   * Injects javascript libraries needed to satisfy feature dependencies.
   */
  private void injectFeatureLibraries(Gadget gadget, Node headTag) throws GadgetException {
    // TODO: If there isn't any js in the document, we can skip this. Unfortunately, that means
    // both script tags (easy to detect) and event handlers (much more complex).
    GadgetContext context = gadget.getContext();
    String forcedLibs = context.getParameter("libs");

    // List of libraries we need
    Set<String> forced;

    // gather the libraries we'll need to generate the forced libs
    if (forcedLibs == null || forcedLibs.length() == 0) {
      // TODO allow containers to have a default set of forced libs
      forced = ImmutableSet.of();
    } else {
      forced = Sets.newTreeSet(Arrays.asList(forcedLibs.split(":")));
    }

    if (!forced.isEmpty()) {
      String jsUrl = urlGenerator.getBundledJsUrl(forced, context);
      Element libsTag = headTag.getOwnerDocument().createElement("script");
      libsTag.setAttribute("src", jsUrl);
      headTag.appendChild(libsTag);

      // Forced transitive deps need to be added as well so that they don't get pulled in twice.
      // Without this, a shared dependency between forced and non-forced libs would get pulled into
      // both the external forced script and the inlined script.
      // TODO: Figure out a clean way to avoid having to call getFeatures twice.
      for (GadgetFeature dep : featureRegistry.getFeatures(forced)) {
        forced.add(dep.getName());
      }
    }
    // Make this read-only
    forced = ImmutableSet.copyOf(forced);

    // Inline any libs that weren't forced. The ugly context switch between inline and external
    // Js is needed to allow both inline and external scripts declared in feature.xml.
    String container = context.getContainer();
    Collection<GadgetFeature> features = getFeatures(gadget, forced);

    // Precalculate the maximum length in order to avoid excessive garbage generation.
    int size = 0;
    for (GadgetFeature feature : features) {
      for (JsLibrary library : feature.getJsLibraries(RenderingContext.GADGET, container)) {
        if (library.getType().equals(JsLibrary.Type.URL)) {
          size += library.getContent().length();
        }
      }
    }

    // Really inexact.
    StringBuilder inlineJs = new StringBuilder(size);

    for (GadgetFeature feature : features) {
      for (JsLibrary library : feature.getJsLibraries(RenderingContext.GADGET, container)) {
        if (library.getType().equals(JsLibrary.Type.URL)) {
          if (inlineJs.length() > 0) {
            Element inlineTag = headTag.getOwnerDocument().createElement("script");
            headTag.appendChild(inlineTag);
            inlineTag.appendChild(headTag.getOwnerDocument().createTextNode(inlineJs.toString()));
            inlineJs.setLength(0);
          }
          Element referenceTag = headTag.getOwnerDocument().createElement("script");
          referenceTag.setAttribute("src", library.getContent());
          headTag.appendChild(referenceTag);
        } else {
          if (!forced.contains(feature.getName())) {
            // already pulled this file in from the shared contents.
            if (context.getDebug()) {
              inlineJs.append(library.getDebugContent());
            } else {
              inlineJs.append(library.getContent());
            }
            inlineJs.append(";\n");
          }
        }
      }
    }

    inlineJs.append(getLibraryConfig(gadget, features));

    if (inlineJs.length() > 0) {
      Element inlineTag = headTag.getOwnerDocument().createElement("script");
      headTag.appendChild(inlineTag);
      inlineTag.appendChild(headTag.getOwnerDocument().createTextNode(inlineJs.toString()));
    }
  }

  /**
   * Get all features needed to satisfy this rendering request.
   *
   * @param forced Forced libraries; added in addition to those found in the spec. Defaults to
   * "core".
   */
  private Collection<GadgetFeature> getFeatures(Gadget gadget, Collection<String> forced)
      throws GadgetException {
    Map<String, Feature> features = gadget.getSpec().getModulePrefs().getFeatures();
    Set<String> libs = Sets.newHashSet(features.keySet());
    if (!forced.isEmpty()) {
      libs.addAll(forced);
    }
    
    libs.removeAll(gadget.getRemovedFeatures());
    libs.addAll(gadget.getAddedFeatures());

    Set<String> unsupported = Sets.newHashSet();
    Collection<GadgetFeature> feats = featureRegistry.getFeatures(libs, unsupported);

    unsupported.removeAll(forced);

    if (!unsupported.isEmpty()) {
      // Remove non-required libs
      Iterator<String> missingIter = unsupported.iterator();
      while (missingIter.hasNext()) {
        String missing = missingIter.next();
        if (!features.get(missing).getRequired()) {
          missingIter.remove();
        }
      }

      // Throw error with full list of unsupported libraries
      if (!unsupported.isEmpty()) {
        throw new UnsupportedFeatureException(unsupported.toString());
      }
    }
    return feats;
  }

  /**
   * Creates a set of all configuration needed to satisfy the requested feature set.
   *
   * Appends special configuration for gadgets.util.hasFeature and gadgets.util.getFeatureParams to
   * the output js.
   *
   * This can't be handled via the normal configuration mechanism because it is something that
   * varies per request.
   *
   * @param reqs The features needed to satisfy the request.
   * @throws GadgetException If there is a problem with the gadget auth token
   */
  private String getLibraryConfig(Gadget gadget, Collection<GadgetFeature> reqs)
      throws GadgetException {
    GadgetContext context = gadget.getContext();

    Map<String, Object> features = containerConfig.getMap(context.getContainer(), FEATURES_KEY);

    Map<String, Object> config
        = Maps.newHashMapWithExpectedSize(features == null ? 2 : features.size() + 2);

    if (features != null) {
      // Discard what we don't care about.
      for (GadgetFeature feature : reqs) {
        String name = feature.getName();
        Object conf = features.get(name);
        if (conf != null) {
          config.put(name, conf);
        }
      }
    }

    // Add gadgets.util support. This is calculated dynamically based on request inputs.
    ModulePrefs prefs = gadget.getSpec().getModulePrefs();
    Collection<Feature> values = prefs.getFeatures().values();
    Map<String, Map<String, String>> featureMap = Maps.newHashMapWithExpectedSize(values.size());
    for (Feature feature : values) {
      featureMap.put(feature.getName(), feature.getParams());
    }
    config.put("core.util", featureMap);

    // Add authentication token config
    SecurityToken authToken = context.getToken();
    if (authToken != null) {
      Map<String, String> authConfig = Maps.newHashMapWithExpectedSize(2);
      String updatedToken = authToken.getUpdatedToken();
      if (updatedToken != null) {
        authConfig.put("authToken", updatedToken);
      }
      String trustedJson = authToken.getTrustedJson();
      if (trustedJson != null) {
        authConfig.put("trustedJson", trustedJson);
      }
      config.put("shindig.auth", authConfig);
    }
    return "gadgets.config.init(" + JsonSerializer.serialize(config) + ");\n";
  }

  /**
   * Injects message bundles into the gadget output.
   * @throws GadgetException If we are unable to retrieve the message bundle.
   */
  private void injectMessageBundles(Gadget gadget, Node scriptTag) throws GadgetException {
    GadgetContext context = gadget.getContext();
    MessageBundle bundle = messageBundleFactory.getBundle(
        gadget.getSpec(), context.getLocale(), context.getIgnoreCache());

    String msgs = bundle.toJSONString();

    Text text = scriptTag.getOwnerDocument().createTextNode("gadgets.Prefs.setMessages_(");
    text.appendData(msgs);
    text.appendData(");");
    scriptTag.appendChild(text);
  }

  /**
   * Injects default values for user prefs into the gadget output.
   */
  private void injectDefaultPrefs(Gadget gadget, Node scriptTag) {
    List<UserPref> prefs = gadget.getSpec().getUserPrefs();
    Map<String, String> defaultPrefs = Maps.newHashMapWithExpectedSize(prefs.size());
    for (UserPref up : prefs) {
      defaultPrefs.put(up.getName(), up.getDefaultValue());
    }
    Text text = scriptTag.getOwnerDocument().createTextNode("gadgets.Prefs.setDefaultPrefs_(");
    text.appendData(JsonSerializer.serialize(defaultPrefs));
    text.appendData(");");
    scriptTag.appendChild(text);
  }

  /**
   * Injects preloads into the gadget output.
   *
   * If preloading fails for any reason, we just output an empty object.
   */
  private void injectPreloads(Gadget gadget, Node scriptTag) {
    List<Object> preload = Lists.newArrayList();
    for (PreloadedData preloaded : gadget.getPreloads()) {
      try {
        preload.addAll(preloaded.toJson());
      } catch (PreloadException pe) {
        // This will be thrown in the event of some unexpected exception. We can move on.
        LOG.log(Level.WARNING, "Unexpected error when preloading", pe);
      }
    }
    Text text = scriptTag.getOwnerDocument().createTextNode("gadgets.io.preloaded_=");
    text.appendData(JsonSerializer.serialize(preload));
    text.appendData(";");
    scriptTag.appendChild(text);
  }
}
