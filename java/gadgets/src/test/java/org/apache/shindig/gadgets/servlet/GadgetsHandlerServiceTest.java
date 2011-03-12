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
import static org.easymock.EasyMock.capture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.apache.commons.codec.binary.Base64;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.FakeTimeSource;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.GadgetException.Code;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.ProxyUriManager.ProxyUri;
import org.apache.shindig.protocol.conversion.BeanDelegator;
import org.apache.shindig.protocol.conversion.BeanFilter;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class GadgetsHandlerServiceTest extends EasyMockTestCase {

  private static final String TOKEN = "<token data>";
  private static final String OWNER = "<owner>";
  private static final String VIEWER = "<viewer>";
  private static final String CONTAINER = "container";
  private static final Long CURRENT_TIME_MS = 123L;
  private static final Long SPEC_REFRESH_INTERVAL_MS = 456L;
  private static final Long METADATA_EXPIRY_TIME_MS = CURRENT_TIME_MS + SPEC_REFRESH_INTERVAL_MS;
  private static final Long TOKEN_EXPIRY_TIME_MS = CURRENT_TIME_MS + 789L;
  private static final Uri RESOURCE = Uri.parse("http://example.com/data");
  private static final String FALLBACK = "http://example.com/data2";

  private final BeanDelegator delegator = new BeanDelegator(
    GadgetsHandlerService.API_CLASSES, GadgetsHandlerService.ENUM_CONVERSION_MAP);

  private final FakeTimeSource timeSource = new FakeTimeSource(CURRENT_TIME_MS);
  private final FeatureRegistry mockRegistry = mock(FeatureRegistry.class);
  private final FakeProcessor processor = new FakeProcessor(mockRegistry);
  private final FakeIframeUriManager urlGenerator = new FakeIframeUriManager();
  private final ProxyUriManager proxyUriManager = mock(ProxyUriManager.class);
  private final JsUriManager jsUriManager = mock(JsUriManager.class);
  private final ProxyHandler proxyHandler = mock(ProxyHandler.class);
  private final CajaContentRewriter cajaContentRewriter = mock(CajaContentRewriter.class);
  private final JsHandler jsHandler = mock(JsHandler.class);

  private FakeSecurityTokenCodec tokenCodec;
  private GadgetsHandlerService gadgetHandler;

  @Before
  public void setUp() {
    tokenCodec = new FakeSecurityTokenCodec();
    gadgetHandler = new GadgetsHandlerService(timeSource, processor, urlGenerator,
        tokenCodec, proxyUriManager, jsUriManager, proxyHandler, jsHandler,
        SPEC_REFRESH_INTERVAL_MS, new BeanFilter(), cajaContentRewriter);
  }

  // Next test verify that the API data classes are configured correctly.
  // The mapping is done using reflection in runtime, so this test verify mapping is complete
  // this test will prevent from not intended change to the API.
  // DO NOT REMOVE TEST
  @Test
  public void testHandlerDataDelegation() throws Exception {
    delegator.validate();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetMetadata() throws Exception {
    GadgetsHandlerApi.MetadataRequest request = createMetadataRequest(
        FakeProcessor.SPEC_URL, CONTAINER, "default",
        createAuthContext(null, null), ImmutableList.of("*"));
    EasyMock.expect(mockRegistry.getFeatures(EasyMock.isA(List.class)))
        .andReturn(Lists.newArrayList("auth-refresh"));
    replay();
    GadgetsHandlerApi.MetadataResponse response =
        gadgetHandler.getMetadata(request);
    assertEquals(FakeIframeUriManager.DEFAULT_IFRAME_URI.toString(), response.getIframeUrl());
    assertTrue(response.getNeedsTokenRefresh());
    assertEquals(1, response.getViews().size());
    assertEquals(FakeProcessor.SPEC_TITLE, response.getModulePrefs().getTitle());
    assertEquals(FakeProcessor.LINK_HREF,
        response.getModulePrefs().getLinks().get(FakeProcessor.LINK_REL).getHref().toString());
    assertEquals(FakeProcessor.LINK_REL,
        response.getModulePrefs().getLinks().get(FakeProcessor.LINK_REL).getRel());
    assertEquals(1, response.getUserPrefs().size());
    assertEquals("up_one", response.getUserPrefs().get("up_one").getDisplayName());
    assertEquals(4, response.getUserPrefs().get("up_one").getOrderedEnumValues().size());
    assertEquals(CURRENT_TIME_MS, response.getResponseTimeMs());
    assertEquals(METADATA_EXPIRY_TIME_MS, response.getExpireTimeMs());
    verify();
  }

  @Test
  public void testGetMetadataOnlyView() throws Exception {
    GadgetsHandlerApi.MetadataRequest request = createMetadataRequest(
        FakeProcessor.SPEC_URL, CONTAINER, null,
        createAuthContext(null, null), ImmutableList.of("views.*"));
    replay();
    GadgetsHandlerApi.MetadataResponse response =
        gadgetHandler.getMetadata(request);
    assertNull(response.getIframeUrl());
    assertNull(response.getUserPrefs());
    assertNull(response.getModulePrefs());
    assertNull(response.getUrl());
    assertEquals(1, response.getViews().size());
    verify();
  }

  @Test(expected = ProcessingException.class)
  public void testGetMetadataNoView() throws Exception {
    GadgetsHandlerApi.MetadataRequest request = createMetadataRequest(
        FakeProcessor.SPEC_URL3, CONTAINER, "invalid_view",
        createAuthContext(null, null), ImmutableList.of("*"));
    replay();
    GadgetsHandlerApi.MetadataResponse response =
        gadgetHandler.getMetadata(request);
  }

  @Test(expected = ProcessingException.class)
  public void testGetMetadataNoContainer() throws Exception {
    GadgetsHandlerApi.MetadataRequest request = createMetadataRequest(
        FakeProcessor.SPEC_URL, null, null,
        createAuthContext(null, null), ImmutableList.of("*"));
    replay();
    GadgetsHandlerApi.BaseResponse response = gadgetHandler.getMetadata(request);
  }

  @Test(expected = ProcessingException.class)
  public void testGetMetadataNoUrl() throws Exception {
    GadgetsHandlerApi.MetadataRequest request = createMetadataRequest(
        null, CONTAINER, null,
        createAuthContext(null, null), ImmutableList.of("*"));
    replay();
    GadgetsHandlerApi.MetadataResponse response =
        gadgetHandler.getMetadata(request);
  }

  @Test(expected = ProcessingException.class)
  public void testGetMetadataNoFields() throws Exception {
    GadgetsHandlerApi.MetadataRequest request = createMetadataRequest(
        FakeProcessor.SPEC_URL, CONTAINER, null,
        createAuthContext(null, null), null);
    replay();
    GadgetsHandlerApi.MetadataResponse response = gadgetHandler.getMetadata(request);
  }

  @Test(expected = ProcessingException.class)
  public void testGetMetadataBadGadget() throws Exception {
    GadgetsHandlerApi.MetadataRequest request = createMetadataRequest(
        Uri.parse("unknown"), CONTAINER, null,
        createAuthContext(null, null), null);
    replay();
    GadgetsHandlerApi.MetadataResponse response = gadgetHandler.getMetadata(request);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetMetadataNoToken() throws Exception {
    GadgetsHandlerApi.MetadataRequest request = createMetadataRequest(
        FakeProcessor.SPEC_URL, CONTAINER, "default", null, ImmutableList.of("*"));
    EasyMock.expect(mockRegistry.getFeatures(EasyMock.isA(List.class)))
        .andReturn(Lists.newArrayList("auth-refresh"));
    replay();
    GadgetsHandlerApi.MetadataResponse response = gadgetHandler.getMetadata(request);
    assertEquals(FakeIframeUriManager.DEFAULT_IFRAME_URI.toString(), response.getIframeUrl());
    verify();
  }

  @Test
  public void testGetToken() throws Exception {
    GadgetsHandlerApi.TokenRequest request = createTokenRequest(
        FakeProcessor.SPEC_URL, CONTAINER,
        createAuthContext(OWNER, VIEWER), ImmutableList.of("*"));
    replay();
    tokenCodec.encodedToken = TOKEN;
    tokenCodec.tokenExpiryTimeMs = TOKEN_EXPIRY_TIME_MS;
    GadgetsHandlerApi.TokenResponse response = gadgetHandler.getToken(request);
    assertEquals(TOKEN, response.getToken());
    assertEquals(CURRENT_TIME_MS, response.getResponseTimeMs());
    assertEquals(TOKEN_EXPIRY_TIME_MS, response.getExpireTimeMs());
    assertEquals(OWNER, tokenCodec.authContext.getOwnerId());
    assertEquals(VIEWER, tokenCodec.authContext.getViewerId());
    assertEquals(CONTAINER, tokenCodec.authContext.getContainer());
    verify();
  }

  @Test(expected = ProcessingException.class)
  public void testGetTokenNoContainer() throws Exception {
    GadgetsHandlerApi.TokenRequest request = createTokenRequest(
        FakeProcessor.SPEC_URL, null,
        createAuthContext(OWNER, VIEWER), ImmutableList.of("*"));
    replay();
    GadgetsHandlerApi.TokenResponse response = gadgetHandler.getToken(request);
  }

  @Test(expected = ProcessingException.class)
  public void testGetTokenNoUrl() throws Exception {
    GadgetsHandlerApi.TokenRequest request = createTokenRequest(
        null, CONTAINER,
        createAuthContext(OWNER, VIEWER), ImmutableList.of("*"));
    replay();
    GadgetsHandlerApi.TokenResponse response = gadgetHandler.getToken(request);
  }

  @Test(expected = ProcessingException.class)
  public void testGetTokenNoFields() throws Exception {
    GadgetsHandlerApi.TokenRequest request = createTokenRequest(
        FakeProcessor.SPEC_URL, CONTAINER,
        createAuthContext(OWNER, VIEWER), null);
    replay();
    GadgetsHandlerApi.TokenResponse response = gadgetHandler.getToken(request);
  }

  @Test(expected = SecurityTokenException.class)
  public void testGetTokenException() throws Exception {
    GadgetsHandlerApi.TokenRequest request = createTokenRequest(
        FakeProcessor.SPEC_URL, CONTAINER,
        createAuthContext(OWNER, VIEWER), ImmutableList.of("*"));
    replay();
    tokenCodec.exc = new SecurityTokenException("bad data");
    GadgetsHandlerApi.TokenResponse response = gadgetHandler.getToken(request);
  }

  @Test
  public void testGetTokenNoToken() throws Exception {
    GadgetsHandlerApi.TokenRequest request = createTokenRequest(
        FakeProcessor.SPEC_URL, CONTAINER,
        null, ImmutableList.of("*"));
    replay();
    tokenCodec.encodedToken = TOKEN;
    GadgetsHandlerApi.TokenResponse response = gadgetHandler.getToken(request);
    assertEquals(TOKEN, response.getToken());
    assertNull(CONTAINER, tokenCodec.authContext);
    verify();
  }

  @Test
  public void testCreateJsResponse() throws Exception {
    Uri jsUri = Uri.parse("http://www.shindig.com/js");
    String content = "content";
    GadgetsHandlerApi.JsResponse jsResponse =
        gadgetHandler.createJsResponse(null, jsUri, content, ImmutableSet.of("*"), null);
    BeanDelegator.validateDelegator(jsResponse);
  }

  @Test
  public void testGetJsUri() throws Exception {
    List<String> fields = ImmutableList.of("jsurl");
    List<String> features = ImmutableList.of("rpc");
    Uri resUri = Uri.parse("server.com/gadgets/js/rpc");
    GadgetsHandlerApi.JsRequest request =
        createJsRequest(null, CONTAINER, fields, features);
    Capture<JsUri> uriCapture = new Capture<JsUri>();
    expect(jsUriManager.makeExternJsUri(capture(uriCapture))).andReturn(resUri);
    replay();

    GadgetsHandlerApi.JsResponse response = gadgetHandler.getJs(request);
    JsUri expectedUri = new JsUri(null, false, false, CONTAINER, null,
        features, null, null, false, false, RenderingContext.GADGET, null);
    assertEquals(expectedUri, uriCapture.getValue());
    assertEquals(resUri, response.getJsUrl());
    assertNull(response.getJsContent());
    assertNull(response.getExpireTimeMs());
    verify();
  }

  @Test(expected = ProcessingException.class)
  public void testJsNoContainer() throws Exception {
    List<String> fields = ImmutableList.of("*");
    GadgetsHandlerApi.JsRequest request =
        createJsRequest(null, null, fields, ImmutableList.of("rpc"));
    GadgetsHandlerApi.JsResponse response = gadgetHandler.getJs(request);
  }

  @Test
  public void testGetJsData() throws Exception {
    List<String> fields = ImmutableList.of("jscontent");
    List<String> features = ImmutableList.of("rpc");
    Uri resUri = Uri.parse("http://server.com/gadgets/js/rpc");
    Capture<JsUri> uriCapture = new Capture<JsUri>();
    String jsContent = "var a;";
    String onload = "do this";
    expect(jsUriManager.makeExternJsUri(capture(uriCapture))).andReturn(resUri);
    expect(jsHandler.getJsContent(EasyMock.isA(JsUri.class), EasyMock.eq("server.com")))
        .andReturn(new JsHandler.Response(new StringBuilder(jsContent), true));
    GadgetsHandlerApi.JsRequest request =
        createJsRequest(FakeProcessor.SPEC_URL.toString(), CONTAINER, fields, features);
    expect(request.getOnload()).andStubReturn(onload);
    expect(request.getContext()).andStubReturn(GadgetsHandlerApi.RenderingContext.CONTAINER);
    replay();

    GadgetsHandlerApi.JsResponse response = gadgetHandler.getJs(request);
    JsUri expectedUri = new JsUri(null, false, false, CONTAINER, FakeProcessor.SPEC_URL.toString(),
        features, null, onload, false, false, RenderingContext.CONTAINER, null);
    assertEquals(expectedUri, uriCapture.getValue());
    assertNull(response.getJsUrl());
    assertEquals(jsContent, response.getJsContent());
    assertEquals(timeSource.currentTimeMillis() + HttpUtil.getDefaultTtl() * 1000,
        response.getExpireTimeMs().longValue());
    verify();
  }

  @Test
  public void testCreateProxyUri() throws Exception {
    GadgetsHandlerApi.ImageParams image = mock(GadgetsHandlerApi.ImageParams.class);
    expect(image.getDoNotExpand()).andStubReturn(true);
    expect(image.getHeight()).andStubReturn(120);
    expect(image.getWidth()).andStubReturn(210);
    expect(image.getQuality()).andStubReturn(77);

    GadgetsHandlerApi.ProxyRequest request = mock(GadgetsHandlerApi.ProxyRequest.class);
    expect(request.getContainer()).andStubReturn(CONTAINER);
    expect(request.getUrl()).andStubReturn(RESOURCE);
    expect(request.getRefresh()).andStubReturn(new Integer(333));
    expect(request.getDebug()).andStubReturn(true);
    expect(request.getFallbackUrl()).andStubReturn(FALLBACK);
    expect(request.getGadget()).andStubReturn(FakeProcessor.SPEC_URL.toString());
    expect(request.getIgnoreCache()).andStubReturn(true);
    expect(request.getImageParams()).andStubReturn(image);
    expect(request.getRewriteMimeType()).andStubReturn("image/png");
    expect(request.getSanitize()).andStubReturn(true);
    replay();
    ProxyUri pUri = gadgetHandler.createProxyUri(request);

    ProxyUri expectedUri = new ProxyUri(333, true, true, CONTAINER,
        FakeProcessor.SPEC_URL.toString(), RESOURCE);
    expectedUri.setRewriteMimeType("image/png").setSanitizeContent(true);
    expectedUri.setResize(210, 120, 77, true).setFallbackUrl(FALLBACK);
    assertEquals(pUri, expectedUri);
    verify();
  }

  @Test
  public void testValidateProxyResponse() throws Exception {
    GadgetsHandlerApi.ProxyResponse response =
        gadgetHandler.createProxyResponse(RESOURCE, null, ImmutableSet.<String>of("*"), 1000001L);

    BeanDelegator.validateDelegator(response);
    assertEquals(RESOURCE, response.getProxyUrl());
    assertNull(response.getProxyContent());
  }

  @Test
  public void testCreateProxyResponse() throws Exception {
    HttpResponseBuilder httpResponse = new HttpResponseBuilder();
    httpResponse.setContent("Content");
    httpResponse.addHeader("header", "hval");
    httpResponse.setEncoding(Charset.forName("UTF8"));
    httpResponse.setHttpStatusCode(404);

    GadgetsHandlerApi.ProxyResponse response = gadgetHandler.createProxyResponse(
        RESOURCE, httpResponse.create(), ImmutableSet.<String>of("*"), 1000001L);
    BeanDelegator.validateDelegator(response);
    assertEquals("Content",
        new String(Base64.decodeBase64(response.getProxyContent().getContentBase64())));
    assertEquals(404, response.getProxyContent().getCode());
    assertEquals(2, response.getProxyContent().getHeaders().size());
    assertEquals("Date", response.getProxyContent().getHeaders().get(0).getName());
    assertEquals("header", response.getProxyContent().getHeaders().get(1).getName());
    assertEquals("hval", response.getProxyContent().getHeaders().get(1).getValue());
  }

  @Test
  public void testFilterProxyResponse() throws Exception {
    HttpResponse httpResponse = new HttpResponse("data");
    GadgetsHandlerApi.ProxyResponse response = gadgetHandler.createProxyResponse(
        RESOURCE, httpResponse, ImmutableSet.<String>of("proxyurl"), 1000001L);
    assertNull(response.getProxyContent());
    assertEquals(RESOURCE, response.getProxyUrl());
  }

  @Test
  public void testGetProxySimple() throws Exception {
    List<String> fields = ImmutableList.of("proxyurl");
    Uri resUri = Uri.parse("server.com/gadgets/proxy?url=" + RESOURCE);
    GadgetsHandlerApi.ProxyRequest request = createProxyRequest(RESOURCE, CONTAINER, fields);
    Capture<List<ProxyUri>> uriCapture = new Capture<List<ProxyUri>>();
    expect(proxyUriManager.make(capture(uriCapture), EasyMock.anyInt()))
        .andReturn(ImmutableList.of(resUri));
    replay();
    GadgetsHandlerApi.ProxyResponse response = gadgetHandler.getProxy(request);
    assertEquals(1, uriCapture.getValue().size());
    ProxyUri pUri = uriCapture.getValue().get(0);
    assertEquals(CONTAINER, pUri.getContainer());
    assertEquals(resUri, response.getProxyUrl());
    assertNull(response.getProxyContent());
    verify();
  }

  @Test(expected = ProcessingException.class)
  public void testGetProxyNoContainer() throws Exception {
    List<String> fields = ImmutableList.of("*");
    GadgetsHandlerApi.ProxyRequest request = createProxyRequest(RESOURCE, null, fields);
    GadgetsHandlerApi.ProxyResponse response = gadgetHandler.getProxy(request);
  }

  @Test(expected = ProcessingException.class)
  public void testGetProxyNoResource() throws Exception {
    List<String> fields = ImmutableList.of("*");
    GadgetsHandlerApi.ProxyRequest request = createProxyRequest(null, CONTAINER, fields);
    GadgetsHandlerApi.ProxyResponse response = gadgetHandler.getProxy(request);
  }

  @Test(expected = ProcessingException.class)
  public void testGetProxyNoFields() throws Exception {
    GadgetsHandlerApi.ProxyRequest request = createProxyRequest(RESOURCE, CONTAINER, null);
    GadgetsHandlerApi.ProxyResponse response = gadgetHandler.getProxy(request);
  }

  @Test
  public void testGetProxyData() throws Exception {
    List<String> fields = ImmutableList.of("proxycontent.*");
    Uri resUri = Uri.parse("server.com/gadgets/proxy?url=" + RESOURCE);
    GadgetsHandlerApi.ProxyRequest request = createProxyRequest(RESOURCE, CONTAINER, fields);
    Capture<List<ProxyUri>> uriCapture = new Capture<List<ProxyUri>>();
    expect(proxyUriManager.make(capture(uriCapture), EasyMock.anyInt()))
        .andReturn(ImmutableList.of(resUri));
    HttpResponse httpResponse = new HttpResponse("response");
    expect(proxyHandler.fetch(EasyMock.isA(ProxyUri.class))).andReturn(httpResponse);
    replay();
    GadgetsHandlerApi.ProxyResponse response = gadgetHandler.getProxy(request);
    assertEquals(1, uriCapture.getValue().size());
    ProxyUri pUri = uriCapture.getValue().get(0);
    assertEquals(CONTAINER, pUri.getContainer());
    assertNull(response.getProxyUrl());
    assertEquals("response",
        new String(Base64.decodeBase64(response.getProxyContent().getContentBase64())));
    verify();
  }

  @Test
  public void testGetProxyEmptyData() throws Exception {
    List<String> fields = ImmutableList.of("proxycontent.*");
    Uri resUri = Uri.parse("server.com/gadgets/proxy?url=" + RESOURCE);
    GadgetsHandlerApi.ProxyRequest request = createProxyRequest(RESOURCE, CONTAINER, fields);
    Capture<List<ProxyUri>> uriCapture = new Capture<List<ProxyUri>>();
    expect(proxyUriManager.make(capture(uriCapture), EasyMock.anyInt()))
        .andReturn(ImmutableList.of(resUri));
    HttpResponse httpResponse = new HttpResponseBuilder().setHttpStatusCode(504).create();
    expect(proxyHandler.fetch(EasyMock.isA(ProxyUri.class))).andReturn(httpResponse);
    replay();
    GadgetsHandlerApi.ProxyResponse response = gadgetHandler.getProxy(request);
    assertEquals(1, uriCapture.getValue().size());
    ProxyUri pUri = uriCapture.getValue().get(0);
    assertEquals(CONTAINER, pUri.getContainer());
    assertNull(response.getProxyUrl());
    assertEquals("", response.getProxyContent().getContentBase64());
    assertEquals(504, response.getProxyContent().getCode());
    verify();
  }

  @Test(expected = ProcessingException.class)
  public void testGetProxyDataFail() throws Exception {
    List<String> fields = ImmutableList.of("proxycontent.*");
    Uri resUri = Uri.parse("server.com/gadgets/proxy?url=" + RESOURCE);
    GadgetsHandlerApi.ProxyRequest request = createProxyRequest(RESOURCE, CONTAINER, fields);
    Capture<List<ProxyUri>> uriCapture = new Capture<List<ProxyUri>>();
    expect(proxyUriManager.make(capture(uriCapture), EasyMock.anyInt()))
        .andReturn(ImmutableList.of(resUri));
    HttpResponse httpResponse = new HttpResponse("response");
    expect(proxyHandler.fetch(EasyMock.isA(ProxyUri.class)))
        .andThrow(new GadgetException(Code.FAILED_TO_RETRIEVE_CONTENT));
    replay();
    GadgetsHandlerApi.ProxyResponse response = gadgetHandler.getProxy(request);
  }

  @Test
  public void testCreateCajaResponse() throws Exception {
    String goldenEntries[][] = {{"name1", "LINT", "msg1"}, {"name2", "LINT", "msg2"}};
    List<GadgetsHandlerApi.Message> goldenMessages = Lists.newArrayList();

    for (String[] goldenEntry : goldenEntries) {
      GadgetsHandlerApi.Message m = mock(GadgetsHandlerApi.Message.class);
      expect(m.getName()).andReturn(goldenEntry[0]);
      expect(m.getLevel()).andReturn(GadgetsHandlerApi.MessageLevel.valueOf(goldenEntry[1]));
      expect(m.getMessage()).andReturn(goldenEntry[2]);
      goldenMessages.add(m);
    }
    replay();

    Uri jsUri = Uri.parse("http://www.shindig.com/js");
    GadgetsHandlerApi.CajaResponse jsResponse =
        gadgetHandler.createCajaResponse(jsUri, "html", "js",
            goldenMessages, ImmutableSet.of("*"), null);
    BeanDelegator.validateDelegator(jsResponse);

    assertEquals("html", jsResponse.getHtml());
    assertEquals("js", jsResponse.getJs());
    List<GadgetsHandlerApi.Message> response = jsResponse.getMessages();
    assertEquals(goldenMessages.size(), response.size());
    for (int i=0; i < response.size(); i++) {
      assertEquals(goldenEntries[i][0], response.get(i).getName());
      assertEquals(goldenEntries[i][1], response.get(i).getLevel().name());
      assertEquals(goldenEntries[i][2], response.get(i).getMessage());
    }
  }

  private GadgetsHandlerApi.AuthContext createAuthContext(String ownerId, String viewerId) {
    GadgetsHandlerApi.AuthContext authContext = mock(GadgetsHandlerApi.AuthContext.class);
    if (ownerId != null) {
      EasyMock.expect(authContext.getOwnerId()).andReturn(ownerId).once();
    }
    if (viewerId != null) {
      EasyMock.expect(authContext.getViewerId()).andReturn(viewerId).once();
    }
    return authContext;
  }

  private GadgetsHandlerApi.MetadataRequest createMetadataRequest(Uri url, String container,
      String view, GadgetsHandlerApi.AuthContext authContext, List<String> fields) {
    GadgetsHandlerApi.MetadataRequest request = mock(GadgetsHandlerApi.MetadataRequest.class);
    EasyMock.expect(request.getFields()).andReturn(fields).anyTimes();
    EasyMock.expect(request.getView()).andReturn(view).once();
    EasyMock.expect(request.getUrl()).andReturn(url).anyTimes();
    EasyMock.expect(request.getContainer()).andReturn(container).anyTimes();
    EasyMock.expect(request.getAuthContext()).andReturn(authContext).once();
    return request;
  }

  private GadgetsHandlerApi.TokenRequest createTokenRequest(Uri url, String container,
      GadgetsHandlerApi.AuthContext authContext, List<String> fields) {
    GadgetsHandlerApi.TokenRequest request = mock(GadgetsHandlerApi.TokenRequest.class);
    EasyMock.expect(request.getFields()).andReturn(fields).anyTimes();
    EasyMock.expect(request.getUrl()).andReturn(url).anyTimes();
    EasyMock.expect(request.getContainer()).andReturn(container).anyTimes();
    EasyMock.expect(request.getAuthContext()).andReturn(authContext).once();
    return request;
  }

  private GadgetsHandlerApi.JsRequest createJsRequest(String gadget, String container,
      List<String> fields, List<String> features) {
    GadgetsHandlerApi.JsRequest request = mock(GadgetsHandlerApi.JsRequest.class);
    EasyMock.expect(request.getFields()).andStubReturn(fields);
    EasyMock.expect(request.getContainer()).andStubReturn(container);
    EasyMock.expect(request.getGadget()).andStubReturn(gadget);
    EasyMock.expect(request.getFeatures()).andStubReturn(features);
    return request;
  }

  private GadgetsHandlerApi.ProxyRequest createProxyRequest(Uri url, String container,
      List<String> fields) {
    GadgetsHandlerApi.ProxyRequest request = mock(GadgetsHandlerApi.ProxyRequest.class);
    EasyMock.expect(request.getFields()).andStubReturn(fields);
    EasyMock.expect(request.getContainer()).andStubReturn(container);
    EasyMock.expect(request.getUrl()).andStubReturn(url);
    return request;
  }

  private class FakeSecurityTokenCodec implements SecurityTokenCodec {
    public SecurityToken authContext = null;
    public SecurityTokenException exc = null;
    public String encodedToken = null;
    public Long tokenExpiryTimeMs = null;

    public String encodeToken(SecurityToken authContext) throws SecurityTokenException {
      this.authContext = authContext;
      if (exc != null) {
        throw exc;
      }
      return encodedToken;
    }

    public SecurityToken createToken(Map<String, String> tokenParameters)
        throws SecurityTokenException {
      if (exc != null) {
        throw exc;
      }
      return authContext;
    }

    public Long getTokenExpiration(SecurityToken authContext) throws SecurityTokenException {
      this.authContext = authContext;
      if (exc != null) {
        throw exc;
      }
      return tokenExpiryTimeMs;
    }
  }
}

