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

import java.util.List;

import org.apache.shindig.common.servlet.HttpServletResponseRecorder;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.uri.ConcatUriManager;
import org.apache.shindig.gadgets.uri.UriStatus;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcatProxyServletTest extends ServletTestFixture {
  private static final String REQUEST_DOMAIN = "example.org";

  private static final Uri URL1 = Uri.parse("http://example.org/1.js");
  private static final Uri URL2 = Uri.parse("http://example.org/2.js");
  private static final Uri URL3 = Uri.parse("http://example.org/3.js");

  private static final String SCRT1 = "var v1 = 1;";
  private static final String SCRT2 = "var v2 = { \"a-b\": 1 , c: \"hello!,\" };";
  private static final String SCRT3 = "var v3 = \"world\";";

  private static final String SCRT1_ESCAPED = "var v1 = 1;";
  private static final String SCRT2_ESCAPED =
      "var v2 = { \\\"a-b\\\": 1 , c: \\\"hello!,\\\" };";
  private static final String SCRT3_ESCAPED = "var v3 = \\\"world\\\";";

  private final ConcatProxyServlet servlet = new ConcatProxyServlet();
  private TestConcatUriManager uriManager;

  private final ExecutorService sequentialExecutor = Executors.newSingleThreadExecutor();
  private final ExecutorService threadedExecutor = Executors.newCachedThreadPool();

  @Before
  public void setUp() throws Exception {
    servlet.setRequestPipeline(pipeline);
    uriManager = new TestConcatUriManager();
    servlet.setConcatUriManager(uriManager);

    expect(request.getHeader("Host")).andReturn(REQUEST_DOMAIN).anyTimes();
    expect(lockedDomainService.isSafeForOpenProxy(REQUEST_DOMAIN))
        .andReturn(true).anyTimes();

    expectGetAndReturnData(URL1, SCRT1);
    expectGetAndReturnData(URL2, SCRT2);
    expectGetAndReturnData(URL3, SCRT3);
  }

  private void expectGetAndReturnData(Uri url, String data) throws Exception {
    HttpRequest req = new HttpRequest(url);
    HttpResponse resp = new HttpResponseBuilder().setResponse(data.getBytes()).create();
    expect(pipeline.execute(req)).andReturn(resp).anyTimes();
  }

  /**
   * Simulate the added comments by concat
   * @param data - concatenated data
   * @param url - data source url
   * @return data with added comments
   */
  private String addComment(String data, String url) {
    String res = "/* ---- Start " + url + " ---- */\r\n"
        + data + "/* ---- End " + url + " ---- */\r\n";
    return res;
  }

  private String addErrComment(String url, int code) {
    return "/* ---- Error " + code + " (" + url + ") ---- */\r\n";
  }

  private String addConcatErrComment(GadgetException.Code code , String url) {
    return "/* ---- Error " + code.toString() + " concat(" + url + ") null ---- */\r\n";
  }
  /**
   * Simulate the asJSON result of one script
   * @param url - the script url
   * @param data - the script escaped content
   * @return simulated hash mapping
   */
  private String addVar(String url, String data) {
    return '\"' + url + "\":\"" + data +"\",\r\n";
  }

  private String addLastVar(String url, String data) {
    return '\"' + url + "\":\"" + data +"\"";
  }

  /**
   * Run a concat test by fetching resources as configured by given Executor
   * @param result - expected concat results
   * @param uris - list of uris to concat
   * @throws Exception
   */
  private void runConcat(ExecutorService exec, String result, String tok, Uri... uris)
      throws Exception {
    expectRequestWithUris(Lists.newArrayList(uris), tok);

    // Run the servlet
    servlet.setExecutor(exec);
    servlet.doGet(request, recorder);
    verify();
    assertEquals(result, recorder.getResponseAsString());
    assertEquals(200, recorder.getHttpStatusCode());
  }

  @Test
  public void testSimpleConcat() throws Exception {
    String results = addComment(SCRT1, URL1.toString()) + addComment(SCRT2,URL2.toString());
    runConcat(sequentialExecutor, results, null, URL1, URL2);
  }

  @Test
  public void testSimpleConcatThreaded() throws Exception {
    String results = addComment(SCRT1, URL1.toString()) + addComment(SCRT2,URL2.toString());
    runConcat(threadedExecutor, results, null, URL1, URL2);
  }

  @Test
  public void testThreeConcat() throws Exception {
    String results = addComment(SCRT1, URL1.toString()) + addComment(SCRT2,URL2.toString())
        + addComment(SCRT3, URL3.toString());
    runConcat(sequentialExecutor, results, null, URL1, URL2, URL3);
  }

  @Test
  public void testThreeConcatThreaded() throws Exception {
    String results = addComment(SCRT1, URL1.toString()) + addComment(SCRT2,URL2.toString())
        + addComment(SCRT3, URL3.toString());
    runConcat(threadedExecutor, results, null, URL1, URL2, URL3);
  }

  @Test
  public void testConcatBadException() throws Exception {
    final Uri URL4 = Uri.parse("http://example.org/4.js");

    HttpRequest req = new HttpRequest(URL4);
    expect(pipeline.execute(req)).andThrow(
        new GadgetException(GadgetException.Code.HTML_PARSE_ERROR)).anyTimes();

    expectRequestWithUris(Lists.newArrayList(URL1, URL4));

    // Run the servlet
    servlet.doGet(request, recorder);
    verify();

    String results = addComment(SCRT1, URL1.toString())
        + addConcatErrComment(GadgetException.Code.HTML_PARSE_ERROR, URL4.toString());
    assertEquals(results, recorder.getResponseAsString());

    assertEquals(400, recorder.getHttpStatusCode());
  }

  @Test
  public void testConcat404() throws Exception {
    String url = "http://nobodyhome.com/";
    HttpRequest req = new HttpRequest(Uri.parse(url));
    HttpResponse resp = new HttpResponseBuilder().setHttpStatusCode(404).create();
    expect(pipeline.execute(req)).andReturn(resp).anyTimes();

    expectRequestWithUris(Lists.newArrayList(URL1, Uri.parse(url)));

    servlet.doGet(request, recorder);
    verify();

    String results = addComment(SCRT1, URL1.toString()) + addErrComment(url,404);
    assertEquals(results, recorder.getResponseAsString());
    assertEquals(200, recorder.getHttpStatusCode());
  }

  @Test
  public void testAsJsonConcat() throws Exception {
    String results = "_js={\r\n"
        + addVar(URL1.toString(), SCRT1_ESCAPED)
        + addLastVar(URL2.toString(), SCRT2_ESCAPED)
        + "};\r\n";
    runConcat(sequentialExecutor, results, "_js", URL1, URL2);
  }

  @Test
  public void testThreeAsJsonConcat() throws Exception {
    String results = "_js={\r\n"
        + addVar(URL1.toString(), SCRT1_ESCAPED)
        + addVar(URL2.toString(), SCRT2_ESCAPED)
        + addLastVar(URL3.toString(), SCRT3_ESCAPED)
        + "};\r\n";
    runConcat(sequentialExecutor, results, "_js", URL1, URL2, URL3);
  }

  @Test
  public void testBadJsonVarConcat() throws Exception {
    expectRequestWithUris(Lists.<Uri>newArrayList(), "bad code;");
    servlet.doGet(request, recorder);
    verify();
    String results = "/* ---- Error 400, Bad json variable name bad code; ---- */\r\n";
    assertEquals(results, recorder.getResponseAsString());
    assertEquals(400, recorder.getHttpStatusCode());
  }

  @Test
  public void testAsJsonConcat404() throws Exception {
    final Uri URL4 = Uri.parse("http://example.org/4.js");

    HttpRequest req = new HttpRequest(URL4);
    HttpResponse resp = new HttpResponseBuilder().setHttpStatusCode(404).create();
    expect(pipeline.execute(req)).andReturn(resp).anyTimes();

    String results = "_js={\r\n"
        + addLastVar(URL1.toString(), SCRT1_ESCAPED)
        + "/* ---- Error 404 (http://example.org/4.js) ---- */\r\n"
        + "};\r\n";
    runConcat(sequentialExecutor, results, "_js", URL1, URL4);
  }

  @Test
  public void testAsJsonConcatException() throws Exception {
    final Uri URL4 = Uri.parse("http://example.org/4.js");

    HttpRequest req = new HttpRequest(URL4);
    expect(pipeline.execute(req)).andThrow(
        new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT)).anyTimes();

    expectRequestWithUris(Lists.newArrayList(URL1, URL4), "_js");
    servlet.doGet(request, recorder);
    verify();
    String results = "_js={\r\n"
      + addLastVar(URL1.toString(), SCRT1_ESCAPED)
      + addConcatErrComment(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT, URL4.toString()) + "};\r\n";
    assertEquals(results, recorder.getResponseAsString());
    assertEquals(400, recorder.getHttpStatusCode());
  }

  @Test
  public void testAsJsonConcatBadException() throws Exception {
    final Uri URL4 = Uri.parse("http://example.org/4.js");

    HttpRequest req = new HttpRequest(URL4);
    expect(pipeline.execute(req)).andThrow(
        new GadgetException(GadgetException.Code.HTML_PARSE_ERROR)).anyTimes();

    String results = "_js={\r\n"
        + addLastVar(URL1.toString(), SCRT1_ESCAPED)
        + addConcatErrComment(GadgetException.Code.HTML_PARSE_ERROR, URL4.toString()) + "};\r\n";

    expectRequestWithUris(Lists.newArrayList(URL1, URL4), "_js");

    // Run the servlet
    servlet.doGet(request, recorder);
    verify();
    assertEquals(results, recorder.getResponseAsString());
    assertEquals(400, recorder.getHttpStatusCode());
  }

  @Test
  public void testMinimumCacheTtl() throws Exception {
    final Uri URL4 = Uri.parse("http://example.org/4.js");
    final Uri URL5 = Uri.parse("http://example.org/5.js");
    final Uri URL6 = Uri.parse("http://example.org/6.js");

    final Integer cacheTtl4 = Integer.MAX_VALUE;
    final Integer cacheTtl5 = 100000;
    final Integer cacheTtl6 = Integer.MAX_VALUE;

    expectGetAndSetCacheTtl(URL4, cacheTtl4);
    expectGetAndSetCacheTtl(URL5, cacheTtl5);
    expectGetAndSetCacheTtl(URL6, cacheTtl6);

    expectRequestWithUris(Lists.newArrayList(URL4, URL5, URL6));

    // Run the servlet
    servlet.doGet(request, recorder);
    verify();
    int cacheValue = getCacheControlMaxAge(recorder);
    assertEquals(cacheTtl5, cacheValue, 10);
  }

  @Test
  public void testDefaultCacheTtlCacheHeaderMissing() throws Exception {
    final Uri URL4 = Uri.parse("http://example.org/4.js");
    final Uri URL5 = Uri.parse("http://example.org/5.js");

    expectGetAndReturnData(URL4, "");
    expectGetAndReturnData(URL5, "");
    expectRequestWithUris(Lists.newArrayList(URL4, URL5));

    servlet.doGet(request, recorder);
    verify();
    int cacheValue = getCacheControlMaxAge(recorder);
    // HttpResponse.defaultTtl is in msec, division by 1000 is required to convert into sec.
    assertEquals((int) (HttpResponse.defaultTtl / 1000), cacheValue, 10);
  }

  private void expectGetAndSetCacheTtl(Uri url, Integer cacheTtl) throws Exception {
    HttpRequest req = new HttpRequest(url);
    HttpResponse resp = new HttpResponseBuilder().setCacheTtl(cacheTtl).create();
    expect(pipeline.execute(req)).andReturn(resp);
  }

  /**
   * Returns cache control max age from HttpServletResponseRecorder
   */
  private int getCacheControlMaxAge(HttpServletResponseRecorder recorder) {
    String cacheControl = recorder.getHeader("Cache-Control");
    assertTrue(cacheControl != null);
    String cacheValue = cacheControl.substring(cacheControl.indexOf('=') + 1);
    return Integer.decode(cacheValue);
  }

  private void expectRequestWithUris(List<Uri> uris) {
    expectRequestWithUris(uris, null);
  }

  private void expectRequestWithUris(List<Uri> uris, String tok) {
    expect(request.getScheme()).andReturn("http").anyTimes();
    expect(request.getServerPort()).andReturn(80).anyTimes();
    expect(request.getServerName()).andReturn("example.com").anyTimes();
    expect(request.getRequestURI()).andReturn("/path").anyTimes();
    expect(request.getQueryString()).andReturn("").anyTimes();
    replay();

    Uri uri = new UriBuilder(request).toUri();
    uriManager.expect(uri, uris, tok);
  }

  private static class TestConcatUriManager implements ConcatUriManager {
    private final Map<Uri, ConcatUri> uriMap;

    private TestConcatUriManager() {
      this.uriMap = Maps.newHashMap();
    }

    public List<ConcatData> make(List<ConcatUri> resourceUris, boolean isAdjacent) {
      // Not used by ConcatProxyServlet
      throw new UnsupportedOperationException();
    }

    public ConcatUri process(Uri uri) {
      return uriMap.get(uri);
    }

    private void expect(Uri orig, UriStatus status, Type type, List<Uri> uris, String json) {
      uriMap.put(orig, new ConcatUri(status, uris, json, type, null));
    }

    private void expect(Uri orig, List<Uri> uris, String tok) {
      expect(orig, UriStatus.VALID_UNVERSIONED, Type.JS, uris, tok);
    }
  }
}
