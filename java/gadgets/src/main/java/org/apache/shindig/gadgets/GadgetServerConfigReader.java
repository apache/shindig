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
package org.apache.shindig.gadgets;

import java.util.concurrent.Executor;

/**
 * Stores configuration data for a GadgetServer.
 *
 * The main purpose of this class is to allow for better readability of
 * GadgetServer ctor parameters and to simplify interacting with its components.
 *
 * Works in conjunction with {@code GadgetServerConfig} to create immutable
 * configuration objects. Note that members are still potentially mutable.
 *
 */
public class GadgetServerConfigReader  {

  protected Executor executor;

  public Executor getExecutor() {
    return executor;
  }

  protected GadgetFeatureRegistry featureRegistry;

  public GadgetFeatureRegistry getFeatureRegistry() {
    return featureRegistry;
  }

  protected GadgetDataCache<GadgetSpec> specCache;

  public GadgetDataCache<GadgetSpec> getSpecCache() {
    return specCache;
  }

  protected GadgetDataCache<MessageBundle> messageBundleCache;

  public GadgetDataCache<MessageBundle> getMessageBundleCache() {
    return messageBundleCache;
  }

  protected RemoteContentFetcher contentFetcher;

  public RemoteContentFetcher getContentFetcher() {
    return contentFetcher;
  }

  protected GadgetBlacklist gadgetBlacklist;

  public GadgetBlacklist getGadgetBlacklist() {
    return gadgetBlacklist;
  }

  protected SyndicatorConfig syndicatorConfig;

  public SyndicatorConfig getSyndicatorConfig() {
    if (syndicatorConfig == null) {
      return SyndicatorConfig.EMPTY;
    }
    return syndicatorConfig;
  }

  /**
   * Copies all fields from {@code base} into this instance.
   * @param base
   */
  public void copyFrom(GadgetServerConfigReader base) {
    // We use the getters here just in case any methods were overridden.
    executor = base.getExecutor();
    featureRegistry = base.getFeatureRegistry();
    contentFetcher = base.getContentFetcher();
    specCache = base.getSpecCache();
    messageBundleCache = base.getMessageBundleCache();
    gadgetBlacklist = base.getGadgetBlacklist();
    syndicatorConfig = base.getSyndicatorConfig();
  }
}
