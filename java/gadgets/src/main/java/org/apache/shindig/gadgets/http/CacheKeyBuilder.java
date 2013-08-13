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
package org.apache.shindig.gadgets.http;

import java.util.Map;
import java.util.SortedMap;

import com.google.common.collect.Maps;

/**
 * Builds the cache key object.
 *
 * <p>Takes extra care to build the cache keys that don't thrash persistent caches.
 */
public class CacheKeyBuilder {
  private static final int NUM_LEGACY_PARAMS = 9;
  private static final String DEFAULT_KEY_VALUE = "0";
  private static final char KEY_SEPARATOR = ':';

  /** The legacy parameters that need to appear in the cache key in a particular order. */
  private Object[] legacyParams;

  /** A sorted parameter map ensures an unique ordering of the hash keys */
  private SortedMap<String, Object> paramMap;

  public CacheKeyBuilder() {
    this.paramMap = Maps.newTreeMap();
    this.legacyParams = new Object[NUM_LEGACY_PARAMS];
  }

  private String getValueOrDefault(Object value) {
    if (value == null) {
      return DEFAULT_KEY_VALUE;
    }
    return String.valueOf(value);
  }

  /**
   * Sets a legacy cache key parameter.
   *
   * <p>The legacy cache key parameters have a fixed order.  Since the cache key is not required
   * to have all of the legacy parameters filled in, the index of the parameter must be given.
   *
   * @param index the index to place this parameter at; valid values are {@code 0} to
   *        {@link CacheKeyBuilder#NUM_LEGACY_PARAMS - 1}
   * @param value the object that determines the legacy parameter
   */
  public CacheKeyBuilder setLegacyParam(int index, Object value) {
    legacyParams[index] = value;
    return this;
  }

  /**
   * Sets a cache key parameter.
   *
   * <p>Each parameter needs to be inserted in the cache key builder manually, so that
   * the user has an option to select the parameters that need to become part of the key.
   *
   * @param name the parameter name; if <code>null</code>, the param will not be inserted
   * @param value the object that determines the value of the parameter
   */
  public CacheKeyBuilder setParam(String name, Object value) {
    if (value != null) {
      paramMap.put(name, String.valueOf(value));
    }
    return this;
  }

  /**
   * Inserting the keys from the parameter map only if the keys are defined, prevents thrashing
   * the existing persistent caches at rolling restart of high-traffic servers.
   * The cache keys of all URIs that don't set these parameters must not change.
   *
   * TODO: String parameters that have KEY_SEPARATOR appearing as a part of a string param need
   * to be escaped (e.g. with a backslash like so: ":" -> "\:") to prevent weird cache key
   * collisions.  Moreover the active character (backslash) needs to be escaped itself for similar
   * reasons.
   */
  public String build() {
    StringBuilder keyBuilder = new StringBuilder(2 * String.valueOf(legacyParams[0]).length());
    appendLegacyKeys(keyBuilder);

    if (!paramMap.isEmpty()) {
      for (Map.Entry<String, Object> mapEntry : paramMap.entrySet()) {
        keyBuilder.append(KEY_SEPARATOR);
        keyBuilder.append(String.format("%s=%s", mapEntry.getKey(), mapEntry.getValue()));
      }
    }
    return keyBuilder.toString();
  }

  private void appendLegacyKeys(StringBuilder key) {
    boolean first = true;
    for (Object legacyParam : legacyParams) {
      if (!first) {
        key.append(KEY_SEPARATOR);
      } else {
        first = false;
      }
      key.append(getValueOrDefault(legacyParam));
    }
  }
}
