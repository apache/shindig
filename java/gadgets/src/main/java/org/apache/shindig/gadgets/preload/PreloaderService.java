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

import org.apache.shindig.gadgets.Gadget;

import java.util.Collection;
import java.util.concurrent.Callable;

import com.google.inject.ImplementedBy;

/**
 * Handles preloading operations, such as HTTP fetches, social data retrieval, or anything else that
 * would benefit from preloading on the server instead of incurring a network request for users.
 */
@ImplementedBy(ConcurrentPreloaderService.class)
public interface PreloaderService {
  /**
   * Begin all preload operations.
   *
   * @param gadget The gadget that the operations will be performed for.
   * @return The preloads for the gadget.
   */
  Collection<PreloadedData> preload(Gadget gadget);

  /**
   * Execute preloads with a specific set of preload tasks.
   */
  Collection<PreloadedData> preload(Collection<Callable<PreloadedData>> tasks);
}
