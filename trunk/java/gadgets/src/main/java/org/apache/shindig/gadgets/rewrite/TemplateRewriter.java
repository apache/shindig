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
package org.apache.shindig.gadgets.rewrite;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.MessageBundleFactory;
import org.apache.shindig.gadgets.parse.SocialDataTags;
import org.apache.shindig.gadgets.render.SanitizingGadgetRewriter;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.MessageBundle;
import org.apache.shindig.gadgets.templates.ContainerTagLibraryFactory;
import org.apache.shindig.gadgets.templates.MessageELResolver;
import org.apache.shindig.gadgets.templates.TagRegistry;
import org.apache.shindig.gadgets.templates.TemplateContext;
import org.apache.shindig.gadgets.templates.TemplateLibrary;
import org.apache.shindig.gadgets.templates.TemplateLibraryFactory;
import org.apache.shindig.gadgets.templates.TemplateParserException;
import org.apache.shindig.gadgets.templates.TemplateProcessor;
import org.apache.shindig.gadgets.templates.TemplateResource;
import org.apache.shindig.gadgets.templates.tags.CompositeTagRegistry;
import org.apache.shindig.gadgets.templates.tags.DefaultTagRegistry;
import org.apache.shindig.gadgets.templates.tags.TagHandler;
import org.apache.shindig.gadgets.templates.tags.TemplateBasedTagHandler;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This ContentRewriter uses a TemplateProcessor to replace os-template
 * tag contents of a gadget spec with their rendered equivalents.
 *
 * Only templates without the @name and @tag attributes are processed
 * automatically.
 */
public class TemplateRewriter implements GadgetRewriter {

  public final static Set<String> TAGS = ImmutableSet.of("script");
  public static final String TEMPLATES_FEATURE_NAME = "opensocial-templates";
  public static final String OSML_FEATURE_NAME = "osml";

  /** Specifies what template libraries to load */
  public static final String REQUIRE_LIBRARY_PARAM = "requireLibrary";

  /** Set to true to block auto-processing of templates */
  static final String DISABLE_AUTO_PROCESSING_PARAM = "disableAutoProcessing";

  /** Enable client support? **/
  static final String CLIENT_SUPPORT_PARAM = "client";

