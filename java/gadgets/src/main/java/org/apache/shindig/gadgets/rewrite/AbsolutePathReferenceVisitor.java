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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.rewrite.DomWalker.Visitor;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Visitor that resolves relative paths relative to the
 * base tag (only if present) / current page url and marks urls as absolute.
 *
 * @since 2.0.0
 */
public class AbsolutePathReferenceVisitor implements Visitor {
  public enum Tags {
    // Resources which would be fetched by the browser when rendering the page.
    //TODO: Document the second parameter for clarity
    // Does it make sense to factor this out into shindig properties?
    RESOURCES(ImmutableMap.<String, String>builder()
        .put("body", "background")
        .put("img", "src")
        .put("input", "src")
        .put("link", "href")
        .put("embed", "src")
        .put("script", "src").build()),

    // Hyperlinks that the user clicks on to navigate pages.
    HYPERLINKS(ImmutableMap.<String, String>builder()
        .put("a", "href")
        .put("area", "href")
        .put("q", "cite").build());

    Map<String, String> resourceTags;
    private Tags(Map<String, String> resourceTags) {
      this.resourceTags = resourceTags;
    }

    public Map<String, String> getResourceTags() {
      return resourceTags;
    }
  }

  // Map of tag name -> attribute type describing uris to make absolute.
  private final Map<String, String> tagsToMakeAbsolute;

  // The base Uri used to absolutify relative uris in the document being visited.
  private Uri baseUri;

  @Inject
  public AbsolutePathReferenceVisitor(Tags... resourceTags) {
    Map<String, String> tagsToMakeAbsolute = new HashMap<String, String>();
    for (Tags r : resourceTags) {
      tagsToMakeAbsolute.putAll(r.getResourceTags());
    }

    this.tagsToMakeAbsolute = tagsToMakeAbsolute;
  }

  // @Override
  public VisitStatus visit(Gadget gadget, Node node) throws RewritingException {
    Attr nodeAttr = getUriAttributeFromNode(node, tagsToMakeAbsolute);

    if (nodeAttr != null) {
      try {
        Uri nodeUri = Uri.parse(nodeAttr.getValue());
        Uri baseUri = getBaseResolutionUri(gadget, node);

        Uri resolved = baseUri.resolve(nodeUri);

        if (!resolved.equals(nodeUri)) {
          nodeAttr.setValue(resolved.toString());
          return VisitStatus.MODIFY;
        }
      } catch (Uri.UriException e) {
        // UriException on illegal input. Ignore.
      }
    }
    return VisitStatus.BYPASS;
  }

  // @Override
  public boolean revisit(Gadget gadget, List<Node> node) throws RewritingException {
    // Modification happens immediately.
    return false;
  }

  /**
   * Returns the uri attribute for the given node by looking up the
   * tag name -> uri attribute map.
   * NOTE: This function returns the node attribute only if the attribute has a
   * non empty value.
   * @param node The node to get uri attribute for.
   * @param resourceTags Map from tag name -> uri attribute name.
   * @return Uri attribute for the node.
   */
  public static Attr getUriAttributeFromNode(Node node, Map<String, String> resourceTags) {
    String nodeName = node.getNodeName().toLowerCase();
    if (node.getNodeType() == Node.ELEMENT_NODE &&
        resourceTags.containsKey(nodeName)) {
      if ("link".equals(nodeName)) {
        // Rewrite link only when it is for css.
        String type = ((Element)node).getAttribute("type");
        String rel = ((Element)node).getAttribute("rel");
        if (!"stylesheet".equalsIgnoreCase(rel) || !"text/css".equalsIgnoreCase(type)) {
          return null;
        }
      }
      Attr attr = (Attr) node.getAttributes().getNamedItem(resourceTags.get(nodeName));
      String nodeUri = attr != null ? attr.getValue() : null;
      if (!Strings.isNullOrEmpty(nodeUri)) {
        return attr;
      }
    }

    return null;
  }

  /**
   * Returns the uri to resolve any relative url on the current page to.
   * This is equal to the base uri (in case the page has one) or the current
   * page uri.
   * @param gadget The gadget (container for page) being processed.
   * @param node The current node being processed.
   * @return The uri to resolve non absolute uri's relative to.
   */
  private Uri getBaseResolutionUri(Gadget gadget, Node node) {
    if (baseUri == null) {
      Uri pageUri = gadget.getSpec().getUrl();
      Uri baseTagUri = getBaseUri(node.getOwnerDocument());
      baseUri = baseTagUri != null ? baseTagUri : pageUri;
    }
    return baseUri;
  }

  /**
   * Returns the base uri of the given document.
   * Base uri is specified as &lt;base href="..."&gt;
   * @param doc The document.
   * @return Base uri of the document.
   */
  @VisibleForTesting
  Uri getBaseUri(Document doc) {
    String baseHref = getBaseHref(doc);
    if (baseHref != null) {
      try {
        return Uri.parse(baseHref);
      } catch (Uri.UriException e) {
        // Ignore.
      }
    }

    return null;
  }

  /**
   * Returns href value of the base tag.
   * @param doc The document to process.
   * @return Value of href attribute of the base tag.
   */
  @VisibleForTesting
  String getBaseHref(Document doc) {
    NodeList list = doc.getElementsByTagName("base");
    if (list.getLength() == 0) {
      return null;
    }

    NamedNodeMap nodeMap = list.item(0).getAttributes();
    if (nodeMap == null) {
      return null;
    }
    Attr attr = (Attr) nodeMap.getNamedItem("href");
    return attr != null ? attr.getValue() : null;
  }
}
