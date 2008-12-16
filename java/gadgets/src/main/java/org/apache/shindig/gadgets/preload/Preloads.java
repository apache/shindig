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
package org.apache.shindig.gadgets.preload;

import java.util.Set;

/**
 * Container that holds preload operations, which are actually contained as futures.
 */
public interface Preloads {
  /**
   * @return Keys for all preloaded data.
   */
  Set<String> getKeys();

  /**
   * Retrieve a single preload.
   * 
   * @param key The key that the preload is stored under.
   * @return The preloaded data, or null if there is no preload under the specified key (including
   * failure to preload).
   * @throws PreloadException If there was any issue while preloading.
   */
  PreloadedData getData(String key) throws PreloadException;
}
