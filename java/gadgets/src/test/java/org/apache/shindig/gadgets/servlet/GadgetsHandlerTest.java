/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.shindig.gadgets.servlet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.apache.commons.codec.binary.Base64;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.JsonAssert;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.testing.TestExecutorService;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.FakeTimeSource;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.ProxyUriManager.ProxyUri;
import org.apache.shindig.protocol.DefaultHandlerRegistry;
import org.apache.shindig.protocol.HandlerExecutionListener;
import org.apache.shindig.protocol.HandlerRegistry;
import org.apache.shindig.protocol.RpcHandler;
import org.apache.shindig.protocol.conversion.BeanFilter;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.protocol.multipart.FormDataItem;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

public class GadgetsHandlerTest extends EasyMockTestCase {
  private static final String GADGET1_URL = FakeProcessor.SPEC_URL.toString();
  private static final String GADGET2_URL = FakeProcessor.SPEC_URL2 .toString();
  private static final String CONTAINER = "container";
  private static final String TOKEN = "_nekot_";
  private static final Long SPEC_REFRESH_INTERVAL = 123L;
  private static final Long EXPIRY_TIME_MS = 456L;

  private final FakeTimeSource timeSource = new FakeTimeSource();
  private final FakeProcessor processor = new FakeProcessor();
  private final FakeIframeUriManager urlGenerator = new FakeIframeUriManager();
  private final Map<String, FormDataItem> emptyFormItems = Collections.emptyMap();
  private final ProxyUriManager proxyUriManager = mock(ProxyUriManager.class);
  private final JsUriManager jsUriManager = mock(JsUriManager.class);
  private final ProxyHandler proxyHandler = mock(ProxyHandler.class);
  private final JsHandler jsHandler = mock(JsHandler.class);

