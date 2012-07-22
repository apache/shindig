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
package org.apache.shindig.gadgets.js;

import static org.apache.shindig.gadgets.js.CajaJsSubtractingProcessor.ATTRIB_VALUE;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.UriCommon;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class CajaJsSubtractingProcessorTest {

  private static final List<String> ERRORS = ImmutableList.<String>of();

  private static final String SOURCE = "source";
  private static final String NORMAL_CONTENT_JS = "normal";
  private static final String CAJA_CONTENT_JS = "cajoled";

  private IMocksControl control;
  private List<JsContent> contents = Lists.newArrayList();
  private JsResponse response;
  private JsResponseBuilder builder;

  private CajaJsSubtractingProcessor processor;

  @Before
  public void setUp() {
    control = createControl();

    contents = Lists.newArrayList();
    contents.add(JsContent.fromFeature(NORMAL_CONTENT_JS, SOURCE, null, null));
    contents.add(JsContent.fromFeature(NORMAL_CONTENT_JS, SOURCE, null,
        mockFeatureResource(null)));
    contents.add(JsContent.fromFeature(NORMAL_CONTENT_JS, SOURCE, null,
        mockFeatureResource(ImmutableMap.of(UriCommon.Param.CAJOLE.getKey(), "blah"))));
    contents.add(JsContent.fromFeature(CAJA_CONTENT_JS, SOURCE, null,
        mockFeatureResource(ImmutableMap.of(UriCommon.Param.CAJOLE.getKey(), ATTRIB_VALUE))));

    response = new JsResponse(contents, -1, -1, false, ERRORS, null);
    builder = new JsResponseBuilder(response);

    processor = new CajaJsSubtractingProcessor();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void noCajoleRequest() throws Exception {
    JsUri uri = mockJsUri(false);
    JsRequest request = mockJsRequest(uri);

    control.replay();
    boolean actualReturn = processor.process(request, builder);
    JsResponse actualResponse = builder.build();

    control.verify();
    assertTrue(actualReturn);
    assertEquals(Strings.repeat(NORMAL_CONTENT_JS, 3), actualResponse.toJsString());
  }

  @Test
  public void cajoleRequest() throws Exception {
    JsUri uri = mockJsUri(true);
    JsRequest request = mockJsRequest(uri);

    control.replay();
    boolean actualReturn = processor.process(request, builder);
    JsResponse actualResponse = builder.build();

    control.verify();
    assertTrue(actualReturn);
    assertEquals(Strings.repeat(NORMAL_CONTENT_JS, 3) + CAJA_CONTENT_JS,
        actualResponse.toJsString());
  }

  private FeatureResource mockFeatureResource(Map<String, String> map) {
    FeatureResource result = control.createMock(FeatureResource.class);
    expect(result.getAttribs()).andReturn(map).anyTimes();
    return result;
  }

  private JsUri mockJsUri(boolean cajole) {
    JsUri uri = control.createMock(JsUri.class);
    expect(uri.cajoleContent()).andReturn(cajole).anyTimes();
    return uri;
  }

  private JsRequest mockJsRequest(JsUri uri) {
    JsRequest request = control.createMock(JsRequest.class);
    expect(request.getJsUri()).andReturn(uri).anyTimes();
    return request;
  }
}
