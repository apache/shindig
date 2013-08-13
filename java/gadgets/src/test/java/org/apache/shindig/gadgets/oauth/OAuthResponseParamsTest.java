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
package org.apache.shindig.gadgets.oauth;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.easymock.EasyMock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class OAuthResponseParamsTest {

  private static final String APP = "http://app/example.xml";

  private HttpRequest origRequest;
  private SecurityToken token;
  private BlobCrypter crypter;
  private OAuthResponseParams params;

  @Before
  public void setUp() {
    crypter = new BasicBlobCrypter("abcdefafadfaxxxx".getBytes());
    token = EasyMock.createMock(SecurityToken.class);
    origRequest = new HttpRequest(Uri.parse("http://originalrequest/"));
    EasyMock.expect(token.getAppUrl()).andStubReturn(APP);
    EasyMock.replay(token);
    params = new OAuthResponseParams(token, origRequest, crypter);
  }

  @Test
  public void testSetAndGet() {
    params.getNewClientState().setAccessToken("access");
    params.setAznUrl("aznurl");
    assertFalse(params.sendTraceToClient());
    params.setSendTraceToClient(true);
    assertTrue(params.sendTraceToClient());
    assertEquals("access", params.getNewClientState().getAccessToken());
    assertEquals("aznurl", params.getAznUrl());
  }

  @Test
  public void testAddParams() {
    params.getNewClientState().setAccessToken("access");
    params.setAznUrl("aznurl");
    OAuthRequestException e = new OAuthRequestException(OAuthError.BAD_OAUTH_CONFIGURATION, "whoa there cowboy");

    HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    params.addToResponse(responseBuilder, e);
    HttpResponse response = responseBuilder.create();
    assertEquals("BAD_OAUTH_CONFIGURATION", response.getMetadata().get("oauthError"));
    String errorText = response.getMetadata().get("oauthErrorText");
    checkStringContains("error text returned", errorText, "whoa there cowboy");
    assertEquals("aznurl", response.getMetadata().get("oauthApprovalUrl"));
    assertNotNull(response.getMetadata().get("oauthState"));
    assertTrue(response.getMetadata().get("oauthState").length() > 10);
  }

  @Test
  public void testSendTraceToClient() {
    OAuthRequestException e = new OAuthRequestException(OAuthError.BAD_OAUTH_CONFIGURATION, "whoa there cowboy");
    params.addRequestTrace(null, null);
    params.addRequestTrace(null, null);

    HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    params.addToResponse(responseBuilder, e);
    HttpResponse response = responseBuilder.create();

    String errorText = response.getMetadata().get("oauthErrorText");
    assertEquals("whoa there cowboy", errorText);

    params.setSendTraceToClient(true);
    params.addToResponse(responseBuilder, e);
    response = responseBuilder.create();
    errorText = response.getMetadata().get("oauthErrorText");
    checkStringContains("includes error text", errorText, "whoa there cowboy");
    checkStringContains("Request 1 logged", errorText, "Sent request 1:\n\n");
    checkStringContains("Request 2 logged", errorText, "Sent request 2:\n\n");
  }

  @Test
  public void testAddEmptyParams() {
    HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    params.addToResponse(responseBuilder, null);
    HttpResponse response = responseBuilder.create();
    assertTrue(response.getMetadata().isEmpty());
  }

  @Test
  public void testSawErrorResponse() {
    HttpRequest req = new HttpRequest(Uri.parse("http://www"));
    HttpResponse ok = new HttpResponseBuilder().setHttpStatusCode(200).create();
    HttpResponse redir = new HttpResponseBuilder().setHttpStatusCode(302).create();
    HttpResponse notFound = new HttpResponseBuilder().setHttpStatusCode(404).create();
    HttpResponse doh = new HttpResponseBuilder().setHttpStatusCode(502).create();

    OAuthResponseParams params = new OAuthResponseParams(token, origRequest, crypter);
    assertFalse(params.sawErrorResponse());
    params.addRequestTrace(req, ok);
    assertFalse(params.sawErrorResponse());
    params.addRequestTrace(req, redir);
    assertFalse(params.sawErrorResponse());
    params.addRequestTrace(req, null);
    assertTrue(params.sawErrorResponse());

    params = new OAuthResponseParams(token, origRequest, crypter);
    params.addRequestTrace(req, notFound);
    assertTrue(params.sawErrorResponse());

    params = new OAuthResponseParams(token, origRequest, crypter);
    params.addRequestTrace(req, doh);
    assertTrue(params.sawErrorResponse());
    params.addRequestTrace(req, ok);
    assertTrue(params.sawErrorResponse());
  }

  @Test
  public void testException() {
    HttpRequest req = new HttpRequest(Uri.parse("http://www"));
    HttpResponse ok = new HttpResponseBuilder().setHttpStatusCode(200).create();
    params.addRequestTrace(req, ok);
    OAuthRequestException e = new OAuthRequestException("error", "errorText");
    checkStringContains(e.toString(), "[error,errorText]");
    params.addRequestTrace(null, null);
    Throwable cause = new RuntimeException();
    e = new OAuthRequestException(OAuthError.UNAUTHENTICATED, "errorText", cause);
    checkStringContains(e.toString(), "[UNAUTHENTICATED,Unauthenticated OAuth fetch]");
    assertEquals(cause, e.getCause());
  }

  @Test
  public void testNullSafe() {
    params.addRequestTrace(null, null);
    new OAuthRequestException("error", "errorText");
    params.logDetailedWarning("org.apache.shindig.gadgets.oauth.OAuthResponseParamsTest","testNullSafe","wow");
    params.logDetailedWarning("org.apache.shindig.gadgets.oauth.OAuthResponseParamsTest","testNullSafe","new runtime", new RuntimeException());
  }

  @Test
  public void testStripSensitiveFromResponse() {
    verifyStrip("oauth_token=dbce9de6d6da692b99b39cdcde60fd83&oauth_token_secret=60c1aabe0f6db96" +
        "f2719956168c08d9d");

    String out = verifyStrip("oauth_token=dbce9de6d6da692b99b39cdcde60fd83&oauth_token_secret" +
              "=60c1aabe0f6db96f2719956168c08d9d&oauth_session_handle=ABCDEFGH");
    checkStringContains(out, "oauth_token=dbce");
    checkStringContains(out, "HTTP/1.1 200");

    out = verifyStrip("oauth_token_secret=x");
    checkStringContains(out, "oauth_token_secret=REMOVED");

    out = verifyStrip("foo&oauth_token_secret=!@#$%$^&(()&");
    checkStringContains(out, "foo&oauth_token_secret=REMOVED&");
  }

  private String verifyStrip(String body) {
    HttpResponseBuilder resp = new HttpResponseBuilder()
        .setHttpStatusCode(200)
        .setHeader("Date", "Date: Fri, 09 Jan 2009 00:35:08 GMT")
        .setResponseString(body);
    String out = OAuthResponseParams.filterSecrets(resp.create().toString());
    if (out.contains("oauth_token_secret")) {
      checkStringContains("should remove secret", out, "oauth_token_secret=REMOVED");
    }
    if (out.contains("oauth_session_handle")) {
      checkStringContains("should remove handle", out, "oauth_session_handle=REMOVED");
    }
    return out;
  }

  @Test
  public void testStripSecretsFromRequestHeader() {
    HttpRequest req = new HttpRequest(Uri.parse("http://www.example.com/foo"));
    req.setHeader("Authorization", "OAuth opensocial_owner_id=\"owner\", opensocial_viewer_id=" +
        "\"owner\", opensocial_app_id=\"app\", opensocial_app_url=\"http%3A%2F%2Fwww.examp" +
        "le.com%2Fheader.xml\", oauth_version=\"1.0\", oauth_timestamp=\"1231461306\", oau" +
        "th_consumer_key=\"consumer\", oauth_signature_method=\"HMAC-SHA1\", oauth_nonce" +
        "=\"1231461308333563000\", oauth_session_handle=\"w0zAI1yN5ZRvmBX5kcVdra5%2BbZE%" +
        "3D\"");
    String filtered = OAuthResponseParams.filterSecrets(req.toString());
    checkStringContains(filtered, "oauth_session_handle=REMOVED");
  }

  @Test
  public void testStripSecretsFromRequestUrl() {
    HttpRequest req = new HttpRequest(Uri.parse("http://www.example.com/access?param=foo&openso" +
        "cial_owner_id=owner&opensocial_viewer_id=owner&opensocial_app_id=app&" +
        "oauth_session_handle" +
        "=http%3A%2F%2Fwww.example.com%2Fgadget.xml&oauth_version=1.0&oauth_timestamp=12" +
        "31461132&oauth_consumer_key=consumer&oauth_signature_method=HMAC-SHA1&oauth_nonce=1" +
        "231461160262578000&oauth_signature=HuFQ%2BRYTrRzcgsi3al6ld9Msvoo%3D"));
    String filtered = OAuthResponseParams.filterSecrets(req.toString());
    checkStringContains(filtered, "oauth_session_handle=REMOVED");
  }

  private void checkStringContains(String text, String expected) {
    if (!text.contains(expected)) {
      fail("expected [" + expected + "], got + [" + text + ']');
    }
  }

  private void checkStringContains(String message, String text, String expected) {
    if (!text.contains(expected)) {
      fail(message + ", expected [" + expected + "], got + [" + text + ']');
    }
  }
}
