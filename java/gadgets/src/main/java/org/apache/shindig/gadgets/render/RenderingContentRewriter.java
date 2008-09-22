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
import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetFeature;
import org.apache.shindig.gadgets.GadgetFeatureRegistry;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.MessageBundleFactory;
import org.apache.shindig.gadgets.MutableContent;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.UnsupportedFeatureException;
import org.apache.shindig.gadgets.UrlGenerator;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.RewriterResults;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;
import org.apache.shindig.gadgets.spec.ModulePrefs;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  static final Pattern DOCUMENT_SPLIT_PATTERN = Pattern.compile(
      "(.*)<head>(.*?)<\\/head>(?:.*)<body(.*?)>(.*?)<\\/body>(?:.*)", Pattern.DOTALL);
  static final int BEFORE_HEAD_GROUP = 1;
  static final int HEAD_GROUP = 2;
  static final int BODY_ATTRIBUTES_GROUP = 3;
  static final int BODY_GROUP = 4;
  static final String DEFAULT_HEAD_CONTENT =
      "<style type=\"text/css\">" +
      "body,td,div,span,p{font-family:arial,sans-serif;}" +
      "a {color:#0000cc;}a:visited {color:#551a8b;}" +
      "a:active {color:#ff0000;}" +
      "body{margin: 0px;padding: 0px;background-color:white;}" +
      "</style>";

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

  public RewriterResults rewrite(HttpRequest request, HttpResponse original,
      MutableContent content) {
    throw new UnsupportedOperationException();
  }

  public RewriterResults rewrite(Gadget gadget) {
    try {
      GadgetContent content = createGadgetContent(gadget);
      injectFeatureLibraries(gadget, content);
      injectOnLoadHandlers(content);
      injectMessageBundles(gadget, content);
      // TODO: Use preloads when RenderedGadget gets promoted to Gadget.
      finalizeDocument(gadget, content);
      return RewriterResults.notCacheable();
    } catch (GadgetException e) {
      // TODO: Rewriter interface needs to be modified to handle GadgetException or
      // RewriterException or something along those lines.
      throw new RuntimeException(e);
    }
  }

  private void injectOnLoadHandlers(GadgetContent content) {
    content.appendBody("<script>gadgets.util.runOnLoadHandlers();</script>");
  }

  /**
   * Injects javascript libraries needed to satisfy feature dependencies.
   */
  private void injectFeatureLibraries(Gadget gadget, GadgetContent content) throws GadgetException {
    // TODO: If there isn't any js in the document, we can skip this. Unfortunately, that means
    // both script tags (easy to detect) and event handlers (much more complex).
    GadgetContext context = gadget.getContext();
    GadgetSpec spec = gadget.getSpec();
    String forcedLibs = context.getParameter("libs");
    Set<String> forced;
    if (forcedLibs == null) {
      forced = Sets.newHashSet();
    } else {
      forced = Sets.newHashSet(forcedLibs.split(":"));
    }

    String externFmt = "<script src=\"%s\"></script>";

    // Forced libs are always done first.
    if (!forced.isEmpty()) {
      String jsUrl = urlGenerator.getBundledJsUrl(forced, context);
      content.appendHead(String.format(externFmt, jsUrl));
      // Forced transitive deps need to be added as well so that they don't get pulled in twice.
      // TODO: Figure out a clean way to avoid having to call getFeatures twice.
      for (GadgetFeature dep : featureRegistry.getFeatures(forced)) {
        forced.add(dep.getName());
      }
    }

    StringBuilder inlineJs = new StringBuilder();

    // Inline any libs that weren't forced. The ugly context switch between inline and external
    // Js is needed to allow both inline and external scripts declared in feature.xml.
    String container = context.getContainer();
    Collection<GadgetFeature> features = getFeatures(spec, forced);

    for (GadgetFeature feature : features) {
      for (JsLibrary library : feature.getJsLibraries(RenderingContext.GADGET, container)) {
        if (library.getType().equals(JsLibrary.Type.URL)) {
          if (inlineJs.length() > 0) {
            content.appendHead("<script>")
                   .appendHead(inlineJs)
                   .appendHead("</script>");
            inlineJs.setLength(0);
          }
          content.appendHead(String.format(externFmt, library.getContent()));
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
      content.appendHead("<script>")
             .appendHead(inlineJs)
             .appendHead("</script>");
    }
  }

  /**
   * Get all features needed to satisfy this rendering request.
   *
   * @param forced Forced libraries; added in addition to those found in the spec. Defaults to
   * "core".
   */
  private Collection<GadgetFeature> getFeatures(GadgetSpec spec, Collection<String> forced)
      throws GadgetException {
    Map<String, Feature> features = spec.getModulePrefs().getFeatures();
    Set<String> libs = Sets.newHashSet(features.keySet());
    if (!forced.isEmpty()) {
      libs.addAll(forced);
    }

    Set<String> unsupported = new HashSet<String>();
    Collection<GadgetFeature> feats = featureRegistry.getFeatures(libs, unsupported);

    unsupported.removeAll(forced);

    if (!unsupported.isEmpty()) {
      // Remove non-required libs
      for (String missing : unsupported) {
        if (!features.get(missing).getRequired()) {
          unsupported.remove(missing);
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

    JSONObject features = containerConfig.getJsonObject(context.getContainer(), "gadgets.features");

    try {
      // Discard what we don't care about.
      JSONObject config;
      if (features == null) {
        config = new JSONObject();
      } else {
        String[] properties = new String[reqs.size()];
        int i = 0;
        for (GadgetFeature feature : reqs) {
          properties[i++] = feature.getName();
        }
        config = new JSONObject(features, properties);
      }

      // Add gadgets.util support. This is calculated dynamically based on request inputs.
      ModulePrefs prefs = gadget.getSpec().getModulePrefs();
      JSONObject featureMap = new JSONObject();

      for (Feature feature : prefs.getFeatures().values()) {
        featureMap.put(feature.getName(), feature.getParams());
      }
      config.put("core.util", featureMap);

      // Add authentication token config
      SecurityToken authToken = context.getToken();
      if (authToken != null) {
        JSONObject authConfig = new JSONObject();
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
      return "gadgets.config.init(" + config.toString() + ");\n";
    } catch (JSONException e) {
      // Shouldn't be possible.
      throw new RuntimeException(e);
    }
  }

  /**
   * Injects message bundles into the gadget output.
   * @throws GadgetException If we are unable to retrieve the message bundle.
   */
  private void injectMessageBundles(Gadget gadget, GadgetContent content) throws GadgetException {
    GadgetContext context = gadget.getContext();
    MessageBundle bundle = messageBundleFactory.getBundle(
        gadget.getSpec(), context.getLocale(), context.getIgnoreCache());

    String msgs = new JSONObject(bundle.getMessages()).toString();
    // TODO: Figure out a simple way to merge scripts.
    content.appendHead("<script>gadgets.Prefs.setMessages_(")
           .appendHead(msgs)
           .appendHead(");</script>");
  }

  /**
   * Produces GadgetContent by parsing the document into 3 pieces (head, body, and tail). If the
   */
  private GadgetContent createGadgetContent(Gadget gadget) {
    GadgetContent content = new GadgetContent();
    String doc = gadget.getContent();
    if (doc.contains("<html>") && doc.contains("</html>")) {
      Matcher matcher = DOCUMENT_SPLIT_PATTERN.matcher(doc);
      if (matcher.matches()) {
        content.appendHead(matcher.group(BEFORE_HEAD_GROUP))
               .appendHead("<head>")
               .appendHead(matcher.group(HEAD_GROUP));

        content.appendBody("</head>")
               .appendBody(createBodyTag(gadget, matcher.group(BODY_ATTRIBUTES_GROUP)))
               .appendBody(matcher.group(BODY_GROUP));

        content.appendTail("</body></html>");
      } else {
        makeDefaultContent(gadget, content);
      }
    } else {
      makeDefaultContent(gadget, content);
    }
    return content;
  }

  /**
   * Inserts basic content for a gadget. Used when the content does not contain a valid html doc.
   */
  private void makeDefaultContent(Gadget gadget, GadgetContent content) {
    content.appendHead("<html><head>");
    content.appendHead(DEFAULT_HEAD_CONTENT);
    content.appendBody("</head>");
    content.appendBody(createBodyTag(gadget, ""));
    content.appendBody(gadget.getContent());
    content.appendTail("</body></html>");
  }

  /**
   * Produces the default body tag, inserting language direction as needed.
   */
  private String createBodyTag(Gadget gadget, String extra) {
    LocaleSpec localeSpec = gadget.getLocale();
    if (localeSpec == null) {
      return "<body" + extra + ">";
    } else {
      return "<body" + extra + " dir='" + localeSpec.getLanguageDirection() + "'>";
    }
  }

  /**
   * Produces a final document for the gadget's content.
   */
  private void finalizeDocument(Gadget gadget, GadgetContent content) {
    gadget.setContent(content.assemble());
  }

  private static class GadgetContent {
    private final StringBuilder head = new StringBuilder();
    private final StringBuilder body = new StringBuilder();
    private final StringBuilder tail = new StringBuilder();

    GadgetContent appendHead(CharSequence content) {
      head.append(content);
      return this;
    }

    GadgetContent appendBody(CharSequence content) {
      body.append(content);
      return this;
    }

    GadgetContent appendTail(CharSequence content) {
      tail.append(content);
      return this;
    }

    /**
     * @return The final content for the gadget.
     */
    String assemble() {
      return new StringBuilder(head.length() + body.length() + tail.length())
          .append(head)
          .append(body)
          .append(tail)
          .toString();
    }

    @Override
    public String toString() {
      return assemble();
    }
  }
}
