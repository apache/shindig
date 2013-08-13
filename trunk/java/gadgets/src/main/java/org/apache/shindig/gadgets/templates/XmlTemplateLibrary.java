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
package org.apache.shindig.gadgets.templates;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.render.SanitizingGadgetRewriter;
import org.apache.shindig.gadgets.templates.tags.DefaultTagRegistry;
import org.apache.shindig.gadgets.templates.tags.TagHandler;
import org.apache.shindig.gadgets.templates.tags.TemplateBasedTagHandler;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Set;

/**
 * An Object representing a Library of Template-based custom OSML tags.
 */
public class XmlTemplateLibrary implements TemplateLibrary {

  public static final String TAG_ATTRIBUTE = "tag";
  public static final String NAMESPACE_TAG = "Namespace";
  public static final String TEMPLATE_TAG = "Template";
  public static final String STYLE_TAG = "Style";
  public static final String JAVASCRIPT_TAG = "JavaScript";
  public static final String TEMPLATEDEF_TAG = "TemplateDef";

  private final Uri libraryUri;
  private final String source;
  private final boolean safe;
  private final TagRegistry registry;
  private String nsPrefix;
  private String nsUri;
  private String style;
  private String javaScript;
  private final Set<TemplateResource> libraryResources;

  /**
   * @param uri URI of the template library
   * @param root Element representing the Templates tag of this library
   */
  public XmlTemplateLibrary(Uri uri, Element root, String source)
      throws GadgetException {
    this(uri, root, source, false);
  }

  /**
   * @param uri URI of the template library
   * @param root Element representing the Templates tag of this library
   * @param safe Is this library exempt from being sanitized?
   */
  public XmlTemplateLibrary(Uri uri, Element root, String source, boolean safe)
      throws GadgetException {
    this.libraryUri = uri;
    this.source = source;
    this.safe = safe;
    this.registry = new DefaultTagRegistry(parseLibraryDocument(root));
    ImmutableSet.Builder<TemplateResource> resources = ImmutableSet.builder();
    if (style != null) {
      resources.add(TemplateResource.newStyleResource(style, this));
    }
    if (javaScript != null) {
      resources.add(TemplateResource.newJavascriptResource(javaScript, this));
    }

    this.libraryResources = resources.build();
  }

  /**
   * @return a registry of tags in this library.
   */
  public TagRegistry getTagRegistry() {
    return registry;
  }

  /**
   * @return the URI from which the library was loaded.  (This is not the
   * namespace of tags in the library.)
   */
  public Uri getLibraryUri() {
    return libraryUri;
  }

  /**
   * @return this library is safe and its content doesn't need to be sanitized.
   */
  public boolean isSafe() {
    return safe;
  }

  /**
   * @return This library as XML source.
   */
  public String serialize() {
    return source;
  }

  /**
   * Creates a tag handler wrapping an element.  By default, creates
   * a {@link TemplateBasedTagHandler}.  Override this to create custom
   * tag handlers.
   */
  protected TagHandler createTagHandler(Element template, String namespaceUri,
      String localName) {
    return new TemplateBasedTagHandler(template, namespaceUri, localName);
  }

  private Set<TagHandler> parseLibraryDocument(Element root) throws GadgetException {
    ImmutableSet.Builder<TagHandler> handlers = ImmutableSet.builder();

    NodeList nodes = root.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if (!(node instanceof Element)) {
        continue;
      }

      Element element = (Element) node;
      if (NAMESPACE_TAG.equals(element.getLocalName())) {
        processNamespace(element);
      } else if (STYLE_TAG.equals(element.getLocalName())) {
        processStyle(element);
      } else if (JAVASCRIPT_TAG.equals(element.getLocalName())) {
        processJavaScript(element);
      } else if (TEMPLATE_TAG.equals(element.getLocalName())) {
        processTemplate(handlers, element);
      } else if (TEMPLATEDEF_TAG.equals(element.getLocalName())) {
        processTemplateDef(handlers, element);
      }
    }

