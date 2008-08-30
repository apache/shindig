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

import org.apache.shindig.gadgets.preload.Preloads;
import org.apache.shindig.gadgets.spec.GadgetSpec;

/**
 * Intermediary representation of all state associated with processing
 * of a single gadget request.
 *
 * TODO: Fully replace the Gadget class with ProcessedGadget.
 */
public class ProcessedGadget {
  private GadgetContext context;
  private GadgetSpec spec;
  private Preloads preloads;

  /**
   * @param context The request that the gadget is being processed for.
   */
  public ProcessedGadget setContext(GadgetContext context) {
    this.context = context;
    return this;
  }

  public GadgetContext getContext() {
    return context;
  }

  /**
   * @param spec The spec for the gadget that is being processed.
   */
  public ProcessedGadget setSpec(GadgetSpec spec) {
    this.spec = spec;
    return this;
  }

  public GadgetSpec getSpec() {
    return spec;
  }

  /**
   * @param preloads The preloads for the gadget that is being processed.
   */
  public ProcessedGadget setPreloads(Preloads preloads) {
    this.preloads = preloads;
    return this;
  }

  public Preloads getPreloads() {
    return preloads;
  }
}