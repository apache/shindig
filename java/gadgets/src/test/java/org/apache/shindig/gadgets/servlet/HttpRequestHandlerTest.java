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
import static org.easymock.EasyMock.reportMatcher;

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.JsonAssert;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.rewrite.CaptureRewriter;
import org.apache.shindig.gadgets.rewrite.RequestRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.DefaultRequestRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.RequestRewriter;
import org.apache.shindig.protocol.DefaultHandlerRegistry;
import org.apache.shindig.protocol.HandlerExecutionListener;
import org.apache.shindig.protocol.HandlerRegistry;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RpcHandler;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.protocol.multipart.FormDataItem;
import org.easymock.IArgumentMatcher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Has coverage for all tests in MakeRequestHandlerTest and should be maintained in sync  until
 * MakeRequestHandler is eliminated.
 */
public class HttpRequestHandlerTest extends EasyMockTestCase {

  private BeanJsonConverter converter;

  private FakeGadgetToken token;

  private final RequestPipeline pipeline = mock(RequestPipeline.class);
  private final CaptureRewriter rewriter = new CaptureRewriter();
  private final RequestRewriterRegistry rewriterRegistry
      = new DefaultRequestRewriterRegistry(Arrays.<RequestRewriter>asList(rewriter), null);

  private HandlerRegistry registry;

  private HttpResponseBuilder builder;

  private Map<String,FormDataItem> emptyFormItems;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    token = new FakeGadgetToken();
    token.setAppUrl("http://www.example.com/gadget.xml");

    Injector injector = Guice.createInjector();
    converter = new BeanJsonConverter(injector);

