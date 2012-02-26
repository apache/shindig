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

import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.gadgets.oauth.OAuthCallbackState;
import org.junit.Test;
import org.junit.Assert;

/**
 * Tests for OAuth callback servlet.
 */
public class OAuthCallbackServletTest extends ServletTestFixture {

  @Test
  public void testServlet() throws Exception {
    OAuthCallbackServlet servlet = new OAuthCallbackServlet();
    replay();
    servlet.doGet(this.request, this.recorder);
    verify();
    assertEquals("text/html; charset=UTF-8", this.recorder.getContentType());
    String body = this.recorder.getResponseAsString();
    Assert.assertNotSame("body is " + body, body.indexOf("window.close()"), -1);
  }

  @Test
  public void testServletWithCallback() throws Exception {
    BlobCrypter crypter = new BasicBlobCrypter("00000000000000000000".getBytes());
    OAuthCallbackState state = new OAuthCallbackState(crypter);
    OAuthCallbackServlet servlet = new OAuthCallbackServlet();
    servlet.setStateCrypter(crypter);
    state.setRealCallbackUrl("http://www.example.com/callback");
    expect(request.getParameter("cs")).andReturn(state.getEncryptedState());
    expect(request.getQueryString()).andReturn("cs=foo&bar=baz");
    replay();
    servlet.doGet(this.request, this.recorder);
    verify();
    assertEquals(302, this.recorder.getHttpStatusCode());
    assertEquals("http://www.example.com/callback?bar=baz", this.recorder.getHeader("Location"));
    String cacheControl = this.recorder.getHeader("Cache-Control");
    assertEquals("private,max-age=3600", cacheControl);
  }

  @Test
  public void testServletWithCallback_noQueryParams() throws Exception {
    BlobCrypter crypter = new BasicBlobCrypter("00000000000000000000".getBytes());
    OAuthCallbackState state = new OAuthCallbackState(crypter);
    OAuthCallbackServlet servlet = new OAuthCallbackServlet();
    servlet.setStateCrypter(crypter);
    state.setRealCallbackUrl("http://www.example.com/callback");
    expect(request.getParameter("cs")).andReturn(state.getEncryptedState());
    expect(request.getQueryString()).andReturn("cs=foo");
    replay();
    servlet.doGet(this.request, this.recorder);
    verify();
    assertEquals(302, this.recorder.getHttpStatusCode());
    assertEquals("http://www.example.com/callback", this.recorder.getHeader("Location"));
    String cacheControl = this.recorder.getHeader("Cache-Control");
    assertEquals("private,max-age=3600", cacheControl);
  }
}
