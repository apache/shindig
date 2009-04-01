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
package org.apache.shindig.gadgets.preload;

import org.apache.shindig.gadgets.Gadget;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Performs an individual preloading operation.
 */
public interface Preloader {
  /**
   * Create new preload tasks for the provided gadget.
   * TODO: instead of requiring each Preloader to check the preload phase,
   * register a different list of preloaders for each phase.  Guice
   * MultimapBinder would come in handy here.
   *
   * @param gadget The gadget that the operations will be performed for.
   * @param phase The preload phase being executed.
   * @return Preloading tasks that will be executed by
   *  {@link PreloaderService#}.
   */
  Collection<Callable<PreloadedData>> createPreloadTasks(Gadget gadget,
      PreloaderService.PreloadPhase phase);
}
