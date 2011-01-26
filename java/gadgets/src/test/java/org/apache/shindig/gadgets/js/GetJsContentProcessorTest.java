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

import org.apache.shindig.gadgets.servlet.JsHandler;
import org.apache.shindig.gadgets.servlet.JsHandler.Response;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.UriStatus;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests for {@link GetJsContentProcessor}.
 */
public class GetJsContentProcessorTest {

  private static final String HOST = "localhost";
  private static final String JS_CODE = "some JS data";
  
  private IMocksControl control;
  private JsHandler handler;
  private JsUri jsUri;
  private JsRequest request;
  private Response handlerResponse;
  private JsResponseBuilder response;
  private GetJsContentProcessor processor;

  @Before
  public void setUp() {
    control = EasyMock.createControl();
    handler = control.createMock(JsHandler.class);
    jsUri = control.createMock(JsUri.class);
    request = control.createMock(JsRequest.class);
    handlerResponse = control.createMock(JsHandler.Response.class);
    response = new JsResponseBuilder();
    processor = new GetJsContentProcessor(handler);
  }
  
  @Test
  public void testPopulatesResponseForUnversionedRequest() throws Exception {
    setExpectations(true, UriStatus.VALID_UNVERSIONED);
    control.replay();
    processor.process(request, response);
    checkResponse(true, 3600);
    control.verify();
  }

  @Test
  public void testPopulatesResponseForVersionedRequest() throws Exception {
    setExpectations(true, UriStatus.VALID_VERSIONED);
    control.replay();
    processor.process(request, response);
    checkResponse(true, -1);
    control.verify();
  }

  @Test
  public void testPopulatesResponseForInvalidVersion() throws Exception {
    setExpectations(true, UriStatus.INVALID_VERSION);
    control.replay();
    processor.process(request, response);
    checkResponse(true, 0);
    control.verify();
  }

  @Test
  public void testPopulatesResponseForNoProxyCacheable() throws Exception {
    setExpectations(false, UriStatus.VALID_UNVERSIONED);
    control.replay();
    processor.process(request, response);
    checkResponse(false, 3600);
    control.verify();
  }

  private void setExpectations(boolean proxyCacheable, UriStatus uriStatus) {
    EasyMock.expect(handler.getJsContent(jsUri, HOST)).andReturn(handlerResponse);
    EasyMock.expect(request.getHost()).andReturn(HOST);
    EasyMock.expect(request.getJsUri()).andReturn(jsUri);
    EasyMock.expect(handlerResponse.getJsData()).andReturn(new StringBuilder(JS_CODE));
    EasyMock.expect(handlerResponse.isProxyCacheable()).andReturn(proxyCacheable);
    EasyMock.expect(jsUri.getStatus()).andReturn(uriStatus);
  }

  private void checkResponse(boolean proxyCacheable, int expectedTtl) {
    assertEquals(proxyCacheable, response.isProxyCacheable());
    assertEquals(expectedTtl, response.getCacheTtlSecs());
    assertEquals(JS_CODE, response.getJsCode().toString());
  }
}
