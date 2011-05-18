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

package org.apache.shindig.gadgets.js;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureRegistry.LookupResult;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.UriStatus;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Tests for {@link AddJslLoadedVariableProcessor}.
 */
public class AddJslLoadedVariableProcessorTest {
  private static final String REQ_1_LIB = "foo";
  private static final String REQ_1_DEP_LIB = "bar";
  private static final String REQ_2_LIB = "gig";
  private static final String LOAD_LIB = "bar";
  private static final String URI = "http://localhost";
  private static final List<String> REQ_LIBS = ImmutableList.of(
      REQ_1_LIB, REQ_2_LIB, REQ_1_LIB, REQ_2_LIB);
  private static final List<String> LOAD_LIBS = ImmutableList.of(
      LOAD_LIB, LOAD_LIB);

  private IMocksControl control;
  private JsRequest request;
  private FeatureRegistry registry;
  private JsUri jsUri;
  private JsResponseBuilder response;
  private AddJslLoadedVariableProcessor processor;

  @Before
  public void setUp() {
    control = EasyMock.createControl();
    request = control.createMock(JsRequest.class);
    registry = control.createMock(FeatureRegistry.class);
    response = new JsResponseBuilder();
    processor = new AddJslLoadedVariableProcessor(registry);
  }

  @Test
  public void testSucceeds() throws Exception {
    setUpJsUri(URI + "/" + REQ_1_LIB + ".js");
    setUpRegistry();
    control.replay();
    processor.process(request, response);
    assertEquals(String.format(AddJslLoadedVariableProcessor.TEMPLATE,
        "['foo','gig']"), response.build().toJsString());
    control.verify();
  }

  @Test
  public void testSkips() throws Exception {
    setUpJsUri(URI + "?nohint=1");
    control.replay();
    processor.process(request, response);
    assertEquals("", response.build().toJsString());
    control.verify();
  }

  private void setUpJsUri(String uri) {
    jsUri = new JsUri(UriStatus.VALID_UNVERSIONED, Uri.parse(uri), REQ_LIBS, LOAD_LIBS);
    EasyMock.expect(request.getJsUri()).andReturn(jsUri);
  }

  @SuppressWarnings("unchecked")
  private void setUpRegistry() {
    FeatureBundle bundle1 = mockBundle(REQ_1_LIB);
    FeatureBundle bundle2 = mockBundle(REQ_1_DEP_LIB);
    FeatureBundle bundle3 = mockBundle(REQ_2_LIB);
    FeatureBundle bundle4 = mockBundle(LOAD_LIB);
    LookupResult reqResult = mockLookupResult(Lists.newArrayList(bundle1, bundle2, bundle3));
    expect(registry.getFeatureResources(isA(JsGadgetContext.class), eq(REQ_LIBS),
        isNull(List.class))).andReturn(reqResult);
    LookupResult loadResult = mockLookupResult(Lists.newArrayList(bundle4));
    expect(registry.getFeatureResources(isA(JsGadgetContext.class), eq(LOAD_LIBS),
        isNull(List.class))).andReturn(loadResult);
  }

  private LookupResult mockLookupResult(List<FeatureBundle> bundles) {
    LookupResult result = control.createMock(LookupResult.class);
    expect(result.getBundles()).andReturn(bundles);
    return result;
  }

  private FeatureBundle mockBundle(String name) {
    FeatureBundle result = control.createMock(FeatureBundle.class);
    expect(result.getName()).andReturn(name);
    return result;
  }
}
