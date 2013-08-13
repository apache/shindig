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
import com.google.inject.Singleton;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.oauth2.OAuth2Arguments;
import org.apache.shindig.gadgets.spec.View;

import java.util.Map;

/**
 * Provides config support for the xhrwrapper feature.
 *
 * @since 2.0.0
 */

@Singleton
public class XhrwrapperConfigContributor implements ConfigContributor {
  /** {@inheritDoc} */
  public void contribute(Map<String, Object> config, Gadget gadget) {
    Map<String, String> xhrWrapperConfig = Maps.newHashMapWithExpectedSize(2);
    View view = gadget.getCurrentView();
    Uri contentsUri = view.getHref();
    xhrWrapperConfig.put("contentUrl", contentsUri == null ? "" : contentsUri.toString());
    if (AuthType.OAUTH.equals(view.getAuthType())) {
      addOAuthConfig(xhrWrapperConfig, view);
    } else if (AuthType.SIGNED.equals(view.getAuthType())) {
      xhrWrapperConfig.put("authorization", "signed");
    } else if (AuthType.OAUTH2.equals(view.getAuthType())) {
      addOAuth2Config(xhrWrapperConfig, view);
    }
    config.put("shindig.xhrwrapper", xhrWrapperConfig);
  }

  /** {@inheritDoc} */
  private void addOAuthConfig(Map<String, String> xhrWrapperConfig, View view) {
    Map<String, String> oAuthConfig = Maps.newHashMapWithExpectedSize(3);
    try {
      OAuthArguments oAuthArguments = new OAuthArguments(view);
      oAuthConfig.put("authorization", "oauth");
      oAuthConfig.put("oauthService", oAuthArguments.getServiceName());
      if (!"".equals(oAuthArguments.getTokenName())) {
        oAuthConfig.put("oauthTokenName", oAuthArguments.getTokenName());
      }
      xhrWrapperConfig.putAll(oAuthConfig);
    } catch (GadgetException e) {
      // Do not add any OAuth configuration if an exception was thrown
    }
  }

  private void addOAuth2Config(Map<String, String> xhrWrapperConfig, View view) {
    Map<String, String> oAuth2Config = Maps.newHashMapWithExpectedSize(3);
    OAuth2Arguments oAuth2Arguments = new OAuth2Arguments(view);
    oAuth2Config.put("authorization", "oauth2");
    oAuth2Config.put("oauthService", oAuth2Arguments.getServiceName());
    xhrWrapperConfig.putAll(oAuth2Config);
  }

  public void contribute(Map<String, Object> config, String container, String host) {
    // no-op, no container specific configuration
  }
}
