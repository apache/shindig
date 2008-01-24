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
 * GadgetServer ctor parameters.
 *
 * Usage:
 *
 * <code>
 * GadgetServer server = new GadgetServer(new GadgetServerConfig()
 *      .setRemoteContentFetcher(new BasicRemoteContentFetcher())
 *      .setGadgetCache(new BasicGadgetDataCache<GadgetSpec>())
 *      .setOtherProperties(...));
 * </code>
 *
 * Any missing data will result in GadgetServer throwing an IllegalArgsException
 * unless noted as "optional" here.
 */
public class GadgetServerConfig {

  private Executor executor;

  public Executor getExecutor() {
    return executor;
  }

  public GadgetServerConfig setExecutor(Executor executor) {
    this.executor = executor;
    return this;
  }

  private GadgetFeatureRegistry featureRegistry;

  public GadgetFeatureRegistry getFeatureRegistry() {
    return featureRegistry;
  }

  public GadgetServerConfig setFeatureRegistry(GadgetFeatureRegistry featureRegistry) {
    this.featureRegistry = featureRegistry;
    return this;
  }

  private GadgetDataCache<GadgetSpec> specCache;

  public GadgetDataCache<GadgetSpec> getSpecCache() {
    return specCache;
  }

  public GadgetServerConfig setSpecCache(GadgetDataCache<GadgetSpec> specCache) {
    this.specCache = specCache;
    return this;
  }

  private GadgetDataCache<MessageBundle> messageBundleCache;

  public GadgetDataCache<MessageBundle> getMessageBundleCache() {
    return messageBundleCache;
  }

  public GadgetServerConfig setMessageBundleCache(GadgetDataCache<MessageBundle> mbCache) {
    messageBundleCache = mbCache;
    return this;
  }

  private RemoteContentFetcher contentFetcher;

  public RemoteContentFetcher getContentFetcher() {
    return contentFetcher;
  }

  public GadgetServerConfig setContentFetcher(RemoteContentFetcher contentFetcher) {
    this.contentFetcher = contentFetcher;
    return this;
  }

  // Optional
  private GadgetBlacklist gadgetBlacklist;

  public GadgetBlacklist getGadgetBlacklist() {
    return gadgetBlacklist;
  }

  public GadgetServerConfig setGadgetBlacklist(GadgetBlacklist gadgetBlacklist) {
    this.gadgetBlacklist = gadgetBlacklist;
    return this;
  }
}
