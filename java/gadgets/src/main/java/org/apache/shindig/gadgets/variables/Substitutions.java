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
package org.apache.shindig.gadgets.variables;

import org.apache.shindig.common.uri.Uri;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Performs string substitutions for message bundles, user prefs, and bidi
 * variables.
 */
public class Substitutions {
  /**
   * Defines all of the valid types of message substitutions.
   *
   * NOTE: Order is critical here, since substitutions are only recursive on nodes with lower order
   * this is to prevent infinite recursion in substitution logic.
   */
  public enum Type {
    /**
     * Localization strings.
     */
    MESSAGE("MSG"),

    /**
     * User preferences.
     */
    USER_PREF("UP"),

    /**
     * MODULE_ variables (i.e. MODULE_ID)
     */
    MODULE("MODULE"),

    /**
     * Bi-directional text transformations.
     */
    BIDI("BIDI");

    private final String prefix;

    /**
     * Creates a Type with the specified prefix.
     *
     * @param prefix
     *        The placeholder prefix for substituted strings.
     */
    Type(String prefix) {
      this.prefix = "__" + prefix + '_';
    }
  }

  private final Map<String, String> substitutions;

  public Substitutions() {
    substitutions = Maps.newHashMap();
  }


  /**
   * Adds a new substitution for the given type.
   *
   * @param type
   * @param key
   * @param value
   */
  public void addSubstitution(Type type, String key, String value) {
    substitutions.put(type.prefix + key, substituteString(value));
  }

  /**
   * @return The value stored for the given type and key, or null.
   */
  public String getSubstitution(Type type, String key) {
    return substitutions.get(type.prefix + key);
  }

  /**
   * Adds many substitutions of the same type at once.
   *
   * @param type
   * @param entries
   */
  public void addSubstitutions(Type type, Map<String, String> entries) {
    for (Map.Entry<String, String> entry : entries.entrySet()) {
      addSubstitution(type, entry.getKey(), entry.getValue());
    }
  }

  private void performSubstitutions(String input, StringBuilder output, boolean isNested) {
    int lastPosition = 0, i;
    while ((i = input.indexOf("__", lastPosition)) != -1) {
      int next = input.indexOf("__", i + 2);
      if (next == -1) {
        // No matches, we're done.
        break;
      }

      output.append(input.substring(lastPosition, i));

      String pattern = input.substring(i, next);

      boolean isMessage = pattern.startsWith(Type.MESSAGE.prefix);
      String replacement;

      if (isMessage && isNested) {
        replacement = pattern + "__";
      } else {
        replacement = substitutions.get(pattern);
      }

      if (replacement == null) {
        // Just append the first underbar of the __ prefix. The substitution
        // selection algorithm will move on to the next underbar, which itself
        // might be a __ prefix suitable for substitution, ensuring proper
        // accommodation of cases such as ___MODULE_ID__.
        output.append('_');
        lastPosition = i + 1;
      } else {
        lastPosition = next + 2;
        if (isMessage && !isNested) {
          // Messages can be recursive
          performSubstitutions(replacement, output, true);
        } else {
          output.append(replacement);
        }
      }
    }

    output.append(input.substring(lastPosition));
  }

  /**
   * Performs string substitution only for the specified type. If no
   * substitution for {@code input} was provided or {@code input} is null,
   * the output is left untouched.
   * @param input The base string, with substitution markers.
   * @return The substituted string.
   */
  public String substituteString(String input) {
    if (input.contains("__")) {
      StringBuilder output = new StringBuilder(input.length() * 120 / 100);
      performSubstitutions(input, output, false);
      return output.toString();
    }
    return input;
  }

  /**
   * Substitutes a uri
   * @param uri
   * @return The substituted uri, or a dummy value if the result is invalid.
   */
  public Uri substituteUri(Uri uri) {
    if (uri == null) {
      return null;
    }
    try {
      return Uri.parse(substituteString(uri.toString()));
    } catch (IllegalArgumentException e) {
      return Uri.parse("");
    }
  }
}
