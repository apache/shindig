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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.uri.PassthruManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.apache.shindig.gadgets.uri.UriCommon.Param;
import org.easymock.Capture;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

public class ProxyHandlerTest extends ServletTestFixture {
  private final static String URL_ONE = "http://www.example.org/test.html";
  private final static String DATA_ONE = "hello world";

  private final ProxyUriManager passthruManager = new PassthruManager();
  private final ProxyHandler proxyHandler
      = new ProxyHandler(pipeline, lockedDomainService, rewriterRegistry, passthruManager);

  private void expectGetAndReturnData(String url, byte[] data) throws Exception {
    HttpRequest req = new HttpRequest(Uri.parse(url));
    HttpResponse resp = new HttpResponseBuilder().setResponse(data).create();
    expect(pipeline.execute(req)).andReturn(resp);
  }

  private void expectGetAndReturnHeaders(String url, Map<String, List<String>> headers)
      throws Exception {
    HttpRequest req = new HttpRequest(Uri.parse(url));
    HttpResponse resp = new HttpResponseBuilder().addAllHeaders(headers).create();
    expect(pipeline.execute(req)).andReturn(resp);
  }
  
  private void setupProxyRequestBase(String host) {
    expect(request.getServerName()).andReturn(host).anyTimes();
    expect(request.getScheme()).andReturn("http").anyTimes();
    expect(request.getServerPort()).andReturn(80).anyTimes();
    expect(request.getRequestURI()).andReturn("/").anyTimes();
  }

  private void setupProxyRequestMock(String host, String url, String... extraParams)
      throws Exception {
    setupProxyRequestBase(host);
    expect(request.getHeader("Host")).andReturn(host);
    StringBuffer query = new StringBuffer("");
    if (null != url) {
      query.append("url=").append(Utf8UrlCoder.encode(url)).append('&');
    }
    query.append("container=default");
    String[] params = extraParams;
    if (params != null && params.length > 0) {
      for (int i = 0; i < params.length; i += 2) {
        query.append('&').append(params[i]).append('=').append(
            Utf8UrlCoder.encode(params[i+1]));
      }
    }
    expect(request.getQueryString()).andReturn(query.toString());
  }

  private void setupFailedProxyRequestMock(String host, String url) throws Exception {
    setupProxyRequestBase(host);
    expect(request.getHeader("Host")).andReturn(host);
  }

