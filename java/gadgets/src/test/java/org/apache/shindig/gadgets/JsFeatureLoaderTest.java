/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class JsFeatureLoaderTest extends GadgetTestFixture {
  JsFeatureLoader loader;

  private static final String FEATURE_NAME = "test";
  private static final String ALT_FEATURE_NAME = "test2";
  private static final String DEF_JS_CONTENT = "var hello = 'world';";
  private static final String ALT_JS_CONTENT = "function test(){while(true);}";
  private static final String CONT_A = "test";
  private static final String CONT_B = "wuwowowaefdf";
  private static final URI JS_URL = URI.create("http://example.org/feature.js");

  @Override
  public void setUp() throws Exception {
    super.setUp();
    loader = new JsFeatureLoader(fetcher);
  }

  private JsLibrary getJsLib(GadgetFeatureRegistry.Entry entry) {
    GadgetFeature feature = entry.getFeature().create();
    return feature.getJsLibraries(new GadgetContext()).get(0);
  }

  public void testBasicLoading() throws Exception {
    String xml = "<feature>" +
                 "  <name>" + FEATURE_NAME + "</name>" +
                 "  <gadget>" +
                 "    <script>" + DEF_JS_CONTENT + "</script>" +
                 "  </gadget>" +
                 "</feature>";
    GadgetFeatureRegistry.Entry entry = loader.loadFeature(registry, xml);

    assertEquals(FEATURE_NAME, entry.getName());
    GadgetFeature feature = entry.getFeature().create();
    List<JsLibrary> libs = feature.getJsLibraries(new GadgetContext());
    assertEquals(1, libs.size());
    assertEquals(JsLibrary.Type.INLINE, libs.get(0).getType());
    assertEquals(DEF_JS_CONTENT, libs.get(0).getContent());
  }

  public void testMultiContainers() throws Exception {
    String xml = "<feature>" +
                 "  <name>" + FEATURE_NAME + "</name>" +
                 "  <gadget container=\"" + CONT_A + "\">" +
                 "    <script>" + DEF_JS_CONTENT + "</script>" +
                 "  </gadget>" +
                 "  <gadget container=\"" + CONT_B + "\">" +
                 "    <script>" + ALT_JS_CONTENT + "</script>" +
                 "  </gadget>" +
                 "</feature>";
    GadgetFeatureRegistry.Entry entry = loader.loadFeature(registry, xml);
    GadgetFeature feature = entry.getFeature().create();
    List<JsLibrary> libs;
    libs = feature.getJsLibraries(new ContainerContext(CONT_A));
    assertEquals(DEF_JS_CONTENT, libs.get(0).getContent());
    libs = feature.getJsLibraries(new ContainerContext(CONT_B));
    assertEquals(ALT_JS_CONTENT, libs.get(0).getContent());
  }

  public void testFileReferences() throws Exception {
    File temp = File.createTempFile(getName(), ".js-noopt");
    BufferedWriter out = new BufferedWriter(new FileWriter(temp));
    out.write(DEF_JS_CONTENT);
    out.close();
    String xml = "<feature>" +
                 "  <name>" + FEATURE_NAME + "</name>" +
                 "  <gadget>" +
                 "    <script src=\"" + temp.getPath() + "\"/>" +
                 "  </gadget>" +
                 "</feature>";
    GadgetFeatureRegistry.Entry entry = loader.loadFeature(registry, xml);
    GadgetFeature feature = entry.getFeature().create();
    List<JsLibrary> libs = feature.getJsLibraries(new GadgetContext());
    assertEquals(1, libs.size());
    assertEquals(DEF_JS_CONTENT, libs.get(0).getContent());
    assertEquals(FEATURE_NAME, libs.get(0).getFeature());
  }

  public void testUrlReferences() throws Exception {
    String xml = "<feature>" +
                 "  <name>" + FEATURE_NAME + "</name>" +
                 "  <gadget>" +
                 "    <script src=\"" + JS_URL + "\"/>" +
                 "  </gadget>" +
                 "</feature>";
    HttpRequest request = new HttpRequest(JS_URL);
    HttpResponse response
        = new HttpResponse(200, ALT_JS_CONTENT.getBytes(), null);
    expect(fetcher.fetch(eq(request))).andReturn(response);
    replay();
    GadgetFeatureRegistry.Entry entry = loader.loadFeature(registry, xml);
    verify();
    GadgetFeature feature = entry.getFeature().create();
    List<JsLibrary> libs = feature.getJsLibraries(new GadgetContext());
    assertEquals(1, libs.size());
    assertEquals(ALT_JS_CONTENT, libs.get(0).getContent());
    assertEquals(FEATURE_NAME, libs.get(0).getFeature());
  }

  private File makeFeatureFile(String name, String content) throws Exception {
    String xml = "<feature>" +
                 "  <name>" + name + "</name>" +
                 "  <gadget>" +
                 "    <script>" + content + "</script>" +
                 "  </gadget>" +
                 "</feature>";
    File file = File.createTempFile(getName(), name + ".xml");
    file.deleteOnExit();
    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    out.write(xml);
    out.close();
    return file;
  }

  public void testMultiplePaths() throws Exception {
    File file1 = makeFeatureFile(FEATURE_NAME, DEF_JS_CONTENT);
    File file2 = makeFeatureFile(ALT_FEATURE_NAME, ALT_JS_CONTENT);

    loader.loadFeatures(file1.getAbsolutePath() +
                        JsFeatureLoader.FILE_SEPARATOR +
                        file2.getAbsolutePath(), registry);
    // TODO: This is too fragile. GadgetFeatureRegistry needs to be fixed.
    Map<String, GadgetFeatureRegistry.Entry> entries
        = registry.getAllFeatures();

    JsLibrary lib1 = getJsLib(entries.get(FEATURE_NAME));
    assertEquals(DEF_JS_CONTENT, lib1.getContent());

    JsLibrary lib2 = getJsLib(entries.get(ALT_FEATURE_NAME));
    assertEquals(ALT_JS_CONTENT, lib2.getContent());
  }
}

class ContainerContext extends GadgetContext {
  private final String container;
  @Override
  public String getContainer() {
    return container;
  }
  public ContainerContext(String container) {
    this.container = container;
  }
}
