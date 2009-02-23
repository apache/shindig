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

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import com.google.common.collect.Maps;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class JsFeatureLoaderTest extends EasyMockTestCase {
  private final HttpFetcher fetcher = mock(HttpFetcher.class);
  private final JsFeatureLoader loader = new JsFeatureLoader(fetcher);
  private GadgetFeatureRegistry registry;

  private static final String FEATURE_NAME = "test";
  private static final String ALT_FEATURE_NAME = "test2";
  private static final String DEF_JS_CONTENT = "var hello = 'world';";
  private static final String ALT_JS_CONTENT = "function test(){while(true);}";
  private static final String CONT_A = "test";
  private static final String CONT_B = "wuwowowaefdf";
  private static final Uri JS_URL = Uri.parse("http://example.org/feature.js");

  @Override
  public void setUp() throws Exception {
    super.setUp();
    registry = new GadgetFeatureRegistry(null, fetcher);
  }

  private JsLibrary getJsLib(GadgetFeature feature) {
    return feature.getJsLibraries(
        RenderingContext.GADGET, ContainerConfig.DEFAULT_CONTAINER).get(0);
  }

  public void testBasicLoading() throws Exception {
    String xml = "<feature>" +
                 "  <name>" + FEATURE_NAME + "</name>" +
                 "  <gadget>" +
                 "    <script>" + DEF_JS_CONTENT + "</script>" +
                 "  </gadget>" +
                 "</feature>";
    GadgetFeature feature = loader.loadFeature(registry, xml);

    assertEquals(FEATURE_NAME, feature.getName());
    JsLibrary lib = getJsLib(feature);
    assertEquals(JsLibrary.Type.INLINE, lib.getType());
    assertEquals(DEF_JS_CONTENT, lib.getContent());
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
    GadgetFeature feature = loader.loadFeature(registry, xml);
    List<JsLibrary> libs;
    libs = feature.getJsLibraries(RenderingContext.GADGET, CONT_A);
    assertEquals(DEF_JS_CONTENT, libs.get(0).getContent());
    libs = feature.getJsLibraries(RenderingContext.GADGET, CONT_B);
    assertEquals(ALT_JS_CONTENT, libs.get(0).getContent());
  }

  public void testFileReferences() throws Exception {
    File temp = File.createTempFile(getName(), ".js-noopt");
    temp.deleteOnExit();
    BufferedWriter out = new BufferedWriter(new FileWriter(temp));
    out.write(DEF_JS_CONTENT);
    out.close();
    String xml = "<feature>" +
                 "  <name>" + FEATURE_NAME + "</name>" +
                 "  <gadget>" +
                 "    <script src=\"" + temp.getPath() + "\"/>" +
                 "  </gadget>" +
                 "</feature>";
    GadgetFeature feature = loader.loadFeature(registry, xml);
    JsLibrary lib = getJsLib(feature);
    assertEquals(DEF_JS_CONTENT, lib.getContent());
    assertEquals(FEATURE_NAME, lib.getFeature());
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
        = new HttpResponseBuilder().setResponse(ALT_JS_CONTENT.getBytes()).create();
    expect(fetcher.fetch(eq(request))).andReturn(response);
    replay();
    GadgetFeature feature = loader.loadFeature(registry, xml);
    verify();

    JsLibrary lib = getJsLib(feature);
    assertEquals(ALT_JS_CONTENT, lib.getContent());
    assertEquals(FEATURE_NAME, lib.getFeature());
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
    Collection<GadgetFeature> features = registry.getAllFeatures();

    Map<String, GadgetFeature> map = Maps.newHashMap();
    for (GadgetFeature feature : features) {
      map.put(feature.getName(), feature);
    }

    JsLibrary lib1 = getJsLib(map.get(FEATURE_NAME));
    assertEquals(DEF_JS_CONTENT, lib1.getContent());

    JsLibrary lib2 = getJsLib(map.get(ALT_FEATURE_NAME));
    assertEquals(ALT_JS_CONTENT, lib2.getContent());

    // Test with comma in the path
    file1 = makeFeatureFile("test,test", DEF_JS_CONTENT);
    file2 = makeFeatureFile("test2,test2", ALT_JS_CONTENT);

    try {
        loader.loadFeatures(file1.getAbsolutePath() +
                            JsFeatureLoader.FILE_SEPARATOR +
                            file2.getAbsolutePath(), registry);
    } catch (GadgetException e ) {
        if (e.getCode() != GadgetException.Code.INVALID_PATH) {
            throw e;
        }

        assertTrue("Invalid path catched", true);
    }
  }
}