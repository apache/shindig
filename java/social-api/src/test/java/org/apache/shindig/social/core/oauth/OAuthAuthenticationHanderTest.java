/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social.core.oauth;

import net.oauth.OAuth;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.auth.AuthenticationHandler;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.testing.FakeHttpServletRequest;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.apache.shindig.social.opensocial.oauth.OAuthEntry;
import org.easymock.classextension.EasyMock;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

/**
 * Verify behavior of OAuth handler for consumer and 3-legged requests.
 */
public class OAuthAuthenticationHanderTest extends EasyMockTestCase {

  OAuthDataStore mockStore = mock(OAuthDataStore.class);

  OAuthAuthenticationHandler reqHandler;

  private FakeOAuthRequest formEncodedPost;
  private FakeOAuthRequest nonFormEncodedPost;

  private static final String TEST_URL = "http://www.example.org/a/b?x=y";
  private static final String TOKEN = "atoken";
  private static final String APP_ID = "app:12345";
  private static final String DOMAIN = "example.org";
  private static final String CONTAINER = "sandbox";

  @Override
  protected void setUp() throws Exception {
    reqHandler = new OAuthAuthenticationHandler(mockStore, true);
    formEncodedPost = new FakeOAuthRequest("POST", TEST_URL, "a=b&c=d",
        OAuth.FORM_ENCODED);
    nonFormEncodedPost = new FakeOAuthRequest("POST", TEST_URL, "BODY",
        "text/plain");
  }

  private void expectTokenEntry() {
    expectTokenEntry(createOAuthEntry());
  }

  private void expectTokenEntry(OAuthEntry authEntry) {
    EasyMock.expect(mockStore.getEntry(
        EasyMock.eq(TOKEN))).
          andReturn(authEntry).anyTimes();
  }

  private OAuthEntry createOAuthEntry() {
    OAuthEntry authEntry = new OAuthEntry();
    authEntry.appId = APP_ID;
    authEntry.authorized = true;
    authEntry.consumerKey = FakeOAuthRequest.CONSUMER_KEY;
    authEntry.token = TOKEN;
    authEntry.tokenSecret = FakeOAuthRequest.CONSUMER_SECRET;
    authEntry.type = OAuthEntry.Type.ACCESS;
    authEntry.userId = FakeOAuthRequest.REQUESTOR;
    authEntry.issueTime = new Date();
    authEntry.domain = DOMAIN;
    authEntry.container = CONTAINER;
    return authEntry;
  }

  private void expectConsumer() {
    EasyMock.expect(mockStore.getConsumer(
        EasyMock.eq(FakeOAuthRequest.CONSUMER_KEY))).
          andReturn(new OAuthConsumer(null, FakeOAuthRequest.CONSUMER_KEY,
              FakeOAuthRequest.CONSUMER_SECRET, new OAuthServiceProvider(null, null, null)))
        .anyTimes();
  }

  private void expectSecurityToken() {
    EasyMock.expect(mockStore.getSecurityTokenForConsumerRequest(
        EasyMock.eq(FakeOAuthRequest.CONSUMER_KEY), EasyMock.eq(FakeOAuthRequest.REQUESTOR))).
          andReturn(new AnonymousSecurityToken());
  }

  @Test
  public void testVerifyOAuthRequest() throws Exception {
    expectTokenEntry();
    expectConsumer();
    replay();
    HttpServletRequest request = formEncodedPost.sign(TOKEN,
        FakeOAuthRequest.OAuthParamLocation.URI_QUERY, FakeOAuthRequest.BodySigning.NONE);
    SecurityToken token = reqHandler.getSecurityTokenFromRequest(request);
    assertEquals(token.getViewerId(), FakeOAuthRequest.REQUESTOR);
    assertEquals(token.getAppId(), APP_ID);
    assertEquals(token.getDomain(), DOMAIN);
    assertEquals(token.getContainer(), CONTAINER);
    assertNotNull(token);
    assertTrue(token instanceof OAuthSecurityToken);
    verify();
  }

  @Test
  public void testVerifyGet() throws Exception {
    expectTokenEntry();
    expectConsumer();
    replay();
    FakeOAuthRequest get =
        new FakeOAuthRequest("GET", TEST_URL, null, null);
    FakeHttpServletRequest request = get.sign(TOKEN,
        FakeOAuthRequest.OAuthParamLocation.URI_QUERY, FakeOAuthRequest.BodySigning.NONE);
    assertNotNull(reqHandler.getSecurityTokenFromRequest(request));
  }

