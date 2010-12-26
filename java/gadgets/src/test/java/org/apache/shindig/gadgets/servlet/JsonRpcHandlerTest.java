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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.testing.ImmediateExecutorService;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

public class JsonRpcHandlerTest {
  private final FakeProcessor processor = new FakeProcessor();
  private final FakeIframeUriManager urlGenerator = new FakeIframeUriManager();
  private final JsonRpcHandler jsonRpcHandler
      = new JsonRpcHandler(new ImmediateExecutorService(), processor, urlGenerator);

  private JSONObject createContext(String lang, String country)
      throws JSONException {
    return new JSONObject().put("language", lang).put("country", country);
  }

  private JSONObject createGadget(String url, int moduleId, Map<String, String> prefs)
      throws JSONException {
    return new JSONObject()
        .put("url", url)
        .put("moduleId", moduleId)
        .put("prefs", prefs == null ? Collections.emptySet() : prefs);
  }

  @Test
  public void testSimpleRequest() throws Exception {
    JSONArray gadgets = new JSONArray()
        .put(createGadget(FakeProcessor.SPEC_URL.toString(), 0, null));
    JSONObject input = new JSONObject()
        .put("context", createContext("en", "US"))
        .put("gadgets", gadgets);

    urlGenerator.iframeUrl = FakeProcessor.SPEC_URL;

    JSONObject response = jsonRpcHandler.process(input);

    JSONArray outGadgets = response.getJSONArray("gadgets");
    JSONObject gadget = outGadgets.getJSONObject(0);
    assertEquals(FakeProcessor.SPEC_URL.toString(), gadget.getString("iframeUrl"));
    assertEquals(FakeProcessor.SPEC_TITLE, gadget.getString("title"));
    assertEquals(0, gadget.getInt("moduleId"));

    JSONObject view = gadget.getJSONObject("views").getJSONObject(GadgetSpec.DEFAULT_VIEW);
    assertEquals(FakeProcessor.PREFERRED_HEIGHT, view.getInt("preferredHeight"));
    assertEquals(FakeProcessor.PREFERRED_WIDTH, view.getInt("preferredWidth"));
    assertEquals(FakeProcessor.LINK_HREF, gadget.getJSONObject("links").getString(FakeProcessor.LINK_REL));

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
  public void testUnexpectedError() throws Exception {
    JSONArray gadgets = new JSONArray()
        .put(createGadget(FakeProcessor.SPEC_URL.toString(), 0, null));
    JSONObject input = new JSONObject()
        .put("context", createContext("en", "US"))
        .put("gadgets", gadgets);

    urlGenerator.throwRandomFault = true;
    JSONObject resp = jsonRpcHandler.process(input);
    String actual = resp.getJSONArray("gadgets").getJSONObject(0).getJSONArray("errors").getString(0);
    assertEquals("BROKEN", actual);
  }

  // TODO: Verify that user pref specs are returned correctly.

  @Test
  public void testMultipleGadgets() throws Exception {
    JSONArray gadgets = new JSONArray()
        .put(createGadget(FakeProcessor.SPEC_URL.toString(), 0, null))
        .put(createGadget(FakeProcessor.SPEC_URL2.toString(), 1, null));
    JSONObject input = new JSONObject()
        .put("context", createContext("en", "US"))
        .put("gadgets", gadgets);

    JSONObject response = jsonRpcHandler.process(input);

    JSONArray outGadgets = response.getJSONArray("gadgets");

    boolean first = false;
    boolean second = false;
    for (int i = 0, j = outGadgets.length(); i < j; ++i) {
      JSONObject gadget = outGadgets.getJSONObject(i);
      if (gadget.getString("url").equals(FakeProcessor.SPEC_URL.toString())) {
        assertEquals(FakeProcessor.SPEC_TITLE, gadget.getString("title"));
        assertEquals(0, gadget.getInt("moduleId"));
        first = true;
      } else {
        assertEquals(FakeProcessor.SPEC_TITLE2, gadget.getString("title"));
        assertEquals(1, gadget.getInt("moduleId"));
        second = true;
      }
    }

    assertTrue("First gadget not returned!", first);
    assertTrue("Second gadget not returned!", second);
  }

  @Test
  public void testMultipleGadgetsWithAnError() throws Exception {
    JSONArray gadgets = new JSONArray()
        .put(createGadget(FakeProcessor.SPEC_URL.toString(), 0, null))
        .put(createGadget(FakeProcessor.SPEC_URL2.toString(), 1, null));
    JSONObject input = new JSONObject()
        .put("context", createContext("en", "US"))
        .put("gadgets", gadgets);

    processor.exceptions.put(FakeProcessor.SPEC_URL2,
        new ProcessingException("broken", HttpServletResponse.SC_BAD_REQUEST));

    JSONObject response = jsonRpcHandler.process(input);

    JSONArray outGadgets = response.getJSONArray("gadgets");

    boolean first = false;
    boolean second = false;
    for (int i = 0, j = outGadgets.length(); i < j; ++i) {
      JSONObject gadget = outGadgets.getJSONObject(i);
      if (gadget.getString("url").equals(FakeProcessor.SPEC_URL.toString())) {
        assertEquals(FakeProcessor.SPEC_TITLE, gadget.getString("title"));
        assertEquals(0, gadget.getInt("moduleId"));
        first = true;
      } else {
        JSONArray errors = gadget.getJSONArray("errors");
        assertEquals(1, errors.length());
        assertEquals("broken", errors.optString(0));
        second = true;
      }
    }

    assertTrue("First gadget not returned!", first);
    assertTrue("Second gadget not returned!", second);
  }

}