  private Injector injector;
  private BeanJsonConverter converter;
  private HandlerRegistry registry;
  private FakeGadgetToken token;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector();
    converter = new BeanJsonConverter(injector);
    token = new FakeGadgetToken();
    token.setAppUrl("http://www.example.com/gadget.xml");
  }

  private void registerGadgetsHandler(SecurityTokenCodec codec) {
    BeanFilter beanFilter = new BeanFilter();
    GadgetsHandlerService service = new GadgetsHandlerService(timeSource, processor,
        urlGenerator, codec, proxyUriManager, jsUriManager, proxyHandler, jsHandler,
        SPEC_REFRESH_INTERVAL, beanFilter);
    GadgetsHandler handler =
        new GadgetsHandler(new TestExecutorService(), service, beanFilter);
    registry = new DefaultHandlerRegistry(
        injector, converter, new HandlerExecutionListener.NoOpHandler());
    registry.addHandlers(ImmutableSet.<Object> of(handler));
  }

  private JSONObject makeMetadataRequest(String lang, String country, String... uris)
      throws JSONException {
    JSONObject req =
        new JSONObject().put("method", "gadgets.metadata").put("id", "req1").put("params",
            new JSONObject().put("ids", ImmutableList.copyOf(uris)).put("container", CONTAINER));
    if (lang != null) req.put("language", lang);
    if (country != null) req.put("country", country);
    return req;
  }

  private JSONObject makeMetadataNoContainerRequest(String... uris)
      throws JSONException {
    JSONObject req =
      new JSONObject().put("method", "gadgets.metadata").put("id", "req1").put("params",
          new JSONObject().put("ids", ImmutableList.copyOf(uris)));
    return req;
  }

  private JSONObject makeTokenRequest(String... uris) throws JSONException {
    JSONObject req =
        new JSONObject().put("method", "gadgets.token").put("id", "req1").put("params",
            new JSONObject().put("ids", ImmutableList.copyOf(uris)).put("container", CONTAINER));
    return req;
  }

  @Test
  public void testMetadataEmptyRequest() throws Exception {
    registerGadgetsHandler(null);
    JSONObject request = makeMetadataRequest(null, null);
    RpcHandler operation = registry.getRpcHandler(request);
    Object empty = operation.execute(emptyFormItems, token, converter).get();
    JsonAssert.assertJsonEquals("{}", converter.convertToString(empty));
  }

  @Test
  public void testMetadataNoContainerRequest() throws Exception {
    registerGadgetsHandler(null);
    JSONObject request = makeMetadataNoContainerRequest(GADGET1_URL);
    RpcHandler operation = registry.getRpcHandler(request);
    try {
      Object empty = operation.execute(emptyFormItems, token, converter).get();
      fail("Missing container");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Missing container"));
    }
  }

  @Test
  public void testTokenEmptyRequest() throws Exception {
    registerGadgetsHandler(null);
    JSONObject request = makeTokenRequest();
    RpcHandler operation = registry.getRpcHandler(request);
    Object empty = operation.execute(emptyFormItems, token, converter).get();
    JsonAssert.assertJsonEquals("{}", converter.convertToString(empty));
  }

  @Test
  public void testMetadataInvalidUrl() throws Exception {
    registerGadgetsHandler(null);
    String badUrl = "[moo]";
    JSONObject request = makeMetadataRequest(null, null, badUrl);
    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));
    JSONObject gadget = response.getJSONObject(badUrl);
    assertEquals("Bad url - " + badUrl, gadget.getJSONObject("error").getString("message"));
    assertEquals(400, gadget.getJSONObject("error").getInt("code"));
  }

  @Test
  public void testTokenInvalidUrl() throws Exception {
    registerGadgetsHandler(null);
    String badUrl = "[moo]";
    JSONObject request = makeTokenRequest(badUrl);
    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));
    JSONObject gadget = response.getJSONObject(badUrl);
    assertEquals("Bad url - " + badUrl, gadget.getJSONObject("error").getString("message"));
    assertEquals(400, gadget.getJSONObject("error").getInt("code"));
  }

  @Test
  public void testMetadataOneGadget() throws Exception {
    registerGadgetsHandler(null);
    JSONObject request = makeMetadataRequest(null, null, GADGET1_URL);
    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));

    JSONObject gadget = response.getJSONObject(GADGET1_URL);
    assertEquals(FakeIframeUriManager.DEFAULT_IFRAME_URI.toString(), gadget.getString("iframeUrl"));
    assertEquals(FakeProcessor.SPEC_TITLE, gadget.getJSONObject("modulePrefs").getString("title"));
    assertFalse(gadget.has("error"));
    assertFalse(gadget.has("url")); // filtered out
    JSONObject view = gadget.getJSONObject("views").getJSONObject(GadgetSpec.DEFAULT_VIEW);
    assertEquals(FakeProcessor.PREFERRED_HEIGHT, view.getInt("preferredHeight"));
    assertEquals(FakeProcessor.PREFERRED_WIDTH, view.getInt("preferredWidth"));
    assertEquals(FakeProcessor.LINK_HREF, gadget.getJSONObject("modulePrefs")
        .getJSONObject("links").getJSONObject(FakeProcessor.LINK_REL).getString("href"));

    JSONObject userPrefs = gadget.getJSONObject("userPrefs");
    assertNotNull(userPrefs);

    JSONObject userPrefData = userPrefs.getJSONObject("up_one");
    assertNotNull(userPrefData);

    JSONArray orderedEnums = userPrefData.getJSONArray("orderedEnumValues");
    assertNotNull(orderedEnums);
    assertEquals(4, orderedEnums.length());
    assertEquals("disp1", orderedEnums.getJSONObject(0).getString("displayValue"));
    assertEquals("val1", orderedEnums.getJSONObject(0).getString("value"));
    assertEquals("disp2", orderedEnums.getJSONObject(1).getString("displayValue"));
    assertEquals("abc", orderedEnums.getJSONObject(1).getString("value"));
    assertEquals("disp3", orderedEnums.getJSONObject(2).getString("displayValue"));
    assertEquals("z_xabc", orderedEnums.getJSONObject(2).getString("value"));
    assertEquals("disp4", orderedEnums.getJSONObject(3).getString("displayValue"));
    assertEquals("foo", orderedEnums.getJSONObject(3).getString("value"));
  }

  @Test
  public void testTokenOneGadget() throws Exception {
    SecurityTokenCodec codec = EasyMock.createMock(SecurityTokenCodec.class);
    Capture<SecurityToken> tokenCapture = new Capture<SecurityToken>();
    EasyMock.expect(codec.encodeToken(EasyMock.capture(tokenCapture))).andReturn(TOKEN).anyTimes();
    EasyMock.expect(codec.getTokenExpiration(EasyMock.capture(tokenCapture)))
        .andReturn(EXPIRY_TIME_MS).anyTimes();
    replay(codec);

    registerGadgetsHandler(codec);
    JSONObject request = makeTokenRequest(GADGET1_URL);
    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));

    JSONObject gadget = response.getJSONObject(GADGET1_URL);
    assertEquals(TOKEN, gadget.getString("token"));
    assertFalse(gadget.has("error"));
    assertFalse(gadget.has("url")); // filtered out
    // next checks verify all fiels that canbe used for token generation are passed in
    assertEquals("container", tokenCapture.getValue().getContainer());
    assertEquals(GADGET1_URL, tokenCapture.getValue().getAppId());
    assertEquals(GADGET1_URL, tokenCapture.getValue().getAppUrl());
    assertSame(token.getOwnerId(), tokenCapture.getValue().getOwnerId());
    assertSame(token.getViewerId(), tokenCapture.getValue().getViewerId());
  }

  @Test
  public void testMetadataOneGadgetFailure() throws Exception {
    registerGadgetsHandler(null);
    JSONObject request = makeMetadataRequest(null, null, GADGET1_URL);
    urlGenerator.throwRandomFault = true;
    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));

    JSONObject gadget = response.getJSONObject(GADGET1_URL);
    assertEquals(GadgetsHandler.FAILURE_METADATA,
        gadget.getJSONObject("error").getString("message"));
    assertEquals(500, gadget.getJSONObject("error").getInt("code"));
  }

  @Test
  public void testTokenOneGadgetFailure() throws Exception {
    SecurityTokenCodec codec = EasyMock.createMock(SecurityTokenCodec.class);
    EasyMock.expect(codec.encodeToken(EasyMock.isA(SecurityToken.class)))
        .andThrow(new SecurityTokenException("blah"));
    replay(codec);

    registerGadgetsHandler(codec);
    JSONObject request = makeTokenRequest(GADGET1_URL);
    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));

    JSONObject gadget = response.getJSONObject(GADGET1_URL);
    assertFalse(gadget.has("token"));
    assertEquals(GadgetsHandler.FAILURE_TOKEN,
        gadget.getJSONObject("error").getString("message"));
    assertEquals(500, gadget.getJSONObject("error").getInt("code"));
  }

  @Test
  public void testMetadataMultipleGadgets() throws Exception {
    registerGadgetsHandler(null);
    JSONObject request = makeMetadataRequest("en", "US", GADGET1_URL, GADGET2_URL);
    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));

    JSONObject modulePrefs1 = response.getJSONObject(GADGET1_URL).getJSONObject("modulePrefs");
    assertEquals(FakeProcessor.SPEC_TITLE, modulePrefs1.getString("title"));

    JSONObject modulePrefs2 = response.getJSONObject(GADGET2_URL).getJSONObject("modulePrefs");
    assertEquals(FakeProcessor.SPEC_TITLE2, modulePrefs2.getString("title"));
  }

  @Test
  public void testTokenMultipleGadgetsWithSuccessAndFailure() throws Exception {
    SecurityTokenCodec codec = EasyMock.createMock(SecurityTokenCodec.class);
    EasyMock.expect(codec.encodeToken(EasyMock.isA(SecurityToken.class)))
        .andReturn(TOKEN);
    EasyMock.expect(codec.encodeToken(EasyMock.isA(SecurityToken.class)))
        .andThrow(new SecurityTokenException("blah"));
    EasyMock.expect(codec.getTokenExpiration(EasyMock.isA(SecurityToken.class)))
        .andReturn(EXPIRY_TIME_MS).anyTimes();
    replay(codec);

    registerGadgetsHandler(codec);
    JSONObject request = makeTokenRequest(GADGET1_URL, GADGET2_URL);

    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));

    JSONObject gadget1 = response.getJSONObject(GADGET1_URL);
    assertEquals(TOKEN, gadget1.getString("token"));
    assertFalse(gadget1.has("error"));

    JSONObject gadget2 = response.getJSONObject(GADGET2_URL);
    assertFalse(gadget2.has("token"));
    assertEquals(GadgetsHandler.FAILURE_TOKEN,
        gadget2.getJSONObject("error").getString("message"));
    assertEquals(500, gadget2.getJSONObject("error").getInt("code"));
  }

  @Test
  public void testMetadataMultipleGadgetsWithFailure() throws Exception {
    registerGadgetsHandler(null);
    JSONObject request = makeMetadataRequest("en", "US", GADGET1_URL, GADGET2_URL);
    processor.exceptions.put(FakeProcessor.SPEC_URL2, new ProcessingException("broken",
        HttpServletResponse.SC_BAD_REQUEST));
    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));

    JSONObject modulePrefs1 = response.getJSONObject(GADGET1_URL).getJSONObject("modulePrefs");
    assertEquals(FakeProcessor.SPEC_TITLE, modulePrefs1.getString("title"));

    JSONObject gadget2 = response.getJSONObject(GADGET2_URL);
    assertNotNull("got gadget2", gadget2);
    assertEquals("broken", // Processing exception message is used
        gadget2.getJSONObject("error").getString("message"));
    assertEquals(HttpServletResponse.SC_BAD_REQUEST,
        gadget2.getJSONObject("error").getInt("code"));
  }

  private JSONObject makeSimpleProxyRequest(String fields, String... uris) throws JSONException {
    JSONObject params = new JSONObject().put("ids", ImmutableList.copyOf(uris))
        .put("container", CONTAINER);
    if (fields != null) {
      params.put("fields", fields);
    }
    JSONObject req =
        new JSONObject().put("method", "gadgets.proxy").put("id", "req1").put("params", params);
    return req;
  }

  @Test
  public void testSimpleProxy() throws Exception {
    registerGadgetsHandler(null);
    String resUri = "http://example.com/data";
    String proxyUri = "http://shindig.com/gadgets/proxy?url=" + resUri;
    JSONObject request = makeSimpleProxyRequest(null, resUri);
    Capture<List<ProxyUri>> captureProxyUri = new Capture<List<ProxyUri>>();
    EasyMock.expect(proxyUriManager.make(EasyMock.capture(captureProxyUri),
        EasyMock.isNull(Integer.class))).andReturn(ImmutableList.<Uri>of(Uri.parse(proxyUri)));
    replay();
    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));

    JSONObject gadget1 = response.getJSONObject(resUri);
    assertEquals(proxyUri, gadget1.getString("proxyUrl"));
    ProxyUri pUri = captureProxyUri.getValue().get(0);
    ProxyUri expectedUri = new ProxyUri(null, false, false, CONTAINER, null, Uri.parse(resUri));
    assertTrue(expectedUri.equals(pUri));
    assertFalse(gadget1.has("error"));
    verify();
  }

  private JSONObject makeSimpleJsRequest(String fields, List<String> features)
      throws JSONException {
    JSONObject params = new JSONObject().put("gadget", GADGET1_URL)
        .put("container", CONTAINER).put("features", features);
    if (fields != null) {
      params.put("fields", fields);
    }
    JSONObject req =
        new JSONObject().put("method", "gadgets.js").put("id", "req1").put("params", params);
    return req;
  }

  @Test
  public void testJsSimple() throws Exception {
    registerGadgetsHandler(null);
    List<String> features = ImmutableList.of("rpc","io");
    Uri jsUri = Uri.parse("http://shindig.com/gadgets/js/rpc:io");
    JSONObject request = makeSimpleJsRequest(null, features);
    Capture<JsUri> captureUri = new Capture<JsUri>();
    EasyMock.expect(jsUriManager.makeExternJsUri(EasyMock.capture(captureUri)))
        .andReturn(jsUri);
    replay();

    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject results = new JSONObject(converter.convertToString(responseObj));
    assertEquals(jsUri.toString(), results.getString("jsUrl"));
    JsUri expectedUri = new JsUri(null, false, false, CONTAINER, GADGET1_URL,
        features, null, false, RenderingContext.GADGET);
    assertEquals(expectedUri, captureUri.getValue());
    assertFalse(results.has("error"));
    assertFalse(results.has("jsContent"));
    verify();
  }

  private JSONObject makeComplexJsRequest(List<String> features, String onload)
      throws JSONException {
    JSONObject params = new JSONObject().put("gadget", GADGET1_URL)
        .put("container", CONTAINER).put("features", features)
        .put("fields", "*").put("refresh", "123").put("debug", "1")
        .put("ignoreCache", "1").put("onload",onload)
        .put("c", "1");
    JSONObject request =
        new JSONObject().put("method", "gadgets.js").put("id", "req1").put("params", params);
    return request;
  }

  @Test
  public void testJsData() throws Exception {
    registerGadgetsHandler(null);
    List<String> features = ImmutableList.of("rpc","io");
    Uri jsUri = Uri.parse("http://shindig.com/gadgets/js/rpc:io");
    String onload = "do \"this\";";

    JSONObject request = makeComplexJsRequest(features, onload);

    Capture<JsUri> captureUri = new Capture<JsUri>();
    EasyMock.expect(jsUriManager.makeExternJsUri(EasyMock.capture(captureUri)))
        .andReturn(jsUri);
    String jsContent = "var b=\"123\";";
    EasyMock.expect(jsHandler.getJsContent(
        EasyMock.isA(JsUri.class), EasyMock.eq(jsUri.getAuthority())))
        .andReturn(new JsHandler.Response(new StringBuilder(jsContent), true));
    replay();

    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject results = new JSONObject(converter.convertToString(responseObj));
    assertEquals(jsUri.toString(), results.getString("jsUrl"));
    JsUri expectedUri = new JsUri(123, true, true, CONTAINER, GADGET1_URL,
        features, onload, false, RenderingContext.CONTAINER);
    assertEquals(expectedUri, captureUri.getValue());
    assertFalse(results.has("error"));
    assertEquals(jsContent, results.getString("jsContent"));
    verify();
  }

  @Test
  public void testJsFailure() throws Exception {
    registerGadgetsHandler(null);
    List<String> features = ImmutableList.of("rpc2");
    Uri jsUri = Uri.parse("http://shindig.com/gadgets/js/rpc:io");
    String onload = "do \"this\";";

    JSONObject request = makeComplexJsRequest(features, onload);

    Capture<JsUri> captureUri = new Capture<JsUri>();
    EasyMock.expect(jsUriManager.makeExternJsUri(EasyMock.capture(captureUri)))
        .andReturn(jsUri);
    EasyMock.expect(jsHandler.getJsContent(
        EasyMock.isA(JsUri.class), EasyMock.eq(jsUri.getAuthority())))
        .andReturn(new JsHandler.Response(new StringBuilder(""), true));
    replay();

    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject results = new JSONObject(converter.convertToString(responseObj));
    assertFalse(results.has("jsUrl"));
    assertEquals(HttpResponse.SC_NOT_FOUND, results.getJSONObject("error").getInt("code"));
    assertTrue(results.getJSONObject("error").getString("message").contains("not found"));
    verify();
  }

  @Test
  public void testSimpleProxyData() throws Exception {
    registerGadgetsHandler(null);
    String resUri = "http://example.com/data";
    String proxyUri = "http://shindig.com/gadgets/proxy?url=" + resUri;
    JSONObject request = makeSimpleProxyRequest("*", resUri);
    Capture<List<ProxyUri>> captureProxyUri = new Capture<List<ProxyUri>>();
    EasyMock.expect(proxyUriManager.make(EasyMock.capture(captureProxyUri),
        EasyMock.isNull(Integer.class))).andReturn(ImmutableList.<Uri>of(Uri.parse(proxyUri)));
    String responseData = "response data";
    HttpResponse httpResponse = new HttpResponse(responseData);
    EasyMock.expect(proxyHandler.fetch(EasyMock.isA(ProxyUri.class))).andReturn(httpResponse);
    replay();

    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));

    JSONObject gadget1 = response.getJSONObject(resUri);
    assertEquals(proxyUri, gadget1.getString("proxyUrl"));
    ProxyUri pUri = captureProxyUri.getValue().get(0);
    ProxyUri expectedUri = new ProxyUri(null, false, false, CONTAINER, null, Uri.parse(resUri));
    assertTrue(expectedUri.equals(pUri));
    assertEquals(responseData, new String(Base64.decodeBase64(((JSONObject)
        gadget1.get("proxyContent")).getString("contentBase64").getBytes())));
    assertFalse(gadget1.has("error"));
    verify();
  }

  private JSONObject makeComplexProxyRequest(String... uris) throws JSONException {
    JSONObject req =
        new JSONObject().put("method", "gadgets.proxy").put("id", "req1").put("params",
            new JSONObject().put("ids", ImmutableList.copyOf(uris)).put("container", CONTAINER)
                .put("ignoreCache", "1").put("debug", "1").put("sanitize", "true")
                .put("cajole", "true").put("gadget", GADGET1_URL).put("refresh", "333")
                .put("rewriteMime", "text/xml").put("fallback_url", uris[0])
                .put("no_expand", "true").put("resize_h", "444").put("resize_w", "555")
                .put("resize_q", "88")
                );
    return req;
  }

  @Test
  public void testComplexProxy() throws Exception {
    registerGadgetsHandler(null);
    String resUri = "http://example.com/data";
    String proxyUri = "http://shindig.com/gadgets/proxy?url=" + resUri;
    JSONObject request = makeComplexProxyRequest(resUri);
    Capture<List<ProxyUri>> captureProxyUri = new Capture<List<ProxyUri>>();
    EasyMock.expect(proxyUriManager.make(EasyMock.capture(captureProxyUri),
        EasyMock.isNull(Integer.class))).andReturn(ImmutableList.<Uri>of(Uri.parse(proxyUri)));
    replay();
    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));

    JSONObject gadget1 = response.getJSONObject(resUri);
    assertEquals(proxyUri, gadget1.getString("proxyUrl"));
    ProxyUri pUri = captureProxyUri.getValue().get(0);
    ProxyUri expectedUri = new ProxyUri(333, true, true, CONTAINER, GADGET1_URL, Uri.parse(resUri));
    expectedUri.setCajoleContent(true).setRewriteMimeType("text/xml").setSanitizeContent(true);
    expectedUri.setFallbackUrl(resUri).setResize(555, 444, 88, true);
    assertTrue(expectedUri.equals(pUri));
    assertFalse(gadget1.has("error"));
    verify();
  }

}