    HttpRequestHandler handler = new HttpRequestHandler(pipeline, rewriterRegistry);
    registry = new DefaultHandlerRegistry(injector, converter,
        new HandlerExecutionListener.NoOpHandler());
    registry.addHandlers(Sets.<Object>newHashSet(handler));
    builder = new HttpResponseBuilder().setResponseString("CONTENT");
    emptyFormItems = Collections.emptyMap();
  }

  @Test
  public void testSimpleGet() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "url:'http://www.example.org/somecontent'"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();

    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals(converter.convertToString(httpApiResponse),
        "{ headers : {}, status : 200, body : 'CONTENT' }}");
  }

  @Test
  public void testFailGetWithBodyGet() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "url:'http://www.example.org/somecontent',"
        + "body:'POSTBODY'"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");
    RpcHandler operation = registry.getRpcHandler(request);
    try {
      operation.execute(emptyFormItems, token, converter).get();
      fail("Body should not be allowed in GET request");
    } catch (ExecutionException ee) {
      assertTrue(ee.getCause() instanceof ProtocolException);
    }
  }

  @Test
  public void testSimplePost() throws Exception {
    JSONObject request = new JSONObject("{method:http.post, id:req1, params : {"
        + "url:'http://www.example.org/somecontent',"
        + "body:'POSTBODY'"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("POST");
    httpRequest.setPostBody("POSTBODY".getBytes());
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();

    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals(converter.convertToString(httpApiResponse),
        "{ headers : {}, status : 200, body : 'CONTENT' }}");
  }

  @Test
  public void testPostWithHeaders() throws Exception {
    JSONObject request = new JSONObject("{method:http.post, id:req1, params : {"
        + "url:'http://www.example.org/somecontent',"
        + "body:'POSTBODY',"
        + "headers:{goodheader:good, host : iamstripped, 'Content-Length':'1000'}"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("POST");
    httpRequest.setPostBody("POSTBODY".getBytes());
    httpRequest.setHeader("goodheader", "good");
    httpRequest.setHeader("Content-Length", "1000");
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();

    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals(converter.convertToString(httpApiResponse),
        "{ headers : {}, status : 200, body : 'CONTENT' }}");
  }

  @Test
  public void testFetchContentTypeFeed() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "url:'http://www.example.org/somecontent',"
        + "contentType : FEED"
        + "}}");

    String entryTitle = "Feed title";
    String entryLink = "http://example.org/entry/0/1";
    String entrySummary = "This is the summary";
    String rss = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                 "<rss version=\"2.0\"><channel>" +
                 "<title>dummy</title>" +
                 "<link>http://example.org/</link>" +
                 "<item>" +
                 "<title>" + entryTitle + "</title>" +
                 "<link>" + entryLink + "</link>" +
                 "<description>" + entrySummary + "</description>" +
                 "</item>" +
                 "</channel></rss>";
    builder.setResponseString(rss);

    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");

    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();

    replay();

    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JSONObject feed = new JSONObject(httpApiResponse.getBody());
    JSONObject entry = feed.getJSONArray("Entry").getJSONObject(0);

    assertEquals(entryTitle, entry.getString("Title"));
    assertEquals(entryLink, entry.getString("Link"));
    assertNull("getSummaries has the wrong default value (should be false).",
        entry.optString("Summary", null));
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testFetchFeedWithParameters() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "url:'http://www.example.org/somecontent',"
        + "contentType : FEED,"
        + "summarize : true,"
        + "entryCount : 2"
        + "}}");

    String entryTitle = "Feed title";
    String entryLink = "http://example.org/entry/0/1";
    String entrySummary = "This is the summary";
    String rss = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                 "<rss version=\"2.0\"><channel>" +
                 "<title>dummy</title>" +
                 "<link>http://example.org/</link>" +
                 "<item>" +
                 "<title>" + entryTitle + "</title>" +
                 "<link>" + entryLink + "</link>" +
                 "<description>" + entrySummary + "</description>" +
                 "</item>" +
                 "<item>" +
                 "<title>" + entryTitle + "</title>" +
                 "<link>" + entryLink + "</link>" +
                 "<description>" + entrySummary + "</description>" +
                 "</item>" +
                 "<item>" +
                 "<title>" + entryTitle + "</title>" +
                 "<link>" + entryLink + "</link>" +
                 "<description>" + entrySummary + "</description>" +
                 "</item>" +
                 "</channel></rss>";

    builder.setResponseString(rss);

    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");

    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();

    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JSONObject feed = new JSONObject(httpApiResponse.getBody());
    JSONArray feeds = feed.getJSONArray("Entry");

    assertEquals("numEntries not parsed correctly.", 2, feeds.length());

    JSONObject entry = feeds.getJSONObject(1);
    assertEquals(entryTitle, entry.getString("Title"));
    assertEquals(entryLink, entry.getString("Link"));
    assertTrue("getSummaries not parsed correctly.", entry.has("Summary"));
    assertEquals(entrySummary, entry.getString("Summary"));
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testSignedGetRequest() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "url:'http://www.example.org/somecontent',"
        + "auth : { type: signed }"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");
    httpRequest.setAuthType(AuthType.SIGNED);
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();
    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals(converter.convertToString(httpApiResponse),
        "{ headers : {}, status : 200, body : 'CONTENT' }}");

    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testSignedPostAndUpdateSecurityToken() throws Exception {
    token.setUpdatedToken("updated");
    JSONObject request = new JSONObject("{method:http.post, id:req1, params : {"
        + "url:'http://www.example.org/somecontent',"
        + "body:'POSTBODY',"
        + "auth : { type: signed }"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("POST");
    httpRequest.setAuthType(AuthType.SIGNED);
    httpRequest.setPostBody("POSTBODY".getBytes());
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();
    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals(converter.convertToString(httpApiResponse),
        "{ headers : {}, status : 200, body : 'CONTENT', token : updated }}");

    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testOAuthRequest() throws Exception {
    JSONObject request = new JSONObject("{method:http.post, id:req1, params : {"
        + "url:'http://www.example.org/somecontent',"
        + "body:'POSTBODY',"
        + "auth : { type: oauth }"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("POST");
    httpRequest.setAuthType(AuthType.OAUTH);
    httpRequest.setPostBody("POSTBODY".getBytes());
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();
    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    operation.execute(emptyFormItems, token, converter).get();
    verify();
  }

  @Test
  public void testInvalidSigningTypeTreatedAsNone() throws Exception {
    JSONObject request = new JSONObject("{method:http.post, id:req1, params : {"
        + "url:'http://www.example.org/somecontent',"
        + "body:'POSTBODY',"
        + "auth : { type: rubbish }"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("POST");
    httpRequest.setAuthType(AuthType.NONE);
    httpRequest.setPostBody("POSTBODY".getBytes());
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();
    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    operation.execute(emptyFormItems, token, converter).get();
    verify();
  }

  @Test
  public void testSignedGetRequestNoSecurityToken() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "url:'http://www.example.org/somecontent',"
        + "auth : { type: signed }"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");
    httpRequest.setAuthType(AuthType.SIGNED);
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();
    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    try {
      operation.execute(emptyFormItems, null, converter).get();
      fail("Cannot execute a request without a security token");
    } catch (ExecutionException ee) {
      assertTrue(ee.getCause() instanceof ProtocolException);
    }
    verify();
  }

  @Test
  public void testBadHttpResponseIsPropagated() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "url:'http://www.example.org/somecontent'"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");
    httpRequest.setAuthType(AuthType.NONE);
    builder.setHttpStatusCode(HttpResponse.SC_INTERNAL_SERVER_ERROR);
    builder.setResponseString("I AM AN ERROR MESSAGE");
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();
    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals(converter.convertToString(httpApiResponse),
        "{ headers : {}, status : 500, body : 'I AM AN ERROR MESSAGE' }}");
  }

  @Test
  public void testMetadataCopied() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "url:'http://www.example.org/somecontent'"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");
    builder.setMetadata("foo", "CONTENT");
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();

    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals(converter.convertToString(httpApiResponse),
        "{ headers : {}, status : 200, body : 'CONTENT', metadata : { foo : 'CONTENT' }}");
  }

  @Test
  public void testSetCookiesReturned() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "url:'http://www.example.org/somecontent',"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");
    builder.addHeader("Set-Cookie", "foo=bar; Secure");
    builder.addHeader("Set-Cookie", "name=value");
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();

    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals(converter.convertToString(httpApiResponse),
        "{ headers : { 'set-cookie' : ['foo=bar; Secure','name=value'] },"
            + " status : 200, body : 'CONTENT' }");    
  }

  @Test
  public void testLocationReturned() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "url:'http://www.example.org/somecontent',"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");
    builder.addHeader("Location", "here");
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();

    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals(converter.convertToString(httpApiResponse),
        "{ headers : { 'location' : ['here'] },"
            + " status : 200, body : 'CONTENT' }");
  }

  private static HttpRequest eqRequest(HttpRequest request) {
    reportMatcher(new RequestMatcher(request));
    return null;
  }

  private static class RequestMatcher implements IArgumentMatcher {

    private final HttpRequest req;

    public RequestMatcher(HttpRequest request) {
      this.req = request;
    }

    public void appendTo(StringBuffer buffer) {
      buffer.append("eqRequest[]");
    }

    public boolean matches(Object obj) {
      HttpRequest match = (HttpRequest)obj;
      return (match.getMethod().equals(req.getMethod()) &&
          match.getUri().equals(req.getUri()) &&
          match.getAuthType().equals(req.getAuthType()) &&
          match.getPostBodyAsString().equals(req.getPostBodyAsString()) &&
          match.getHeaders().equals(req.getHeaders()));
    }
  }
}
