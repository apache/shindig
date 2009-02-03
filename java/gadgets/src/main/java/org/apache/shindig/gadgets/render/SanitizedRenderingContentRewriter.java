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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.RewriterResults;

import com.google.common.collect.ImmutableSet;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A content rewriter that will sanitize output for simple 'badge' like display.
 *
 * This is intentionally not as robust as Caja. It is a simple element whitelist. It can not be used
 * for sanitizing either javascript or CSS. CSS is desired in the long run, but it can't be proven
 * safe in the short term.
 *
 * Generally used in conjunction with a gadget that gets its dynamic behavior externally (proxied
 * rendering, OSML, etc.)
 */
public class SanitizedRenderingContentRewriter implements ContentRewriter {
  private static final Set<String> URI_ATTRIBUTES = ImmutableSet.of("href", "src");

  private final Set<String> allowedTags;
  private final Set<String> allowedAttributes;

  @Inject
  public SanitizedRenderingContentRewriter(@AllowedTags Set<String> allowedTags,
                                           @AllowedAttributes Set<String> allowedAttributes) {
    this.allowedTags = allowedTags;
    this.allowedAttributes = allowedAttributes;
  }

  public RewriterResults rewrite(HttpRequest request, HttpResponse resp, MutableContent content) {
    return null;
  }

  public RewriterResults rewrite(Gadget gadget, MutableContent content) {
    if ("1".equals(gadget.getContext().getParameter("sanitize"))) {
      sanitize(content.getDocument().getDocumentElement());
      content.documentChanged();
    }

    return RewriterResults.notCacheable();
  }

  private void sanitize(Node node) {
    switch (node.getNodeType()) {
      case Node.CDATA_SECTION_NODE:
      case Node.TEXT_NODE:
      case Node.ENTITY_REFERENCE_NODE:
        break;
      case Node.ELEMENT_NODE:
      case Node.DOCUMENT_NODE:
        Element element = (Element) node;
        if (allowedTags.contains(element.getTagName().toLowerCase())) {
          filterAttributes(element);
          for (Node child : toList(node.getChildNodes())) {
            sanitize(child);
          }
        } else {
          node.getParentNode().removeChild(node);
        }
        break;
      case Node.COMMENT_NODE:
      default:
        // Must remove all comments to avoid conditional comment evaluation.
        // There might be other, unknown types as well. Don't trust them.
        node.getParentNode().removeChild(node);
        break;
    }
  }

  private void filterAttributes(Element element) {

    for (Attr attribute : toList(element.getAttributes())) {
      String name = attribute.getNodeName();
      if (allowedAttributes.contains(name)) {
        if (URI_ATTRIBUTES.contains(name)) {
          try {
            Uri uri = Uri.parse(attribute.getNodeValue());
            String scheme = uri.getScheme();
            if (!isAllowedScheme(scheme)) {
              element.removeAttributeNode(attribute);
            }
          } catch (IllegalArgumentException e) {
            // Not a valid URI.
            element.removeAttributeNode(attribute);
          }
        }
      } else {
        element.removeAttributeNode(attribute);
      }
    }
  }

  /** Convert a NamedNodeMap to a list for easy and safe operations */
  private static List<Attr> toList(NamedNodeMap nodes) {
    List<Attr> list = new ArrayList<Attr>(nodes.getLength());

    for (int i = 0, j = nodes.getLength(); i < j; ++i) {
      list.add((Attr) nodes.item(i));
    }

    return list;
  }

  /** Convert a NamedNodeMap to a list for easy and safe operations */
  private static List<Node> toList(NodeList nodes) {
    List<Node> list = new ArrayList<Node>(nodes.getLength());

    for (int i = 0, j = nodes.getLength(); i < j; ++i) {
      list.add(nodes.item(i));
    }

    return list;
  }

  private static boolean isAllowedScheme(String scheme) {
    return scheme == null || scheme.equals("http") || scheme.equals("https");
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @BindingAnnotation
  public @interface AllowedTags { }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @BindingAnnotation
  public @interface AllowedAttributes { }
}