  @Test
  public void testVerifyGetSignatureInHeader() throws Exception {
    expectTokenEntry();
    expectConsumer();
    replay();
    FakeOAuthRequest get =
        new FakeOAuthRequest("GET", TEST_URL, null, null);
    FakeHttpServletRequest request = get.sign(TOKEN,
        FakeOAuthRequest.OAuthParamLocation.AUTH_HEADER, FakeOAuthRequest.BodySigning.NONE);
    assertNotNull(reqHandler.getSecurityTokenFromRequest(request));
  }

  @Test
  public void testVerifyRequestSignatureInBody() throws Exception {
    expectTokenEntry();
    expectConsumer();
    replay();
    HttpServletRequest request = formEncodedPost.sign(TOKEN,
        FakeOAuthRequest.OAuthParamLocation.POST_BODY, FakeOAuthRequest.BodySigning.NONE);
    SecurityToken token = reqHandler.getSecurityTokenFromRequest(request);
    assertNotNull(token);
    verify();
  }


  @Test
  public void testVerifyFailNoTokenEntry() throws Exception {
    expectTokenEntry(null);
    expectConsumer();
    replay();
    HttpServletRequest request = formEncodedPost.sign(TOKEN,
        FakeOAuthRequest.OAuthParamLocation.URI_QUERY, FakeOAuthRequest.BodySigning.NONE);
    try {
      reqHandler.getSecurityTokenFromRequest(request);
      fail("Expect failure as no token entry in store");
    } catch (AuthenticationHandler.InvalidAuthenticationException iae) {
      // Pass
    }
    verify();
  }

  @Test
  public void testVerifyFailTokenSecretMismatch() throws Exception {
    OAuthEntry authEntry = createOAuthEntry();
    authEntry.tokenSecret = "badsecret";
    expectTokenEntry(authEntry);
    expectConsumer();
    replay();
    HttpServletRequest request = formEncodedPost.sign(TOKEN,
        FakeOAuthRequest.OAuthParamLocation.URI_QUERY, FakeOAuthRequest.BodySigning.NONE);
    try {
      reqHandler.getSecurityTokenFromRequest(request);
      fail("Expect failure as token secrets mismatch");
    } catch (AuthenticationHandler.InvalidAuthenticationException iae) {
      // Pass
    }
    verify();
  }

  @Test
  public void testVerifyFailTokenIsRequest() throws Exception {
    OAuthEntry authEntry = createOAuthEntry();
    authEntry.type = OAuthEntry.Type.REQUEST;
    expectTokenEntry(authEntry);
    expectConsumer();
    replay();
    HttpServletRequest request = formEncodedPost.sign(TOKEN,
        FakeOAuthRequest.OAuthParamLocation.URI_QUERY, FakeOAuthRequest.BodySigning.NONE);
    try {
      reqHandler.getSecurityTokenFromRequest(request);
      fail("Expect failure as token is a request token not an access token");
    } catch (AuthenticationHandler.InvalidAuthenticationException iae) {
      // Pass
    }
    verify();
  }

  @Test
  public void testVerifyFailTokenIsExpired() throws Exception {
    OAuthEntry authEntry = createOAuthEntry();
    authEntry.issueTime = new Date(System.currentTimeMillis() - (OAuthEntry.ONE_YEAR + 1));
    authEntry.type = OAuthEntry.Type.REQUEST;
    expectTokenEntry(authEntry);
    expectConsumer();
    replay();
    HttpServletRequest request = formEncodedPost.sign(TOKEN,
        FakeOAuthRequest.OAuthParamLocation.URI_QUERY, FakeOAuthRequest.BodySigning.NONE);
    try {
      reqHandler.getSecurityTokenFromRequest(request);
      fail("Expect failure as token is expired");
    } catch (AuthenticationHandler.InvalidAuthenticationException iae) {
      // Pass
    }
    verify();
  }

  @Test
  public void testVerifyConsumerRequest() throws Exception {
    expectConsumer();
    expectSecurityToken();
    replay();
    HttpServletRequest request = formEncodedPost.sign(null,
        FakeOAuthRequest.OAuthParamLocation.URI_QUERY, FakeOAuthRequest.BodySigning.NONE);
    SecurityToken token = reqHandler.getSecurityTokenFromRequest(request);
    assertNotNull(token);
    assertFalse(token instanceof OAuthSecurityToken);
    verify();
  }

