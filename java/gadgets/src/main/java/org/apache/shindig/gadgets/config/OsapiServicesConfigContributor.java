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
package org.apache.shindig.gadgets.config;

import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.render.RpcServiceLookup;

import java.util.Map;

/**
 * Populates the osapi.services configuration, which includes
 * the osapi endpoints this container supports.
 *
 * TODO osapi.services as a configuration parameter does not
 * match a specific feature.  It would be better to store this as
 * 'osapi:{services: {...}}}
 *
 * @since 2.0.0
 */
@Singleton
public class OsapiServicesConfigContributor implements ConfigContributor {

  protected final RpcServiceLookup rpcServiceLookup;

  @Inject
  public OsapiServicesConfigContributor(RpcServiceLookup rpcServiceLookup) {
    this.rpcServiceLookup = rpcServiceLookup;
  }

  /** {@inheritDoc} */
  public void contribute(Map<String, Object> config, Gadget gadget) {
    GadgetContext ctx = gadget.getContext();
    addServicesConfig(config, ctx.getContainer(), ctx.getHost());
  }

  /** {@inheritDoc} */
  public void contribute(Map<String,Object> config, String container, String host) {
    addServicesConfig(config, container, host);
  }

  /**
   * Add osapi.services to the config
   * @param config config map to add it to.
   * @param container container to use to add osapi.services.
   * @param host hostname to query from.
   */
  private void addServicesConfig(Map<String,Object> config, String container, String host) {
    if (rpcServiceLookup != null) {
      Multimap<String, String> endpoints = rpcServiceLookup.getServicesFor(container, host);
      config.put("osapi.services", endpoints);
    }
  }
}
