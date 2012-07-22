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
package org.apache.shindig.social.core.oauth;

import org.apache.commons.codec.binary.Base64;
import org.apache.shindig.common.testing.FakeHttpServletRequest;
import org.apache.shindig.social.core.oauth2.OAuth2Servlet;
import org.apache.shindig.social.dataservice.integration.AbstractLargeRestfulTests;
import org.easymock.EasyMock;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;

import java.io.PrintWriter;

public class OAuth2ClientCredentialFlowTest extends AbstractLargeRestfulTests {

  protected static final String CLIENT_CRED_CLIENT = "testClientCredentialsClient";
  protected static final String CLIENT_CRED_SECRET = "clientCredentialsClient_secret";

  protected OAuth2Servlet servlet = null;

  @Before
  @Override
  public void abstractLargeRestfulBefore() throws Exception {
    super.abstractLargeRestfulBefore();
    servlet = new OAuth2Servlet();
    injector.injectMembers(servlet);
  };

  /**
   * Test using basic authentication scheme for client cred flow
   *
   * @throws Exception
   */
  @Test
  public void testClientCredFlowBadHeader() throws Exception {
    FakeHttpServletRequest req = new FakeHttpServletRequest(
        "http://localhost:8080", "/oauth2", "grant_type=client_credentials");
    req.setHeader("Authorization", "Basic *^%#");
    req.setMethod("GET");
    req.setServletPath("/oauth2");
    req.setPathInfo("/access_token");
    HttpServletResponse resp = mock(HttpServletResponse.class);
    resp.setStatus(EasyMock.eq(HttpServletResponse.SC_BAD_REQUEST));
    MockServletOutputStream outputStream = new MockServletOutputStream();
    EasyMock.expect(resp.getOutputStream()).andReturn(outputStream).anyTimes();
    PrintWriter writer = new PrintWriter(outputStream);
    EasyMock.expect(resp.getWriter()).andReturn(writer).anyTimes();
    replay();
    servlet.service(req, resp);
    writer.flush();

    String response = new String(outputStream.getBuffer(), "UTF-8");
    JSONObject respObj = new JSONObject(response);
    assertTrue(respObj.has("error"));
    verify();
  }

  /**
   * Test using basic authentication scheme for client cred flow
   *
   * @throws Exception
   */
  @Test
  public void testClientCredFlowBadPass() throws Exception {
    FakeHttpServletRequest req = new FakeHttpServletRequest(
        "http://localhost:8080", "/oauth2", "grant_type=client_credentials");
    req.setHeader(
        "Authorization",
        "Basic "
            + Base64.encodeBase64String((CLIENT_CRED_CLIENT + ":badsecret")
                .getBytes("UTF-8")));
    req.setMethod("GET");
    req.setServletPath("/oauth2");
    req.setPathInfo("/access_token");
    HttpServletResponse resp = mock(HttpServletResponse.class);
    resp.setStatus(EasyMock.eq(HttpServletResponse.SC_BAD_REQUEST));
    MockServletOutputStream outputStream = new MockServletOutputStream();
    EasyMock.expect(resp.getOutputStream()).andReturn(outputStream).anyTimes();
    PrintWriter writer = new PrintWriter(outputStream);
    EasyMock.expect(resp.getWriter()).andReturn(writer).anyTimes();
    replay();
    servlet.service(req, resp);
    writer.flush();

    String response = new String(outputStream.getBuffer(), "UTF-8");
    JSONObject respObj = new JSONObject(response);
    assertTrue(respObj.has("error"));
    verify();
  }

  /**
   * Test using basic authentication scheme for client cred flow
   *
   * @throws Exception
   */
  @Test
  public void testClientCredFlowBasicAuth() throws Exception {
    FakeHttpServletRequest req = new FakeHttpServletRequest(
        "http://localhost:8080", "/oauth2", "grant_type=client_credentials");
    req.setHeader(
        "Authorization",
        "Basic "
            + Base64
                .encodeBase64String((CLIENT_CRED_CLIENT + ":" + CLIENT_CRED_SECRET)
                    .getBytes("UTF-8")));
    req.setMethod("GET");
    req.setServletPath("/oauth2");
    req.setPathInfo("/access_token");
    HttpServletResponse resp = mock(HttpServletResponse.class);
    resp.setStatus(HttpServletResponse.SC_OK);
    MockServletOutputStream outputStream = new MockServletOutputStream();
    EasyMock.expect(resp.getOutputStream()).andReturn(outputStream).anyTimes();
    PrintWriter writer = new PrintWriter(outputStream);
    EasyMock.expect(resp.getWriter()).andReturn(writer).anyTimes();
    replay();
    servlet.service(req, resp);
    writer.flush();

    JSONObject tokenResponse = new JSONObject(new String(
        outputStream.getBuffer(), "UTF-8"));

    assertEquals("bearer", tokenResponse.getString("token_type"));
    assertNotNull(tokenResponse.getString("access_token"));
    assertTrue(tokenResponse.getLong("expires_in") > 0);
    verify();
  }

  /**
   * Test using URL parameter with client cred flow
   *
   * @throws Exception
   */
  @Test
  public void testClientCredFlowParams() throws Exception {
    FakeHttpServletRequest req = new FakeHttpServletRequest(
        "http://localhost:8080", "/oauth2", "client_id=" + CLIENT_CRED_CLIENT
            + "&grant_type=client_credentials&client_secret="
            + CLIENT_CRED_SECRET);
    req.setMethod("GET");
    req.setServletPath("/oauth2");
    req.setPathInfo("/access_token");
    HttpServletResponse resp = mock(HttpServletResponse.class);
    resp.setStatus(HttpServletResponse.SC_OK);
    MockServletOutputStream outputStream = new MockServletOutputStream();
    EasyMock.expect(resp.getOutputStream()).andReturn(outputStream).anyTimes();
    PrintWriter writer = new PrintWriter(outputStream);
    EasyMock.expect(resp.getWriter()).andReturn(writer).anyTimes();
    replay();
    servlet.service(req, resp);
    writer.flush();

    JSONObject tokenResponse = new JSONObject(new String(
        outputStream.getBuffer(), "UTF-8"));

    assertEquals("bearer", tokenResponse.getString("token_type"));
    assertNotNull(tokenResponse.getString("access_token"));
    assertTrue(tokenResponse.getLong("expires_in") > 0);
    verify();
  }

}
