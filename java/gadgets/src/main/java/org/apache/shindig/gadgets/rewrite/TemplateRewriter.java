/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.rewrite;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.MessageBundleFactory;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.MessageBundle;
import org.apache.shindig.gadgets.templates.CompositeTagRegistry;
import org.apache.shindig.gadgets.templates.DefaultTagRegistry;
import org.apache.shindig.gadgets.templates.MessageELResolver;
import org.apache.shindig.gadgets.templates.TagHandler;
import org.apache.shindig.gadgets.templates.TagRegistry;
import org.apache.shindig.gadgets.templates.TemplateBasedTagHandler;
import org.apache.shindig.gadgets.templates.TemplateContext;
import org.apache.shindig.gadgets.templates.TemplateLibrary;
import org.apache.shindig.gadgets.templates.TemplateLibraryFactory;
import org.apache.shindig.gadgets.templates.TemplateParserException;
import org.apache.shindig.gadgets.templates.TemplateProcessor;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * This ContentRewriter uses a TemplateProcessor to replace os-template
 * tag contents of a gadget spec with their rendered equivalents.
 *
 * Only templates without the @name and @tag attributes are processed
 * automatically.
 */
public class TemplateRewriter implements GadgetRewriter {

  public final static Set<String> TAGS = ImmutableSet.of("script");

  /** Set to true to block auto-processing of templates */
  static final Object DISABLE_AUTO_PROCESSING_PARAM = "disableAutoProcessing";
  
  /** Specifies what template libraries to load */
  static final Object REQUIRE_LIBRARY_PARAM = "requireLibrary";

  static private final Logger logger = Logger.getLogger(TemplateRewriter.class.getName());
  /**
   * Provider of the processor.  TemplateRewriters are stateless and multithreaded,
   * processors are not.
   */
  private final Provider<TemplateProcessor> processor;
  private final MessageBundleFactory messageBundleFactory;
  private final Expressions expressions;
  private final TagRegistry baseTagRegistry;
  private final TemplateLibraryFactory libraryFactory;

  @Inject
  public TemplateRewriter(Provider<TemplateProcessor> processor,
      MessageBundleFactory messageBundleFactory, Expressions expressions, 
      TagRegistry baseTagRegistry, TemplateLibraryFactory libraryFactory) {
    this.processor = processor;
    this.messageBundleFactory = messageBundleFactory;
    this.expressions = expressions;
    this.baseTagRegistry = baseTagRegistry;
    this.libraryFactory = libraryFactory;
  }

  public void rewrite(Gadget gadget, MutableContent content) {
    Feature f = gadget.getSpec().getModulePrefs().getFeatures()
        .get("opensocial-templates");
    if (f != null && isServerTemplatingEnabled(f)) {
      try {
        rewriteImpl(gadget, f, content);
      } catch (GadgetException ge) {
        // TODO: Rewriter interface needs to be modified to handle GadgetException or
        // RewriterException or something along those lines.
        throw new RuntimeException(ge);
      }
    }
  }

  /**
   * Disable server-side templating when the feature contains:
   * <pre>
   *   &lt;Param name="disableAutoProcessing"&gt;true&lt;/Param&gt;
   * </pre>
   */
  private boolean isServerTemplatingEnabled(Feature f) {
    return (!"true".equalsIgnoreCase(f.getParams().get(DISABLE_AUTO_PROCESSING_PARAM)));
  }

  private void rewriteImpl(Gadget gadget, Feature f, MutableContent content)
      throws GadgetException {
    List<Element> templates = ImmutableList.copyOf(
      Iterables.filter(
          DomUtil.getElementsByTagNameCaseInsensitive(content.getDocument(), TAGS),
          new Predicate<Element>() {
            public boolean apply(Element element) {
              return "text/os-template".equals(element.getAttribute("type"));
            }
          }));
    
    List<TagRegistry> registries = Lists.newArrayList();
    
    registries.add(baseTagRegistry);

    Element head = (Element) DomUtil.getFirstNamedChildNode(
        content.getDocument().getDocumentElement(), "head");

    loadTemplateLibraries(gadget.getContext(), f, registries, head);
    registries.add(registerCustomTags(templates));
    
    TagRegistry registry = new CompositeTagRegistry(registries);
    
    processInlineTemplates(gadget, content, templates, registry);
  }

