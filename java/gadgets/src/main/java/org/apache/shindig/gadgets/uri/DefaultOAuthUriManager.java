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
package org.apache.shindig.gadgets.uri;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;

import com.google.inject.Inject;

/**
 * Straightforward implemenation of an OAuth callback Uri generator.
 */
public class DefaultOAuthUriManager implements OAuthUriManager {
  static final String OAUTH_GADGET_CALLBACK_URI_PARAM =
      "gadgets.uri.oauth.callbackTemplate";

  private final ContainerConfig config;

  @Inject
  public DefaultOAuthUriManager(ContainerConfig config) {
    this.config = config;
  }

  public Uri makeOAuthCallbackUri(String container, String host) {
    String callback = config.getString(container, OAUTH_GADGET_CALLBACK_URI_PARAM);
    if (callback == null) {
      return null;
    }
    return Uri.parse(callback.replace("%host%", host));
  }

}
