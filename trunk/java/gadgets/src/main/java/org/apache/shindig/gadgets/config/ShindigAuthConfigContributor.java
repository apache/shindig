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

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;

import java.util.Map;

@Singleton
/**
 * Injects auth configuration information for gadgets.config
 *
 * @since 2.0.0
 */
public class ShindigAuthConfigContributor implements ConfigContributor {

  private SecurityTokenCodec securityTokenCodec;

  @Inject
  public ShindigAuthConfigContributor(SecurityTokenCodec codec) {
    this.securityTokenCodec = codec;
  }

  /** {@inheritDoc} */
  public void contribute(Map<String,Object> config, Gadget gadget) {
    final GadgetContext context = gadget.getContext();
    final SecurityToken authToken = context.getToken();
    if (authToken != null) {
      Map<String, String> authConfig = Maps.newHashMapWithExpectedSize(2);
      String updatedToken = authToken.getUpdatedToken();
      if (updatedToken != null) {
        authConfig.put("authToken", updatedToken);
      }
      String trustedJson = authToken.getTrustedJson();
      if (trustedJson != null) {
        authConfig.put("trustedJson", trustedJson);
      }
      config.put("shindig.auth", authConfig);
    }
  }

  /** {@inheritDoc} */
  public void contribute(Map<String,Object> config, String container, String host) {
    // TODO: This currently will throw an exception when using BlobCrypterSecurityTokens...
    // It seems to be ok for now, we need some good token cleanup.
    SecurityToken containerToken = new AnonymousSecurityToken(container, 0L, "*");
    Map<String, String> authConfig = Maps.newHashMapWithExpectedSize(2);

    try {
      config.put("shindig.auth", authConfig);
      authConfig.put("authToken", securityTokenCodec.encodeToken(containerToken));

    } catch (SecurityTokenException e) {
      // ignore
    }
  }
}
