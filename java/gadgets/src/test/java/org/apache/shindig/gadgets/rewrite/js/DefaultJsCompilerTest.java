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
package org.apache.shindig.gadgets.rewrite.js;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.js.JsContent;
import org.apache.shindig.gadgets.js.JsResponse;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.junit.Before;
import org.junit.Test;

import java.util.List;


public class DefaultJsCompilerTest {
  private final String COMPILE_CONTENT = "alert('compile');";
  private final String RESOURCE_CONTENT_DEB = "alert('deb');";
  private final String RESOURCE_CONTENT_OPT = "alert('opt');";
  private final String RESOURCE_URL_DEB = "deb.js";
  private final String RESOURCE_URL_OPT = "opt.js";

  private DefaultJsCompiler compiler;

  @Before
  public void setUp() throws Exception {
    compiler = new DefaultJsCompiler();
  }

  @Test
  public void testGetJsContentWithDeb() throws Exception {
    JsUri jsUri = mockJsUri(true); // debug
    FeatureResource extRes = mockResource(true, RESOURCE_URL_DEB, RESOURCE_URL_OPT);
    FeatureResource intRes = mockResource(false, RESOURCE_CONTENT_DEB, RESOURCE_CONTENT_OPT);
    FeatureBundle bundle = mockBundle(Lists.newArrayList(extRes, intRes));
    Iterable<JsContent> actual = compiler.getJsContent(jsUri, bundle);
    assertEquals(
        "document.write('<script src=\"" + RESOURCE_URL_DEB + "\"></script>');\n" +
        RESOURCE_CONTENT_DEB + ";\n",
        getContent(actual));
  }

  @Test
  public void testGetJsContentWithOpt() throws Exception {
    JsUri jsUri = mockJsUri(false); // optimized
    FeatureResource extRes = mockResource(true, RESOURCE_URL_DEB, RESOURCE_URL_OPT);
    FeatureResource intRes = mockResource(false, RESOURCE_CONTENT_DEB, RESOURCE_CONTENT_OPT);
    FeatureBundle bundle = mockBundle(Lists.newArrayList(extRes, intRes));
    Iterable<JsContent> actual = compiler.getJsContent(jsUri, bundle);
    assertEquals(
        "document.write('<script src=\"" + RESOURCE_URL_OPT + "\"></script>');\n" +
        RESOURCE_CONTENT_OPT + ";\n",
        getContent(actual));
  }

  @Test
  public void testCompile() throws Exception {
    JsResponse actual = compiler.compile(null,
        ImmutableList.of(JsContent.fromText(COMPILE_CONTENT, "js")), null);
    assertEquals(COMPILE_CONTENT, actual.toJsString());
    assertEquals(0, actual.getErrors().size());
  }

  private JsUri mockJsUri(boolean debug) {
    JsUri result = createMock(JsUri.class);
    expect(result.isDebug()).andReturn(debug).anyTimes();
    replay(result);
    return result;
  }

  private FeatureBundle mockBundle(List<FeatureResource> resources) {
    FeatureBundle result = createMock(FeatureBundle.class);
    expect(result.getResources()).andReturn(resources).anyTimes();
    expect(result.getName()).andReturn("feature").anyTimes();
    replay(result);
    return result;
  }

  private FeatureResource mockResource(boolean external, String debContent, String optContent) {
    FeatureResource result = createMock(FeatureResource.class);
    expect(result.getDebugContent()).andReturn(debContent).anyTimes();
    expect(result.getContent()).andReturn(optContent).anyTimes();
    expect(result.isExternal()).andReturn(external).anyTimes();
    expect(result.getName()).andReturn("source").anyTimes();
    replay(result);
    return result;
  }

  private String getContent(Iterable<JsContent> jsContent) {
    StringBuilder sb = new StringBuilder();
    for (JsContent js : jsContent) {
      sb.append(js.get());
    }
    return sb.toString();
  }
}
