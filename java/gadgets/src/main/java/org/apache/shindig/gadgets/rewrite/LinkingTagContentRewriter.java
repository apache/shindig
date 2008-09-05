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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.GadgetHtmlNode;

public class LinkingTagContentRewriter implements ContentRewriter {
  private final LinkRewriter linkRewriter;
  private final Map<String, Set<String>> tagAttributeTargets;
  
  public LinkingTagContentRewriter(LinkRewriter linkRewriter,
      Map<String, Set<String>> attributeTargets) {
    this.linkRewriter = linkRewriter;
    if (attributeTargets != null) {
      this.tagAttributeTargets = attributeTargets;
    } else {
      this.tagAttributeTargets = getDefaultTargets();
    }
  }

  public HttpResponse rewrite(HttpRequest request, HttpResponse original) {
		// TODO Auto-generated method stub
		return null;
	}

	public void rewrite(Gadget gadget) {
	  if (linkRewriter == null) {
	    // Sanity test.
	    return;
	  }
	  
    Queue<GadgetHtmlNode> nodesToProcess =
      new LinkedList<GadgetHtmlNode>();
    GadgetHtmlNode root = gadget.getParseTree();
    if (root == null) {
      return;
    }
    
    nodesToProcess.addAll(root.getChildren());
  
    while (!nodesToProcess.isEmpty()) {
      GadgetHtmlNode curNode = nodesToProcess.remove();
      if (!curNode.isText()) {
        // Depth-first iteration over children. Order doesn't matter anyway.
        nodesToProcess.addAll(curNode.getChildren());
        
        Set<String> curTagAttrs =
            tagAttributeTargets.get(curNode.getTagName().toLowerCase());
        if (curTagAttrs != null) {
          for (String attrKey : curNode.getAttributeKeys()) {
            if (curTagAttrs.contains(attrKey.toLowerCase())) {
              String attrValue = curNode.getAttributeValue(attrKey);
            
              // Attribute marked for rewriting: do it!
              curNode.setAttribute(attrKey,
                  linkRewriter.rewrite(attrValue, gadget.getSpec().getUrl()));
            }
          }
        }
      }
    }
	}

  private static Map<String, Set<String>> getDefaultTargets() {
    Map<String, Set<String>> targets  = new HashMap<String, Set<String>>();
    targets.put("img", new HashSet<String>(Arrays.asList("src")));
    targets.put("embed", new HashSet<String>(Arrays.asList("src")));
    targets.put("link", new HashSet<String>(Arrays.asList("href")));
    return targets;
  }

}
