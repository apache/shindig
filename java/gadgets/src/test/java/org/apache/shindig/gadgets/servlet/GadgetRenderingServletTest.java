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
package org.apache.shindig.gadgets.servlet;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.apache.shindig.gadgets.http.HttpRequest;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GadgetRenderingServletTest {
  private final IMocksControl control = EasyMock.createNiceControl();
  private final HttpServletRequest request = control.createMock(HttpServletRequest.class);
  private final HttpServletResponse response = control.createMock(HttpServletResponse.class);
  public final HttpServletResponseRecorder recorder = new HttpServletResponseRecorder(response);
  private final GadgetRenderingServlet servlet = new GadgetRenderingServlet();

  @Test
  public void dosHeaderRejected() throws Exception {
    expect(request.getHeader(HttpRequest.DOS_PREVENTION_HEADER)).andReturn("foo");
    control.replay();
    servlet.doGet(request, recorder);

    assertEquals(HttpServletResponse.SC_FORBIDDEN, recorder.getHttpStatusCode());
  }
}
