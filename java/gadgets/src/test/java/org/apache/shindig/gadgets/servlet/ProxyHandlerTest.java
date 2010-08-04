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

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.rewrite.CaptureRewriter;
import org.apache.shindig.gadgets.rewrite.DefaultResponseRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.ResponseRewriter;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterRegistry;
import org.apache.shindig.gadgets.uri.PassthruManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.apache.shindig.gadgets.uri.UriCommon.Param;
import org.easymock.Capture;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ProxyHandlerTest extends EasyMockTestCase {
  private final static String URL_ONE = "http://www.example.org/test.html";
  private final static String DATA_ONE = "hello world";

  private final ProxyUriManager passthruManager = new PassthruManager();
  public final LockedDomainService lockedDomainService = mock(LockedDomainService.class);
  public final RequestPipeline pipeline = mock(RequestPipeline.class);
  public CaptureRewriter rewriter = new CaptureRewriter();
  public ResponseRewriterRegistry rewriterRegistry
      = new DefaultResponseRewriterRegistry(Arrays.<ResponseRewriter>asList(rewriter), null);
  private HttpRequest request;
  
  private final ProxyHandler proxyHandler
      = new ProxyHandler(pipeline, lockedDomainService, rewriterRegistry, passthruManager);

  @Before
  public void setUp() {
    request = new HttpRequest(Uri.parse(URL_ONE)); 
  }
  
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
  
  private UriBuilder setupProxyRequestBase(String host) {
    UriBuilder builder = new UriBuilder().setScheme("http").setAuthority(host);
    request.setHeader("Host", host);
    return builder;
  }

  private void setupProxyRequestMock(String host, String url, String... params)
      throws Exception {
    UriBuilder builder = setupProxyRequestBase(host);
    if (url != null) {
      builder.addQueryParameter(Param.URL.getKey(), url);
    }
    builder.addQueryParameter(Param.CONTAINER.getKey(), ContainerConfig.DEFAULT_CONTAINER);
    if (params != null && params.length > 0) {
      for (int i = 0; i < params.length; i += 2) {
        builder.addQueryParameter(params[i], params[i+1]);
      }
    }
    request.setUri(builder.toUri());
  }

  private void setupFailedProxyRequestMock(String host, String url) throws Exception {
    UriBuilder builder = setupProxyRequestBase(host);
    request.setUri(builder.toUri());
  }

  @Test
  public void testLockedDomainEmbed() throws Exception {
    setupProxyRequestMock("www.example.com", URL_ONE);
    expect(lockedDomainService.isSafeForOpenProxy("www.example.com")).andReturn(true);
    expectGetAndReturnData(URL_ONE, DATA_ONE.getBytes());
   
    replay();
    HttpResponse response = proxyHandler.fetch(request);
    verify();

    assertEquals(DATA_ONE, response.getResponseAsString());
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test(expected=GadgetException.class)
  public void testNoUrl() throws Exception {
    setupProxyRequestMock("www.example.com", null);
    expect(lockedDomainService.isSafeForOpenProxy("www.example.com")).andReturn(true);
    replay();

    proxyHandler.fetch(request);
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
    HttpResponse response = proxyHandler.fetch(request);
    verify();

    // Check that the HttpRequest passed in has all the relevant fields sets
    assertEquals("default", httpRequest.getValue().getContainer());
    assertEquals(Uri.parse(URL_ONE), httpRequest.getValue().getUri());

    assertEquals(DATA_ONE, response.getResponseAsString());
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test(expected=GadgetException.class)
  public void testLockedDomainFailedEmbed() throws Exception {
    setupFailedProxyRequestMock("www.example.com", URL_ONE);
    expect(lockedDomainService.isSafeForOpenProxy("www.example.com")).andReturn(false);
    replay();

    proxyHandler.fetch(request);
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
    HttpResponse response = proxyHandler.fetch(request);
    verify();

    assertEquals(contentType, response.getHeader("Content-Type"));
    assertEquals(magicGarbage, response.getHeader("X-Magic-Garbage"));
    assertTrue(rewriter.responseWasRewritten());
  }
  
  @Test
  public void testOctetSetOnNullContentType() throws Exception {
    String url = "http://example.org/file.evil";
    String domain = "example.org";

    expect(lockedDomainService.isSafeForOpenProxy(domain)).andReturn(true).atLeastOnce();
    setupProxyRequestMock(domain, url);
    expectGetAndReturnHeaders(url, Maps.<String, List<String>>newHashMap());

    replay();
    HttpResponse response = proxyHandler.fetch(request);
    verify();

    assertEquals("application/octet-stream", response.getHeader("Content-Type"));
    assertNotNull(response.getHeader("Content-Disposition"));
    assertTrue(rewriter.responseWasRewritten());
  }
  
  @Test
  public void testNoContentDispositionForFlash() throws Exception {
    // Some headers may be blacklisted. These are OK.
    String url = "http://example.org/file.evil";
    String domain = "example.org";
    Map<String, List<String>> headers = Maps.newHashMap();
    headers.put("Content-Type", Arrays.asList("application/x-shockwave-flash"));

    expect(lockedDomainService.isSafeForOpenProxy(domain)).andReturn(true).atLeastOnce();
    setupProxyRequestMock(domain, url);
    expectGetAndReturnHeaders(url, headers);

    replay();
    HttpResponse response = proxyHandler.fetch(request);
    verify();

    assertEquals("application/x-shockwave-flash", response.getHeader("Content-Type"));
    assertNull(response.getHeader("Content-Disposition"));
    assertTrue(rewriter.responseWasRewritten());
  }
  
  @Test
  public void testGetFallback() throws Exception {
    String url = "http://example.org/file.evil";
    String domain = "example.org";
    String fallback_url = "http://fallback.com/fallback.png";

    expect(lockedDomainService.isSafeForOpenProxy(domain)).andReturn(true).atLeastOnce();
    setupProxyRequestMock(domain, url, Param.NO_CACHE.getKey(), "1",
        Param.FALLBACK_URL_PARAM.getKey(), fallback_url);

    HttpRequest req = new HttpRequest(Uri.parse(url)).setIgnoreCache(true);
    HttpResponse resp = HttpResponse.error();
    HttpResponse fallback_resp = new HttpResponse("Fallback");
    expect(pipeline.execute(req)).andReturn(resp);
    expect(pipeline.execute(isA(HttpRequest.class))).andReturn(fallback_resp);

    replay();
    proxyHandler.fetch(request);
    verify();
  }

  @Test
  public void testNoCache() throws Exception {
    String url = "http://example.org/file.evil";
    String domain = "example.org";

    expect(lockedDomainService.isSafeForOpenProxy(domain)).andReturn(true).atLeastOnce();
    setupProxyRequestMock(domain, url, Param.NO_CACHE.getKey(), "1");

    HttpRequest req = new HttpRequest(Uri.parse(url)).setIgnoreCache(true);
    HttpResponse resp = new HttpResponse("Hello");
    expect(pipeline.execute(req)).andReturn(resp);

    replay();
    proxyHandler.fetch(request);
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
    setupProxyRequestMock(domain, url, Param.REFRESH.getKey(), "120");

    HttpRequest req = new HttpRequestCache(Uri.parse(url)).setCacheTtl(120).setIgnoreCache(false);
    HttpResponse resp = new HttpResponse("Hello");
    expect(pipeline.execute(req)).andReturn(resp);

    replay();
    proxyHandler.fetch(request);
    verify();
  }

  @Test
  public void testWithBadTtl() throws Exception {
    String url = "http://example.org/file.evil";
    String domain = "example.org";

    expect(lockedDomainService.isSafeForOpenProxy(domain)).andReturn(true).atLeastOnce();
    setupProxyRequestMock(domain, url, Param.REFRESH.getKey(), "foo");

    HttpRequest req = new HttpRequestCache(Uri.parse(url)).setCacheTtl(-1).setIgnoreCache(false);
    HttpResponse resp = new HttpResponse("Hello");
    expect(pipeline.execute(req)).andReturn(resp);

    replay();
    proxyHandler.fetch(request);
    verify();
  }

  @Test
  public void testXForwardedFor() throws Exception {
    String url = "http://example.org/";
    String domain = "example.org";

    expect(lockedDomainService.isSafeForOpenProxy(domain)).andReturn(true).atLeastOnce();
    request.setHeader("X-Forwarded-For", "127.0.0.1");
    setupProxyRequestMock(domain, url);

    HttpRequest req = new HttpRequest(Uri.parse(url));
    req.setHeader("X-Forwarded-For", "127.0.0.1");

    HttpResponse resp = new HttpResponse("Hello");

    expect(pipeline.execute(req)).andReturn(resp);

    replay();
    proxyHandler.fetch(request);
    verify();
  }

  private void expectMime(String expectedMime, String contentMime, String outputMime)
      throws Exception {
    String url = "http://example.org/file.img?" + Param.REWRITE_MIME_TYPE.getKey() +
        '=' + expectedMime;
    String domain = "example.org";

    expect(lockedDomainService.isSafeForOpenProxy(domain)).andReturn(true).atLeastOnce();
    setupProxyRequestMock(domain, url, Param.REWRITE_MIME_TYPE.getKey(), expectedMime);

    HttpRequest req = new HttpRequest(Uri.parse(url))
        .setRewriteMimeType(expectedMime);

    HttpResponse resp = new HttpResponseBuilder()
      .setResponseString("Hello")
      .addHeader("Content-Type", contentMime)
      .create();

    expect(pipeline.execute(req)).andReturn(resp);
    
    replay();
    HttpResponse response = proxyHandler.fetch(request);
    verify();
    
    assertEquals(outputMime, response.getHeader("Content-Type"));
    reset();
  }

  @Test
  public void testMimeMatchPass() throws Exception {
    expectMime("text/css", "text/css", "text/css; charset=UTF-8");
  }

  @Test
  public void testMimeMatchPassWithAdditionalAttributes() throws Exception {
    expectMime("text/css", "text/css", "text/css; charset=UTF-8");
  }

  @Test
  public void testMimeMatchOverrideNonMatch() throws Exception {
    expectMime("text/css", "image/png", "text/css; charset=UTF-8");
  }

  @Test
  public void testMimeMatchVarySupport() throws Exception {
    // We use CaptureRewrite which always rewrite - always set encoding
    expectMime("image/*", "image/gif", "image/gif");
  }
}