  private void loadTemplateLibraries(GadgetContext context,
      Feature f, List<TagRegistry> registries, Element head)  throws GadgetException {
    // TODO: Support multiple values when Shindig does
    String url = f.getParams().get(REQUIRE_LIBRARY_PARAM);
    if (url != null) {
      Uri uri = Uri.parse(url.trim());
      uri = context.getUrl().resolve(uri);
      
      try {
        TemplateLibrary library = libraryFactory.loadTemplateLibrary(context, uri);
        String script = library.getJavaScript();
        
        // Append any needed Javascript
        if (!StringUtils.isEmpty(script)) {
          Element scriptElement = head.getOwnerDocument().createElement("script");
          scriptElement.setAttribute("type", "text/javascript");
          scriptElement.setTextContent(script);
          head.appendChild(scriptElement);
        }
        
        // Append any needed CSS
        String style = library.getStyle();
        if (!StringUtils.isEmpty(style)) {
          Element styleElement = head.getOwnerDocument().createElement("style");
          styleElement.setAttribute("type", "text/css");
          styleElement.setTextContent(style);
          head.appendChild(styleElement);
        }

        registries.add(library.getTagRegistry());
      } catch (TemplateParserException te) {
        // Suppress exceptions due to malformed template libraries
        logger.log(Level.WARNING, null, te);
      }
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
      
      // TODO: split() is a regex compile, and should be avoided
      String [] nameParts = template.getAttribute("tag").split(":");
      // At this time, we only support 
      if (nameParts.length != 2) {
        continue;
      }
      String namespaceUri = template.lookupNamespaceURI(nameParts[0]);      
      if (namespaceUri != null) {
        handlers.add(new TemplateBasedTagHandler(template, namespaceUri, nameParts[1]));
      }
    }
    
    return new DefaultTagRegistry(handlers.build());
  }
  
  private void processInlineTemplates(Gadget gadget, MutableContent content,
      List<Element> allTemplates, TagRegistry registry) throws GadgetException {
    Map<String, Object> pipelinedData = content.getPipelinedData();

    // If true, client-side processing will be needed
    boolean needsFeature = false;
    List<Element> templates = Lists.newArrayList();
    for (Element element : allTemplates) {
      String name = element.getAttribute("name");
      String tag = element.getAttribute("tag");
      String require = element.getAttribute("require");

      if (!"".equals(name) ||
          !checkRequiredData(require, pipelinedData.keySet())) {
        // Can't be processed on the server at all;  keep client-side processing
        needsFeature = true;
      } else if ("".equals(tag)) {
        templates.add(element);
      }
    }
    
    if (!templates.isEmpty()) {
      TemplateContext templateContext = new TemplateContext(gadget, pipelinedData);
      
      MessageBundle bundle = messageBundleFactory.getBundle(gadget.getSpec(),
          gadget.getContext().getLocale(), gadget.getContext().getIgnoreCache());
      MessageELResolver messageELResolver = new MessageELResolver(expressions, bundle);
  
      for (Element template : templates) {
        DocumentFragment result = processor.get().processTemplate(
            template, templateContext, messageELResolver, registry);
        template.getParentNode().insertBefore(result, template);
        if ("true".equals(template.getAttribute("autoUpdate"))) {
          // autoUpdate requires client-side processing.
          // TODO: give client-side processing some hope of finding the pre-rendered content
          needsFeature = true;
        } else {
          template.getParentNode().removeChild(template);
        }
      }
  
      MutableContent.notifyEdit(content.getDocument());
    }
    
    // Remove the opensocial-templates feature if we've fully processed all
    // inline templates.
    // TODO: remove inline custom tags as well.
    if (!needsFeature) {
      gadget.removeFeature("opensocial-templates");
    }
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
