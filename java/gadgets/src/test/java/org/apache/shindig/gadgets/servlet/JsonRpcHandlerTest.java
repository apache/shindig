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
import static org.easymock.EasyMock.isA;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;

public class JsonRpcHandlerTest extends ServletTestFixture {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/g.xml");
  private static final HttpRequest SPEC_REQUEST = new HttpRequest(SPEC_URL);
  private static final Uri SPEC_URL2 = Uri.parse("http://example.org/g2.xml");
  private static final HttpRequest SPEC_REQUEST2 = new HttpRequest(SPEC_URL2);
  private static final String SPEC_TITLE = "JSON-TEST";
  private static final String SPEC_TITLE2 = "JSON-TEST2";
  private static final int PREFERRED_HEIGHT = 100;
  private static final int PREFERRED_WIDTH = 242;
  private static final String LINK_REL = "rel";
  private static final String LINK_HREF = "http://example.org/foo";
  private static final String SPEC_XML
      = "<Module>" +
        "<ModulePrefs title=\"" + SPEC_TITLE + "\">" +
        "  <Link rel='" + LINK_REL + "' href='" + LINK_HREF + "'/>" +
        "</ModulePrefs>" +
        "<UserPref name=\"up_one\">" +
        "  <EnumValue value=\"val1\" display_value=\"disp1\"/>" +
        "  <EnumValue value=\"abc\" display_value=\"disp2\"/>" +
        "  <EnumValue value=\"z_xabc\" display_value=\"disp3\"/>" +
        "  <EnumValue value=\"foo\" display_value=\"disp4\"/>" +
        "</UserPref>" +
        "<Content type=\"html\"" +
        " preferred_height = \"" + PREFERRED_HEIGHT + '\"' +
        " preferred_width = \"" + PREFERRED_WIDTH + '\"' +
        ">Hello, world</Content>" +
        "</Module>";
  private static final String SPEC_XML2
      = "<Module>" +
        "<ModulePrefs title=\"" + SPEC_TITLE2 + "\"/>" +
        "<Content type=\"html\">Hello, world</Content>" +
        "</Module>";

  private JSONObject createContext(String lang, String country)
      throws JSONException {
    return new JSONObject().put("language", lang).put("country", country);
  }

  private JSONObject createGadget(String url, int moduleId,
      Map<String, String> prefs) throws JSONException {
    return new JSONObject()
        .put("url", url)
        .put("moduleId", moduleId)
        .put("prefs", prefs == null ? Collections.emptySet() : prefs);
  }

  public void testSimpleRequest() throws Exception {
    JSONArray gadgets = new JSONArray()
      .put(createGadget(SPEC_URL.toString(), 0, null));
    JSONObject input = new JSONObject()
        .put("context", createContext("en", "US"))
        .put("gadgets", gadgets);

    expect(fetcher.fetch(SPEC_REQUEST)).andReturn(new HttpResponse(SPEC_XML));
    expect(urlGenerator.getIframeUrl(isA(Gadget.class)))
        .andReturn(SPEC_URL.toString());

    replay();
    JSONObject response = jsonRpcHandler.process(input);
    verify();

    JSONArray outGadgets = response.getJSONArray("gadgets");
    JSONObject gadget = outGadgets.getJSONObject(0);
    assertEquals(SPEC_URL.toString(), gadget.getString("iframeUrl"));
    assertEquals(SPEC_TITLE, gadget.getString("title"));
    assertEquals(0, gadget.getInt("moduleId"));
    JSONObject view = gadget.getJSONObject("views")
        .getJSONObject(GadgetSpec.DEFAULT_VIEW);
    assertEquals(PREFERRED_HEIGHT, view.getInt("preferredHeight"));
    assertEquals(PREFERRED_WIDTH, view.getInt("preferredWidth"));
    assertEquals(LINK_HREF, gadget.getJSONObject("links").getString(LINK_REL));
    
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

  // TODO: Verify that user pref specs are returned correctly.

  public void testMultipleGadgets() throws Exception {
    JSONArray gadgets = new JSONArray()
     .put(createGadget(SPEC_URL.toString(), 0, null))
     .put(createGadget(SPEC_URL2.toString(), 1, null));
    JSONObject input = new JSONObject()
        .put("context", createContext("en", "US"))
        .put("gadgets", gadgets);

    expect(fetcher.fetch(SPEC_REQUEST))
        .andReturn(new HttpResponse(SPEC_XML));
    expect(fetcher.fetch(SPEC_REQUEST2))
        .andReturn(new HttpResponse(SPEC_XML2));

    replay();
    JSONObject response = jsonRpcHandler.process(input);
    verify();

    JSONArray outGadgets = response.getJSONArray("gadgets");

    boolean first = false;
    boolean second = false;
    for (int i = 0, j = outGadgets.length(); i < j; ++i) {
      JSONObject gadget = outGadgets.getJSONObject(i);
      if (gadget.getString("url").equals(SPEC_URL.toString())) {
        assertEquals(SPEC_TITLE, gadget.getString("title"));
        assertEquals(0, gadget.getInt("moduleId"));
        first = true;
      } else {
        assertEquals(SPEC_TITLE2, gadget.getString("title"));
        assertEquals(1, gadget.getInt("moduleId"));
        second = true;
      }
    }

    assertTrue("First gadget not returned!", first);
    assertTrue("Second gadget not returned!", second);
  }
}
