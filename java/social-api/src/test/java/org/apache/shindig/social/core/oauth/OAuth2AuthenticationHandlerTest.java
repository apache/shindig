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
package org.apache.shindig.social.core.oauth;

import org.apache.shindig.auth.AuthenticationHandler.InvalidAuthenticationException;
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.testing.FakeHttpServletRequest;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.core.oauth2.OAuth2AuthenticationHandler;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;

public class OAuth2AuthenticationHandlerTest extends EasyMockTestCase {

  private final String ACCESS_TOKEN = "testClient_accesstoken_1";
  protected OAuth2AuthenticationHandler handler = null;

  @Before
  public void setUp() {
    handler = Guice.createInjector(new SocialApiTestsGuiceModule())
        .getInstance(OAuth2AuthenticationHandler.class);
  }

  @Test
  public void testValidAccessTokenViaURL()
      throws InvalidAuthenticationException {
    replay();
    handler.getSecurityTokenFromRequest(new FakeHttpServletRequest(
        "http://localhost:8080/oauth2", "/some_protected_uri", "access_token="
            + ACCESS_TOKEN));
    // Should not throw exception
  }

  @Test
  public void testInvalidAccessTokenViaURL() {
    replay();
    try {
      handler.getSecurityTokenFromRequest(new FakeHttpServletRequest(
          "http://localhost:8080/oauth2", "/some_protected_uri",
          "access_token=BADTOKEN"));
    } catch (InvalidAuthenticationException ex) {
      return;
    }
    fail("Handler allowed invalid token without throwing exception");
    // Should not throw exception
  }

  @Test
  public void testValidAccessTokenViaHeader()
      throws InvalidAuthenticationException {
    replay();
    FakeHttpServletRequest req = new FakeHttpServletRequest(
        "http://localhost:8080/oauth2", "/some_protected_uri", "");
    req.setHeader("Authorization", "Bearer " + ACCESS_TOKEN);
    handler.getSecurityTokenFromRequest(req);
    // Should not throw exception
  }

  @Test
  public void testInvalidAccessTokenViaHeader() {
    replay();
    FakeHttpServletRequest req = new FakeHttpServletRequest(
        "http://localhost:8080/oauth2", "/some_protected_uri", "");
    req.setHeader("Authorization", "Bearer BADVALUEK");
    try {
      handler.getSecurityTokenFromRequest(req);
    } catch (InvalidAuthenticationException ex) {
      return;
    }
    fail("Handler allowed invalid token without throwing exception");
    // Should not throw exception
  }

}
