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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;


/**
 * Tests for {@link JsLoadProcessor}.
 */
public class JsLoadProcessorTest {

  private static final String ONLOAD_FUNCTION = "onloadFunc";

  private IMocksControl control;
  private JsRequest request;
  private JsUriManager jsUriManager;
  private JsUri jsUri;
  private Uri uri;
  private JsResponseBuilder response;
  private JsLoadProcessor processor;

  @Before
  public void setUp() {
    control = EasyMock.createControl();
    request = control.createMock(JsRequest.class);
    jsUriManager = control.createMock(JsUriManager.class);
    jsUri = control.createMock(JsUri.class);
    uri = Uri.parse("http://example.org/foo.xml");
    response = new JsResponseBuilder();
    processor = new JsLoadProcessor(jsUriManager, 1234, true);

    EasyMock.expect(request.getJsUri()).andReturn(jsUri);
  }

  @Test
  public void testSkipsWhenNoJsLoad() throws Exception {
    EasyMock.expect(jsUri.isJsload()).andReturn(false);
    response = control.createMock(JsResponseBuilder.class);
    control.replay();
    assertTrue(processor.process(request, response));
    control.verify();
  }

  @Test
  public void testFailsWhenNoOnloadIsSpecified() throws Exception {
    EasyMock.expect(jsUri.isJsload()).andReturn(true);
    EasyMock.expect(jsUri.getOnload()).andReturn(null);
    control.replay();
    try {
      processor.process(request, response);
      fail("A JsException should have been thrown by the processor.");
    } catch (JsException e) {
      assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getStatusCode());
      assertEquals(JsLoadProcessor.JSLOAD_ONLOAD_ERROR, e.getMessage());
    }
    control.verify();
  }

  @Test
  public void testGeneratesLoaderCodeWithNoCache() throws Exception {
    setExpectations(true, null);
    control.replay();
    checkGeneratedCode(0);
    control.verify();
  }

  @Test
  public void testGeneratesLoaderCodeWithDefaultTtl() throws Exception {
    setExpectations(false, null);
    control.replay();
    checkGeneratedCode(1234);
    control.verify();
  }

  @Test
  public void testGeneratesLoaderCodeWithRefresh() throws Exception {
    setExpectations(false, 300);
    control.replay();
    checkGeneratedCode(300);
    control.verify();
  }

  private void setExpectations(boolean noCache, Integer refresh) {
    EasyMock.expect(jsUri.isJsload()).andReturn(true);
    EasyMock.expect(jsUri.getOnload()).andReturn(ONLOAD_FUNCTION);
    jsUri.setJsload(false);
    jsUri.setNohint(true);
    EasyMock.expect(jsUriManager.makeExternJsUri(jsUri)).andReturn(uri);
    EasyMock.expect(jsUri.isNoCache()).andReturn(noCache);
    if (!noCache) {
      EasyMock.expect(jsUri.getRefresh()).andReturn(refresh);
    }
  }

  private void checkGeneratedCode(int expectedTtl) throws JsException {
    assertFalse(processor.process(request, response));
    assertEquals(HttpServletResponse.SC_OK, response.getStatusCode());
    assertEquals(expectedTtl, response.getCacheTtlSecs());
    String expectedBody = String.format(JsLoadProcessor.JSLOAD_JS_TPL,
        uri.toString() + "?jsload=0");
    assertEquals(expectedBody, response.build().toJsString());
  }

}
