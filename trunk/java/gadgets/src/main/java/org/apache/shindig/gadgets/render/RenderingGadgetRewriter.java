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

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.PropertyNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetELResolver;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetException.Code;
import org.apache.shindig.gadgets.MessageBundleFactory;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.UnsupportedFeatureException;
import org.apache.shindig.gadgets.admin.GadgetAdminStore;
import org.apache.shindig.gadgets.config.ConfigProcessor;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.js.JsException;
import org.apache.shindig.gadgets.js.JsRequest;
import org.apache.shindig.gadgets.js.JsRequestBuilder;
import org.apache.shindig.gadgets.js.JsResponse;
import org.apache.shindig.gadgets.js.JsServingPipeline;
import org.apache.shindig.gadgets.preload.PreloadException;
import org.apache.shindig.gadgets.preload.PreloadedData;
import org.apache.shindig.gadgets.rewrite.GadgetRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.MessageBundle;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.templates.MessageELResolver;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.UriCommon;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.google.common.base.Splitter;
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
  //class name for logging purpose
  private static final String classname = RenderingGadgetRewriter.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  protected static final String DEFAULT_CSS =
      "body,td,div,span,p{font-family:arial,sans-serif;}" +
      "a {color:#0000cc;}a:visited {color:#551a8b;}" +
      "a:active {color:#ff0000;}" +
      "body{margin: 0px;padding: 0px;background-color:white;}";
  protected static final String SCROLLING_CSS =
      "html,body{height:100%;width:100%;overflow:auto;}";
  static final String IS_GADGET_BEACON = "window['__isgadget']=true;";
  static final String INSERT_BASE_ELEMENT_KEY = "gadgets.insertBaseElement";
  static final String REWRITE_DOCTYPE_QNAME = "gadgets.doctype_qname";
  static final String REWRITE_DOCTYPE_PUBID = "gadgets.doctype_pubid";
  static final String REWRITE_DOCTYPE_SYSID = "gadgets.doctype_sysid";
  static final String FEATURES_KEY = "gadgets.features";

  protected final MessageBundleFactory messageBundleFactory;
  protected final ContainerConfig containerConfig;
  protected final FeatureRegistryProvider featureRegistryProvider;
  protected final JsServingPipeline jsServingPipeline;
  protected final JsUriManager jsUriManager;
  protected final ConfigProcessor configProcessor;
  protected final GadgetAdminStore gadgetAdminStore;

  protected Set<String> defaultExternLibs = ImmutableSet.of();

  protected Boolean externalizeFeatures = false;

  // DOCTYPE for HTML5, OpenSocial 2.0 default
  private String defaultDoctypeQName = "html";
  private String defaultDoctypePubId = null;
  private String defaultDoctypeSysId = null;

  private final Expressions expressions;
  private ELContext elContext;
  /**
   * @param messageBundleFactory Used for injecting message bundles into gadget output.
   */
  @Inject
  public RenderingGadgetRewriter(MessageBundleFactory messageBundleFactory,
                                 Expressions expressions,
                                 ContainerConfig containerConfig,
                                 FeatureRegistryProvider featureRegistryProvider,
                                 JsServingPipeline jsServingPipeline,
                                 JsUriManager jsUriManager,
                                 ConfigProcessor configProcessor,
                                 GadgetAdminStore gadgetAdminStore) {
    this.messageBundleFactory = messageBundleFactory;
    this.expressions = expressions;
    this.containerConfig = containerConfig;
    this.featureRegistryProvider = featureRegistryProvider;
    this.jsServingPipeline = jsServingPipeline;
    this.jsUriManager = jsUriManager;
    this.configProcessor = configProcessor;
    this.gadgetAdminStore = gadgetAdminStore;
  }

  public void setDefaultDoctypeQName(String qname) {
      this.defaultDoctypeQName = qname;
  }

  public void setDefaultDoctypePubId( String pubid) {
      this.defaultDoctypePubId = pubid;
  }

  public void setDefaultDoctypeSysId( String sysid) {
    this.defaultDoctypeSysId = sysid;
  }

  @Inject
  public void setDefaultForcedLibs(@Named("shindig.gadget-rewrite.default-forced-libs")String forcedLibs) {
    if (StringUtils.isNotBlank(forcedLibs)) {
      defaultExternLibs = ImmutableSortedSet.copyOf(Splitter.on(':').split(forcedLibs));
    }
  }

  @Inject(optional = true)
  public void setExternalizeFeatureLibs(@Named("shindig.gadget-rewrite.externalize-feature-libs")Boolean externalizeFeatures) {
    this.externalizeFeatures = externalizeFeatures;
  }

  /** Process the children of an element or document. */
  public void processChildNodes(Node source) {
    NodeList nodes = source.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      processNode(nodes.item(i));
    }
  }

  /**
   * Process a node.
   *
   * @param result the target node where results should be inserted
   * @param source the source node of the template being processed
   */
  private void processNode(Node source) {
    switch (source.getNodeType()) {
    case Node.TEXT_NODE:
      try {
        source.setTextContent(String.valueOf(expressions.parse(source.getTextContent(), String.class)
              .getValue(elContext)));
      } catch (PropertyNotFoundException pe) {
        if (LOG.isLoggable(Level.INFO)) {
          LOG.log(Level.INFO, pe.getMessage(), pe);
        }
      }
      break;
    case Node.ELEMENT_NODE:
      processChildNodes(source);
      break;
    case Node.DOCUMENT_NODE:
      processChildNodes(source);
      break;
    }
  }

  public void rewrite(Gadget gadget, MutableContent mutableContent) throws RewritingException {
    // Don't touch sanitized gadgets.
    if (gadget.sanitizeOutput()) {
      return;
    }

    try {
      GadgetContext context = gadget.getContext();
      MessageBundle bundle = messageBundleFactory.getBundle(gadget.getSpec(), context.getLocale(),
              context.getIgnoreCache(), context.getContainer(), context.getView());

      MessageELResolver messageELResolver = new MessageELResolver(expressions, bundle);

      this.elContext = expressions.newELContext(messageELResolver,
              new GadgetELResolver(gadget.getContext()));
      this.elContext.putContext(GadgetContext.class, elContext);
      Document document = mutableContent.getDocument();
      processChildNodes(document);
      Element head = (Element) DomUtil.getFirstNamedChildNode(document.getDocumentElement(), "head");

      // Insert new content before any of the existing children of the head element
      Node firstHeadChild = head.getFirstChild();

      Element injectedStyle = document.createElement("style");
      injectedStyle.setAttribute("type", "text/css");
      head.insertBefore(injectedStyle, firstHeadChild);

      // Inject default scrolling to the body
      this.injectDefaultScrolling(injectedStyle);

      // Only inject default styles if no doctype was specified.
      if (document.getDoctype() == null) {
        injectedStyle.appendChild(injectedStyle.getOwnerDocument().
            createTextNode(DEFAULT_CSS));
      }
      // Override & insert DocType if Gadget is written for OpenSocial 2.0 or greater,
      // if quirksmode is not set
      if(gadget.getSpecificationVersion().isEqualOrGreaterThan("2.0.0")
          && !gadget.useQuirksMode()){
        String container = gadget.getContext().getContainer();
        String doctype_qname = defaultDoctypeQName;
        String doctype_sysid = defaultDoctypeSysId;
        String doctype_pubid = defaultDoctypePubId;
        String value = containerConfig.getString(container, REWRITE_DOCTYPE_QNAME);
        if(value != null){
          doctype_qname = value;
        }
        value = containerConfig.getString(container, REWRITE_DOCTYPE_SYSID);
        if(value != null){
          doctype_sysid = value;
        }
        value = containerConfig.getString(container, REWRITE_DOCTYPE_PUBID);
        if(value != null){
          doctype_pubid = value;
        }
        //Don't inject DOCTYPE if QName is null
        if(doctype_qname != null){
          DocumentType docTypeNode = document.getImplementation()
              .createDocumentType(doctype_qname, doctype_pubid, doctype_sysid);
          if(document.getDoctype() != null){
            document.removeChild(document.getDoctype());
          }
          document.insertBefore(docTypeNode, document.getFirstChild());
        }
      }

      Element html= (Element)document.getElementsByTagName("html").item(0);
      if(html != null){
        Locale locale = gadget.getContext().getLocale();
        if (locale != null) {
          String locStr = locale.toString();
          String locValue = locStr.replace("_", "-");
          html.setAttribute("lang", locValue);
          html.setAttribute("xml:lang", locValue);
        }
      }

      injectBaseTag(gadget, head);
      injectGadgetBeacon(gadget, head, firstHeadChild);
      injectFeatureLibraries(gadget, head, firstHeadChild);

      // This can be one script block.
      Element mainScriptTag = document.createElement("script");
      injectMessageBundles(bundle, mainScriptTag);
      injectDefaultPrefs(gadget, mainScriptTag);
      injectPreloads(gadget, mainScriptTag);

      // We need to inject our script before any developer scripts.
      head.insertBefore(mainScriptTag, firstHeadChild);

      Element body = (Element)DomUtil.getFirstNamedChildNode(document.getDocumentElement(), "body");

      body.setAttribute("dir", bundle.getLanguageDirection());

      // With Caja enabled, onloads are triggered by features/caja/taming.js
      if (!gadget.requiresCaja()) {
        injectOnLoadHandlers(body);
      }

      mutableContent.documentChanged();
    } catch (GadgetException e) {
      throw new RewritingException(e.getLocalizedMessage(), e, e.getHttpStatusCode());
    }
  }

  protected void injectDefaultScrolling(Element injectedStyle) {
    injectedStyle.appendChild(injectedStyle.getOwnerDocument().
        createTextNode(SCROLLING_CSS));
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


  /**
   * @throws GadgetException
   */
  protected void injectGadgetBeacon(Gadget gadget, Node headTag, Node firstHeadChild)
          throws GadgetException {
    Element beaconNode = headTag.getOwnerDocument().createElement("script");
    beaconNode.setTextContent(IS_GADGET_BEACON);
    headTag.insertBefore(beaconNode, firstHeadChild);
  }

  protected String getFeatureRepositoryId(Gadget gadget) {
    GadgetContext context = gadget.getContext();
    return context.getRepository();
  }

  /**
   * Injects javascript libraries needed to satisfy feature dependencies.
   */
  protected void injectFeatureLibraries(Gadget gadget, Node headTag, Node firstHeadChild)
          throws GadgetException {
    // TODO: If there isn't any js in the document, we can skip this. Unfortunately, that means
    // both script tags (easy to detect) and event handlers (much more complex).
    GadgetContext context = gadget.getContext();
    String repository = getFeatureRepositoryId(gadget);
    FeatureRegistry featureRegistry = featureRegistryProvider.get(repository);

    checkRequiredFeatures(gadget, featureRegistry);
    //Check to make sure all the required features that are about to be injected are allowed
    if(!gadgetAdminStore.checkFeatureAdminInfo(gadget)) {
      throw new GadgetException(Code.GADGET_ADMIN_FEATURE_NOT_ALLOWED);
    }

    // Set of extern libraries requested by the container
    Set<String> externForcedLibs = defaultExternLibs;

    // gather the libraries we'll need to generate the extern script for
    String externParam = context.getParameter("libs");
    if (StringUtils.isNotBlank(externParam)) {
      externForcedLibs = Sets.newTreeSet(Splitter.on(':').split(externParam));
    }

    // Inject extern script
    if (!externForcedLibs.isEmpty()) {
      injectScript(externForcedLibs, null, false, gadget, headTag, firstHeadChild, "");
    }

    Collection<String> gadgetLibs = Lists.newArrayList(gadget.getDirectFeatureDeps());
    List<Feature> gadgetFeatures = gadget.getSpec().getModulePrefs().getAllFeatures();
    for(Feature feature : gadgetFeatures) {
      if(!feature.getRequired() &&
              !gadgetAdminStore.isAllowedFeature(feature, gadget)) {
        //If the feature is optional and the admin has not allowed it don't include it
        gadgetLibs.remove(feature.getName());
      }
    }

    // Get config for all features
    Set<String> allLibs = ImmutableSet.<String>builder()
        .addAll(externForcedLibs).addAll(gadgetLibs).build();
    String libraryConfig =
      getLibraryConfig(gadget, featureRegistry.getFeatures(allLibs));

    // Inject internal script
    injectScript(gadgetLibs, externForcedLibs, !externalizeFeatures,
        gadget, headTag, firstHeadChild, libraryConfig);
  }

  /**
   * Check that all gadget required features exists
   */
  protected void checkRequiredFeatures(Gadget gadget, FeatureRegistry featureRegistry)
      throws GadgetException {
    List<String> unsupported = Lists.newLinkedList();

    // Get all resources requested by the gadget's requires/optional features.
    Map<String, Feature> featureMap = gadget.getViewFeatures();
    List<String> gadgetFeatureKeys = Lists.newLinkedList(gadget.getDirectFeatureDeps());
    featureRegistry.getFeatureResources(gadget.getContext(), gadgetFeatureKeys, unsupported)
                   .getResources();
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
  }

  /**
   * Get the JS content for a request (JsUri)
   */
  protected String getFeaturesContent(JsUri jsUri) throws GadgetException {
    // Inject js content, fetched from JsPipeline
    JsRequest jsRequest = new JsRequestBuilder(jsUriManager,
        featureRegistryProvider.get(jsUri.getRepository())).build(jsUri, null);
    JsResponse jsResponse;
    try {
      jsResponse = jsServingPipeline.execute(jsRequest);
    } catch (JsException e) {
      throw new GadgetException(Code.JS_PROCESSING_ERROR, e, e.getStatusCode());
    }
    return jsResponse.toJsString();
  }

  /**
   * Add script tag with either js content (inline=true) or script src tag
   */
  protected void injectScript(Collection<String> libs, Collection<String> loaded, boolean inline,
      Gadget gadget, Node headTag, Node firstHeadChild, String extraContent)
      throws GadgetException {

    GadgetContext context = gadget.getContext();
    // Gadget is not specified in request in order to support better caching
    JsUri jsUri = new JsUri(null, context.getDebug(), false, context.getContainer(), null,
        libs, loaded, null, false, false, RenderingContext.getDefault(), null,
        getFeatureRepositoryId(gadget));
    jsUri.setCajoleContent(gadget.requiresCaja());

    String content = "";
    if (!inline) {
      String jsUrl = new UriBuilder(jsUriManager.makeExternJsUri(jsUri))
          // Avoid jsload by adding jsload=0
          .addQueryParameter(UriCommon.Param.JSLOAD.getKey(), "0")
          .toString();
      Element libsTag = headTag.getOwnerDocument().createElement("script");
      libsTag.setAttribute("src", jsUrl);
      headTag.insertBefore(libsTag, firstHeadChild);
    } else {
      content = getFeaturesContent(jsUri);
    }

    content = content + extraContent;
    if (content.length() > 0) {
      Element inlineTag = headTag.getOwnerDocument().createElement("script");
      headTag.insertBefore(inlineTag, firstHeadChild);
      inlineTag.appendChild(headTag.getOwnerDocument().createTextNode(content));
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
    Map<String, Object> config =
        configProcessor.getConfig(gadget.getContext().getContainer(), reqs, null, gadget);

    if (!config.isEmpty()) {
      return "gadgets.config.init(" + JsonSerializer.serialize(config) + ");\n";
    }

    return "";
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
        if (LOG.isLoggable(Level.WARNING)) {
          LOG.logp(Level.WARNING, classname, "injectPreloads", MessageKeys.UNEXPECTED_ERROR_PRELOADING);
          LOG.log(Level.WARNING, pe.getMessage(), pe);
        }
      }
    }
    Text text = scriptTag.getOwnerDocument().createTextNode("gadgets.io.preloaded_=");
    text.appendData(JsonSerializer.serialize(preload));
    text.appendData(";");
    scriptTag.appendChild(text);
  }
}
