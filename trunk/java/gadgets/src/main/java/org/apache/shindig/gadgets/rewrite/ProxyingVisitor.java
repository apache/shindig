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

import com.google.common.collect.Lists;
import org.apache.shindig.common.Pair;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.Uri.UriException;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple visitor that, when plugged into a DomWalker, rewrites
 * resource links to proxied versions of the same.
 *
 * @since 2.0.0
 */
public class ProxyingVisitor extends ResourceMutateVisitor {
  //class name for logging purpose
  private static final String classname = ProxyingVisitor.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  private final ProxyUriManager uriManager;

  public ProxyingVisitor(ContentRewriterFeature.Config featureConfig,
                         ProxyUriManager uriManager,
                         Tags... resourceTags) {
    super(featureConfig, resourceTags);
    this.uriManager = uriManager;
  }

  @Override
  protected Collection<Pair<Node, Uri>> mutateUris(Gadget gadget, Collection<Node> nodes) {
    List<ProxyUriManager.ProxyUri> reservedUris =
        Lists.newArrayListWithCapacity(nodes.size());
    List<Node> reservedNodes = Lists.newArrayListWithCapacity(nodes.size());

    for (Node node : nodes) {
      Element element = (Element)node;
      String nodeName = node.getNodeName().toLowerCase();
      String uriStr = element.getAttribute(resourceTags.get(nodeName)).trim();
      try {
        ProxyUriManager.ProxyUri proxiedUri = new ProxyUriManager.ProxyUri(
            gadget, Uri.parse(uriStr));

        // Set the html tag context as the current node being processed.
        proxiedUri.setHtmlTagContext(nodeName);
        reservedUris.add(proxiedUri);
        reservedNodes.add(node);
      } catch (UriException e) {
        // Uri parse exception, ignore.
        if (LOG.isLoggable(Level.WARNING)) {
          LOG.logp(Level.WARNING, classname, "mutateUris", MessageKeys.URI_EXCEPTION_PARSING, new Object[] {uriStr});
          LOG.log(Level.WARNING, e.getMessage(), e);
        }
      }
    }

    List<Uri> resourceUris = uriManager.make(reservedUris, featureConfig.getExpires());

    // By contract, resourceUris matches by index with inbound Uris. Create an easy-access
    // List with the results.
    List<Pair<Node, Uri>> proxiedUris = Lists.newArrayListWithCapacity(nodes.size());

    Iterator<Uri> uriIt = resourceUris.iterator();
    for (Node node : reservedNodes) {
      proxiedUris.add(Pair.of(node, uriIt.next()));
    }

    return proxiedUris;
  }
}
