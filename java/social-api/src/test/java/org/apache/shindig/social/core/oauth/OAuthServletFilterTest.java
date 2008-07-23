/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social.core.oauth;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.crypto.Crypto;
import org.apache.shindig.social.EasyMockTestCase;
import org.apache.shindig.social.core.oauth.AuthenticationServletFilter;
import org.apache.shindig.social.core.oauth.DelegatedPrincipal;
import org.apache.shindig.social.core.oauth.RequestorIdPrincipal;
import org.apache.shindig.social.opensocial.oauth.OAuthConsumerStore;
import org.apache.shindig.social.opensocial.oauth.OAuthPrincipal;
import org.apache.shindig.social.opensocial.oauth.OAuthTokenPrincipalMapper;
import org.apache.shindig.social.opensocial.oauth.OAuthTokenStore;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthServiceProvider;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OAuthServletFilterTest extends EasyMockTestCase {

  private AuthenticationServletFilter filter;
  private String url;
  private String consumerKey;
  private String consumerSecret;
  private String accessToken;
  private String tokenSecret;

  private OAuthValidator validator;
  private OAuthTokenStore tokenStore;
  private OAuthConsumerStore consumerStore;
  private OAuthTokenPrincipalMapper tokenMapper;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    filter = new AuthenticationServletFilter();

    validator = mock(OAuthValidator.class);
    tokenStore = mock(OAuthTokenStore.class);
    consumerStore = mock(OAuthConsumerStore.class);
    tokenMapper = mock(OAuthTokenPrincipalMapper.class);

    filter.setOAuthUtils(validator, tokenStore, consumerStore, tokenMapper);

    url = "http://www.foo.com/bar.xml";
    consumerKey = "consumerKey";
    consumerSecret = "consumerSecret";

    accessToken = "accessToken";
    tokenSecret = "tokenSecret";
  }

  private OAuthAccessor createAccessor() throws Exception {
    OAuthServiceProvider provider = new OAuthServiceProvider("", "", "");

    OAuthConsumer consumer = new OAuthConsumer("", consumerKey, consumerSecret,
        provider);

    OAuthAccessor accessor = new OAuthAccessor(consumer);

    accessor.accessToken = accessToken;
    accessor.tokenSecret = tokenSecret;

    return accessor;
  }

  private OAuthMessage createOAuthMessage(
      Collection<? extends Map.Entry<String, String>> params) throws Exception {

    if (params == null) {
      params = new ArrayList<Map.Entry<String,String>>();
    }

    OAuthAccessor accessor = createAccessor();

    OAuthMessage message = new OAuthMessage("GET", url, params);
    message.addParameter(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
    message.addParameter(OAuth.OAUTH_CONSUMER_KEY, consumerKey);
    message.addParameter(OAuth.OAUTH_TOKEN, accessToken);

    String nonce = Long.toHexString(Crypto.rand.nextLong());
    message.addParameter(OAuth.OAUTH_NONCE, nonce);

    String timestamp = Long.toString(System.currentTimeMillis()/1000L);
    message.addParameter(OAuth.OAUTH_TIMESTAMP, timestamp);

    message.sign(accessor);

    return message;
  }

  private HttpServletRequest createServletRequest(
      Collection<? extends Map.Entry<String, String>> params) throws Exception {

    OAuthMessage message = createOAuthMessage(params);

    HttpServletRequest request = mock(HttpServletRequest.class);

    HashMap<String, String[]> map = new HashMap<String, String[]>();
    for (Map.Entry<String, String> entry : message.getParameters()) {
      map.put(entry.getKey(), new String[] { entry.getValue() });
    }

    expect(request.getRequestURL())
        .andStubReturn(new StringBuffer(message.URL));
    expect(request.getParameterMap()).andStubReturn(map);

    return request;
  }

  private AuthenticationServletFilter createFilterForHandleRequest() throws Exception {

    Method requestUsesSecurityToken = AuthenticationServletFilter.class
        .getDeclaredMethod("requestUsesSecurityToken", HttpServletRequest.class);

    Method handleSecurityTokenRequest = AuthenticationServletFilter.class
        .getDeclaredMethod("handleSecurityTokenRequest", HttpServletRequest.class);

    Method handleConsumerRequest = AuthenticationServletFilter.class
        .getDeclaredMethod("handleConsumerRequest", HttpServletRequest.class,
            OAuthMessage.class);

    Method handleFullOAuth = AuthenticationServletFilter.class
        .getDeclaredMethod("handleFullOAuth", HttpServletRequest.class,
            OAuthMessage.class);

    Method getOAuthMessageFromRequest = AuthenticationServletFilter.class
        .getDeclaredMethod("getOAuthMessageFromRequest", HttpServletRequest.class);

    return mock(AuthenticationServletFilter.class, new Method[] {
        requestUsesSecurityToken,
        handleSecurityTokenRequest,
        handleConsumerRequest,
        handleFullOAuth,
        getOAuthMessageFromRequest
    });
  }

  private AuthenticationServletFilter createFilterForDoFilter() throws Exception {
    Method handleRequest = AuthenticationServletFilter.class
        .getDeclaredMethod("handleRequest", HttpServletRequest.class);

    Method handleException = AuthenticationServletFilter.class
        .getDeclaredMethod("handleException", HttpServletResponse.class,
            OAuthProblemException.class);

    return mock(AuthenticationServletFilter.class, new Method[] {
        handleRequest,
        handleException,
    });
  }

  public void testDoFilter_authSucceeds() throws Exception {
    filter = createFilterForDoFilter();
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletRequest wrappedRequest = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    expect(filter.handleRequest(request)).andReturn(wrappedRequest);
    chain.doFilter(wrappedRequest, response);
    expectLastCall().once();

    replay();

    filter.doFilter(request, response, chain);

    verify();
  }

  public void testDoFilter_authFails() throws Exception {
    filter = createFilterForDoFilter();
    HttpServletRequest request = mock(HttpServletRequest.class);
    OAuthProblemException problemException = new OAuthProblemException("foo");
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    expect(filter.handleRequest(request)).andThrow(problemException);
    filter.handleException(response, problemException);
    expectLastCall().once();

    replay();

    filter.doFilter(request, response, chain);

    verify();
  }

  public void testHandleRequest_securityToken() throws Exception {
    filter = createFilterForHandleRequest();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletRequest result = mock(HttpServletRequest.class);

    expect(filter.requestUsesSecurityToken(request)).andReturn(true);
    expect(filter.handleSecurityTokenRequest(request)).andReturn(result);

    replay();

    assertSame(result, filter.handleRequest(request));

    verify();
  }

  public void testHandleRequest_consumerRequest() throws Exception {
    filter = createFilterForHandleRequest();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletRequest result = mock(HttpServletRequest.class);
    OAuthMessage message = mock(OAuthMessage.class);

    expect(filter.requestUsesSecurityToken(request)).andReturn(false);
    expect(filter.getOAuthMessageFromRequest(request)).andReturn(message);
    message.requireParameters(OAuth.OAUTH_CONSUMER_KEY, OAuth.OAUTH_SIGNATURE);
    expectLastCall();

    expect(message.getToken()).andReturn(null);

    expect(filter.handleConsumerRequest(request, message)).andReturn(result);

    replay();

    assertSame(result, filter.handleRequest(request));

    verify();
  }

  public void testHandleRequest_fullOAuth() throws Exception {
    filter = createFilterForHandleRequest();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletRequest result = mock(HttpServletRequest.class);
    OAuthMessage message = mock(OAuthMessage.class);

    expect(filter.requestUsesSecurityToken(request)).andReturn(false);
    expect(filter.getOAuthMessageFromRequest(request)).andReturn(message);
    message.requireParameters(OAuth.OAUTH_CONSUMER_KEY, OAuth.OAUTH_SIGNATURE);
    expectLastCall();

    expect(message.getToken()).andReturn("token");

    expect(filter.handleFullOAuth(request, message)).andReturn(result);

    replay();

    assertSame(result, filter.handleRequest(request));

    verify();
  }

  public void testGetRequestorId_present() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);

    expect(request.getParameter(AuthenticationServletFilter.REQUESTOR_ID_PARAM))
        .andReturn("abc");

    replay();

    assertSame("abc", filter.getRequestorId(request));

    verify();
  }

  public void testGetRequestorId_absent() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);

    expect(request.getParameter(AuthenticationServletFilter.REQUESTOR_ID_PARAM))
        .andReturn(null);

    replay();

    assertNull(filter.getRequestorId(request));

    verify();
  }

  public void testHandleRequestorIdRequest() throws Exception {
    HttpServletRequest request = createServletRequest(null);
    OAuthMessage message = createOAuthMessage(null);
    OAuthAccessor accessor = createAccessor();
    DelegatedPrincipal principal = new RequestorIdPrincipal("john doe", message.getConsumerKey());

    // set a real validator
    filter.setOAuthUtils(new SimpleOAuthValidator(), tokenStore, consumerStore,
        tokenMapper);

    expect(consumerStore.getAccessor(consumerKey)).andReturn(accessor);
    expect(request.getParameter("xoauth_requestor_id")).andReturn("john doe");

    replay();

    HttpServletRequest result = filter.handleRequestorIdRequest(request, message);

    verify();

    assertEquals(principal.getName(), result.getUserPrincipal().getName());
    assertEquals("OAuth-ConsumerRequest", result.getAuthType());
  }

  public void testGetSecurityToken_present() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);

    SecurityToken token = mock(SecurityToken.class);
    SecurityTokenDecoder decoder = mock(SecurityTokenDecoder.class);

    filter.setSecurityTokenDecoder(decoder);

    expect(request.getParameter(AuthenticationServletFilter.SECURITY_TOKEN_PARAM))
        .andReturn("abc");
    expect(decoder.createToken(
        Collections.singletonMap(SecurityTokenDecoder.SECURITY_TOKEN_NAME,
            "abc"))).andReturn(token);

    replay();

    assertSame(token, filter.getSecurityToken(request));

    verify();
  }

  public void testGetSecurityToken_absent() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);

    SecurityTokenDecoder decoder = mock(SecurityTokenDecoder.class);

    filter.setSecurityTokenDecoder(decoder);

    expect(request.getParameter(AuthenticationServletFilter.SECURITY_TOKEN_PARAM))
        .andReturn(null);

    replay();

    assertNull(filter.getSecurityToken(request));

    verify();
  }

  public void testHandleFullOAuth_success() throws Exception {
    HttpServletRequest request = createServletRequest(null);
    OAuthMessage message = createOAuthMessage(null);
    OAuthAccessor accessor = createAccessor();

    // set a real validator
    filter.setOAuthUtils(new SimpleOAuthValidator(), tokenStore, consumerStore,
        tokenMapper);

    OAuthPrincipal principal = new OAuthPrincipal(message, "john.doe");

    expect(tokenStore.getAccessor(accessToken, consumerKey))
        .andReturn(accessor);
    expect(tokenMapper.getPrincipalForToken(message))
        .andReturn(principal);

    replay();

    HttpServletRequest result = filter.handleFullOAuth(request, message);

    verify();

    assertSame(principal, result.getUserPrincipal());
    assertEquals("OAuth", result.getAuthType());
  }

  public void testHandleFullOAuth_failure() throws Exception {
    HttpServletRequest request = createServletRequest(null);
    OAuthMessage message = createOAuthMessage(null);
    OAuthAccessor accessor = createAccessor();

    // set a real validator
    filter.setOAuthUtils(new SimpleOAuthValidator(), tokenStore, consumerStore,
        tokenMapper);

    // mess up the signature...
    message.addParameter("oauth_some_other_parameter", "xyz");

    expect(tokenStore.getAccessor(accessToken, consumerKey))
        .andReturn(accessor);

    replay();

    try {
      filter.handleFullOAuth(request, message);
      fail("expected OAuthProblemException, but didn't get it");
    } catch (OAuthProblemException e) {
      assertEquals(OAuth.Problems.SIGNATURE_INVALID, e.getProblem());
    }

    verify();
  }

  public void testValidateMessage() throws Exception {

    OAuthMessage message = mock(OAuthMessage.class);
    OAuthAccessor accessor = mock(OAuthAccessor.class);

    validator.validateMessage(message, accessor);
    expectLastCall();

    replay();

    filter.validateMessage(message, accessor);

    verify();
  }

  public void testGetOAuthMessageFromRequest() throws Exception {

    HttpServletRequest request = createServletRequest(null);

    replay();

    OAuthMessage message = filter.getOAuthMessageFromRequest(request);

    verify();

    assertEquals(url, message.URL);
    assertEquals(consumerKey, message.getConsumerKey());
    assertEquals(accessToken, message.getToken());
  }
}
