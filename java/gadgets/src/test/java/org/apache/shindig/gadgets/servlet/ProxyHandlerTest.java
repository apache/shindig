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

import com.google.common.collect.Maps;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

public class ProxyHandlerTest extends ServletTestFixture {
  private final static String URL_ONE = "http://www.example.org/test.html";
  private final static String DATA_ONE = "hello world";

  private final ProxyHandler proxyHandler
      = new ProxyHandler(fetcher, lockedDomainService, rewriterRegistry);

  private void expectGetAndReturnData(String url, byte[] data) throws Exception {
    HttpRequest req = new HttpRequest(Uri.parse(url));
    HttpResponse resp = new HttpResponseBuilder().setResponse(data).create();
    expect(fetcher.fetch(req)).andReturn(resp);
  }

  private void expectGetAndReturnHeaders(String url, Map<String, List<String>> headers)
      throws Exception {
    HttpRequest req = new HttpRequest(Uri.parse(url));
    HttpResponse resp = new HttpResponseBuilder().addAllHeaders(headers).create();
    expect(fetcher.fetch(req)).andReturn(resp);
  }

  private void setupProxyRequestMock(String host, String url) throws Exception {
    expect(request.getHeader("Host")).andReturn(host);
    expect(request.getParameter("url")).andReturn(url).atLeastOnce();
  }

  private void setupFailedProxyRequestMock(String host, String url) throws Exception {
    expect(request.getHeader("Host")).andReturn(host);
  }

  public void testIfModifiedSinceAlwaysReturnsEarly() throws Exception {
    expect(request.getHeader("If-Modified-Since"))
        .andReturn("Yes, this is an invalid header.");
    replay();

    proxyHandler.fetch(request, recorder);
    verify();

    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, recorder.getHttpStatusCode());
    assertFalse(rewriter.responseWasRewritten());
  }

  public void testLockedDomainEmbed() throws Exception {
    setupProxyRequestMock("www.example.com", URL_ONE);
    expect(lockedDomainService.isSafeForOpenProxy("www.example.com")).andReturn(true);
    expectGetAndReturnData(URL_ONE, DATA_ONE.getBytes());
    replay();

    proxyHandler.fetch(request, recorder);
    verify();

    assertEquals(DATA_ONE, recorder.getResponseAsString());
    assertTrue(rewriter.responseWasRewritten());
  }

  public void testLockedDomainFailedEmbed() throws Exception {
    setupFailedProxyRequestMock("www.example.com", URL_ONE);
    expect(lockedDomainService.isSafeForOpenProxy("www.example.com")).andReturn(false);
    replay();
    try {
      proxyHandler.fetch(request, response);
      fail("Should have thrown");
    } catch (GadgetException e) {
      // good
    }
  }

  public void testHeadersPreserved() throws Exception {
    // Some headers may be blacklisted. These are ok.
    String url = "http://example.org/file.evil";
    String domain = "example.org";
    String contentType = "text/evil; charset=utf-8";
    String magicGarbage = "fadfdfdfd";
    Map<String, List<String>> headers = Maps.newHashMap();
    headers.put("Content-Type", Arrays.asList(contentType));
    headers.put("X-Magic-Garbage", Arrays.asList(magicGarbage));

    expect(lockedDomainService.isSafeForOpenProxy(domain)).andReturn(true).atLeastOnce();
    setupProxyRequestMock(domain, url);
    expectGetAndReturnHeaders(url, headers);

    replay();

    proxyHandler.fetch(request, recorder);

    assertEquals(contentType, recorder.getHeader("Content-Type"));
    assertEquals(magicGarbage, recorder.getHeader("X-Magic-Garbage"));
    assertTrue(rewriter.responseWasRewritten());
  }

  public void testNoCache() throws Exception {
    String url = "http://example.org/file.evil";
    String domain = "example.org";

    expect(lockedDomainService.isSafeForOpenProxy(domain)).andReturn(true).atLeastOnce();
    setupProxyRequestMock(domain, url);
    expect(request.getParameter(ProxyBase.IGNORE_CACHE_PARAM)).andReturn("1").atLeastOnce();

    HttpRequest req = new HttpRequest(Uri.parse(url)).setIgnoreCache(true);
    HttpResponse resp = new HttpResponse("Hello");
    expect(fetcher.fetch(req)).andReturn(resp);

    replay();

    proxyHandler.fetch(request, recorder);

    verify();
  }
}
