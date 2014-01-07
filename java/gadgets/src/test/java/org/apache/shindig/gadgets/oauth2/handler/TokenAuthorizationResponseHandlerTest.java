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

import com.google.inject.Provider;

import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.oauth2.MockUtils;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2Error;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;
import org.apache.shindig.gadgets.oauth2.OAuth2Store;
import org.apache.shindig.gadgets.oauth2.OAuth2Token;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TokenAuthorizationResponseHandlerTest extends MockUtils {
  private static TokenAuthorizationResponseHandler tarh;
  private static OAuth2Store store;

  @Before
  public void setUp() throws Exception {
    final Provider<OAuth2Message> oauth2MessageProvider = MockUtils.getDummyMessageProvider();
    TokenAuthorizationResponseHandlerTest.store = MockUtils.getDummyStore();

    TokenAuthorizationResponseHandlerTest.tarh = new TokenAuthorizationResponseHandler(
            oauth2MessageProvider, TokenAuthorizationResponseHandlerTest.store);
  }

  @Test
  public void testTokenAuthorizationResponseHandler_1() throws Exception {
    Assert.assertNotNull(TokenAuthorizationResponseHandlerTest.tarh);
    Assert.assertTrue(TokenEndpointResponseHandler.class
            .isInstance(TokenAuthorizationResponseHandlerTest.tarh));
  }

  @Test
  public void testHandlesResponse_1() throws Exception {
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Error();
    final HttpResponse response = new HttpResponse();

    final boolean result = TokenAuthorizationResponseHandlerTest.tarh.handlesResponse(accessor,
            response);
    Assert.assertFalse(result);
  }

  @Test
  public void testHandlesResponse_2() throws Exception {
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();
    final HttpResponse response = null;

    final boolean result = TokenAuthorizationResponseHandlerTest.tarh.handlesResponse(accessor,
            response);
    Assert.assertFalse(result);
  }

  @Test
  public void testHandlesResponse_3() throws Exception {
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();
    final HttpResponse response = new HttpResponse();

    final boolean result = TokenAuthorizationResponseHandlerTest.tarh.handlesResponse(accessor,
            response);
    Assert.assertTrue(result);
  }

  @Test
  public void testHandleResponse_1() throws Exception {
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Error();
    final HttpResponse response = new HttpResponse();

    final OAuth2HandlerError result = TokenAuthorizationResponseHandlerTest.tarh.handleResponse(
            accessor, response);
    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getCause());
    Assert.assertEquals(OAuth2Error.TOKEN_RESPONSE_PROBLEM, result.getError());
    Assert.assertTrue(result.getContextMessage().startsWith("accessor is invalid"));
  }

  @Test
  public void testHandleResponse_2() throws Exception {
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();
    final HttpResponse response = null;

    final OAuth2HandlerError result = TokenAuthorizationResponseHandlerTest.tarh.handleResponse(
            accessor, response);
    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getCause());
    Assert.assertEquals(OAuth2Error.TOKEN_RESPONSE_PROBLEM, result.getError());
    Assert.assertEquals("response is null", result.getContextMessage());
  }

  @Test
  public void testHandleResponse_3() throws Exception {
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();
    final HttpResponseBuilder builder = new HttpResponseBuilder().setStrictNoCache();
    builder.setHttpStatusCode(HttpResponse.SC_FORBIDDEN);
    final HttpResponse response = builder.create();

    final OAuth2HandlerError result = TokenAuthorizationResponseHandlerTest.tarh.handleResponse(
            accessor, response);

    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getCause());
    Assert.assertEquals(OAuth2Error.TOKEN_RESPONSE_PROBLEM, result.getError());
    Assert.assertTrue(result.getContextMessage().startsWith("can't handle error response"));
  }

  @Test
  public void testHandleResponse_4() throws Exception {
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();
    final HttpResponseBuilder builder = new HttpResponseBuilder().setStrictNoCache();
    builder.setHttpStatusCode(HttpResponse.SC_OK);
    builder.setHeader("Content-Type", "text/plain");
    builder.setContent("access_token=xxx&token_type=Bearer&expires=1&refresh_token=yyy&example_parameter=example_value");
    final HttpResponse response = builder.create();

    final OAuth2HandlerError result = TokenAuthorizationResponseHandlerTest.tarh.handleResponse(
            accessor, response);

    Assert.assertNull(result);

    final OAuth2Token accessToken = TokenAuthorizationResponseHandlerTest.store.getToken(
            accessor.getGadgetUri(), accessor.getServiceName(), accessor.getUser(),
            accessor.getScope(), OAuth2Token.Type.ACCESS);
    Assert.assertNotNull(accessToken);
    Assert.assertEquals("xxx", new String(accessToken.getSecret(), "UTF-8"));
    Assert.assertEquals(OAuth2Message.BEARER_TOKEN_TYPE, accessToken.getTokenType());
    Assert.assertTrue(accessToken.getExpiresAt() > 1000);

    final OAuth2Token refreshToken = TokenAuthorizationResponseHandlerTest.store.getToken(
            accessor.getGadgetUri(), accessor.getServiceName(), accessor.getUser(),
            accessor.getScope(), OAuth2Token.Type.REFRESH);
    Assert.assertNotNull(refreshToken);
    Assert.assertEquals("yyy", new String(refreshToken.getSecret(), "UTF-8"));
  }

  @Test
  public void testHandleResponse_5() throws Exception {
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();
    final HttpResponseBuilder builder = new HttpResponseBuilder().setStrictNoCache();
    builder.setHttpStatusCode(HttpResponse.SC_OK);
    builder.setHeader("Content-Type", "application/json");
    builder.setContent("{\"access_token\":\"xxx\",\"token_type\":\"Bearer\",\"expires_in\":\"1\",\"refresh_token\":\"yyy\",\"example_parameter\":\"example_value\"}");
    final HttpResponse response = builder.create();

    final OAuth2HandlerError result = TokenAuthorizationResponseHandlerTest.tarh.handleResponse(
            accessor, response);

    Assert.assertNull(result);

    final OAuth2Token accessToken = TokenAuthorizationResponseHandlerTest.store.getToken(
            accessor.getGadgetUri(), accessor.getServiceName(), accessor.getUser(),
            accessor.getScope(), OAuth2Token.Type.ACCESS);
    Assert.assertNotNull(accessToken);
    Assert.assertEquals("xxx", new String(accessToken.getSecret(), "UTF-8"));
    Assert.assertEquals(OAuth2Message.BEARER_TOKEN_TYPE, accessToken.getTokenType());
    Assert.assertTrue(accessToken.getExpiresAt() > 1000);

    final OAuth2Token refreshToken = TokenAuthorizationResponseHandlerTest.store.getToken(
            accessor.getGadgetUri(), accessor.getServiceName(), accessor.getUser(),
            accessor.getScope(), OAuth2Token.Type.REFRESH);
    Assert.assertNotNull(refreshToken);
    Assert.assertEquals("yyy", new String(refreshToken.getSecret(), "UTF-8"));
  }

  @Test
  public void testHandleResponse_6() throws Exception {
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();
    final HttpResponseBuilder builder = new HttpResponseBuilder().setStrictNoCache();
    builder.setHttpStatusCode(HttpResponse.SC_OK);
    builder.setHeader("Content-Type", "BAD");
    builder.setContent("access_token=xxx&token_type=Bearer&expires=1&refresh_token=yyy&example_parameter=example_value");
    final HttpResponse response = builder.create();

    final OAuth2HandlerError result = TokenAuthorizationResponseHandlerTest.tarh.handleResponse(
            accessor, response);

    Assert.assertNull(result);
    final OAuth2Token accessToken = TokenAuthorizationResponseHandlerTest.store.getToken(
            accessor.getGadgetUri(), accessor.getServiceName(), accessor.getUser(),
            accessor.getScope(), OAuth2Token.Type.ACCESS);
    Assert.assertEquals("xxx", new String(accessToken.getSecret(), "UTF-8"));
    Assert.assertEquals(OAuth2Message.BEARER_TOKEN_TYPE, accessToken.getTokenType());
    Assert.assertTrue(accessToken.getExpiresAt() > 1000);
  }
}
