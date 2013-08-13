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

import java.net.URI;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.oauth2.MockUtils;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2Error;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;
import org.junit.Assert;
import org.junit.Test;

public class StandardAuthenticationHandlerTest extends MockUtils {
  @Test
  public void testStandardAuthenticationHandler_1() throws Exception {
    final StandardAuthenticationHandler result = new StandardAuthenticationHandler();

    Assert.assertNotNull(result);
    Assert.assertTrue(ClientAuthenticationHandler.class.isInstance(result));
    Assert.assertEquals(OAuth2Message.STANDARD_AUTH_TYPE, result.geClientAuthenticationType());

  }

  @Test
  public void testAddOAuth2Authentication_1() throws Exception {
    final StandardAuthenticationHandler fixture = new StandardAuthenticationHandler();
    final HttpRequest request = null;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_StandardAuth();

    final OAuth2HandlerError result = fixture.addOAuth2Authentication(request, accessor);

    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getCause());
    Assert.assertEquals(OAuth2Error.AUTHENTICATION_PROBLEM, result.getError());
    Assert.assertEquals("request is null", result.getContextMessage());
  }

  @Test
  public void testAddOAuth2Authentication_2() throws Exception {
    final StandardAuthenticationHandler fixture = new StandardAuthenticationHandler();
    final HttpRequest request = new HttpRequest(Uri.fromJavaUri(new URI("")));
    final OAuth2Accessor accessor = null;

    final OAuth2HandlerError result = fixture.addOAuth2Authentication(request, accessor);

    Assert.assertNotNull(result);
    Assert.assertEquals(OAuth2Error.AUTHENTICATION_PROBLEM, result.getError());
    Assert.assertEquals(null, result.getCause());
    Assert.assertEquals("accessor is null", result.getContextMessage());
  }

  @Test
  public void testAddOAuth2Authentication_3() throws Exception {
    final StandardAuthenticationHandler fixture = new StandardAuthenticationHandler();
    final HttpRequest request = new HttpRequest(Uri.fromJavaUri(new URI("")));
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Error();

    final OAuth2HandlerError result = fixture.addOAuth2Authentication(request, accessor);

    Assert.assertNotNull(result);
    Assert.assertEquals(OAuth2Error.AUTHENTICATION_PROBLEM, result.getError());
    Assert.assertEquals(null, result.getCause());
    Assert.assertEquals("accessor is invalid", result.getContextMessage());
  }

  @Test
  public void testAddOAuth2Authentication_4() throws Exception {
    final StandardAuthenticationHandler fixture = new StandardAuthenticationHandler();
    final HttpRequest request = new HttpRequest(Uri.fromJavaUri(new URI("")));
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_StandardAuth();

    final OAuth2HandlerError result = fixture.addOAuth2Authentication(request, accessor);

    Assert.assertNull(result);
    final String header1 = request.getHeader(OAuth2Message.CLIENT_ID);
    Assert.assertNotNull(header1);
    Assert.assertEquals(MockUtils.CLIENT_ID1, header1);

    final String header2 = request.getHeader(OAuth2Message.CLIENT_SECRET);
    Assert.assertNotNull(header2);
    Assert.assertEquals(MockUtils.CLIENT_SECRET1, header2);

    final String requestUri = request.getUri().toString();
    Assert.assertNotNull(requestUri);
    Assert.assertEquals("", requestUri);

    final String param1 = request.getParam(OAuth2Message.CLIENT_ID);
    Assert.assertNotNull(param1);
    Assert.assertEquals(MockUtils.CLIENT_ID1, param1);

    final String param2 = request.getHeader(OAuth2Message.CLIENT_SECRET);
    Assert.assertNotNull(param2);
    Assert.assertEquals(MockUtils.CLIENT_SECRET1, param2);
  }

  @Test
  public void testGeClientAuthenticationType_1() throws Exception {
    final StandardAuthenticationHandler fixture = new StandardAuthenticationHandler();

    final String result = fixture.geClientAuthenticationType();

    Assert.assertEquals("STANDARD", result);
  }
}
