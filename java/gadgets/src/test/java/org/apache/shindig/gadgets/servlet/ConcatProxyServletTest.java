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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.junit.Before;
import org.junit.Test;

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

  private final ProxyHandler proxyHandler =
      new ProxyHandler(pipeline, lockedDomainService, null);
  private final ConcatProxyServlet servlet = new ConcatProxyServlet();
  
  @Before
  public void setUp() throws Exception {
    servlet.setProxyHandler(proxyHandler);
    expect(request.getHeader("Host")).andReturn(REQUEST_DOMAIN).anyTimes();
    expect(lockedDomainService.isSafeForOpenProxy(REQUEST_DOMAIN))
        .andReturn(true).anyTimes();

    expectGetAndReturnData(URL1,SCRT1);
    expectGetAndReturnData(URL2,SCRT2);
    expectGetAndReturnData(URL3,SCRT3);
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
  
  /**
   * Simulate the asJSON result of one script
   * @param url - the script url
   * @param data - the script escaped content
   * @return simulated hash mapping
   */
  private String addVar(String url, String data) {
    return  "\"" + url + "\":\"" + data +"\",\r\n"; 
    
  }
  
  /**
   * Run a concat test
   * @param result - expected concat results
   * @param uris - list of uris to concat
   * @throws Exception
   */
  private void runConcat(String result, Uri... uris) throws Exception {
    for (int i = 0 ; i < uris.length ; i++) {
      expect(request.getParameter(Integer.toString(i+1))).andReturn(uris[i].toString()).once();
    }
    expect(request.getParameter(Integer.toString(uris.length+1))).andReturn(null).once();
    replay();
    // Run the servlet
    servlet.doGet(request, recorder);
    verify();
    assertEquals(result, recorder.getResponseAsString());
    assertEquals(200, recorder.getHttpStatusCode());
  }
  
  @Test
  public void testSimpleConcat() throws Exception {
    String results = addComment(SCRT1, URL1.toString()) + addComment(SCRT2,URL2.toString());
    runConcat(results, URL1,URL2);
  }
  
  @Test
  public void testThreeConcat() throws Exception {
    String results = addComment(SCRT1, URL1.toString()) + addComment(SCRT2,URL2.toString())
        + addComment(SCRT3,URL3.toString());
    runConcat(results, URL1, URL2, URL3);
  }

  @Test
  public void testConcatBadException() throws Exception {
    final Uri URL4 = Uri.parse("http://example.org/4.js");

    HttpRequest req = new HttpRequest(URL4);
    expect(pipeline.execute(req)).andThrow(
        new GadgetException(GadgetException.Code.HTML_PARSE_ERROR)).anyTimes();

    String results = addComment(SCRT1, URL1.toString())
        + "/* ---- Start http://example.org/4.js ---- */\r\n"
        + "HTML_PARSE_ERROR concat(http://example.org/4.js) null";

    expect(request.getParameter(Integer.toString(1))).andReturn(URL1.toString()).once();
    expect(request.getParameter(Integer.toString(2))).andReturn(URL4.toString()).once();
    replay();
    // Run the servlet
    servlet.doGet(request, recorder);
    verify();
    assertEquals(results, recorder.getResponseAsString());
    assertEquals(400, recorder.getHttpStatusCode());
  }

  @Test
  public void testAsJsonConcat() throws Exception {
    expect(request.getParameter("json")).andReturn("_js").once();
    String results = "_js={\r\n"
        + addVar(URL1.toString(), SCRT1_ESCAPED)
        + addVar(URL2.toString(), SCRT2_ESCAPED)
        + "};\r\n";
    runConcat(results, URL1, URL2);
  }

  @Test
  public void testThreeAsJsonConcat() throws Exception {
    expect(request.getParameter("json")).andReturn("testJs").once();
    String results = "testJs={\r\n"
        + addVar(URL1.toString(), SCRT1_ESCAPED)
        + addVar(URL2.toString(), SCRT2_ESCAPED)
        + addVar(URL3.toString(), SCRT3_ESCAPED)
        + "};\r\n";
    runConcat(results, URL1, URL2, URL3);
  }
  
  @Test
  public void testBadJsonVarConcat() throws Exception {
    expect(request.getParameter("json")).andReturn("bad code;").once();
    replay();
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

    expect(request.getParameter("json")).andReturn("_js").once();
    String results = "_js={\r\n"
        + addVar(URL1.toString(), SCRT1_ESCAPED)
        + addVar(URL4.toString(),"")
        + "/* ---- Error 404 ---- */\r\n"
        + "};\r\n";
    runConcat(results, URL1, URL4);
  }
  
  @Test
  public void testAsJsonConcatException() throws Exception {
    final Uri URL4 = Uri.parse("http://example.org/4.js");

    HttpRequest req = new HttpRequest(URL4);
    expect(pipeline.execute(req)).andThrow(
        new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT)).anyTimes();

    expect(request.getParameter("json")).andReturn("_js").once();
    String results = "_js={\r\n"
        + addVar(URL1.toString(), SCRT1_ESCAPED)
        + "/* ---- End http://example.org/4.js 404 ---- */\r\n"
        + addVar(URL4.toString(),"") 
        + "};\r\n";
    runConcat(results, URL1, URL4);
  }

  @Test
  public void testAsJsonConcatBadException() throws Exception {
    final Uri URL4 = Uri.parse("http://example.org/4.js");

    HttpRequest req = new HttpRequest(URL4);
    expect(pipeline.execute(req)).andThrow(
        new GadgetException(GadgetException.Code.HTML_PARSE_ERROR)).anyTimes();

    expect(request.getParameter("json")).andReturn("_js").once();
    String results = "_js={\r\n"
        + addVar(URL1.toString(), SCRT1_ESCAPED)
        + addVar(URL4.toString(),"")
        + "};\r\n"
        + "HTML_PARSE_ERROR concat(http://example.org/4.js) null";

    expect(request.getParameter(Integer.toString(1))).andReturn(URL1.toString()).once();
    expect(request.getParameter(Integer.toString(2))).andReturn(URL4.toString()).once();
    replay();
    // Run the servlet
    servlet.doGet(request, recorder);
    verify();
    assertEquals(results, recorder.getResponseAsString());
    assertEquals(400, recorder.getHttpStatusCode());
  }

}
