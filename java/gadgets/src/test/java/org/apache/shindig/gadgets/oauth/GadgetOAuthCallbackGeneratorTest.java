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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.reportMatcher;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.apache.shindig.auth.BasicSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.process.Processor;
import org.apache.shindig.gadgets.uri.OAuthUriManager;
import org.easymock.IArgumentMatcher;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;

public class GadgetOAuthCallbackGeneratorTest {

  private static final String MAKE_REQUEST_URL = "http://renderinghost/gadgets/makeRequest";
  private static final Uri DEST_URL = Uri.parse("http://www.example.com/destination");

  private IMocksControl control;
  private Processor processor;
  private LockedDomainService lockedDomainService;
  private OAuthUriManager oauthUriManager;
  private BlobCrypter stateCrypter;
  private SecurityToken securityToken;
  private Gadget gadget;
  private OAuthFetcherConfig fetcherConfig;
  private OAuthResponseParams responseParams;

  @Before
  public void setUp() throws Exception {
    control = EasyMock.createNiceControl();
    processor = control.createMock(Processor.class);
    lockedDomainService = control.createMock(LockedDomainService.class);
    oauthUriManager = control.createMock(OAuthUriManager.class);
    stateCrypter = new BasicBlobCrypter("1111111111111111111".getBytes());
    securityToken = new BasicSecurityToken("viewer", "viewer", "app", "container.com",
        "gadget", "0", "default", MAKE_REQUEST_URL, null);
    gadget = control.createMock(Gadget.class);
    fetcherConfig = new OAuthFetcherConfig(null, null, null, null, false);
    responseParams = new OAuthResponseParams(null, null, null);
  }

  private GadgetOAuthCallbackGenerator getGenerator() {
    return new GadgetOAuthCallbackGenerator(processor, lockedDomainService, oauthUriManager,
        stateCrypter);
  }

  @Test
  public void testWrongDomain() throws Exception {
    HttpRequest request = new HttpRequest(DEST_URL);
    request.setSecurityToken(securityToken);
    request.setOAuthArguments(new OAuthArguments());
    expect(processor.process(eqContext(securityToken, request.getOAuthArguments())))
        .andReturn(gadget);
    expect(lockedDomainService.isGadgetValidForHost("renderinghost", gadget, "default"))
        .andReturn(false);

    control.replay();

    try {
      getGenerator().generateCallback(fetcherConfig, "base", request, responseParams);
      fail("Should have thrown");
    } catch (OAuthRequestException e) {
      assertEquals(OAuthError.UNKNOWN_PROBLEM.name(), e.getError());
    }

    control.verify();
  }

  @Test
  public void testBadGadget() throws Exception {
    HttpRequest request = new HttpRequest(DEST_URL);
    request.setSecurityToken(securityToken);
    request.setOAuthArguments(new OAuthArguments());
    expect(processor.process(eqContext(securityToken, request.getOAuthArguments())))
        .andThrow(new ProcessingException("doh", HttpServletResponse.SC_BAD_REQUEST));

    control.replay();

    try {
      getGenerator().generateCallback(fetcherConfig, "base", request, responseParams);
      fail("Should have thrown");
    } catch (OAuthRequestException e) {
      assertEquals(OAuthError.UNKNOWN_PROBLEM.name(), e.getError());
    }

    control.verify();
  }

  @Test
  public void testGenerateUrl_schemeRelative() throws Exception {
    HttpRequest request = new HttpRequest(DEST_URL);
    request.setSecurityToken(securityToken);
    request.setOAuthArguments(new OAuthArguments());
    expect(processor.process(eqContext(securityToken, request.getOAuthArguments())))
        .andReturn(gadget);
    expect(lockedDomainService.isGadgetValidForHost("renderinghost", gadget, "default"))
        .andReturn(true);
    expect(oauthUriManager.makeOAuthCallbackUri("default", "renderinghost"))
        .andReturn(Uri.parse("//renderinghost/final/callback"));

    control.replay();

    String callback = getGenerator().generateCallback(fetcherConfig, "http://base/basecallback",
        request, responseParams);
    Uri callbackUri = Uri.parse(callback);
    assertEquals("http", callbackUri.getScheme());
    assertEquals("base", callbackUri.getAuthority());
    assertEquals("/basecallback", callbackUri.getPath());
    OAuthCallbackState state = new OAuthCallbackState(stateCrypter,
        callbackUri.getQueryParameter("cs"));
    assertEquals("http://renderinghost/final/callback", state.getRealCallbackUrl());

    control.verify();
  }

