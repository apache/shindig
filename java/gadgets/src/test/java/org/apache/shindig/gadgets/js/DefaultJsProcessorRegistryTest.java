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

import static org.junit.Assert.*;

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
  private JsProcessor processor1;
  private JsProcessor processor2;
  private DefaultJsProcessorRegistry registry;

  @Before
  public void setUp() {
    control = EasyMock.createControl();
    request = control.createMock(JsRequest.class);
    response = new JsResponseBuilder();
    processor1 = control.createMock(JsProcessor.class);
    processor2 = control.createMock(JsProcessor.class);
    registry = new DefaultJsProcessorRegistry(ImmutableList.of(processor1, processor2));
  }

  @Test
  public void testProcessorModifiesResponse() throws Exception {
    JsProcessor processor = new JsProcessor() {
      public boolean process(JsRequest request, JsResponseBuilder builder) {
        builder.setJsCode(JS_CODE);
        return true;
      }
    };
    registry = new DefaultJsProcessorRegistry(ImmutableList.of(processor));
    control.replay();
    registry.process(request, response);
    assertEquals(JS_CODE, response.getJsCode().toString());
    control.verify();
  }

  @Test
  public void testTwoProcessorsAreRunOneAfterAnother() throws Exception {
    EasyMock.expect(processor1.process(request, response)).andReturn(true);
    EasyMock.expect(processor2.process(request, response)).andReturn(true);
    control.replay();
    registry.process(request, response);
    control.verify();
  }
  
  @Test
  public void testProcessorStopsProcessingWhenItReturnsFalse() throws Exception {
    EasyMock.expect(processor1.process(request, response)).andReturn(false);
    control.replay();
    registry.process(request, response);
    control.verify();    
  }
}
