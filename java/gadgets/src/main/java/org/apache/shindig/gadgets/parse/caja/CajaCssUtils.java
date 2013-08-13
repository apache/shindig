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
package org.apache.shindig.gadgets.parse.caja;

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Utility functions for traversing Caja's CSS DOM
 */
public final class CajaCssUtils {
  private CajaCssUtils() {}

  /**
   * Get the immediate children of the passed node with the specified node type
   */
  public static <T extends CssTree> List<T> children(CssTree node, Class<T> nodeType) {
    List<T> result = Lists.newArrayList();
    for (CssTree child : node.children()) {
      if (nodeType.isAssignableFrom(child.getClass())) {
        result.add(nodeType.cast(child));
      }
    }
    return result;
  }

  /**
   * Get all descendants of the passed node with the specified node type
   */
  public static <T extends CssTree> List<T> descendants(CssTree node, final Class<T> nodeType) {
    final List<T> descendants = Lists.newArrayList();
    node.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> ancestorChain) {
        if (nodeType.isAssignableFrom(ancestorChain.node.getClass())) {
          descendants.add(nodeType.cast(ancestorChain.node));
        }
        return true;
      }
    }, null);
    return descendants;
  }
}

