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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.JsonAssert;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.testing.TestExecutorService;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.spec.GadgetSpec;
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
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletResponse;

public class GadgetsHandlerTest extends EasyMockTestCase {
  private static final String GADGET1_URL = FakeProcessor.SPEC_URL.toString();
  private static final String GADGET2_URL = FakeProcessor.SPEC_URL2 .toString();
  private static final String CONTAINER = "container";
  private static final String TOKEN = "_nekot_";

  private final FakeProcessor processor = new FakeProcessor();
  private final FakeIframeUriManager urlGenerator = new FakeIframeUriManager();
  private final Map<String, FormDataItem> emptyFormItems = Collections.emptyMap();

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
    GadgetsHandlerService service =
        new GadgetsHandlerService(processor, urlGenerator, codec, beanFilter);
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
  public void testTokenEmptyRequest() throws Exception {
    registerGadgetsHandler(null);
    JSONObject request = makeTokenRequest();
    RpcHandler operation = registry.getRpcHandler(request);
    Object empty = operation.execute(emptyFormItems, token, converter).get();
    JsonAssert.assertJsonEquals("{}", converter.convertToString(empty));
  }

  @Test(expected = ExecutionException.class)
  public void testMetadataInvalidUrl() throws Exception {
    registerGadgetsHandler(null);
    JSONObject request = makeMetadataRequest(null, null, "[moo]");
    RpcHandler operation = registry.getRpcHandler(request);
    operation.execute(emptyFormItems, token, converter).get();
  }

  @Test(expected = ExecutionException.class)
  public void testTokenInvalidUrl() throws Exception {
    registerGadgetsHandler(null);
    JSONObject request = makeTokenRequest("[moo]");
    RpcHandler operation = registry.getRpcHandler(request);
    operation.execute(emptyFormItems, token, converter).get();
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

    JSONObject view = gadget.getJSONObject("views").getJSONObject(GadgetSpec.DEFAULT_VIEW);
    assertEquals(FakeProcessor.PREFERRED_HEIGHT, view.getInt("preferredHeight"));
    assertEquals(FakeProcessor.PREFERRED_WIDTH, view.getInt("preferredWidth"));
    assertEquals(FakeProcessor.LINK_HREF, gadget.getJSONObject("modulePrefs")
        .getJSONObject("links").getJSONObject(FakeProcessor.LINK_REL).getString("href"));

    JSONObject userPrefs = gadget.getJSONObject("userPrefs");
    assertNotNull(userPrefs);

    JSONObject userPrefData = userPrefs.getJSONObject("up_one");
    assertNotNull(userPrefData);

    JSONObject upEnums = userPrefData.getJSONObject("enumValues");
    assertNotNull(upEnums);
    assertEquals("disp1", upEnums.get("val1"));
    assertEquals("disp2", upEnums.get("abc"));
    assertEquals("disp3", upEnums.get("z_xabc"));
    assertEquals("disp4", upEnums.get("foo"));

    JSONArray orderedEnums = userPrefData.getJSONArray("orderedEnumValues");
    assertNotNull(orderedEnums);
    assertEquals(4, orderedEnums.length());
    assertEquals("val1", orderedEnums.getJSONObject(0).getString("value"));
    assertEquals("abc", orderedEnums.getJSONObject(1).getString("value"));
    assertEquals("z_xabc", orderedEnums.getJSONObject(2).getString("value"));
    assertEquals("foo", orderedEnums.getJSONObject(3).getString("value"));
  }

  @Test
  public void testTokenOneGadget() throws Exception {
    SecurityTokenCodec codec = EasyMock.createMock(SecurityTokenCodec.class);
    Capture<SecurityToken> tokenCapture = new Capture<SecurityToken>();
    EasyMock.expect(codec.encodeToken(EasyMock.capture(tokenCapture))).andReturn(TOKEN);
    replay(codec);

    registerGadgetsHandler(codec);
    JSONObject request = makeTokenRequest(GADGET1_URL);
    RpcHandler operation = registry.getRpcHandler(request);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));

    JSONObject gadget = response.getJSONObject(GADGET1_URL);
    assertEquals(TOKEN, gadget.getString("token"));
    assertFalse(gadget.has("error"));
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
    assertEquals(GadgetsHandler.FAILURE_METADATA, gadget.getString("error"));
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
    assertEquals(GadgetsHandler.FAILURE_TOKEN, gadget.getString("error"));
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
    assertEquals(GadgetsHandler.FAILURE_TOKEN, gadget2.getString("error"));
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
    assertEquals(GadgetsHandler.FAILURE_METADATA, gadget2.getString("error"));
  }
}
