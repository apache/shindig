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

import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.testing.FakeHttpServletRequest;
import org.apache.shindig.common.testing.TestExecutorService;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.UrlGenerator;
import org.apache.shindig.gadgets.UrlValidationStatus;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.process.Processor;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;

import com.google.common.collect.Maps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JsonRpcHandlerTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/g.xml");
  private static final Uri SPEC_URL2 = Uri.parse("http://example.org/g2.xml");
  private static final String SPEC_TITLE = "JSON-TEST";
  private static final String SPEC_TITLE2 = "JSON-TEST2";
  private static final int PREFERRED_HEIGHT = 100;
  private static final int PREFERRED_WIDTH = 242;
  private static final String LINK_REL = "rel";
  private static final String LINK_HREF = "http://example.org/foo";
  private static final String SPEC_XML =
      "<Module>" +
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
  private static final String SPEC_XML2 =
      "<Module>" +
      "<ModulePrefs title=\"" + SPEC_TITLE2 + "\"/>" +
      "<Content type=\"html\">Hello, world</Content>" +
      "</Module>";

  private final FakeProcessor processor = new FakeProcessor();
  private final FakeUrlGenerator urlGenerator = new FakeUrlGenerator();
  private final JsonRpcHandler jsonRpcHandler
      = new JsonRpcHandler(new TestExecutorService(), processor, urlGenerator, null, new FakeGadgetToken.Decoder());

  private JSONObject createContext(String lang, String country)
      throws JSONException {
    return new JSONObject().put("language", lang).put("country", country);
  }

  @Before
  public void setUp() {
    processor.gadgets.put(SPEC_URL, SPEC_XML);
    processor.gadgets.put(SPEC_URL2, SPEC_XML2);
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
        .put(createGadget(SPEC_URL.toString(), 0, null));
    JSONObject input = new JSONObject()
        .put("context", createContext("en", "US"))
        .put("gadgets", gadgets);

    urlGenerator.iframeUrl = SPEC_URL.toString();
    HttpServletRequest req = new FakeHttpServletRequest();

    JSONObject response = jsonRpcHandler.process(req, input);

    JSONArray outGadgets = response.getJSONArray("gadgets");
    JSONObject gadget = outGadgets.getJSONObject(0);
    assertEquals(SPEC_URL.toString(), gadget.getString("iframeUrl"));
    assertEquals(SPEC_TITLE, gadget.getString("title"));
    assertEquals(0, gadget.getInt("moduleId"));

    JSONObject view = gadget.getJSONObject("views").getJSONObject(GadgetSpec.DEFAULT_VIEW);
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

  @Test
  public void testUnexpectedError() throws Exception {
    HttpServletRequest req = new FakeHttpServletRequest();
    JSONArray gadgets = new JSONArray()
        .put(createGadget(SPEC_URL.toString(), 0, null));
    JSONObject input = new JSONObject()
        .put("context", createContext("en", "US"))
        .put("gadgets", gadgets);

    urlGenerator.throwRandomFault = true;
    JSONObject resp = jsonRpcHandler.process(req, input);
    String actual = resp.getJSONArray("gadgets").getJSONObject(0).getJSONArray("errors").getString(0);
    assertEquals("BROKEN", actual);
  }

  // TODO: Verify that user pref specs are returned correctly.

  @Test
  public void testMultipleGadgets() throws Exception {
    HttpServletRequest req = new FakeHttpServletRequest();

    JSONArray gadgets = new JSONArray()
        .put(createGadget(SPEC_URL.toString(), 0, null))
        .put(createGadget(SPEC_URL2.toString(), 1, null));
    JSONObject input = new JSONObject()
        .put("context", createContext("en", "US"))
        .put("gadgets", gadgets);

    JSONObject response = jsonRpcHandler.process(req, input);

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

  @Test
  public void testMultipleGadgetsWithAnError() throws Exception {
    HttpServletRequest req = new FakeHttpServletRequest();

    JSONArray gadgets = new JSONArray()
        .put(createGadget(SPEC_URL.toString(), 0, null))
        .put(createGadget(SPEC_URL2.toString(), 1, null));
    JSONObject input = new JSONObject()
        .put("context", createContext("en", "US"))
        .put("gadgets", gadgets);

    processor.exceptions.put(SPEC_URL2, 
        new ProcessingException("broken", HttpServletResponse.SC_BAD_REQUEST));

    JSONObject response = jsonRpcHandler.process(req, input);

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
        JSONArray errors = gadget.getJSONArray("errors");
        assertEquals(1, errors.length());
        assertEquals("broken", errors.optString(0));
        second = true;
      }
    }

    assertTrue("First gadget not returned!", first);
    assertTrue("Second gadget not returned!", second);
  }

  private static class FakeProcessor extends Processor {
    protected final Map<Uri, ProcessingException> exceptions = Maps.newHashMap();
    protected final Map<Uri, String> gadgets = Maps.newHashMap();

    public FakeProcessor() {
      super(null, null, null, null, null);
    }

    @Override
    public Gadget process(GadgetContext context) throws ProcessingException {

      ProcessingException exception = exceptions.get(context.getUrl());
      if (exception != null) {
        throw exception;
      }

      try {
        GadgetSpec spec = new GadgetSpec(Uri.parse("#"), gadgets.get(context.getUrl()));
        View view = spec.getView(context.getView());
        return new Gadget()
            .setContext(context)
            .setSpec(spec)
            .setCurrentView(view);
      } catch (GadgetException e) {
        throw new RuntimeException(e);
      }
    }
  }

  protected static class FakeUrlGenerator implements UrlGenerator {
    protected boolean throwRandomFault = false;
    protected String iframeUrl = "http://example.org/gadgets/foo-does-not-matter";

    protected FakeUrlGenerator() {
    }

    public String getBundledJsParam(Collection<String> features, GadgetContext context) {
      throw new UnsupportedOperationException();
    }

    public String getBundledJsUrl(Collection<String> features, GadgetContext context) {
      throw new UnsupportedOperationException();
    }
    
    public UrlValidationStatus validateJsUrl(String jsUrl) {
      throw new UnsupportedOperationException();
    }

    public String getIframeUrl(Gadget gadget) {
      if (throwRandomFault) {
        throw new RuntimeException("BROKEN");
      }
      return iframeUrl;
    }
    
    public UrlValidationStatus validateIframeUrl(String url) {
      throw new UnsupportedOperationException();
    }

    public String getGadgetDomainOAuthCallback(String container, String gadgetHost) {
      throw new UnsupportedOperationException();
    }
  }
}
