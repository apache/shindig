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

import org.apache.shindig.common.EasyMockTestCase;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

public class ProxyServletRequestTest extends EasyMockTestCase {
  private final static String URL = "http://proxy/url";

  private final HttpServletRequest request = mock(HttpServletRequest.class);

  public ProxyServletRequest setupMockRequest(String url) {
    String query = null;
    int qPos = url.indexOf('?');
    if (qPos != -1) {
      query = url.substring(qPos + 1);
      url = url.substring(0, qPos);
    }
    expect(request.getRequestURI()).andReturn(url).atLeastOnce();
    expect(request.getQueryString()).andReturn(query).anyTimes();
    expect(request.getParameter("url")).andReturn(URL).anyTimes();
    replay();
    return new ProxyServletRequest(request);
  }

  @Test
  public void testOldRequestSyntax() throws Exception {
    ProxyServletRequest req = setupMockRequest(
      "http://localhost/gadgets/proxy?url=" + URL
    );
    assertFalse(req.isUsingChainedSyntax());
    assertEquals(URL, req.getParameter("url"));
    verify();
  }

  @Test
  public void testChainedSyntaxWithNoParameters() throws Exception {
    ProxyServletRequest req = setupMockRequest(
      "http://localhost/gadgets/proxy//http://remote/proxy?query=foo"
    );
    assertTrue(req.isUsingChainedSyntax());
    assertEquals("http://remote/proxy?query=foo", req.getParameter("url"));
    assertNull(req.getParameter("query"));
    verify();
  }

  @Test
  public void testChainedSyntaxWithOneParameter() throws Exception {
    ProxyServletRequest req = setupMockRequest(
      "http://localhost/gadgets/proxy/nocache=1/http://remote/proxy?nocache=0"
    );
    assertTrue(req.isUsingChainedSyntax());
    assertEquals("http://remote/proxy?nocache=0", req.getParameter("url"));
    assertEquals("1", req.getParameter("nocache"));
    verify();
  }

  @Test
  public void testChainedSyntaxWithParametersDontDecode() throws Exception {
    ProxyServletRequest req = setupMockRequest(
      "http://u:p@127.0.0.1:80/g/proxy/a=b%20+c&url=u/http://r/p%3Fa+e"
    );
    assertTrue(req.isUsingChainedSyntax());
    assertEquals("http://r/p%3Fa+e", req.getParameter("url"));
    assertEquals("b  c", req.getParameter("a"));
    verify();
  }
}

