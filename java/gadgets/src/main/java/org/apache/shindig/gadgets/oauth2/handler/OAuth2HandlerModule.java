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
package org.apache.shindig.gadgets.oauth2.handler;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import org.apache.shindig.gadgets.oauth2.logger.FilteredLogger;

import java.util.List;

/**
 * Injects the default handlers.
 *
 */
public class OAuth2HandlerModule extends AbstractModule {
  private static final FilteredLogger LOG = FilteredLogger
          .getFilteredLogger(OAuth2HandlerModule.class.getName());

  @Override
  protected void configure() {
    if (OAuth2HandlerModule.LOG.isLoggable()) {
      OAuth2HandlerModule.LOG.entering(OAuth2HandlerModule.class.getName(), "configure");
    }
  }

  @Provides
  @Singleton
  public static List<AuthorizationEndpointResponseHandler> provideAuthorizationEndpointResponseHandlers(
          final CodeAuthorizationResponseHandler codeAuthorizationResponseHandler) {
    return ImmutableList
            .of((AuthorizationEndpointResponseHandler) codeAuthorizationResponseHandler);
  }

  @Provides
  @Singleton
  public static List<ClientAuthenticationHandler> provideClientAuthenticationHandlers(
          final BasicAuthenticationHandler basicAuthenticationHandler,
          final StandardAuthenticationHandler standardAuthenticationHandler) {
    return ImmutableList.of(basicAuthenticationHandler, standardAuthenticationHandler);
  }

  @Provides
  @Singleton
  public static List<GrantRequestHandler> provideGrantRequestHandlers(
          final ClientCredentialsGrantTypeHandler clientCredentialsGrantTypeHandler,
          final CodeGrantTypeHandler codeGrantTypeHandler) {
    return ImmutableList.of(clientCredentialsGrantTypeHandler, codeGrantTypeHandler);
  }

  @Provides
  @Singleton
  public static List<TokenEndpointResponseHandler> provideTokenEndpointResponseHandlers(
          final TokenAuthorizationResponseHandler tokenAuthorizationResponseHandler) {
    return ImmutableList.of((TokenEndpointResponseHandler) tokenAuthorizationResponseHandler);
  }

  @Provides
  @Singleton
  public static List<ResourceRequestHandler> provideTokenHandlers(
          final BearerTokenHandler bearerTokenHandler, final MacTokenHandler macTokenHandler) {
    return ImmutableList.of(bearerTokenHandler, macTokenHandler);
  }
}
