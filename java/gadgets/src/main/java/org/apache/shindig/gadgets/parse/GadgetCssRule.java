/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.parse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mutable wrapper around a {@code ParsedCssRule}.
 * Used by rewriting to manipulate parsed gadget CSS, and
 * to separate parsing from manipulation code.
 */
public class GadgetCssRule {
  private final List<String> selectors;
  private final Map<String, String> declarations;

  /**
   * Create a new {@code GadgetCssRule} from a {@code ParsedCssRule}
   * @param source Parsed CSS rule
   */
  public GadgetCssRule(ParsedCssRule source) {
    this();
    for (String selector : source.getSelectors()) {
      addSelector(selector, null);
    }

    // Last decl with a given key "wins" - duplicates are therefore ignored.
    for (ParsedCssDeclaration decl : source.getDeclarations()) {
      setDeclaration(decl.getName(), decl.getValue());
    }
  }

  /**
   * Create a new, blank rule. At least one selector must be added
   * for the rule to be valid (and serializable).
   */
  public GadgetCssRule() {
    selectors = Lists.newLinkedList();
    declarations = Maps.newHashMap();
  }

  /**
   * Adds a new selector after the provided entry. Selector order in a given
   * rule is significant in CSS.
   * @param selector Selector to add (will be automatically trimmed)
   * @param before Selector key after which to add new, null for list end
   * @return Whether or not the selector was freshly added
   */
  public boolean addSelector(String selector, String before) {
    selector = selector.trim();
    int selIx = selectors.indexOf(selector);
    if (selIx >= 0) {
      return false;
    }
    if (before == null) {
      return selectors.add(selector);
    }
    int befIx = selectors.indexOf(before);
    if (befIx >= 0) {
      selectors.add(befIx, selector);
    } else {
      selectors.add(selector);
    }
    return true;
  }

  /**
   * @param selector Selector to remove
   * @return Whether or not the selector was present and removed
   */
  public boolean removeSelector(String selector) {
    return selectors.remove(selector);
  }

  /**
   * @param selector Selector whose presence in the rule to test
   * @return Whether or not the selector exists in the rule
   */
  public boolean hasSelector(String selector) {
    return selectors.contains(selector);
  }

  /**
   * @return Unmodifiable list of selectors
   */
  public List<String> getSelectors() {
    return Collections.unmodifiableList(selectors);
  }

  /**
   * Add a declaration by key/value. Key is trimmed.
   * @param key Declaration key, either new or replaced
   * @param value Declaration value, either new or replaced
   */
  public void setDeclaration(String key, String value) {
    key = key.trim();
    declarations.put(key, value);
  }

  /**
   * @param key Key for the declaration to remove.
   * @return Whether or not the declaration existed and was removed
   */
  public boolean removeDeclaration(String key) {
    key = key.trim();
    return declarations.remove(key) != null;
  }

  /**
   * Get a given declaration's value by key.
   * @param key Key for the declaration
   * @return Declaration's value, or null if not present
   */
  public String getDeclarationValue(String key) {
    key = key.trim();
    return declarations.get(key);
  }

  /**
   * @return Unmodifiable set of existing declaration keys.
   */
  public Set<String> getDeclarationKeys() {
    return Collections.unmodifiableSet(declarations.keySet());
  }
}
