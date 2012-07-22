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
import org.apache.shindig.gadgets.uri.UriStatus;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;


/**
 * Tests for {@link IfModifiedSinceProcessor}.
 */
public class IfModifiedSinceProcessorTest {

  private IMocksControl control;
  private JsUri jsUri;
  private JsRequest request;
  private JsResponseBuilder response;
  private IfModifiedSinceProcessor processor;

  @Before
  public void setUp() {
    control = EasyMock.createControl();
    jsUri = control.createMock(JsUri.class);
    request = control.createMock(JsRequest.class);
    response = new JsResponseBuilder();
    processor = new IfModifiedSinceProcessor();
  }

  @Test
  public void testDoesNothingAndContinuesProcessingWhenHeaderIsAbsent() throws Exception {
    EasyMock.expect(request.isInCache()).andReturn(false);
    control.replay();
    assertTrue(processor.process(request, response));
    control.verify();
  }

  @Test
  public void testDoesNothingAndContinuesProcessingWhenNotVersioned() throws Exception {
    EasyMock.expect(request.isInCache()).andReturn(true);
    EasyMock.expect(request.getJsUri()).andReturn(jsUri);
    EasyMock.expect(jsUri.getStatus()).andReturn(UriStatus.VALID_UNVERSIONED);
    control.replay();
    assertTrue(processor.process(request, response));
    control.verify();
  }

  @Test
  public void testReturnsNotModifiedAndStopsProcessingWithHeaderAndVersion() throws Exception {
    EasyMock.expect(request.isInCache()).andReturn(true);
    EasyMock.expect(request.getJsUri()).andReturn(jsUri);
    EasyMock.expect(jsUri.getStatus()).andReturn(UriStatus.VALID_VERSIONED);
    control.replay();
    assertFalse(processor.process(request, response));
    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatusCode());
    control.verify();
  }
}
