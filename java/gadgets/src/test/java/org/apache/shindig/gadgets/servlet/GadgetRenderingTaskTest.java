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
package org.apache.shindig.gadgets.servlet;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthFetcher;
import org.apache.shindig.gadgets.oauth.OAuthRequestParams;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import org.easymock.EasyMock;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

// TODO: Migrate this to new Servlet testing setup.
public class GadgetRenderingTaskTest extends HttpTestFixture {

  final static Enumeration<String> EMPTY_PARAMS = new Enumeration<String>() {
    public boolean hasMoreElements() {
      return false;
    }
    public String nextElement() {
      return null;
    }
  };

  final static URI SPEC_URL = URI.create("http://example.org/gadget.xml");
  final static HttpRequest SPEC_REQUEST = new HttpRequest(SPEC_URL);
  final static String CONTENT = "Hello, world!";
  final static String ALT_CONTENT = "Goodbye, city.";
  final static String SPEC_XML
      = "<Module>" +
        "<ModulePrefs title='hello'/>" +
        "<Content type='html' quirks='false'>" + CONTENT + "</Content>" +
        "<Content type='html' view='quirks' quirks='true'/>" +
        "<Content type='html' view='ALIAS'>" + ALT_CONTENT + "</Content>" +
        "</Module>";
  final static String LIBS = "dummy:blah";
  
  final static String PRELOAD_XML =
      "<Module>" +
      "<ModulePrefs title='hello'>" +
      "<Preload authz='oauth' href='http://oauth.example.com'/>" +
      "</ModulePrefs>" +
      "<Content type='html' quirks='false'>" + CONTENT + "</Content>" +
      "<Content type='html' view='quirks' quirks='true'/>" +
      "<Content type='html' view='ALIAS'>" + ALT_CONTENT + "</Content>" +
      "</Module>";

