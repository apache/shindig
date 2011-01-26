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
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetException.Code;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;


/**
 * Tests for {@link JsRequestBuilder}.
 */
public class JsRequestBuilderTest {

  private static final String HOST_HEADER_KEY = "Host";
  private static final String IMS_HEADER_KEY = "If-Modified-Since";
  private static final String HOST = "localhost";
  
  private IMocksControl control;
  private JsUriManager jsUriManager;
  private JsUri jsUri;
  private HttpServletRequest request;
  private JsRequestBuilder builder;

  @Before
  public void setUp() {
    control = EasyMock.createControl();
    jsUriManager = control.createMock(JsUriManager.class);
    jsUri = control.createMock(JsUri.class);
    request = control.createMock(HttpServletRequest.class);
    builder = new JsRequestBuilder(jsUriManager);
    
    EasyMock.expect(request.getScheme()).andReturn("http");
    EasyMock.expect(request.getServerPort()).andReturn(80);
    EasyMock.expect(request.getServerName()).andReturn("HOST");
    EasyMock.expect(request.getRequestURI()).andReturn("/foo");
    EasyMock.expect(request.getQueryString()).andReturn("");
  }

  @Test
  public void testCreateRequestNotInCache() throws Exception {
    EasyMock.expect(jsUriManager.processExternJsUri(EasyMock.isA(Uri.class))).andReturn(jsUri);
    EasyMock.expect(request.getHeader(HOST_HEADER_KEY)).andReturn(HOST);
    EasyMock.expect(request.getHeader(IMS_HEADER_KEY)).andReturn(null);
    control.replay();
    JsRequest jsRequest = builder.build(request);
    control.verify();
    assertSame(jsUri, jsRequest.getJsUri());
    assertEquals(HOST, jsRequest.getHost());
    assertFalse(jsRequest.isInCache());
  }

  @Test
  public void testCreateRequestInCache() throws Exception {
    EasyMock.expect(jsUriManager.processExternJsUri(EasyMock.isA(Uri.class))).andReturn(jsUri);
    EasyMock.expect(request.getHeader(HOST_HEADER_KEY)).andReturn(HOST);
    EasyMock.expect(request.getHeader(IMS_HEADER_KEY)).andReturn("today");
    control.replay();
    JsRequest jsRequest = builder.build(request);
    control.verify();
    assertSame(jsUri, jsRequest.getJsUri());
    assertEquals(HOST, jsRequest.getHost());
    assertTrue(jsRequest.isInCache());
  }

  @Test
  public void testCreateRequestThrowsExceptionOnParseError() throws Exception {
    EasyMock.expect(jsUriManager.processExternJsUri(EasyMock.isA(Uri.class))).andThrow(
        new GadgetException(Code.INVALID_PARAMETER));
    control.replay();
    try {
      builder.build(request);
      fail("Should have thrown a GadgetException.");
    } catch (GadgetException e) {
      // pass
    }
    control.verify();
  }
}
