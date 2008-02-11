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
package org.apache.shindig.gadgets;

import java.util.HashMap;
import java.util.Map;
import java.util.EnumMap;

/**
 * Performs string substitutions for message bundles, user prefs, and bidi
 * variables.
 *
 * @author etnu
 */
public class Substitutions {

  /**
   * Defines all of the valid types of message substitutions.
   * Note: Order is important here, since the order of the enum is the
   * order of evaluation. Don't change this unless you know what you're doing.
   */
  public enum Type {
    /**
     * Localization strings.
     */
    MESSAGE("MSG"),

    /**
     * Bi-directional text transformations.
     */
    BIDI("BIDI"),

    /**
     * User preferences.
     */
    USER_PREF("UP"),

    /**
     * MODULE_ variables (i.e. MODULE_ID)
     */
    MODULE("MODULE");

    private String prefix;

    /**
     * Creates a Type with the specified prefix.
     *
     * @param prefix
     *        The placeholder prefix for substituted strings.
     */
    Type(String prefix) {
      this.prefix = "__" + prefix + '_';
    }

    public String getPrefix() {
      return prefix;
    }
  }

  private Map<Type, Map<String, String>> substitutions =
      new EnumMap<Type, Map<String, String>>(Type.class);

  /**
   * Create a basic substitution coordinator.
   */
  public Substitutions() {
    for (Type type : Type.values()) {
      substitutions.put(type, new HashMap<String, String>());
    }
  }

  /**
   * Adds a new substitution for the given type.
   *
   * @param type
   * @param key
   * @param value
   */
  public void addSubstitution(Type type, String key, String value) {
    substitutions.get(type).put(key, value);
  }

  /**
   * Adds many substitutions of the same type at once.
   *
   * @param type
   * @param entries
   */
  public void addSubstitutions(Type type, Map<String, String> entries) {
    substitutions.get(type).putAll(entries);
  }

  /**
   * Substitutes all substitutions into the given string. The order of
   * substitutions is the same as defined for Type.
   *
   * @param input
   *        The base string, with substitution markers.
   * @return The substituted string or null if {@code input} is null.
   */
  public String substitute(String input) {
    if (input != null) {
      for (Type type : Type.values()) {
        input = substituteType(type, input);
      }
    }
    return input;
  }

  /**
   * Performs string substitution only for the specified type. If no
   * substitution for {@code input} was provided or {@code input} is null,
   * the output is left untouched.
   *
   * @param type
   *        The type you wish to perform substitutions for.
   * @param input
   *        The base string, with substitution markers.
   * @return The substituted string.
   */
  public String substituteType(Type type, String input) {
    if (input == null || substitutions.get(type).size() == 0 ||
        !input.contains(type.prefix)) {
      return input;
    }

    StringBuilder output = new StringBuilder();
    for (int i = 0, j = input.length(); i < j; ++i) {
      if (input.regionMatches(i, type.prefix, 0, type.prefix.length())) {
        // Look for a trailing "__". If we don't find it, then this isn't a
        // properly formed substitution.
        int start = i + type.prefix.length();
        int end = input.indexOf("__", start);
        if (end != -1) {
          String name = input.substring(start, end);
          String replacement = substitutions.get(type).get(name);
          if (replacement != null) {
            output.append(replacement);
          } else {
            output.append(input.substring(i, end + 2));
          }
          i = end + 1;
        } else {
          // If we didn't find any occurances of "__", then the rest of the
          // string can't contain any more valid translations.
          output.append(input.substring(i));
          break;
        }
      } else {
        output.append(input.charAt(i));
      }
    }

    return output.toString();
  }
}