  private ServletTestFixture fixture;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    fixture = new ServletTestFixture();
  }


  /**
   * Performs boilerplate operations to get basic gadgets rendered
   * @return Output of the rendering request
   * @throws Exception
   */
  private String parseBasicGadget(String view, String xml) throws Exception {
    expectParseRequestParams(view);
    expectFetchGadget(xml);
    expectLockedDomainCheck();
    expectWriteResponse();
    replay();
    fixture.replay();
    gadgetRenderer.process(fixture.request, fixture.recorder);
    verify();
    fixture.verify();
    return fixture.recorder.getResponseAsString();
  }

  private void expectParseRequestParams(String view) throws Exception {
    expect(fixture.request.getParameter("url")).andReturn(SPEC_URL.toString());
    expect(fixture.request.getParameter("view")).andReturn(view);
    expect(fixture.request.getParameterNames()).andReturn(EMPTY_PARAMS);
    expect(fixture.request.getParameter("container")).andReturn(null);
    expect(fixture.request.getHeader("Host")).andReturn("www.example.org");
    expect(fixture.request.getParameter("st")).andStubReturn("fake-token");
  }

  private void expectLockedDomainCheck() throws Exception {
    expect(lockedDomainService.gadgetCanRender(
        EasyMock.eq("www.example.org"),
        isA(Gadget.class),
        EasyMock.eq("default"))).andReturn(true);
  }

  private void expectFetchGadget(String xml) throws Exception {
    expect(fetcher.fetch(SPEC_REQUEST)).andReturn(new HttpResponse(xml));
  }

  private void expectWriteResponse() throws Exception {
    expect(fixture.request.getParameter("libs")).andReturn(LIBS);
  }

  public void testStandardsMode() throws Exception {
    String content = parseBasicGadget(GadgetSpec.DEFAULT_VIEW, SPEC_XML);
    assertTrue(content.contains(GadgetRenderingTask.STRICT_MODE_DOCTYPE));
  }

  public void testQuirksMode() throws Exception {
    String content = parseBasicGadget("quirks", SPEC_XML);
    assertTrue(!content.contains(GadgetRenderingTask.STRICT_MODE_DOCTYPE));
  }


  public void testContentRendered() throws Exception {
    String content = parseBasicGadget(GadgetSpec.DEFAULT_VIEW, SPEC_XML);
    assertTrue(content.contains(CONTENT));
  }

  @SuppressWarnings("unchecked")
  public void testForcedLibsIncluded() throws Exception {
    String jsLibs = "http://example.org/js/foo:bar.js";
    expect(urlGenerator.getBundledJsUrl(isA(Collection.class),
        isA(GadgetContext.class))).andReturn(jsLibs);
    String content = parseBasicGadget(GadgetSpec.DEFAULT_VIEW, SPEC_XML);
    assertTrue(content.contains("<script src=\"" + jsLibs + "\">"));
  }

  public void testViewAliases() throws Exception {
    JSONArray aliases = new JSONArray().put("ALIAS");
    expect(containerConfig.getJsonArray(ContainerConfig.DEFAULT_CONTAINER,
        "gadgets.features/views/dummy/aliases")).andReturn(aliases);

    String content = parseBasicGadget("dummy", SPEC_XML);

    assertTrue(content.contains(ALT_CONTENT));
  }
  
  public void testOAuthPreload_data() throws Exception {
    expectParseRequestParams(GadgetSpec.DEFAULT_VIEW);
    expectFetchGadget(PRELOAD_XML);
    expect(securityTokenDecoder.createToken(
        Collections.singletonMap(SecurityTokenDecoder.SECURITY_TOKEN_NAME, "fake-token"))).
        andStubReturn(mock(SecurityToken.class));
    OAuthFetcher oauthFetcher = mock(OAuthFetcher.class);
    expect(fetcherFactory.getOAuthFetcher(
        isA(SecurityToken.class), isA(OAuthRequestParams.class))).
        andReturn(oauthFetcher);

    expect(oauthFetcher.fetch(isA(HttpRequest.class))).
        andReturn(new HttpResponse("preloaded data"));

    expectLockedDomainCheck();
    expectWriteResponse();
    replay();
    fixture.replay();
    gadgetRenderer.process(fixture.request, fixture.recorder);
    verify();
    fixture.verify();
    String content = fixture.recorder.getResponseAsString();
    assertTrue(content.contains("preloaded data"));
  }
  
  public void testOAuthPreload_metadata() throws Exception {
    expectParseRequestParams(GadgetSpec.DEFAULT_VIEW);
    expectFetchGadget(PRELOAD_XML);
    expect(securityTokenDecoder.createToken(
        Collections.singletonMap(SecurityTokenDecoder.SECURITY_TOKEN_NAME, "fake-token"))).
        andStubReturn(mock(SecurityToken.class));
    OAuthFetcher oauthFetcher = mock(OAuthFetcher.class);
    expect(fetcherFactory.getOAuthFetcher(
        isA(SecurityToken.class), isA(OAuthRequestParams.class))).
        andReturn(oauthFetcher);

    HttpResponse resp = new HttpResponse(401, null, null);
    resp.getMetadata().put(OAuthFetcher.APPROVAL_URL, "approval please");
    resp.getMetadata().put(OAuthFetcher.CLIENT_STATE, "state blob");

    expect(oauthFetcher.fetch(isA(HttpRequest.class))).andReturn(resp);

    expectLockedDomainCheck();
    expectWriteResponse();
    replay();
    fixture.replay();
    gadgetRenderer.process(fixture.request, fixture.recorder);
    verify();
    fixture.verify();
    String content = fixture.recorder.getResponseAsString();
    assertTrue(content.contains("approval please"));
    assertTrue(content.contains("state blob"));
  }

  public void testLockedDomainFailure() throws Exception {
    expectParseRequestParams(GadgetSpec.DEFAULT_VIEW);
    expectFetchGadget(SPEC_XML);
    expectLockedDomainFailure();
    expectSendRedirect();
    replay();
    fixture.replay();
    gadgetRenderer.process(fixture.request, response);
    verify();
    fixture.verify();
  }

  private void expectLockedDomainFailure() {
    expect(lockedDomainService.gadgetCanRender(
        EasyMock.eq("www.example.org"),
        isA(Gadget.class),
        EasyMock.eq("default"))).andReturn(false);
    expect(fixture.request.getScheme()).andReturn("http");
    expect(fixture.request.getServletPath()).andReturn("/gadgets/ifr");
    expect(fixture.request.getQueryString()).andReturn("stuff=foo%20bar");
    expect(lockedDomainService.getLockedDomainForGadget(
        SPEC_URL.toString(), "default")).andReturn("locked.example.org");
  }

  private void expectSendRedirect() throws Exception {
    response.sendRedirect("http://locked.example.org/gadgets/ifr?stuff=foo%20bar");
    EasyMock.expectLastCall().once();
  }

  /**
   * Picks out the shindig.auth configuration object by looking for balanced
   * curly brackets.
   */
  private JSONObject parseShindigAuthConfig(String content) throws Exception {
    String startStr = "\"shindig.auth\":";
    int start = content.indexOf(startStr);
    if (start == -1) {
      return null;
    }
    start += startStr.length();
    char[] text = content.toCharArray();
    int bracketCount = 0;
    for (int i=start; i < text.length; ++i) {
      if (text[i] == '{') {
        ++bracketCount;
      } else if (text[i] == '}') {
        --bracketCount;
      }
      if (bracketCount == 0) {
        return new JSONObject(content.substring(start, i+1));
      }
    }
    return null;
  }

  public void testAuthTokenInjection_allparams() throws Exception {
    expect(fixture.request.getParameter("st")).andReturn("fake-token");
    expect(securityTokenDecoder.createToken(Collections.singletonMap(SecurityTokenDecoder.SECURITY_TOKEN_NAME, "fake-token"))).andReturn(
        new FakeGadgetToken().setUpdatedToken("updated-token")
        .setTrustedJson("{ \"foo\" : \"bar\" }"));
    String content = parseBasicGadget(GadgetSpec.DEFAULT_VIEW, SPEC_XML);
    JSONObject auth = parseShindigAuthConfig(content);
    assertEquals("updated-token", auth.getString("authToken"));
    assertEquals("{ \"foo\" : \"bar\" }", auth.getString("trustedJson"));
  }

  public void testAuthTokenInjection_none() throws Exception {
    expect(fixture.request.getParameter("st")).andReturn("fake-token");
    expect(securityTokenDecoder.createToken(Collections.singletonMap(SecurityTokenDecoder.SECURITY_TOKEN_NAME, "fake-token"))).andReturn(
        new FakeGadgetToken());
    String content = parseBasicGadget(GadgetSpec.DEFAULT_VIEW, SPEC_XML);
    JSONObject auth = parseShindigAuthConfig(content);
    assertEquals(0, auth.length());
  }

  public void testAuthTokenInjection_trustedJson() throws Exception {
    expect(fixture.request.getParameter("st")).andReturn("fake-token");
    expect(securityTokenDecoder.createToken(Collections.singletonMap(SecurityTokenDecoder.SECURITY_TOKEN_NAME, "fake-token"))).andReturn(
        new FakeGadgetToken().setTrustedJson("trusted"));
    String content = parseBasicGadget(GadgetSpec.DEFAULT_VIEW, SPEC_XML);
    JSONObject auth = parseShindigAuthConfig(content);
    assertEquals(1, auth.length());
    assertEquals("trusted", auth.getString("trustedJson"));
  }

  public void testAuthTokenInjection_updatedToken() throws Exception {
    expect(fixture.request.getParameter("st")).andReturn("fake-token");
    expect(securityTokenDecoder.createToken(Collections.singletonMap(SecurityTokenDecoder.SECURITY_TOKEN_NAME, "fake-token"))).andReturn(
        new FakeGadgetToken().setUpdatedToken("updated-token"));
    String content = parseBasicGadget(GadgetSpec.DEFAULT_VIEW, SPEC_XML);
    JSONObject auth = parseShindigAuthConfig(content);
    assertEquals(1, auth.length());
    assertEquals("updated-token", auth.getString("authToken"));
  }

  public void testRenderSetsProperCacheControlHeaders() throws Exception {
    parseBasicGadget(GadgetSpec.DEFAULT_VIEW, SPEC_XML);
    fixture.checkCacheControlHeaders(GadgetRenderingTask.DEFAULT_CACHE_TTL, true);
  }

  public void testRenderSetsLongLivedCacheControlHeadersWhenVParamIsSet() throws Exception {
    expect(fixture.request.getParameter("v")).andReturn("some value");
    parseBasicGadget(GadgetSpec.DEFAULT_VIEW, SPEC_XML);
    fixture.checkCacheControlHeaders(HttpUtil.DEFAULT_TTL, true);
  }

  public void testRenderSetsNoCacheHeadersWhenNoCacheParamIsSet() throws Exception {
    expect(fixture.request.getParameter("nocache")).andReturn("1");
    parseBasicGadget(GadgetSpec.DEFAULT_VIEW, SPEC_XML);
    fixture.checkCacheControlHeaders(0, true);
  }

  @SuppressWarnings("unchecked")
  public void testParseUrlGadget() throws Exception {
    String contentUrl = "http://www.example.org/content.php";
    String libsUrl = "&libs=";
    String jsUrl = "core.js?v=12345&container=apache&debug=0";
    String gadgetXml
        = "<Module>" +
          "<ModulePrefs title='hello'/>" +
          "<Content type='url' href='" + contentUrl + "'/>" +
          "</Module>";

    expect(fixture.request.getParameter("url")).andReturn(SPEC_URL.toString());
    expect(fixture.request.getParameter("view")).andReturn(GadgetSpec.DEFAULT_VIEW);
    expect(fixture.request.getParameterNames()).andReturn(EMPTY_PARAMS);
    expect(fixture.request.getParameter("container")).andReturn(null);
    expect(fetcher.fetch(SPEC_REQUEST)).andReturn(new HttpResponse(gadgetXml));
    expect(urlGenerator.getBundledJsParam(isA(Collection.class), isA(GadgetContext.class)))
        .andReturn(jsUrl);
    response.sendRedirect(contentUrl + '?' + libsUrl + Utf8UrlCoder.encode(jsUrl));
    expectLastCall().once();

    replay();
    fixture.replay();
    gadgetRenderer.process(fixture.request, response);
  }

  @SuppressWarnings("unchecked")
  public void testParseUrlGadgetWithQueryAndFragment() throws Exception {
    String contentUrl = "http://www.example.org/content.php";
    String contentQuery = "?foo=bar";
    String contentFragment = "#foo";
    String libsUrl = "&libs=";
    String jsUrl = "core.js?v=12345&container=apache&debug=0";
    String gadgetXml
        = "<Module>" +
          "<ModulePrefs title='hello'/>" +
          "<Content type='url' href='" + contentUrl + contentQuery + contentFragment + "'/>" +
          "</Module>";

    expect(fixture.request.getParameter("url")).andReturn(SPEC_URL.toString());
    expect(fixture.request.getParameter("view")).andReturn(GadgetSpec.DEFAULT_VIEW);
    expect(fixture.request.getParameterNames()).andReturn(EMPTY_PARAMS);
    expect(fixture.request.getParameter("container")).andReturn(null);
    expect(fetcher.fetch(SPEC_REQUEST)).andReturn(new HttpResponse(gadgetXml));
    expect(urlGenerator.getBundledJsParam(isA(Collection.class), isA(GadgetContext.class)))
        .andReturn(jsUrl);
    response.sendRedirect(contentUrl + contentQuery + libsUrl + Utf8UrlCoder.encode(jsUrl) +
                          contentFragment);
    expectLastCall().once();

    replay();
    fixture.replay();
    gadgetRenderer.process(fixture.request, response);
  }

  // TODO: Lots of ugly tests on html content.
}
