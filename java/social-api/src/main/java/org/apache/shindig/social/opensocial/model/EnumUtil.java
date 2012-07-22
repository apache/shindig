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
package org.apache.shindig.social.opensocial.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Utility class for OpenSocial enums.
 */
public final class EnumUtil {

  /**
   * This is a utility class and can't be constructed.
   */
  private EnumUtil() {

  }
  /**
   *
   * @param vals array of enums
   * @return a set of the names for a list of Enum values defined by toString
   */
  // TODO: Because we have a Enum interface in this package we have to explicitly state the java.lang.Enum (bad ?)
  public static Set<String> getEnumStrings(java.lang.Enum<?>... vals) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (java.lang.Enum<?> v : vals) {
      builder.add(v.toString());
    }
    Set<String> result = builder.build();

    Preconditions.checkArgument(result.size() == vals.length, "Enum names are not disjoint set");
    return result;
  }
}
