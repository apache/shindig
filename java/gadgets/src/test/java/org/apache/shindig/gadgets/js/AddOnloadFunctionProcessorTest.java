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

import static org.junit.Assert.*;

import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;


/**
 * Tests for {@link AddOnloadFunctionProcessor}.
 */
public class AddOnloadFunctionProcessorTest {

  private static final String ONLOAD_FUNCTION = "onloadFunc";

  private IMocksControl control;
  private JsUri jsUri;
  private JsRequest request;
  private JsResponseBuilder response;
  private AddOnloadFunctionProcessor processor;

  @Before
  public void setUp() {
    control = EasyMock.createControl();
    jsUri = control.createMock(JsUri.class);
    request = control.createMock(JsRequest.class);
    response = new JsResponseBuilder();
    processor = new AddOnloadFunctionProcessor();

    EasyMock.expect(request.getJsUri()).andReturn(jsUri);
  }

  @Test
  public void testSkipsWhenNoOnloadAndWithHintSpecified() throws Exception {
    EasyMock.expect(jsUri.getOnload()).andReturn(null);
    EasyMock.expect(jsUri.isNohint()).andReturn(false);
    response = control.createMock(JsResponseBuilder.class);
    control.replay();
    assertTrue(processor.process(request, response));
    control.verify();
  }

  @Test
  public void testFailsWithInvalidFunctionName() throws Exception {
    EasyMock.expect(jsUri.getOnload()).andReturn("!!%%!!%%");
    control.replay();
    try {
      processor.process(request, response);
      fail("A JsException should have been thrown by the processor.");
    } catch (JsException e) {
      assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getStatusCode());
      assertEquals(AddOnloadFunctionProcessor.ONLOAD_FUNCTION_NAME_ERROR, e.getMessage());
    }
    control.verify();
  }

  @Test
  public void testGeneratesCallbackCode() throws Exception {
    EasyMock.expect(jsUri.getOnload()).andReturn(ONLOAD_FUNCTION);
    control.replay();
    assertTrue(processor.process(request, response));
    assertEquals(HttpServletResponse.SC_OK, response.getStatusCode());
    String expectedBody = String.format(AddOnloadFunctionProcessor.ONLOAD_JS_TPL, ONLOAD_FUNCTION);
    assertEquals(expectedBody, response.build().toJsString());
    control.verify();
  }

  @Test
  public void testWithoutHint() throws Exception {
    EasyMock.expect(jsUri.getOnload()).andReturn(null);
    EasyMock.expect(jsUri.isNohint()).andReturn(true);
    control.replay();
    assertTrue(processor.process(request, response));
    assertEquals(AddOnloadFunctionProcessor.JSL_CALLBACK_JS, response.build().toJsString());
    control.verify();
  }

  @Test
  public void testWithHint() throws Exception {
    EasyMock.expect(jsUri.getOnload()).andReturn(null);
    EasyMock.expect(jsUri.isNohint()).andReturn(false);
    control.replay();
    assertTrue(processor.process(request, response));
    assertEquals("", response.build().toJsString());
    control.verify();
  }
}
