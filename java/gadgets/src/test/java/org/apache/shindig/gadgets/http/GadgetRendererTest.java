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

package org.apache.shindig.gadgets.http;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import org.apache.shindig.gadgets.GadgetTestFixture;
import org.apache.shindig.gadgets.SyndicatorConfig;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class GadgetRendererTest extends GadgetTestFixture {

  final static Enumeration<String> EMPTY_PARAMS = new Enumeration<String>() {
    public boolean hasMoreElements() {
      return false;
    }
    public String nextElement() {
      return null;
    }
  };

  final ByteArrayOutputStream baos = new ByteArrayOutputStream();
  final PrintWriter writer = new PrintWriter(baos);

  final static URI SPEC_URL = URI.create("http://example.org/gadget.xml");
  final static String CONTENT = "Hello, world!";
  final static String ALT_CONTENT = "Goodbye, city.";
  final static String SPEC_XML
      = "<Module>" +
        "<ModulePrefs title=\"hello\"/>" +
        "<Content type=\"html\" quirks=\"false\">" + CONTENT + "</Content>" +
        "<Content type=\"html\" view=\"quirks\" quirks=\"true\"/>" +
        "<Content type=\"html\" view=\"ALIAS\">" + ALT_CONTENT + "</Content>" +
        "</Module>";
  final static String LIBS = "dummy:blah";

  /**
   * Performs boilerplate operations to get basic gadgets rendered
   * @return Output of the rendering request
   * @throws Exception
   */
  private String parseBasicGadget(String view) throws Exception {
    GadgetSpec spec = new GadgetSpec(SPEC_URL, SPEC_XML);
    expect(request.getParameter("url")).andReturn(SPEC_URL.toString());
    expect(request.getParameter("libs")).andReturn(LIBS);
    expect(request.getParameter("view")).andReturn(view);
    expect(request.getParameterNames()).andReturn(EMPTY_PARAMS);
    expect(specFetcher.fetch(eq(SPEC_URL), eq(false))).andReturn(spec);
    expect(response.getWriter()).andReturn(writer);
    replay();
    GadgetRenderer renderer = new GadgetRenderer(request, response, state);
    renderer.process();
    verify();
    writer.close();
    return new String(baos.toByteArray(), "UTF-8");
  }

  public void testStandardsMode() throws Exception {
    String content = parseBasicGadget(GadgetSpec.DEFAULT_VIEW);
    assertTrue(-1 != content.indexOf(GadgetRenderer.STRICT_MODE_DOCTYPE));
  }

  public void testQuirksMode() throws Exception {
    String content = parseBasicGadget("quirks");
    assertTrue(-1 == content.indexOf(GadgetRenderer.STRICT_MODE_DOCTYPE));
  }


  public void testContentRendered() throws Exception {
    String content = parseBasicGadget(GadgetSpec.DEFAULT_VIEW);
    assertTrue(-1 != content.indexOf(CONTENT));
  }

  public void testForcedLibsIncluded() throws Exception {
    String content = parseBasicGadget(GadgetSpec.DEFAULT_VIEW);
    Set<String> libs = new HashSet<String>();
    libs.addAll(Arrays.asList(LIBS.split(":")));
    String libStr = state.getJsUrl(libs, null);
    assertTrue(-1 != content.indexOf("<script src=\"" + libStr + "\">"));
  }

  public void testViewAliases() throws Exception {
    JSONObject json = new JSONObject();
    json.put("gadgets.syndicator",
             new JSONArray().put(SyndicatorConfig.DEFAULT_SYNDICATOR));
    JSONArray aliases = new JSONArray().put("ALIAS");
    JSONObject dummy = new JSONObject().put("aliases", aliases);
    JSONObject views = new JSONObject().put("dummy", dummy);
    JSONObject features = new JSONObject().put("views", views);
    json.put("gadgets.features", features);
    syndicatorConfig.loadFromString(json.toString());

    String content = parseBasicGadget("dummy");

    assertTrue(-1 != content.indexOf(ALT_CONTENT));
  }

  // TODO: Lots of ugly tests on html content.
}