    return handlers.build();
  }

  private void processTemplateDef(Builder<TagHandler> handlers, Element defElement)
      throws TemplateParserException {
    Attr tagAttribute = defElement.getAttributeNode(TAG_ATTRIBUTE);
    if (tagAttribute == null) {
      throw new TemplateParserException("Missing tag attribute on TemplateDef");
    }

    ImmutableSet.Builder<TemplateResource> resources = ImmutableSet.builder();

    Element scriptElement = (Element) DomUtil.getFirstNamedChildNode(defElement, JAVASCRIPT_TAG);
    if (scriptElement != null) {
      resources.add(TemplateResource.newJavascriptResource(scriptElement.getTextContent(), this));
    }

    Element styleElement = (Element) DomUtil.getFirstNamedChildNode(defElement, STYLE_TAG);
    if (styleElement != null) {
      resources.add(TemplateResource.newStyleResource(styleElement.getTextContent(), this));
    }

    Element templateElement = (Element) DomUtil.getFirstNamedChildNode(defElement, TEMPLATE_TAG);
    TagHandler handler = createHandler(tagAttribute.getNodeValue(), templateElement,
        resources.build());
    if (handler != null) {
      handlers.add(handler);
    }
  }

  private void processTemplate(Builder<TagHandler> handlers, Element templateElement)
      throws TemplateParserException {
    Attr tagAttribute = templateElement.getAttributeNode(TAG_ATTRIBUTE);
    if (tagAttribute == null) {
      throw new TemplateParserException("Missing tag attribute on Template");
    }

    TagHandler handler = createHandler(tagAttribute.getNodeValue(), templateElement,
        ImmutableSet.<TemplateResource>of());
    if (handler != null) {
      handlers.add(handler);
    }
  }

  private void processStyle(Element element) {
    if (style == null) {
      style = element.getTextContent();
    } else {
      style = style + '\n' + element.getTextContent();
    }
  }

  private void processJavaScript(Element element) {
    if (javaScript == null) {
      javaScript = element.getTextContent();
    } else {
      javaScript = javaScript + '\n' + element.getTextContent();
    }
  }

  private void processNamespace(Element namespaceNode) throws TemplateParserException {
    if ((nsPrefix != null) || (nsUri != null)) {
      throw new TemplateParserException("Duplicate Namespace elements");
    }

    nsPrefix = namespaceNode.getAttribute("prefix");
    if ("".equals(nsPrefix)) {
      throw new TemplateParserException("Missing prefix attribute on Namespace");
    }

    nsUri = namespaceNode.getAttribute("url");
    if ("".equals(nsUri)) {
      throw new TemplateParserException("Missing url attribute on Namespace");
    }
  }

  private TagHandler createHandler(String tagName, Element template,
      Set<TemplateResource> resources)
      throws TemplateParserException {
    String [] nameParts = StringUtils.splitPreserveAllTokens(tagName, ':');
    // At this time, we only support namespaced tags
    if (nameParts.length != 2) {
      return null;
    }
    String namespaceUri = template.lookupNamespaceURI(nameParts[0]);
    if (!nsPrefix.equals(nameParts[0]) || !nsUri.equals(namespaceUri)) {
      throw new TemplateParserException(
          "Can't create tags in undeclared namespace: " + nameParts[0]);
    }

    if (isSafe()) {
      bypassTemplateSanitization(template);
    }

    return new LibraryTagHandler(
        createTagHandler(template, namespaceUri, nameParts[1]),
        resources);
  }

  /**
   * For "safe" libraries, bypass sanitization.  Sanitization should
   * be bypassed on each element in the tree, but not on the whole
   * tree (false, not true, in the call to bypassSanitization() below),
   * since os:Render elements will insert unsafe content.
   */
  private void bypassTemplateSanitization(Element template) {
    SanitizingGadgetRewriter.bypassSanitization(template, false);
    NodeList children = template.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (node instanceof Element) {
        bypassTemplateSanitization((Element) node);
      }
    }
  }

  /**
   * TagHandler delegate reponsible for adding necessary tag resources
   * as each tag gets processed.
   */
  private class LibraryTagHandler implements TagHandler {
    private final TagHandler tagHandler;
    private final Set<TemplateResource> tagResources;

    public LibraryTagHandler(TagHandler tagHandler, Set<TemplateResource> resources) {
      this.tagHandler = tagHandler;
      tagResources = resources;
    }

    public String getNamespaceUri() {
      return tagHandler.getNamespaceUri();
    }

    public String getTagName() {
      return tagHandler.getTagName();
    }

    public void process(Node result, Element tag, TemplateProcessor processor) {
      // Add all template resources and library resources.  Use the resource
      // instance as its own key, since we're careful to create the resource
      // objects once.  NOTE: this assumes that TemplateResource uses instance
      // equality, not value equality.
      for (TemplateResource resource : tagResources) {
        processor.getTemplateContext().addResource(resource, resource);
      }

      for (TemplateResource resource : libraryResources) {
        processor.getTemplateContext().addResource(resource, resource);
      }

      tagHandler.process(result, tag, processor);
    }
  }
}