  //class name for logging purpose
  private static final String classname = TemplateRewriter.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);


  /**
   * Provider of the processor.  TemplateRewriters are stateless and multithreaded,
   * processors are not.
   */
  private final Provider<TemplateProcessor> processor;
  private final MessageBundleFactory messageBundleFactory;
  private final Expressions expressions;
  private final TagRegistry baseTagRegistry;
  private final TemplateLibraryFactory libraryFactory;
  private final ContainerTagLibraryFactory containerTagLibraryFactory;

  @Inject
  public TemplateRewriter(Provider<TemplateProcessor> processor,
      MessageBundleFactory messageBundleFactory, Expressions expressions,
      TagRegistry baseTagRegistry, TemplateLibraryFactory libraryFactory,
      ContainerTagLibraryFactory containerTagLibraryFactory) {
    this.processor = processor;
    this.messageBundleFactory = messageBundleFactory;
    this.expressions = expressions;
    this.baseTagRegistry = baseTagRegistry;
    this.libraryFactory = libraryFactory;
    this.containerTagLibraryFactory = containerTagLibraryFactory;
  }

  public void rewrite(Gadget gadget, MutableContent content) throws RewritingException {
    Map<String, Feature> directFeatures = gadget.getViewFeatures();

    Feature feature = directFeatures.get(TEMPLATES_FEATURE_NAME);
    if (feature == null && directFeatures.containsKey(OSML_FEATURE_NAME)) {
      feature = directFeatures.get(OSML_FEATURE_NAME);
    }

    if (feature != null && isServerTemplatingEnabled(feature)) {
      try {
        rewriteImpl(gadget, feature, content);
      } catch (GadgetException ge) {
        throw new RewritingException(ge, ge.getHttpStatusCode());
      }
    }
  }

  /**
   * Disable server-side templating when the feature contains:
   * <pre>
   *   &lt;Param name="disableAutoProcessing"&gt;true&lt;/Param&gt;
   * </pre>
   */
  private boolean isServerTemplatingEnabled(Feature feature) {
    return (!"true".equalsIgnoreCase(feature.getParam(DISABLE_AUTO_PROCESSING_PARAM)));
  }

  private void rewriteImpl(Gadget gadget, Feature feature, MutableContent content)
      throws GadgetException {
    List<TagRegistry> registries = Lists.newArrayList();
    List<TemplateLibrary> libraries = Lists.newArrayList();

    // TODO: Add View-specific library as Priority 0

    // Built-in Java-based tags - Priority 1
    registries.add(baseTagRegistry);

    TemplateLibrary osmlLibrary = containerTagLibraryFactory.getLibrary(gadget.getContext().getContainer());

    // OSML Built-in tags - Priority 2
    registries.add(osmlLibrary.getTagRegistry());
    libraries.add(osmlLibrary);

    List<Element> templateElements = SocialDataTags.getTags(content.getDocument(),
        SocialDataTags.OSML_TEMPLATE_TAG);
    List<Element> templates = ImmutableList.copyOf(templateElements);

    if (!OSML_FEATURE_NAME.equals(feature.getName())) {
      // User-defined custom tags - Priority 3
      registries.add(registerCustomTags(templates));

      // User-defined libraries - Priority 4
      loadTemplateLibraries(gadget.getContext(), feature, registries, libraries);
    }

    TagRegistry registry = new CompositeTagRegistry(registries);

    TemplateContext templateContext = new TemplateContext(gadget, content.getPipelinedData());
    boolean needsFeature = executeTemplates(templateContext, content, templates, registry);

    // Check if a feature param overrides  our guess at whether the client-side
    // feature is needed.
    String clientOverride = feature.getParam(CLIENT_SUPPORT_PARAM);
    if ("true".equalsIgnoreCase(clientOverride)) {
      needsFeature = true;
    } else if ("false".equalsIgnoreCase(clientOverride)) {
      needsFeature = false;
    }

    Element head = (Element) DomUtil.getFirstNamedChildNode(
        content.getDocument().getDocumentElement(), "head");
    postProcess(templateContext, needsFeature, head, templates, libraries);
  }

  /**
   * Post-processes the gadget content after rendering templates.
   *
   * @param templateContext TemplateContext to operate on
   * @param needsFeature Should the templates feature be made available to
   * client?
   * @param head Head element of the gadget's document
   * @param libraries Keeps track of all libraries, and which got used
   * @param allTemplates A list of all the template nodes
   * @param libraries A list of all registered libraries
   */
  private void postProcess(TemplateContext templateContext, boolean needsFeature, Element head,
      List<Element> allTemplates, List<TemplateLibrary> libraries) {
    // Inject all the needed library assets.
    // TODO: inject library assets that aren't used on the server, but will
    // be needed on the client
    for (TemplateResource resource : templateContext.getResources()) {
      injectTemplateLibraryAssets(resource, head);
    }

    // If we don't need the feature, remove it and all templates from the gadget
    if (!needsFeature) {
      templateContext.getGadget().removeFeature(TEMPLATES_FEATURE_NAME);
      for (Element template : allTemplates) {
        Node parent = template.getParentNode();
        if (parent != null) {
          parent.removeChild(template);
        }
      }
    } else {
      // If the feature is to be kept, inject the libraries.
      // Library assets will be generated on the client.
      // TODO: only inject the templates, not the full scripts/styles
      for (TemplateLibrary library : libraries) {
        injectTemplateLibrary(library, head);
      }
    }
  }

  private void loadTemplateLibraries(GadgetContext context, Feature feature,
      List<TagRegistry> registries, List<TemplateLibrary> libraries)  throws GadgetException {
    Collection<String> urls = feature.getParams().get(REQUIRE_LIBRARY_PARAM);
    if (urls != null) {
      for (String url : urls) {
        Uri uri = Uri.parse(url.trim());
        uri = context.getUrl().resolve(uri);

        try {
          TemplateLibrary library = libraryFactory.loadTemplateLibrary(context, uri);
          registries.add(library.getTagRegistry());
          libraries.add(library);
        } catch (TemplateParserException te) {
          // Suppress exceptions due to malformed template libraries
          if (LOG.isLoggable(Level.WARNING)) {
            LOG.logp(Level.WARNING, classname, "loadTemplateLibraries", MessageKeys.MALFORMED_TEMPLATE_LIB);
            LOG.log(Level.WARNING, te.getMessage(),te);
          }
        }
      }
    }
  }

  private void injectTemplateLibraryAssets(TemplateResource resource, Element head) {
    Element contentElement;
    switch (resource.getType()) {
      case JAVASCRIPT:
        contentElement = head.getOwnerDocument().createElement("script");
        contentElement.setAttribute("type", "text/javascript");
        break;
      case STYLE:
        contentElement = head.getOwnerDocument().createElement("style");
        contentElement.setAttribute("type", "text/css");
        break;
      default:
        throw new IllegalStateException("Unhandled type");
    }

    if (resource.isSafe()) {
      SanitizingGadgetRewriter.bypassSanitization(contentElement, false);
    }
    contentElement.setTextContent(resource.getContent());
    head.appendChild(contentElement);
  }

  private void injectTemplateLibrary(TemplateLibrary library, Element head) {
    try {
      String libraryContent = library.serialize();
      if (Strings.isNullOrEmpty(libraryContent)) {
        return;
      }

      Element scriptElement = head.getOwnerDocument().createElement("script");
      scriptElement.setAttribute("type", "text/javascript");
      StringBuilder buffer = new StringBuilder();
      buffer.append("opensocial.template.Loader.loadContent(");
      JsonSerializer.appendString(buffer, library.serialize());
      buffer.append(',');
      JsonSerializer.appendString(buffer, library.getLibraryUri().toString());
      buffer.append(");");
      scriptElement.setTextContent(buffer.toString());
      head.appendChild(scriptElement);
    } catch (IOException ioe) {
      // This should never happen.
    }
  }

  /**
   * Register templates with a "tag" attribute.
   */
  private TagRegistry registerCustomTags(List<Element> allTemplates) {
    ImmutableSet.Builder<TagHandler> handlers = ImmutableSet.builder();
    for (Element template : allTemplates) {
      // Only process templates with a tag attribute
      if (template.getAttribute("tag").length() == 0) {
        continue;
      }

      Iterable<String> nameParts = Splitter.on(':').split(template.getAttribute("tag"));
      // At this time, we only support
      if (Iterables.size(nameParts) != 2) {
        continue;
      }
      String namespaceUri = template.lookupNamespaceURI(Iterables.get(nameParts, 0));
      if (namespaceUri != null) {
        handlers.add(new TemplateBasedTagHandler(template, namespaceUri, Iterables.get(nameParts, 1)));
      }
    }

    return new DefaultTagRegistry(handlers.build());
  }

  /**
   * Processes and renders inline templates.
   * @return Do we think the templates feature is still needed on the client?
   */
  private boolean executeTemplates(TemplateContext templateContext, MutableContent content,
      List<Element> allTemplates, TagRegistry registry) throws GadgetException {
    Map<String, Object> pipelinedData = content.getPipelinedData();

    // If true, client-side processing will be needed
    boolean needsFeature = false;
    List<Element> templates = Lists.newArrayList();
    for (Element element : allTemplates) {
      String tag = element.getAttribute("tag");
      String require = element.getAttribute("require");

      if (!checkRequiredData(require, pipelinedData.keySet())) {
        // Can't be processed on the server at all;  keep client-side processing
        needsFeature = true;
      } else if ("".equals(tag)) {
        templates.add(element);
      }
    }

    if (!templates.isEmpty()) {
      Gadget gadget = templateContext.getGadget();

      MessageBundle bundle = messageBundleFactory.getBundle(gadget.getSpec(),
          gadget.getContext().getLocale(), gadget.getContext().getIgnoreCache(),
          gadget.getContext().getContainer(), gadget.getContext().getView());
      MessageELResolver messageELResolver = new MessageELResolver(expressions, bundle);

      int autoUpdateID = 0;
      for (Element template : templates) {
        DocumentFragment result = processor.get().processTemplate(
            template, templateContext, messageELResolver, registry);
        // TODO: sanitized renders should ignore this value
        if ("true".equals(template.getAttribute("autoUpdate"))) {
          // autoUpdate requires client-side processing.
          needsFeature = true;
          Element span = template.getOwnerDocument().createElement("span");
          String id = "template_auto" + (autoUpdateID++);
          span.setAttribute("id", "_T_" + id);
          template.setAttribute("name", id);
          template.getParentNode().insertBefore(span, template);
          span.appendChild(result);
        } else {
          template.getParentNode().insertBefore(result, template);
          template.getParentNode().removeChild(template);
        }
      }
      MutableContent.notifyEdit(content.getDocument());
    }
    return needsFeature;
  }

  /**
   * Checks that all the required data is available at rewriting time.
   * @param requiredData A string of comma-separated data set names
   * @param availableData A map of available data sets
   * @return true if all required data sets are present, false otherwise
   */
  private static boolean checkRequiredData(String requiredData, Set<String> availableData) {
    if ("".equals(requiredData)) {
      return true;
    }
    StringTokenizer st = new StringTokenizer(requiredData, ",");
    while (st.hasMoreTokens()) {
      if (!availableData.contains(st.nextToken().trim())) {
        return false;
      }
    }
    return true;
  }
}
