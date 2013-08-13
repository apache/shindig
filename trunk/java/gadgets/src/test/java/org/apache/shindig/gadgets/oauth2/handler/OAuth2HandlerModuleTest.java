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

import java.util.List;

import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.oauth2.OAuth2Store;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;

public class OAuth2HandlerModuleTest {
  @Test
  public void testConfigure_1() throws Exception {
    final OAuth2HandlerModule fixture = new OAuth2HandlerModule();

    Assert.assertTrue(AbstractModule.class.isInstance(fixture));
  }

  @Test
  @SuppressWarnings({ "unchecked", "unused" })
  public void testProvideAuthorizationEndpointResponseHandlers_1() throws Exception {
    final OAuth2HandlerModule fixture = new OAuth2HandlerModule();
    final CodeAuthorizationResponseHandler codeAuthorizationResponseHandler = new CodeAuthorizationResponseHandler(
        EasyMock.createNiceMock(Provider.class), EasyMock.createNiceMock(List.class),
        EasyMock.createNiceMock(List.class), EasyMock.createNiceMock(HttpFetcher.class));
    final TokenAuthorizationResponseHandler tokenAuthorizationResponseHandler = new TokenAuthorizationResponseHandler(
        EasyMock.createNiceMock(Provider.class), EasyMock.createNiceMock(OAuth2Store.class));

    final List<AuthorizationEndpointResponseHandler> result = OAuth2HandlerModule
        .provideAuthorizationEndpointResponseHandlers(codeAuthorizationResponseHandler);

    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.size());
  }

  @SuppressWarnings("unused")
  @Test
  public void testProvideClientAuthenticationHandlers_1() throws Exception {
    final OAuth2HandlerModule fixture = new OAuth2HandlerModule();
    final BasicAuthenticationHandler basicAuthenticationHandler = new BasicAuthenticationHandler();
    final StandardAuthenticationHandler standardAuthenticationHandler = new StandardAuthenticationHandler();

    final List<ClientAuthenticationHandler> result = OAuth2HandlerModule
        .provideClientAuthenticationHandlers(basicAuthenticationHandler,
            standardAuthenticationHandler);

    Assert.assertNotNull(result);
    Assert.assertEquals(2, result.size());
  }

  @Test
  @SuppressWarnings({ "unchecked", "unused" })
  public void testProvideGrantRequestHandlers_1() throws Exception {
    final OAuth2HandlerModule fixture = new OAuth2HandlerModule();
    final ClientCredentialsGrantTypeHandler clientCredentialsGrantTypeHandler = new ClientCredentialsGrantTypeHandler(
        EasyMock.createNiceMock(List.class));
    final CodeGrantTypeHandler codeGrantTypeHandler = new CodeGrantTypeHandler();

    final List<GrantRequestHandler> result = OAuth2HandlerModule.provideGrantRequestHandlers(
        clientCredentialsGrantTypeHandler, codeGrantTypeHandler);

    Assert.assertNotNull(result);
    Assert.assertEquals(2, result.size());
  }

  @Test
  @SuppressWarnings({ "unchecked", "unused" })
  public void testProvideTokenEndpointResponseHandlers_1() throws Exception {
    final OAuth2HandlerModule fixture = new OAuth2HandlerModule();
    final TokenAuthorizationResponseHandler tokenAuthorizationResponseHandler = new TokenAuthorizationResponseHandler(
        EasyMock.createNiceMock(Provider.class), EasyMock.createNiceMock(OAuth2Store.class));

    final List<TokenEndpointResponseHandler> result = OAuth2HandlerModule
        .provideTokenEndpointResponseHandlers(tokenAuthorizationResponseHandler);

    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.size());
  }

  @SuppressWarnings("unused")
  @Test
  public void testProvideTokenHandlers_1() throws Exception {
    final OAuth2HandlerModule fixture = new OAuth2HandlerModule();
    final BearerTokenHandler bearerTokenHandler = new BearerTokenHandler();
    final MacTokenHandler macTokenHandler = new MacTokenHandler();

    final List<ResourceRequestHandler> result = OAuth2HandlerModule.provideTokenHandlers(
        bearerTokenHandler, macTokenHandler);

    Assert.assertNotNull(result);
    Assert.assertEquals(2, result.size());
  }
}
