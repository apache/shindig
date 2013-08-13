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
package org.apache.shindig.auth;

import org.apache.shindig.common.testing.FakeHttpServletRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class UrlParameterAuthenticationHandlerTest {
  SecurityToken expectedToken;
  UrlParameterAuthenticationHandler authHandler;
  SecurityTokenCodec codec;
  HttpServletRequest req;

  @Before
  public void setup() throws Exception {
    expectedToken = new BasicSecurityToken(
        "owner", "viewer", "app",
        "domain", "appUrl", "0", "container", "activeUrl", 1000L);
    // Mock token codec
    codec = new SecurityTokenCodec() {
      public SecurityToken createToken(Map<String, String> tokenParameters) throws SecurityTokenException {
        return tokenParameters == null ? null :
               "1234".equals(tokenParameters.get(SecurityTokenCodec.SECURITY_TOKEN_NAME)) ? expectedToken : null;
      }

      public String encodeToken(SecurityToken token) throws SecurityTokenException {
        return null;
      }

      public int getTokenTimeToLive() {
        return 0; // Not used.
      }

      public int getTokenTimeToLive(String container) {
        return 0; // Not used.
      }
    };

    authHandler = new UrlParameterAuthenticationHandler(codec, true);
  }

  @Test
  public void testGetSecurityTokenFromRequest() throws Exception {
    Assert.assertEquals(authHandler.getName(), AuthenticationMode.SECURITY_TOKEN_URL_PARAMETER.name());
  }

  @Test
  public void testInvalidRequests() throws Exception {
    // Empty request
    req = new FakeHttpServletRequest();
    Assert.assertNull(authHandler.getSecurityTokenFromRequest(req));

    // Old behavior, no longer supported
    req = new FakeHttpServletRequest().setHeader("Authorization", "Token token=\"1234\"");
    Assert.assertNull(authHandler.getSecurityTokenFromRequest(req));

    req = new FakeHttpServletRequest().setHeader("Authorization", "OAuth 1234");
    Assert.assertNull(authHandler.getSecurityTokenFromRequest(req));
  }

  @Test
  public void testSecurityToken() throws Exception {
    // security token in request
    req = new FakeHttpServletRequest("http://example.org/rpc?st=1234");
    Assert.assertEquals(expectedToken, authHandler.getSecurityTokenFromRequest(req));
  }

  @Test
  public void testOAuth1() throws Exception {
    // An OAuth 1.0 request, we should not process this.
    req = new FakeHttpServletRequest()
        .setHeader("Authorization", "OAuth oauth_signature_method=\"RSA-SHA1\"");
    SecurityToken token = authHandler.getSecurityTokenFromRequest(req);
    Assert.assertNull(token);
  }

  @Test
  public void testOAuth2Header() throws Exception {
    req = new FakeHttpServletRequest("https://www.example.org/")
        .setHeader("Authorization", "OAuth2  1234");
    Assert.assertEquals(expectedToken, authHandler.getSecurityTokenFromRequest(req));

    req = new FakeHttpServletRequest("https://www.example.org/")
        .setHeader("Authorization", "   OAuth2    1234 ");
    Assert.assertEquals(expectedToken, authHandler.getSecurityTokenFromRequest(req));

    req = new FakeHttpServletRequest("https://www.example.org/")
        .setHeader("Authorization", "OAuth2 1234 x=1,y=\"2 2 2\"");
    Assert.assertEquals(expectedToken, authHandler.getSecurityTokenFromRequest(req));

    req = new FakeHttpServletRequest("http://www.example.org/")
        .setHeader("Authorization", "OAuth2 1234");
    Assert.assertNull(authHandler.getSecurityTokenFromRequest(req));
  }

  @Test
  public void testOAuth2Param() throws Exception
  {
    req = new FakeHttpServletRequest("https://www.example.com?oauth_token=1234");
    Assert.assertEquals(expectedToken, authHandler.getSecurityTokenFromRequest(req));

    req = new FakeHttpServletRequest("https://www.example.com?oauth_token=1234&oauth_signature_method=RSA-SHA1");
    Assert.assertNull(authHandler.getSecurityTokenFromRequest(req));
  }
}
