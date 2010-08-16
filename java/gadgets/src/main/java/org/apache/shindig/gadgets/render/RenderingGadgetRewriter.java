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

import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.MessageBundleFactory;
import org.apache.shindig.gadgets.UnsupportedFeatureException;
import org.apache.shindig.gadgets.config.ConfigContributor;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.preload.PreloadException;
import org.apache.shindig.gadgets.preload.PreloadedData;
import org.apache.shindig.gadgets.rewrite.GadgetRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.MessageBundle;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Produces a valid HTML document for the gadget output, automatically inserting appropriate HTML
 * document wrapper data as needed.
 *
 * Currently, this is only invoked directly since the rewriting infrastructure doesn't properly
 * deal with uncacheable rewrite operations.
 *
 * TODO: Break this up into multiple rewriters.
 *
 * Should be:
 *
 * - UserPrefs injection
 * - Javascript injection (including configuration)
 * - html document normalization
 */
public class RenderingGadgetRewriter implements GadgetRewriter {
  private static final Logger LOG = Logger.getLogger(RenderingGadgetRewriter.class.getName());
  
  private static final int INLINE_JS_BUFFER = 50;

  protected static final String DEFAULT_CSS =
      "body,td,div,span,p{font-family:arial,sans-serif;}" +
      "a {color:#0000cc;}a:visited {color:#551a8b;}" +
      "a:active {color:#ff0000;}" +
      "body{margin: 0px;padding: 0px;background-color:white;}";
  static final String IS_GADGET_BEACON = "window['__isgadget']=true;";
  static final String INSERT_BASE_ELEMENT_KEY = "gadgets.insertBaseElement";
  static final String FEATURES_KEY = "gadgets.features";

  protected final MessageBundleFactory messageBundleFactory;
  protected final ContainerConfig containerConfig;
  protected final FeatureRegistry featureRegistry;
  protected final JsUriManager jsUriManager;
  protected final Map<String, ConfigContributor> configContributors;


  protected Set<String> defaultExternLibs = ImmutableSet.of();

  protected Boolean externalizeFeatures = false;

  /**
   * @param messageBundleFactory Used for injecting message bundles into gadget output.
   */
  @Inject
  public RenderingGadgetRewriter(MessageBundleFactory messageBundleFactory,
                                 ContainerConfig containerConfig,
                                 FeatureRegistry featureRegistry,
                                 JsUriManager jsUriManager,
                                 Map<String, ConfigContributor> configContributors) {
    this.messageBundleFactory = messageBundleFactory;
    this.containerConfig = containerConfig;
    this.featureRegistry = featureRegistry;
    this.jsUriManager = jsUriManager;
    this.configContributors = configContributors;
  }

  @Inject
  public void setDefaultForcedLibs(@Named("shindig.gadget-rewrite.default-forced-libs")String forcedLibs) {
    if (StringUtils.isNotBlank(forcedLibs)) {
      defaultExternLibs = ImmutableSortedSet.copyOf(StringUtils.split(forcedLibs, ':'));
    }
  }

  @Inject(optional = true)
  public void setExternalizeFeatureLibs(@Named("shindig.gadget-rewrite.externalize-feature-libs")Boolean externalizeFeatures) {
    this.externalizeFeatures = externalizeFeatures;
  }

  public void rewrite(Gadget gadget, MutableContent mutableContent) throws RewritingException {
    // Don't touch sanitized gadgets.
    if (gadget.sanitizeOutput()) {
      return;
    }

    try {
      Document document = mutableContent.getDocument();

      Element head = (Element)DomUtil.getFirstNamedChildNode(document.getDocumentElement(), "head");

      // Insert new content before any of the existing children of the head element
      Node firstHeadChild = head.getFirstChild();

      // Only inject default styles if no doctype was specified.
      if (document.getDoctype() == null) {
        Element defaultStyle = document.createElement("style");
        defaultStyle.setAttribute("type", "text/css");
        head.insertBefore(defaultStyle, firstHeadChild);
        defaultStyle.appendChild(defaultStyle.getOwnerDocument().
            createTextNode(DEFAULT_CSS));
      }

      injectBaseTag(gadget, head);
      injectGadgetBeacon(gadget, head, firstHeadChild);
      injectFeatureLibraries(gadget, head, firstHeadChild);

      // This can be one script block.
      Element mainScriptTag = document.createElement("script");
      GadgetContext context = gadget.getContext();
      MessageBundle bundle = messageBundleFactory.getBundle(
          gadget.getSpec(), context.getLocale(), context.getIgnoreCache(), context.getContainer());
      injectMessageBundles(bundle, mainScriptTag);
      injectDefaultPrefs(gadget, mainScriptTag);
      injectPreloads(gadget, mainScriptTag);

      // We need to inject our script before any developer scripts.
      head.insertBefore(mainScriptTag, firstHeadChild);

      Element body = (Element)DomUtil.getFirstNamedChildNode(document.getDocumentElement(), "body");

      body.setAttribute("dir", bundle.getLanguageDirection());

      injectOnLoadHandlers(body);

      mutableContent.documentChanged();
    } catch (GadgetException e) {
      throw new RewritingException(e.getLocalizedMessage(), e, e.getHttpStatusCode());
    }
  }

