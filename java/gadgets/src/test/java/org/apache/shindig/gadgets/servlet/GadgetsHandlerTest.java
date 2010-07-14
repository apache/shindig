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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.JsonAssert;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.testing.TestExecutorService;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.protocol.DefaultHandlerRegistry;
import org.apache.shindig.protocol.HandlerExecutionListener;
import org.apache.shindig.protocol.HandlerRegistry;
import org.apache.shindig.protocol.RpcHandler;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.protocol.multipart.FormDataItem;
import org.easymock.EasyMock;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.easymock.EasyMock.expect;

public class GadgetsHandlerTest extends EasyMockTestCase {
  private final RequestPipeline pipeline = mock(RequestPipeline.class);
  private final FakeProcessor processor = new FakeProcessor();
  private final FakeIframeUriManager urlGenerator = new FakeIframeUriManager();
  private final Map<String,FormDataItem> emptyFormItems = Collections.emptyMap();

  private BeanJsonConverter converter;
  private HandlerRegistry registry;
  private FakeGadgetToken token;


  @Before
  public void setUp() throws Exception {
    Injector injector = Guice.createInjector();
    converter = new BeanJsonConverter(injector);

    GadgetsHandler gadgetsHandler = new GadgetsHandler(new TestExecutorService(),
        processor,
        urlGenerator);

    registry = new DefaultHandlerRegistry(injector, converter,
        new HandlerExecutionListener.NoOpHandler());
    registry.addHandlers(ImmutableSet.<Object>of(gadgetsHandler));

    token = new FakeGadgetToken();
    token.setAppUrl("http://www.example.com/gadget.xml");

  }

  private JSONObject createContext(String lang, String country)
      throws JSONException {
    return new JSONObject().put("language", lang).put("country", country);
  }

  private JSONObject makeMetadataRequest(String lang, String country, Collection<String> uris) throws JSONException {
    JSONObject req = new JSONObject()
        .put("method", "gadgets.metadata")
        .put("id", "req1")
        .put("params", new JSONObject().put("ids", uris));

    if (lang != null) req.put("language", lang);
    if (country != null) req.put("country", country);
    return req;
  }
  
  @Test
  public void testMetadataEmptyRequest() throws Exception {
    JSONObject emptyrequest = makeMetadataRequest(null, null, Collections.<String>emptyList());

    RpcHandler operation = registry.getRpcHandler(emptyrequest);

    Object empty = operation.execute(emptyFormItems, token, converter).get();
    JsonAssert.assertJsonEquals("{}", converter.convertToString(empty));
  }

  @Test(expected=ExecutionException.class)
  public void testMetadataInvalidUrl() throws Exception {
    JSONObject invalidRequest = makeMetadataRequest(null, null, ImmutableList.of("[moo]"));

    RpcHandler operation = registry.getRpcHandler(invalidRequest);

    Object empty = operation.execute(emptyFormItems, token, converter).get();
  }

  @Test
  public void testMetadataOneGadget() throws Exception {
    JSONObject oneGadget = makeMetadataRequest(null, null, ImmutableList.of(FakeProcessor.SPEC_URL.toString()));

    RpcHandler operation = registry.getRpcHandler(oneGadget);

    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));

    JSONObject gadget = response.getJSONObject(FakeProcessor.SPEC_URL.toString());
    assertEquals(FakeIframeUriManager.DEFAULT_IFRAME_URI.toString(), gadget.getString("iframeUrl"));
    assertEquals(FakeProcessor.SPEC_TITLE, gadget.getJSONObject("modulePrefs").getString("title"));

    JSONObject view = gadget.getJSONObject("views").getJSONObject(GadgetSpec.DEFAULT_VIEW);
    assertEquals(FakeProcessor.PREFERRED_HEIGHT, view.getInt("preferredHeight"));
    assertEquals(FakeProcessor.PREFERRED_WIDTH, view.getInt("preferredWidth"));
    assertEquals(FakeProcessor.LINK_HREF, gadget.getJSONObject("modulePrefs").getJSONObject("links")
        .getJSONObject(FakeProcessor.LINK_REL).getString("href"));

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
  public void testMetadataUnexpectedError() throws Exception {
    JSONObject oneGadget = makeMetadataRequest(null, null, ImmutableList.of(FakeProcessor.SPEC_URL.toString()));

    urlGenerator.throwRandomFault = true;

    RpcHandler operation = registry.getRpcHandler(oneGadget);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));

    String actual = response.getJSONObject(FakeProcessor.SPEC_URL.toString()).getString("error");
    assertEquals("BROKEN", actual);
  }

  @Test
  public void testMultipleGadgets() throws Exception {
    JSONObject metadataRequest = makeMetadataRequest("en", "US",
        ImmutableList.of(FakeProcessor.SPEC_URL.toString(),
                         FakeProcessor.SPEC_URL2.toString()));

    RpcHandler operation = registry.getRpcHandler(metadataRequest);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));

    JSONObject gadget;

    // First gadget..
    gadget = response.getJSONObject(FakeProcessor.SPEC_URL.toString());
    assertNotNull("got gadget1", gadget);
    assertEquals(FakeProcessor.SPEC_TITLE, gadget.getJSONObject("modulePrefs").getString("title"));

    gadget = response.getJSONObject(FakeProcessor.SPEC_URL2.toString());
    assertNotNull("got gadget2", gadget);
    assertEquals(FakeProcessor.SPEC_TITLE2, gadget.getJSONObject("modulePrefs").getString("title"));
  }

  @Test
  public void testMultipleGadgetsWithAnError() throws Exception {
    JSONObject metadataRequest = makeMetadataRequest("en", "US",
        ImmutableList.of(FakeProcessor.SPEC_URL.toString(),
                         FakeProcessor.SPEC_URL2.toString()));

    processor.exceptions.put(FakeProcessor.SPEC_URL2,
        new ProcessingException("broken", HttpServletResponse.SC_BAD_REQUEST));

    RpcHandler operation = registry.getRpcHandler(metadataRequest);
    Object responseObj = operation.execute(emptyFormItems, token, converter).get();
    JSONObject response = new JSONObject(converter.convertToString(responseObj));

    JSONObject gadget;

    // First gadget..
    gadget = response.getJSONObject(FakeProcessor.SPEC_URL.toString());
    assertNotNull("got gadget1", gadget);
    assertEquals(FakeProcessor.SPEC_TITLE, gadget.getJSONObject("modulePrefs").getString("title"));

    gadget = response.getJSONObject(FakeProcessor.SPEC_URL2.toString());
    assertNotNull("got gadget2", gadget);
    assertEquals("broken", gadget.getString("error"));
  }
}
