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

import com.google.common.collect.Maps;

import org.apache.shindig.gadgets.oauth2.MockUtils;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2RequestException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class CodeGrantTypeHandlerTest extends MockUtils {

  private static CodeGrantTypeHandler cgth;

  @Before
  public void setUp() throws Exception {
    CodeGrantTypeHandlerTest.cgth = new CodeGrantTypeHandler();
  }

  @Test
  public void testCodeGrantTypeHandler_1() throws Exception {
    final CodeGrantTypeHandler result = new CodeGrantTypeHandler();

    Assert.assertNotNull(result);
    Assert.assertTrue(GrantRequestHandler.class.isInstance(result));
    Assert.assertEquals("code", result.getGrantType());
    Assert.assertEquals("authorization_code", CodeGrantTypeHandler.getResponseType());
    Assert.assertEquals(true, result.isAuthorizationEndpointResponse());
    Assert.assertEquals(true, result.isRedirectRequired());
    Assert.assertEquals(false, result.isTokenEndpointResponse());
  }

  @Test(expected = OAuth2RequestException.class)
  public void testGetAuthorizationRequest_1() throws Exception {
    final CodeGrantTypeHandler fixture = CodeGrantTypeHandlerTest.cgth;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();
    final String completeAuthorizationUrl = "xxx";

    fixture.getAuthorizationRequest(accessor, completeAuthorizationUrl);
  }

  @Test(expected = OAuth2RequestException.class)
  public void testGetCompleteUrl_1() throws Exception {
    final CodeGrantTypeHandler fixture = CodeGrantTypeHandlerTest.cgth;
    final OAuth2Accessor accessor = null;
    fixture.getCompleteUrl(accessor);
  }

  @Test(expected = OAuth2RequestException.class)
  public void testGetCompleteUrl_2() throws Exception {
    final CodeGrantTypeHandler fixture = CodeGrantTypeHandlerTest.cgth;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Error();
    fixture.getCompleteUrl(accessor);
  }

  @Test(expected = OAuth2RequestException.class)
  public void testGetCompleteUrl_3() throws Exception {
    final CodeGrantTypeHandler fixture = CodeGrantTypeHandlerTest.cgth;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_ClientCredentialsRedirecting();
    fixture.getCompleteUrl(accessor);
  }

  @Test
  public void testGetCompleteUrl_4() throws Exception {
    final CodeGrantTypeHandler fixture = CodeGrantTypeHandlerTest.cgth;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();
    final String result = fixture.getCompleteUrl(accessor);

    Assert.assertNotNull(result);
    Assert.assertTrue(result
            .startsWith("http://www.example.com/authorize?client_id=clientId1&redirect_uri=https%3A%2F%2Fwww.example.com%2Fgadgets%2Foauth2callback&response_type=code&scope=testScope&state="));
  }

  @Test
  public void testGetCompleteUrl_5() throws Exception {
    final CodeGrantTypeHandler fixture = CodeGrantTypeHandlerTest.cgth;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();
    final Map<String, String> additionalParams = Maps.newHashMap();
    additionalParams.put("param1", "value1");
    accessor.setAdditionalRequestParams(additionalParams);
    final String result = fixture.getCompleteUrl(accessor);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.contains("&param1=value1"));
  }

  @Test
  public void testGetGrantType_1() throws Exception {
    final CodeGrantTypeHandler fixture = CodeGrantTypeHandlerTest.cgth;

    final String result = fixture.getGrantType();

    Assert.assertEquals("code", result);
  }

  @Test
  public void testGetResponseType_1() throws Exception {
    final String result = CodeGrantTypeHandler.getResponseType();

    Assert.assertEquals("authorization_code", result);
  }

  @Test
  public void testIsAuthorizationEndpointResponse_1() throws Exception {
    final CodeGrantTypeHandler fixture = CodeGrantTypeHandlerTest.cgth;

    final boolean result = fixture.isAuthorizationEndpointResponse();

    Assert.assertEquals(true, result);
  }

  @Test
  public void testIsRedirectRequired_1() throws Exception {
    final CodeGrantTypeHandler fixture = CodeGrantTypeHandlerTest.cgth;

    final boolean result = fixture.isRedirectRequired();

    Assert.assertEquals(true, result);
  }

  @Test
  public void testIsTokenEndpointResponse_1() throws Exception {
    final CodeGrantTypeHandler fixture = CodeGrantTypeHandlerTest.cgth;

    final boolean result = fixture.isTokenEndpointResponse();

    Assert.assertEquals(false, result);
  }
}
