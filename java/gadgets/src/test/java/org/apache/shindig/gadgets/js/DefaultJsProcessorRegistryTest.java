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

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link DefaultJsProcessorRegistry}.
 */
public class DefaultJsProcessorRegistryTest {

  private static final String JS_CODE = "some JS code";

  private IMocksControl control;
  private JsRequest request;
  private JsResponseBuilder response;
  private JsProcessor processor0;
  private JsProcessor processor1;
  private JsProcessor processor2;
  private JsProcessor processor3;
  private DefaultJsProcessorRegistry registry;

  @Before
  public void setUp() {
    control = EasyMock.createControl();
    request = control.createMock(JsRequest.class);
    response = new JsResponseBuilder();
    processor0 = control.createMock(JsProcessor.class);
    processor1 = control.createMock(JsProcessor.class);
    processor2 = control.createMock(JsProcessor.class);
    processor3 = control.createMock(JsProcessor.class);
    registry = new DefaultJsProcessorRegistry(
        ImmutableList.of(processor0),
        ImmutableList.of(processor1, processor2),
        ImmutableList.of(processor3));
  }

  @Test
  public void testProcessorModifiesResponse() throws Exception {
    JsProcessor preprocessor = new JsProcessor() {
      public boolean process(JsRequest request, JsResponseBuilder builder) {
        return true;
      }
    };
    JsProcessor processor = new JsProcessor() {
      public boolean process(JsRequest request, JsResponseBuilder builder) {
        builder.clearJs().appendJs(JS_CODE, "js");
        return true;
      }
    };
    registry = new DefaultJsProcessorRegistry(
        ImmutableList.of(preprocessor),
        ImmutableList.of(processor),
        ImmutableList.<JsProcessor>of());
    control.replay();
    registry.process(request, response);
    assertEquals(JS_CODE, response.build().toJsString());
    control.verify();
  }

  @Test
  public void testTwoProcessorsAreRunOneAfterAnother() throws Exception {
    EasyMock.expect(processor0.process(request, response)).andReturn(true);
    EasyMock.expect(processor1.process(request, response)).andReturn(true);
    EasyMock.expect(processor2.process(request, response)).andReturn(true);
    EasyMock.expect(processor3.process(request, response)).andReturn(true);
    control.replay();
    registry.process(request, response);
    control.verify();
  }

  @Test
  public void testProcessorStopsProcessingWhenItReturnsFalse() throws Exception {
    EasyMock.expect(processor0.process(request, response)).andReturn(true);
    EasyMock.expect(processor1.process(request, response)).andReturn(false);
    EasyMock.expect(processor3.process(request, response)).andReturn(true);
    control.replay();
    registry.process(request, response);
    control.verify();
  }

  @Test
  public void testProcessorStopsProcessingWhenPreProcessorsReturnsFalse() throws Exception {
    EasyMock.expect(processor0.process(request, response)).andReturn(false);
    control.replay();
    registry.process(request, response);
    control.verify();
  }
}