  @Test
  public void testVerifyConsumerGet() throws Exception {
    expectConsumer();
    expectSecurityToken();
    replay();
    FakeOAuthRequest get =
        new FakeOAuthRequest("GET", TEST_URL, null, null);
    FakeHttpServletRequest request = get.sign(null,
        FakeOAuthRequest.OAuthParamLocation.URI_QUERY, FakeOAuthRequest.BodySigning.NONE);
    assertNotNull(reqHandler.getSecurityTokenFromRequest(request));
  }

  @Test
  public void testVerifyConsumerGetSignatureInHeader() throws Exception {
    expectConsumer();
    expectSecurityToken();
    replay();
    FakeOAuthRequest get =
        new FakeOAuthRequest("GET", TEST_URL, null, null);
    FakeHttpServletRequest request = get.sign(null,
        FakeOAuthRequest.OAuthParamLocation.AUTH_HEADER, FakeOAuthRequest.BodySigning.NONE);
    assertNotNull(reqHandler.getSecurityTokenFromRequest(request));
  }

  @Test
  public void testVerifyConsumerRequestSignatureInAuthHeader() throws Exception {
    expectConsumer();
    expectSecurityToken();
    replay();
    HttpServletRequest request = formEncodedPost.sign(null,
        FakeOAuthRequest.OAuthParamLocation.AUTH_HEADER, FakeOAuthRequest.BodySigning.NONE);
    reqHandler.getSecurityTokenFromRequest(request);
    verify();
  }

  @Test
  public void testVerifyConsumerRequestSignatureInBody() throws Exception {
    expectConsumer();
    expectSecurityToken();
    replay();
    HttpServletRequest request = formEncodedPost.sign(null,
        FakeOAuthRequest.OAuthParamLocation.POST_BODY, FakeOAuthRequest.BodySigning.NONE);
    reqHandler.getSecurityTokenFromRequest(request);
    verify();
  }

  @Test
  public void testNoSignature() throws Exception {
    replay();
    FakeHttpServletRequest request = formEncodedPost.sign(null,
        FakeOAuthRequest.OAuthParamLocation.URI_QUERY, FakeOAuthRequest.BodySigning.NONE);
    // A request without a signature is not an OAuth request
    request.setParameter(OAuth.OAUTH_SIGNATURE, "");
    SecurityToken st = reqHandler.getSecurityTokenFromRequest(request);
    assertNull(st);
  }


  @Test
  public void testBodyHashSigning() throws Exception {
    expectConsumer();
    expectSecurityToken();
    replay();

    FakeHttpServletRequest request = nonFormEncodedPost.sign(null,
        FakeOAuthRequest.OAuthParamLocation.URI_QUERY, FakeOAuthRequest.BodySigning.HASH);
    assertNotNull(reqHandler.getSecurityTokenFromRequest(request));
  }

  @Test
  public void testLegacyBodySigning() throws Exception {
    expectConsumer();
    expectSecurityToken();
    replay();
    FakeHttpServletRequest request = nonFormEncodedPost.sign(null,
        FakeOAuthRequest.OAuthParamLocation.URI_QUERY, FakeOAuthRequest.BodySigning.LEGACY);
    assertNotNull(reqHandler.getSecurityTokenFromRequest(request));
  }

  @Test
  public void testLegacyBodySigningNotEnabled() throws Exception {
    reqHandler = new OAuthAuthenticationHandler(mockStore, false);
    expectConsumer();
    expectSecurityToken();
    replay();
    FakeHttpServletRequest request = nonFormEncodedPost.sign(null,
        FakeOAuthRequest.OAuthParamLocation.URI_QUERY, FakeOAuthRequest.BodySigning.LEGACY);
    try {
      reqHandler.getSecurityTokenFromRequest(request);
      fail("Legacy signing not enabled");
    } catch (AuthenticationHandler.InvalidAuthenticationException iae) {
      // Pass
    }
  }

  @Test
  public void testConsumerFailBodyHashSigningWithFormEncoding() throws Exception {
    replay();
    FakeOAuthRequest bodyHashPost =
        new FakeOAuthRequest("POST", TEST_URL, "a=b&c=d&oauth_body_hash=hash",
        OAuth.FORM_ENCODED);
    FakeHttpServletRequest request = bodyHashPost
        .sign(null, FakeOAuthRequest.OAuthParamLocation.URI_QUERY,
            FakeOAuthRequest.BodySigning.NONE);
    try {
      reqHandler.getSecurityTokenFromRequest(request);
      fail("Cant have body signing with form-encoded post bodies");
    } catch (AuthenticationHandler.InvalidAuthenticationException iae) {
      // Pass
    }
  }

