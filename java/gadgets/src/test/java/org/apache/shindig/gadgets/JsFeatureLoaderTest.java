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

import junit.framework.TestCase;

import java.util.List;

public class JsFeatureLoaderTest extends TestCase {

  GadgetFeatureRegistry registry;
  JsFeatureLoader loader;

  private static final String FEATURE_NAME = "test";
  private static final String DEF_JS_CONTENT = "var hello = 'world';";
  private static final String ALT_JS_CONTENT = "function test(){while(true);}";
  private static final String SYND_A = "test";
  private static final String SYND_B = "wuwowowaefdf";


  @Override
  public void setUp() throws Exception {
    registry = new GadgetFeatureRegistry(null);
    loader = new JsFeatureLoader();
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
    List<JsLibrary> libs = feature.getJsLibraries(RenderingContext.GADGET,
                                                  new ProcessingOptions());
    assertEquals(1, libs.size());
    assertEquals(JsLibrary.Type.INLINE, libs.get(0).getType());
    assertEquals(DEF_JS_CONTENT, libs.get(0).getContent());
  }

  public void testMultiSyndicators() throws Exception {
    String xml = "<feature>" +
                 "  <name>" + FEATURE_NAME + "</name>" +
                 "  <gadget synd=\"" + SYND_A + "\">" +
                 "    <script>" + DEF_JS_CONTENT + "</script>" +
                 "  </gadget>" +
                 "  <gadget synd=\"" + SYND_B + "\">" +
                 "    <script>" + ALT_JS_CONTENT + "</script>" +
                 "  </gadget>" +
                 "</feature>";
    GadgetFeatureRegistry.Entry entry = loader.loadFeature(registry, xml);
    GadgetFeature feature = entry.getFeature().create();
    List<JsLibrary> libs;
    libs = feature.getJsLibraries(RenderingContext.GADGET,
                                  new SyndOptions(SYND_A));
    assertEquals(DEF_JS_CONTENT, libs.get(0).getContent());
   libs = feature.getJsLibraries(RenderingContext.GADGET,
                                 new SyndOptions(SYND_B));
    assertEquals(ALT_JS_CONTENT, libs.get(0).getContent());
  }
}

class SyndOptions extends ProcessingOptions {
  private final String syndicator;
  @Override
  public String getSyndicator() {
    return syndicator;
  }
  public SyndOptions(String syndicator) {
    this.syndicator = syndicator;
  }
}