  @Test
  public void testIfModifiedSinceAlwaysReturnsEarly() throws Exception {
    expect(request.getHeader("If-Modified-Since"))
        .andReturn("Yes, this is an invalid header.");
    replay();

    proxyHandler.fetch(request, recorder);
    verify();

    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, recorder.getHttpStatusCode());
    assertFalse(rewriter.responseWasRewritten());
  }

  @Test
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

  @Test(expected=GadgetException.class)
  public void testNoUrl() throws Exception {
    setupProxyRequestMock("www.example.com", null);
    expect(lockedDomainService.isSafeForOpenProxy("www.example.com")).andReturn(true);
    replay();

    proxyHandler.doFetch(request, recorder);
    fail("Proxy should raise exception if there is no url");
  }

  @Test
  public void testHttpRequestFillsParentAndContainer() throws Exception {
    setupProxyRequestMock("www.example.com", URL_ONE);
    expect(lockedDomainService.isSafeForOpenProxy("www.example.com")).andReturn(true);
    //HttpRequest req = new HttpRequest(Uri.parse(URL_ONE));
    HttpResponse resp = new HttpResponseBuilder().setResponse(DATA_ONE.getBytes()).create();

    Capture<HttpRequest> httpRequest = new Capture<HttpRequest>();
    expect(pipeline.execute(capture(httpRequest))).andReturn(resp);

    replay();
    proxyHandler.fetch(request, recorder);
    verify();

    // Check that the HttpRequest passed in has all the relevant fields sets
    assertEquals("default", httpRequest.getValue().getContainer());
    assertEquals(Uri.parse(URL_ONE), httpRequest.getValue().getUri());

    assertEquals(DATA_ONE, recorder.getResponseAsString());
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test(expected=GadgetException.class)
  public void testLockedDomainFailedEmbed() throws Exception {
    setupFailedProxyRequestMock("www.example.com", URL_ONE);
    expect(lockedDomainService.isSafeForOpenProxy("www.example.com")).andReturn(false);
    replay();

    proxyHandler.doFetch(request, response);
  }

  @Test
  public void testHeadersPreserved() throws Exception {
    // Some headers may be blacklisted. These are OK.
    String url = "http://example.org/file.evil";
    String domain = "example.org";
    String contentType = "text/evil; charset=UTF-8";
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
  
  @Test
  public void testGetFallback() throws Exception {
    String url = "http://example.org/file.evil";
    String domain = "example.org";
    String fallback_url = "http://fallback.com/fallback.png";

    expect(lockedDomainService.isSafeForOpenProxy(domain)).andReturn(true).atLeastOnce();
    setupProxyRequestMock(domain, url, ProxyBase.IGNORE_CACHE_PARAM, "1",
        Param.FALLBACK_URL_PARAM.getKey(), fallback_url);

    HttpRequest req = new HttpRequest(Uri.parse(url)).setIgnoreCache(true);
    HttpResponse resp = HttpResponse.error();
    HttpResponse fallback_resp = new HttpResponse("Fallback");
    expect(pipeline.execute(req)).andReturn(resp);
    expect(pipeline.execute(isA(HttpRequest.class))).andReturn(fallback_resp);

    replay();

    proxyHandler.fetch(request, recorder);

    verify();
  }

  @Test
  public void testNoCache() throws Exception {
    String url = "http://example.org/file.evil";
    String domain = "example.org";

    expect(lockedDomainService.isSafeForOpenProxy(domain)).andReturn(true).atLeastOnce();
    setupProxyRequestMock(domain, url, ProxyBase.IGNORE_CACHE_PARAM, "1");

    HttpRequest req = new HttpRequest(Uri.parse(url)).setIgnoreCache(true);
    HttpResponse resp = new HttpResponse("Hello");
    expect(pipeline.execute(req)).andReturn(resp);

    replay();

    proxyHandler.fetch(request, recorder);

    verify();
  }

  /**
   * Override HttpRequest equals to check for cache control fields
   */
  static class HttpRequestCache extends HttpRequest {
    public HttpRequestCache(Uri uri) {
      super(uri);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof HttpRequest)) {
        return false;
      }
      HttpRequest req = (HttpRequest)obj;
      return super.equals(obj) && req.getCacheTtl() == getCacheTtl() &&
              req.getIgnoreCache() == getIgnoreCache();
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(super.hashCode(), getCacheTtl(), getIgnoreCache());
    }
  }

  @Test
  public void testWithCache() throws Exception {
    String url = "http://example.org/file.evil";
    String domain = "example.org";

    expect(lockedDomainService.isSafeForOpenProxy(domain)).andReturn(true).atLeastOnce();
    setupProxyRequestMock(domain, url, ProxyBase.REFRESH_PARAM, "120");

    HttpRequest req = new HttpRequestCache(Uri.parse(url)).setCacheTtl(120).setIgnoreCache(false);
    HttpResponse resp = new HttpResponse("Hello");
    expect(pipeline.execute(req)).andReturn(resp);

    replay();

    proxyHandler.fetch(request, recorder);

    verify();
  }

  @Test
  public void testWithBadTtl() throws Exception {
    String url = "http://example.org/file.evil";
    String domain = "example.org";

    expect(lockedDomainService.isSafeForOpenProxy(domain)).andReturn(true).atLeastOnce();
    setupProxyRequestMock(domain, url, ProxyBase.REFRESH_PARAM, "foo");

    HttpRequest req = new HttpRequestCache(Uri.parse(url)).setCacheTtl(-1).setIgnoreCache(false);
    HttpResponse resp = new HttpResponse("Hello");
    expect(pipeline.execute(req)).andReturn(resp);

    replay();

    proxyHandler.fetch(request, recorder);

    verify();
  }

  @Test
  public void testXForwardedFor() throws Exception {
    String url = "http://example.org/";
    String domain = "example.org";

    expect(lockedDomainService.isSafeForOpenProxy(domain)).andReturn(true).atLeastOnce();
    expect(request.getRemoteAddr()).andReturn("127.0.0.1").atLeastOnce();
    setupProxyRequestMock(domain, url);

    HttpRequest req = new HttpRequest(Uri.parse(url));
    req.setHeader("X-Forwarded-For","127.0.0.1");

    HttpResponse resp = new HttpResponse("Hello");

    expect(pipeline.execute(req)).andReturn(resp);

    replay();

    proxyHandler.fetch(request, recorder);

    verify();
  }

  private void expectMime(String expectedMime, String contentMime, String outputMime)
      throws Exception {
    String url = "http://example.org/file.img?" + ProxyHandler.REWRITE_MIME_TYPE_PARAM +
        '=' + expectedMime;
    String domain = "example.org";

    expect(lockedDomainService.isSafeForOpenProxy(domain)).andReturn(true).atLeastOnce();
    setupProxyRequestMock(domain, url, ProxyHandler.REWRITE_MIME_TYPE_PARAM, expectedMime);

    HttpRequest req = new HttpRequest(Uri.parse(url))
        .setRewriteMimeType(expectedMime);

    HttpResponse resp = new HttpResponseBuilder()
      .setResponseString("Hello")
      .addHeader("Content-Type", contentMime)
      .create();

    expect(pipeline.execute(req)).andReturn(resp);
    replay();
    proxyHandler.fetch(request, recorder);
    verify();
    assertEquals(recorder.getContentType(), outputMime);
    reset();
  }

  @Test
  public void testMimeMatchPass() throws Exception {
    expectMime("text/css", "text/css", "text/css");
  }

  @Test
  public void testMimeMatchPassWithAdditionalAttributes() throws Exception {
    expectMime("text/css", "text/css; charset=UTF-8", "text/css");
  }

  @Test
  public void testMimeMatchOverrideNonMatch() throws Exception {
    expectMime("text/css", "image/png; charset=UTF-8", "text/css");
  }

  @Test
  public void testMimeMatchVarySupport() throws Exception {
    // We use CaptureRewrite which always rewrite - always set encoding
    expectMime("image/*", "image/gif", "image/gif");
  }
}
