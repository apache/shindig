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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.shindig.common.Pair;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Abstract visitor that walks over html tags as specified by
 * {@code resourceTags} and prepares list of html tag nodes whose uri
 * attributes can be mutated.
 * Implementations can override {@link #mutateUris} for uses cases like
 * proxying resources, making url's absolute, prefetching images etc.
 *
 * TODO: Refactor AbsolutePathReferenceVisitor to extend ResourceMutateVisitor.
 *
 * @since 2.0.0
 */
public abstract class ResourceMutateVisitor implements DomWalker.Visitor {
  /**
   * Enum for resource tags and associated attributes that should be mutated.
   */
  public enum Tags {
    // Javascript resources requested by the current page.
    SCRIPT(ImmutableMap.of("script", "src")),

    // Css stylesheet resources requested by the current page.
    STYLESHEET(ImmutableMap.of("link", "href")),

    // Other embedded resources requested on the same page.
    EMBEDDED_IMAGES(ImmutableMap.of("body", "background",
                                    "img", "src",
                                    "input", "src")),

    // All resources that possibly be rewritten. Useful for testing.
    ALL_RESOURCES(ImmutableMap.<String, String>builder()
        .putAll(SCRIPT.getResourceTags())
        .putAll(STYLESHEET.getResourceTags())
        .putAll(EMBEDDED_IMAGES.getResourceTags())
        .build());

    private Map<String, String> resourceTags;
    private Tags(Map<String, String> resourceTags) {
      this.resourceTags = resourceTags;
    }

    public Map<String, String> getResourceTags() {
      return resourceTags;
    }
  }

  // Map of tag name to attribute of resources to rewrite.
  protected final Map<String, String> resourceTags;
  protected final ContentRewriterFeature.Config featureConfig;

  public ResourceMutateVisitor(ContentRewriterFeature.Config featureConfig,
                               Tags... resourceTags) {
    this.featureConfig = featureConfig;

    Map<String, String> rTags = Maps.newHashMap();
    for (Tags r : resourceTags) {
      rTags.putAll(r.getResourceTags());
    }
    this.resourceTags = ImmutableMap.<String, String>builder().putAll(rTags).build();
  }

  /**
   * {@inheritDoc}
   */
  public VisitStatus visit(Gadget gadget, Node node) throws RewritingException {
    String nodeName = node.getNodeName().toLowerCase();
    if (node.getNodeType() == Node.ELEMENT_NODE &&
        resourceTags.containsKey(nodeName) &&
        featureConfig.shouldRewriteTag(nodeName)) {
      if ("link".equals(nodeName)) {
        // Rewrite link only when it is for css.
        String type = ((Element)node).getAttribute("type");
        String rel = ((Element)node).getAttribute("rel");
        if (!"stylesheet".equalsIgnoreCase(rel) || !"text/css".equalsIgnoreCase(type)) {
          return VisitStatus.BYPASS;
        }
      }

      Attr attr = (Attr) node.getAttributes().getNamedItem(
          resourceTags.get(nodeName));
      if (attr != null) {
        String urlValue = attr.getValue();
        if (!Strings.isNullOrEmpty(urlValue) && featureConfig.shouldRewriteURL(urlValue)) {
          return VisitStatus.RESERVE_NODE;
        }
      }
    }
    return VisitStatus.BYPASS;
  }

  /**
   * {@inheritDoc}
   */
  public boolean revisit(Gadget gadget, List<Node> nodes) throws RewritingException {
    Collection<Pair<Node, Uri>> proxiedUris = mutateUris(gadget, nodes);

    boolean mutated = false;
    for (Pair<Node, Uri> proxyPair : proxiedUris) {
      if (proxyPair.two == null) {
        continue;
      }
      Element element = (Element) proxyPair.one;
      String nodeName = element.getNodeName().toLowerCase();
      element.setAttribute(resourceTags.get(nodeName), proxyPair.two.toString());
      mutated = true;
    }

    return mutated;
  }

  // Mutate the list of nodes reserved by revisit().
  protected abstract Collection<Pair<Node, Uri>> mutateUris(Gadget gadget, Collection<Node> nodes);
}