  @Test
  public void testLegacyBodySigningJson() throws Exception {
    expectConsumer();
    expectSecurityToken();
    replay();
    FakeOAuthRequest jsonPost =
        new FakeOAuthRequest("POST", TEST_URL,
        "{a:b,b:'c=d&d==f&&g=y'}",
        "application/json");
    FakeHttpServletRequest request = jsonPost.sign(null,
        FakeOAuthRequest.OAuthParamLocation.URI_QUERY, FakeOAuthRequest.BodySigning.LEGACY);
    assertNotNull(reqHandler.getSecurityTokenFromRequest(request));
  }

  @Test
  public void testStashBody() throws Exception {
    FakeHttpServletRequest req = new FakeHttpServletRequest();
    String body = "BODY";
    req.setPostData(CharsetUtil.getUtf8Bytes(body));
    byte[] bytes = reqHandler.readBody(req);
    assertTrue(Arrays.equals(bytes, CharsetUtil.getUtf8Bytes(body)));
    assertEquals(req.getAttribute(AuthenticationHandler.STASHED_BODY), bytes);
  }

  @Test
  public void testBodySigning() throws Exception {
    FakeHttpServletRequest req = new FakeHttpServletRequest();
    req.setContentType("text/plain");
    String body = "BODY";
    req.setPostData(CharsetUtil.getUtf8Bytes(body));
    String hash = new String(Base64.encodeBase64(DigestUtils.sha(CharsetUtil.getUtf8Bytes(body))),
        "UTF-8");
    req.setParameter(OAuthAuthenticationHandler.OAUTH_BODY_HASH, hash);
    reqHandler.verifyBodyHash(req, hash);
  }

  @Test
  public void testFailBodySigning() throws Exception {
    FakeHttpServletRequest req = new FakeHttpServletRequest();
    req.setContentType("text/plain");
    String body = "BODY";
    req.setPostData(CharsetUtil.getUtf8Bytes(body));
    String hash = new String(Base64.encodeBase64(
        DigestUtils.sha(CharsetUtil.getUtf8Bytes("NOTBODY"))), "UTF-8");
    req.setParameter(OAuthAuthenticationHandler.OAUTH_BODY_HASH, hash);
    try {
      reqHandler.verifyBodyHash(req, hash);
      fail("Body verification should fail");
    } catch (AuthenticationHandler.InvalidAuthenticationException iae) {
      // Pass
    }
  }

  @Test
  public void testFailBodySigningWithFormEncoded() throws Exception {
    FakeHttpServletRequest req = new FakeHttpServletRequest();
    req.setContentType(OAuth.FORM_ENCODED);
    String body = "BODY";
    req.setPostData(CharsetUtil.getUtf8Bytes(body));
    String hash = new String(Base64.encodeBase64(DigestUtils.sha(CharsetUtil.getUtf8Bytes(body))),
        "UTF-8");
    req.setParameter(OAuthAuthenticationHandler.OAUTH_BODY_HASH, hash);
    try {
      reqHandler.verifyBodyHash(req, hash);
      fail("Body verification should fail");
    } catch (AuthenticationHandler.InvalidAuthenticationException iae) {
      // Pass
    }
  }

  @Test
  public void testFailBodyForGet() throws Exception {
    FakeHttpServletRequest req = new FakeHttpServletRequest();
    req.setContentType("text/plain");
    String body = "BODY";
    req.setPostData(CharsetUtil.getUtf8Bytes(body));
    req.setMethod("GET");
    String hash = new String(Base64.encodeBase64(DigestUtils.sha(CharsetUtil.getUtf8Bytes(body))),
        "UTF-8");
    req.setParameter(OAuthAuthenticationHandler.OAUTH_BODY_HASH, hash);
    try {
      reqHandler.verifyBodyHash(req, hash);
      fail("Body verification should fail");
    } catch (AuthenticationHandler.InvalidAuthenticationException iae) {
      // Pass
    }
  }

  @Test
  public void testFailBodyForHead() throws Exception {
    FakeHttpServletRequest req = new FakeHttpServletRequest();
    req.setContentType("text/plain");
    String body = "BODY";
    req.setPostData(CharsetUtil.getUtf8Bytes(body));
    req.setMethod("HEAD");
    String hash = new String(Base64.encodeBase64(DigestUtils.sha(CharsetUtil.getUtf8Bytes(body))),
        "UTF-8");
    req.setParameter(OAuthAuthenticationHandler.OAUTH_BODY_HASH, hash);
    try {
      reqHandler.verifyBodyHash(req, hash);
      fail("Body verification should fail");
    } catch (AuthenticationHandler.InvalidAuthenticationException iae) {
      // Pass
    }
  }
}
