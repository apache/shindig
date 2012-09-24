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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.oauth2.MockUtils;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2Error;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;

import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

public class BasicAuthenticationHandlerTest extends MockUtils {
  @Test
  public void testBasicAuthenticationHandler1() throws Exception {
    final BasicAuthenticationHandler result = new BasicAuthenticationHandler();

    Assert.assertNotNull(result);
    Assert.assertTrue(ClientAuthenticationHandler.class.isInstance(result));
    Assert.assertEquals(OAuth2Message.BASIC_AUTH_TYPE, result.geClientAuthenticationType());
  }

  @Test
  public void testAddOAuth2Authentication1() throws Exception {
    final BasicAuthenticationHandler fixture = new BasicAuthenticationHandler();
    final HttpRequest request = null;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();

    final OAuth2HandlerError result = fixture.addOAuth2Authentication(request, accessor);

    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getCause());
    Assert.assertEquals("request is null", result.getContextMessage());
    Assert.assertEquals(
            "org.apache.shindig.gadgets.oauth2.handler.OAuth2HandlerError : AUTHENTICATION_PROBLEM : request is null :  : :null",
            result.toString());
  }

  @Test
  public void testAddOAuth2Authentication2() throws Exception {
    final BasicAuthenticationHandler fixture = new BasicAuthenticationHandler();
    final HttpRequest request = new HttpRequest(Uri.fromJavaUri(new URI("")));
    final OAuth2Accessor accessor = null;

    final OAuth2HandlerError result = fixture.addOAuth2Authentication(request, accessor);

    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getCause());
    Assert.assertEquals("accessor is invalid null", result.getContextMessage());
    Assert.assertEquals(
            "org.apache.shindig.gadgets.oauth2.handler.OAuth2HandlerError : AUTHENTICATION_PROBLEM : accessor is invalid null :  : :null",
            result.toString());
  }

  @Test
  public void testAddOAuth2Authentication3() throws Exception {
    final BasicAuthenticationHandler fixture = new BasicAuthenticationHandler();
    final HttpRequest request = new HttpRequest(Uri.fromJavaUri(new URI("")));
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Error();

    final OAuth2HandlerError result = fixture.addOAuth2Authentication(request, accessor);

    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getCause());
    Assert.assertEquals(OAuth2Error.AUTHENTICATION_PROBLEM, result.getError());
    Assert.assertTrue(result.getContextMessage().startsWith("accessor is invalid"));
  }

  @Test
  public void testAddOAuth2Authentication4() throws Exception {
    final BasicAuthenticationHandler fixture = new BasicAuthenticationHandler();
    final HttpRequest request = new HttpRequest(Uri.fromJavaUri(new URI("")));
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();

    final OAuth2HandlerError result = fixture.addOAuth2Authentication(request, accessor);

    Assert.assertNull(result);

    final String authHeader = request.getHeader("Authorization");

    Assert.assertNotNull(authHeader);

    Assert.assertEquals("Basic: Y2xpZW50SWQxOmNsaWVudFNlY3JldDE=", authHeader);
  }

  @Test
  public void testGeClientAuthenticationType1() throws Exception {
    final BasicAuthenticationHandler fixture = new BasicAuthenticationHandler();

    final String result = fixture.geClientAuthenticationType();
    Assert.assertEquals(OAuth2Message.BASIC_AUTH_TYPE, result);
  }
}
