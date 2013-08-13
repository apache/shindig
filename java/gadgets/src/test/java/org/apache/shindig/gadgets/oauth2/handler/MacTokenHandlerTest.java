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

public class MacTokenHandlerTest extends MockUtils {
  @Test
  public void testMacTokenHandler_1() throws Exception {

    final MacTokenHandler result = new MacTokenHandler();

    Assert.assertNotNull(result);
    Assert.assertTrue(ResourceRequestHandler.class.isInstance(result));
    Assert.assertEquals(OAuth2Message.MAC_TOKEN_TYPE, result.getTokenType());
  }

  @Test
  public void testAddOAuth2Params_1() throws Exception {
    final MacTokenHandler fixture = new MacTokenHandler();
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();
    final HttpRequest request = new HttpRequest(Uri.fromJavaUri(new URI("")));

    final OAuth2HandlerError result = fixture.addOAuth2Params(accessor, request);

    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getCause());
    Assert.assertEquals(OAuth2Error.MAC_TOKEN_PROBLEM, result.getError());
    Assert.assertEquals("token type mismatch expected mac but got Bearer",
        result.getContextMessage());
  }

  @Test
  public void testAddOAuth2Params_2() throws Exception {
    final MacTokenHandler fixture = new MacTokenHandler();
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_MacToken();
    final HttpRequest request = null;

    final OAuth2HandlerError result = fixture.addOAuth2Params(accessor, request);

    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getCause());
    Assert.assertEquals(OAuth2Error.MAC_TOKEN_PROBLEM, result.getError());
    Assert.assertEquals("request is null", result.getContextMessage());
  }

  @Test
  public void testAddOAuth2Params_3() throws Exception {
    final MacTokenHandler fixture = new MacTokenHandler();
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Error();
    final HttpRequest request = new HttpRequest(Uri.fromJavaUri(new URI("")));

    final OAuth2HandlerError result = fixture.addOAuth2Params(accessor, request);

    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getCause());
    Assert.assertEquals(OAuth2Error.MAC_TOKEN_PROBLEM, result.getError());
    Assert.assertTrue(result.getContextMessage().startsWith("accessor is invalid"));
  }

  @Test
  public void testAddOAuth2Params_4() throws Exception {
    final MacTokenHandler fixture = new MacTokenHandler();
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_BadMacToken();
    final HttpRequest request = new HttpRequest(Uri.fromJavaUri(new URI("a")));
    request.setMethod("");

    final OAuth2HandlerError result = fixture.addOAuth2Params(accessor, request);

    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getCause());
    Assert.assertEquals(OAuth2Error.MAC_TOKEN_PROBLEM, result.getError());
    Assert.assertEquals("unsupported algorithm hmac-sha-256", result.getContextMessage());
  }

  @Test
  public void testAddOAuth2Params_5() throws Exception {
    final MacTokenHandler fixture = new MacTokenHandler();
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_MacToken();
    final HttpRequest request = new HttpRequest(Uri.fromJavaUri(new URI(
        "http://www.example.com:9080/xxx")));
    request.setMethod("");

    final OAuth2HandlerError result = fixture.addOAuth2Params(accessor, request);

    Assert.assertNull(result);
    final String authHeader = request.getHeader("Authorization");
    Assert.assertNotNull(authHeader);
    Assert.assertTrue(authHeader.startsWith("MAC id = \"accessSecret\",nonce="));
  }

  @Test
  public void testGetTokenType_1() throws Exception {
    final MacTokenHandler fixture = new MacTokenHandler();

    final String result = fixture.getTokenType();

    Assert.assertEquals(OAuth2Message.MAC_TOKEN_TYPE, result);
  }
}
