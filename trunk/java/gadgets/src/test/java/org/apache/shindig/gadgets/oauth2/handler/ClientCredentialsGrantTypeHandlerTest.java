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

import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.oauth2.MockUtils;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;
import org.apache.shindig.gadgets.oauth2.OAuth2RequestException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ClientCredentialsGrantTypeHandlerTest extends MockUtils {
  private static ClientCredentialsGrantTypeHandler ccgth;

  @Before
  public void setUp() throws Exception {
    ClientCredentialsGrantTypeHandlerTest.ccgth = new ClientCredentialsGrantTypeHandler(
        MockUtils.getDummyClientAuthHandlers());
  }

  @Test
  public void testClientCredentialsGrantTypeHandler_1() throws Exception {
    final ClientCredentialsGrantTypeHandler result = ClientCredentialsGrantTypeHandlerTest.ccgth;

    Assert.assertNotNull(result);
    Assert.assertEquals("client_credentials", result.getGrantType());
    Assert.assertTrue(GrantRequestHandler.class.isInstance(result));
    Assert.assertEquals(false, result.isAuthorizationEndpointResponse());
    Assert.assertEquals(false, result.isRedirectRequired());
    Assert.assertEquals(true, result.isTokenEndpointResponse());
  }

  @Test(expected = OAuth2RequestException.class)
  public void testGetAuthorizationRequest_1() throws Exception {
    final ClientCredentialsGrantTypeHandler fixture = ClientCredentialsGrantTypeHandlerTest.ccgth;
    final OAuth2Accessor accessor = null;

    final String completeAuthorizationUrl = "xxx";

    fixture.getAuthorizationRequest(accessor, completeAuthorizationUrl);
  }

  @Test(expected = OAuth2RequestException.class)
  public void testGetAuthorizationRequest_2() throws Exception {
    final ClientCredentialsGrantTypeHandler fixture = ClientCredentialsGrantTypeHandlerTest.ccgth;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();

    final String completeAuthorizationUrl = null;

    fixture.getAuthorizationRequest(accessor, completeAuthorizationUrl);
  }

  @Test(expected = OAuth2RequestException.class)
  public void testGetAuthorizationRequest_3() throws Exception {
    final ClientCredentialsGrantTypeHandler fixture = ClientCredentialsGrantTypeHandlerTest.ccgth;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Error();

    final String completeAuthorizationUrl = "xxx";

    fixture.getAuthorizationRequest(accessor, completeAuthorizationUrl);
  }

  @Test(expected = OAuth2RequestException.class)
  public void testGetAuthorizationRequest_4() throws Exception {
    final ClientCredentialsGrantTypeHandler fixture = ClientCredentialsGrantTypeHandlerTest.ccgth;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();
    final String completeAuthorizationUrl = "xxx";

    fixture.getAuthorizationRequest(accessor, completeAuthorizationUrl);
  }

  @Test
  public void testGetAuthorizationRequest_5() throws Exception {
    final ClientCredentialsGrantTypeHandler fixture = ClientCredentialsGrantTypeHandlerTest.ccgth;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_ClientCredentials();
    final String completeAuthorizationUrl = "xxx";

    final HttpRequest result = fixture.getAuthorizationRequest(accessor, completeAuthorizationUrl);

    Assert.assertNotNull(result);
    final String postBody = result.getPostBodyAsString();
    Assert.assertNotNull(postBody);
    Assert.assertEquals(
        "client_id=clientId1&client_secret=clientSecret1&grant_type=client_credentials", postBody);
    Assert.assertNotNull( result.getSecurityToken() );
    Assert.assertTrue( result.getSecurityToken().isAnonymous() );
    Assert.assertEquals( accessor.getGadgetUri(), result.getSecurityToken().getAppUrl() );
  }

  @Test(expected = OAuth2RequestException.class)
  public void testGetCompleteUrl_1() throws Exception {
    final ClientCredentialsGrantTypeHandler fixture = ClientCredentialsGrantTypeHandlerTest.ccgth;
    final OAuth2Accessor accessor = null;
    fixture.getCompleteUrl(accessor);
  }

  @Test(expected = OAuth2RequestException.class)
  public void testGetCompleteUrl_2() throws Exception {
    final ClientCredentialsGrantTypeHandler fixture = ClientCredentialsGrantTypeHandlerTest.ccgth;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Error();
    fixture.getCompleteUrl(accessor);
  }

  @Test(expected = OAuth2RequestException.class)
  public void testGetCompleteUrl_3() throws Exception {
    final ClientCredentialsGrantTypeHandler fixture = ClientCredentialsGrantTypeHandlerTest.ccgth;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();

    fixture.getCompleteUrl(accessor);
  }

  @Test
  public void testGetCompleteUrl_4() throws Exception {
    final ClientCredentialsGrantTypeHandler fixture = ClientCredentialsGrantTypeHandlerTest.ccgth;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_ClientCredentials();

    final String result = fixture.getCompleteUrl(accessor);

    Assert.assertNotNull(result);
    Assert
        .assertEquals(
            "http://www.example.com/token?client_id=clientId1&client_secret=clientSecret1&grant_type=client_credentials&scope=testScope",
            result);
  }

  @Test
  public void testGetGrantType_1() throws Exception {
    final ClientCredentialsGrantTypeHandler fixture = ClientCredentialsGrantTypeHandlerTest.ccgth;

    final String result = fixture.getGrantType();

    Assert.assertEquals(OAuth2Message.CLIENT_CREDENTIALS, result);
  }

  @Test
  public void testIsAuthorizationEndpointResponse_1() throws Exception {
    final ClientCredentialsGrantTypeHandler fixture = ClientCredentialsGrantTypeHandlerTest.ccgth;

    final boolean result = fixture.isAuthorizationEndpointResponse();

    Assert.assertEquals(false, result);
  }

  @Test
  public void testIsRedirectRequired_1() throws Exception {
    final ClientCredentialsGrantTypeHandler fixture = ClientCredentialsGrantTypeHandlerTest.ccgth;

    final boolean result = fixture.isRedirectRequired();

    Assert.assertEquals(false, result);
  }

  @Test
  public void testIsTokenEndpointResponse_1() throws Exception {
    final ClientCredentialsGrantTypeHandler fixture = ClientCredentialsGrantTypeHandlerTest.ccgth;

    final boolean result = fixture.isTokenEndpointResponse();

    Assert.assertEquals(true, result);
  }
}