  protected void injectBaseTag(Gadget gadget, Node headTag) {
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

  protected void injectOnLoadHandlers(Node bodyTag) {
    Element onloadScript = bodyTag.getOwnerDocument().createElement("script");
    bodyTag.appendChild(onloadScript);
    onloadScript.appendChild(bodyTag.getOwnerDocument().createTextNode(
        "gadgets.util.runOnLoadHandlers();"));
  }
  
  protected void injectGadgetBeacon(Gadget gadget, Node headTag, Node firstHeadChild)
          throws GadgetException {
    Element beaconNode = headTag.getOwnerDocument().createElement("script");
    beaconNode.setTextContent(IS_GADGET_BEACON);
    headTag.insertBefore(beaconNode, firstHeadChild);
  }

  /**
   * Injects javascript libraries needed to satisfy feature dependencies.
   */
  protected void injectFeatureLibraries(Gadget gadget, Node headTag, Node firstHeadChild)
          throws GadgetException {
    // TODO: If there isn't any js in the document, we can skip this. Unfortunately, that means
    // both script tags (easy to detect) and event handlers (much more complex).
    GadgetContext context = gadget.getContext();

    // Set of extern libraries requested by the container
    Set<String> externForcedLibs = defaultExternLibs;

    // gather the libraries we'll need to generate the extern libs
    String externParam = context.getParameter("libs");    
    if (StringUtils.isNotBlank(externParam)) {
      externForcedLibs = Sets.newTreeSet(Arrays.asList(StringUtils.split(externParam, ':')));
    }

    if (!externForcedLibs.isEmpty()) {
      String jsUrl = jsUriManager.makeExternJsUri(gadget, externForcedLibs).toString();
      Element libsTag = headTag.getOwnerDocument().createElement("script");
      libsTag.setAttribute("src", jsUrl.replace("&", "&amp;"));
      headTag.insertBefore(libsTag, firstHeadChild);
    }

    List<String> unsupported = Lists.newLinkedList();

    List<FeatureResource> externForcedResources =
        featureRegistry.getFeatureResources(context, externForcedLibs, unsupported); 
    if (!unsupported.isEmpty()) {
      LOG.info("Unknown feature(s) in extern &libs=: " + unsupported.toString());
      unsupported.clear();
    }

    // Get all resources requested by the gadget's requires/optional features.
    Map<String, Feature> featureMap = gadget.getSpec().getModulePrefs().getFeatures();
    List<String> gadgetFeatureKeys = Lists.newArrayList(gadget.getDirectFeatureDeps());
    List<FeatureResource> gadgetResources =
        featureRegistry.getFeatureResources(context, gadgetFeatureKeys, unsupported);
    if (!unsupported.isEmpty()) {
      List<String> requiredUnsupported = Lists.newLinkedList();
      for (String notThere : unsupported) {
        if (!featureMap.containsKey(notThere) || featureMap.get(notThere).getRequired()) {
          // if !containsKey, the lib was forced with Gadget.addFeature(...) so implicitly req'd.
          requiredUnsupported.add(notThere);
        }
      }
      if (!requiredUnsupported.isEmpty()) {
        throw new UnsupportedFeatureException(requiredUnsupported.toString());
      }
    }    

    // Inline or externalize the gadgetFeatureKeys
    List<FeatureResource> inlineResources = Lists.newArrayList();        
    List<String> allRequested = Lists.newArrayList(gadgetFeatureKeys);

    if (externalizeFeatures) {
      Set<String> externGadgetLibs = Sets.newTreeSet(featureRegistry.getFeatures(gadgetFeatureKeys));
      externGadgetLibs.removeAll(externForcedLibs);

      if (!externGadgetLibs.isEmpty()) {
        String jsUrl = jsUriManager.makeExternJsUri(gadget, externGadgetLibs).toString();
        Element libsTag = headTag.getOwnerDocument().createElement("script");
        libsTag.setAttribute("src", jsUrl.replace("&", "&amp;"));
        headTag.insertBefore(libsTag, firstHeadChild);
      }
    } else {
      inlineResources.addAll(gadgetResources);
    }

    // Calculate inlineResources as all resources that are needed by the gadget to
    // render, minus all those included through externResources.
    // TODO: profile and if needed, optimize this a bit.
    if (!externForcedLibs.isEmpty()) {
      allRequested.addAll(externForcedLibs);
      inlineResources.removeAll(externForcedResources);
    }

    // Precalculate the maximum length in order to avoid excessive garbage generation.
    int size = 0;
    for (FeatureResource resource : inlineResources) {
      if (!resource.isExternal()) {
        if (context.getDebug()) {
          size += resource.getDebugContent().length();
        } else {
          size += resource.getContent().length();
        }
      }
    }

    String libraryConfig =
        getLibraryConfig(gadget, featureRegistry.getFeatures(allRequested));
    
    // Size has a small fudge factor added to it for delimiters and such.
    StringBuilder inlineJs = new StringBuilder(size + libraryConfig.length() + INLINE_JS_BUFFER);

    // Inline any libs that weren't extern. The ugly context switch between inline and external
    // Js is needed to allow both inline and external scripts declared in feature.xml.
    for (FeatureResource resource : inlineResources) {
      String theContent = context.getDebug() ? resource.getDebugContent() : resource.getContent();
      if (resource.isExternal()) {
        if (inlineJs.length() > 0) {
          Element inlineTag = headTag.getOwnerDocument().createElement("script");
          headTag.insertBefore(inlineTag, firstHeadChild);
          inlineTag.appendChild(headTag.getOwnerDocument().createTextNode(inlineJs.toString()));
          inlineJs.setLength(0);
        }
        Element referenceTag = headTag.getOwnerDocument().createElement("script");
        referenceTag.setAttribute("src", theContent.replace("&", "&amp;"));
        headTag.insertBefore(referenceTag, firstHeadChild);
      } else {
        inlineJs.append(theContent).append(";\n");
      }
    }

    inlineJs.append(libraryConfig);

    if (inlineJs.length() > 0) {
      Element inlineTag = headTag.getOwnerDocument().createElement("script");
      headTag.insertBefore(inlineTag, firstHeadChild);
      inlineTag.appendChild(headTag.getOwnerDocument().createTextNode(inlineJs.toString()));
    }
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
  protected String getLibraryConfig(Gadget gadget, List<String> reqs)
      throws GadgetException {
    GadgetContext context = gadget.getContext();

    Map<String, Object> features = containerConfig.getMap(context.getContainer(), FEATURES_KEY);

    Map<String, Object> config
        = Maps.newHashMapWithExpectedSize(features == null ? 2 : features.size() + 2);

    if (features != null) {
      // Discard what we don't care about.
      for (String name : reqs) {
        Object conf = features.get(name);
        if (conf != null) {
          config.put(name, conf);
        }
        
        // See if this feature has configuration data
        ConfigContributor contributor = configContributors.get(name);
        if (contributor != null) {
          contributor.contribute(config, gadget);
        }
      }
    }

    return "gadgets.config.init(" + JsonSerializer.serialize(config) + ");\n";
  }

  /**
   * Injects message bundles into the gadget output.
   * @throws GadgetException If we are unable to retrieve the message bundle.
   */
  protected void injectMessageBundles(MessageBundle bundle, Node scriptTag) throws GadgetException {
    String msgs = bundle.toJSONString();

    Text text = scriptTag.getOwnerDocument().createTextNode("gadgets.Prefs.setMessages_(");
    text.appendData(msgs);
    text.appendData(");");
    scriptTag.appendChild(text);
  }

  /**
   * Injects default values for user prefs into the gadget output.
   */
  protected void injectDefaultPrefs(Gadget gadget, Node scriptTag) {
    Collection<UserPref> prefs = gadget.getSpec().getUserPrefs().values();
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
  protected void injectPreloads(Gadget gadget, Node scriptTag) {
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