  @Test
  public void testGenerateUrl_absolute() throws Exception {
    HttpRequest request = new HttpRequest(DEST_URL);
    request.setSecurityToken(securityToken);
    request.setOAuthArguments(new OAuthArguments());
    expect(processor.process(eqContext(securityToken, request.getOAuthArguments())))
        .andReturn(gadget);
    expect(lockedDomainService.isGadgetValidForHost("renderinghost", gadget, "default"))
        .andReturn(true);
    expect(oauthUriManager.makeOAuthCallbackUri("default", "renderinghost"))
        .andReturn(Uri.parse("https://renderinghost/final/callback"));

    control.replay();

    String callback = getGenerator().generateCallback(fetcherConfig, "http://base/basecallback",
        request, responseParams);
    Uri callbackUri = Uri.parse(callback);
    assertEquals("http", callbackUri.getScheme());
    assertEquals("base", callbackUri.getAuthority());
    assertEquals("/basecallback", callbackUri.getPath());
    OAuthCallbackState state = new OAuthCallbackState(stateCrypter,
        callbackUri.getQueryParameter("cs"));
    assertEquals("https://renderinghost/final/callback", state.getRealCallbackUrl());

    control.verify();
  }

  @Test
  public void testGenerateUrl_otherQueryParams() throws Exception {
    HttpRequest request = new HttpRequest(DEST_URL);
    request.setSecurityToken(securityToken);
    request.setOAuthArguments(new OAuthArguments());
    expect(processor.process(eqContext(securityToken, request.getOAuthArguments())))
        .andReturn(gadget);
    expect(lockedDomainService.isGadgetValidForHost("renderinghost", gadget, "default"))
        .andReturn(true);
    expect(oauthUriManager.makeOAuthCallbackUri("default", "renderinghost"))
        .andReturn(Uri.parse("https://renderinghost/final/callback"));

    control.replay();

    String callback = getGenerator().generateCallback(fetcherConfig,
        "http://base/basecallback?foo=bar%20baz", request, responseParams);
    Uri callbackUri = Uri.parse(callback);
    assertEquals("http", callbackUri.getScheme());
    assertEquals("base", callbackUri.getAuthority());
    assertEquals("/basecallback", callbackUri.getPath());
    assertEquals("bar baz", callbackUri.getQueryParameter("foo"));
    OAuthCallbackState state = new OAuthCallbackState(stateCrypter,
        callbackUri.getQueryParameter("cs"));
    assertEquals("https://renderinghost/final/callback", state.getRealCallbackUrl());

    control.verify();
  }

  @Test
  public void testGenerateUrl_noGadgetDomainCallback() throws Exception {
    HttpRequest request = new HttpRequest(DEST_URL);
    request.setSecurityToken(securityToken);
    request.setOAuthArguments(new OAuthArguments());
    expect(processor.process(eqContext(securityToken, request.getOAuthArguments())))
        .andReturn(gadget);
    expect(lockedDomainService.isGadgetValidForHost("renderinghost", gadget, "default"))
        .andReturn(true);
    expect(oauthUriManager.makeOAuthCallbackUri("default", "renderinghost"))
        .andReturn(null);

    control.replay();

    assertNull(getGenerator().generateCallback(fetcherConfig,
        "http://base/basecallback?foo=bar%20baz", request, responseParams));

    control.verify();
  }

  private GadgetContext eqContext(SecurityToken securityToken, OAuthArguments arguments) {
    reportMatcher(new GadgetContextMatcher(securityToken, arguments));
    return null;
  }

  private static class GadgetContextMatcher implements IArgumentMatcher {
    private final SecurityToken securityToken;
    private final OAuthArguments arguments;

    public GadgetContextMatcher(SecurityToken securityToken, OAuthArguments arguments) {
      this.securityToken = securityToken;
      this.arguments = arguments;
    }

    public boolean matches(Object argument) {
      if (!(argument instanceof OAuthGadgetContext)) {
        return false;
      }
      OAuthGadgetContext context = (OAuthGadgetContext) argument;
      return (securityToken == context.getToken() &&
          arguments.getBypassSpecCache() == context.getIgnoreCache());
  }

    public void appendTo(StringBuffer buffer) {
      buffer.append("GadgetContextMatcher(").append(securityToken).append(", ").append(arguments).append(')');
    }
  }
}
