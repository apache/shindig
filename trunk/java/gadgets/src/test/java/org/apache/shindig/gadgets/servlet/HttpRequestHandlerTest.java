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
import static org.easymock.EasyMock.reportMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.JsonAssert;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.FeedProcessor;
import org.apache.shindig.gadgets.FeedProcessorImpl;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.process.Processor;
import org.apache.shindig.gadgets.rewrite.CaptureRewriter;
import org.apache.shindig.gadgets.rewrite.DefaultResponseRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.ResponseRewriter;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterRegistry;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.apache.shindig.protocol.DefaultHandlerRegistry;
import org.apache.shindig.protocol.HandlerExecutionListener;
import org.apache.shindig.protocol.HandlerRegistry;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RpcHandler;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.protocol.multipart.FormDataItem;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * Has coverage for all tests in MakeRequestHandlerTest and should be maintained in sync  until
 * MakeRequestHandler is eliminated.
 */
public class HttpRequestHandlerTest extends EasyMockTestCase {

  private BeanJsonConverter converter;

  private FakeGadgetToken token;

  private final RequestPipeline pipeline = mock(RequestPipeline.class);
  private final CaptureRewriter rewriter = new CaptureRewriter();
  private final ResponseRewriterRegistry rewriterRegistry
      = new DefaultResponseRewriterRegistry(Arrays.<ResponseRewriter>asList(rewriter), null);
  private final Processor mockProcessor = mock(Processor.class);
  private final GadgetContext mockContext = mock(GadgetContext.class);
  private final GadgetSpec mockSpec = mock(GadgetSpec.class);
  private final ModulePrefs mockPrefs = mock(ModulePrefs.class);
  private final Gadget mockGadget = mock(Gadget.class);

  private HandlerRegistry registry;

  private HttpResponseBuilder builder;

  private final Map<String,FormDataItem> emptyFormItems = Collections.emptyMap();

  private final Provider<FeedProcessor> feedProcessorProvider = new Provider<FeedProcessor>() {
        public FeedProcessor get() {
            return new FeedProcessorImpl();
        }
  };

  private void mockGadget(List<Feature> allFeatures, String container, String gadgetUrl) {
    mockGadgetContext(container);
    mockGadgetSpec(allFeatures, gadgetUrl);
    EasyMock.expect(mockGadget.getContext()).andReturn(mockContext).anyTimes();
    EasyMock.expect(mockGadget.getSpec()).andReturn(mockSpec).anyTimes();
  }

  private void mockGadgetContext(String container) {
    EasyMock.expect(mockContext.getContainer()).andReturn(container).anyTimes();
  }

  private void mockGadgetSpec(List<Feature> allFeatures, String gadgetUrl) {
    mockModulePrefs(allFeatures);
    EasyMock.expect(mockSpec.getUrl()).andReturn(Uri.parse(gadgetUrl)).anyTimes();
    EasyMock.expect(mockSpec.getModulePrefs()).andReturn(mockPrefs).anyTimes();
  }

  private void mockModulePrefs(List<Feature> features) {
    EasyMock.expect(mockPrefs.getAllFeatures()).andReturn(features).anyTimes();
  }

  @Before
  public void setUp() throws Exception {
    token = new FakeGadgetToken();
    token.setAppUrl("http://www.example.com/gadget.xml");

    Injector injector = Guice.createInjector();
    converter = new BeanJsonConverter(injector);

    HttpRequestHandler handler = new HttpRequestHandler(pipeline, rewriterRegistry, feedProcessorProvider,
            mockProcessor);
    registry = new DefaultHandlerRegistry(injector, converter,
        new HandlerExecutionListener.NoOpHandler());
    registry.addHandlers(ImmutableSet.<Object>of(handler));
    builder = new HttpResponseBuilder().setResponseString("CONTENT");
  }

  @Test
  public void testGetWithValidGadget() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
            + "href:'http://www.example.org/somecontent'"
            + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");
    mockGadget(new ArrayList<Feature>(), "default","http://www.example.com/gadget.xml");
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();
    expect(mockProcessor.process(EasyMock.isA(GadgetContext.class))).andReturn(mockGadget);

    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
            (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals("{ headers : {}, status : 200, content : 'CONTENT' }}",
            converter.convertToString(httpApiResponse));
  }

  @Test
  public void testGetWithValidGadgeWithProcessorExceptiont() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
            + "href:'http://www.example.org/somecontent'"
            + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();
    expect(mockProcessor.process(EasyMock.isA(GadgetContext.class))).andThrow(
            new ProcessingException("error", HttpServletResponse.SC_BAD_REQUEST)).anyTimes();

    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
            (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals("{ headers : {}, status : 200, content : 'CONTENT' }}",
            converter.convertToString(httpApiResponse));
  }

  @Test
  public void testSimpleGet() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "href:'http://www.example.org/somecontent'"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();

    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals("{ headers : {}, status : 200, content : 'CONTENT' }}",
        converter.convertToString(httpApiResponse));
  }

  @Test
  public void testFailGetWithBodyGet() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "href:'http://www.example.org/somecontent',"
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
        + "href:'http://www.example.org/somecontent',"
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

    JsonAssert.assertJsonEquals("{ headers : {}, status : 200, content : 'CONTENT' }}",
        converter.convertToString(httpApiResponse));
  }

  @Test
  public void testPostWithHeaders() throws Exception {
    JSONObject request = new JSONObject("{method:http.post, id:req1, params : {"
        + "href:'http://www.example.org/somecontent',"
        + "body:'POSTBODY',"
        + "headers:{goodheader:[good], host : [iamstripped], 'Content-Length':['1000']}"
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

    JsonAssert.assertJsonEquals("{ headers : {}, status : 200, content : 'CONTENT' }}",
        converter.convertToString(httpApiResponse));
  }

  @Test
  public void testFetchContentTypeFeed() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "href:'http://www.example.org/somecontent',"
        + "format : FEED"
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

    JSONObject feed = (JSONObject) httpApiResponse.getContent();
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
        + "href:'http://www.example.org/somecontent',"
        + "format : FEED,"
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

    JSONObject feed = (JSONObject) httpApiResponse.getContent();
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
  public void testJsonObjectGet() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "href:'http://www.example.org/somecontent', format:'json'"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");
    builder.setResponseString("{key:1}");
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();

    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals("{ headers : {}, status : 200, content : {key: 1}}}",
        converter.convertToString(httpApiResponse));
  }

  @Test
  public void testJsonArrayGet() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "href:'http://www.example.org/somecontent', format:'json'"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");
    builder.setResponseString("[{key:1},{key:2}]");
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();

    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals("{ headers : {}, status : 200, content : [{key:1},{key:2}]}}",
        converter.convertToString(httpApiResponse));
  }

  @Test
  public void testSignedGetRequest() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "href:'http://www.example.org/somecontent',"
        + "authz : 'signed' }"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");
    httpRequest.setAuthType(AuthType.SIGNED);
    httpRequest.setOAuthArguments(
        new OAuthArguments(AuthType.SIGNED, ImmutableMap.<String, String>of()));
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();
    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals("{ headers : {}, status : 200, content : 'CONTENT' }}",
        converter.convertToString(httpApiResponse));

    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testSignedPostAndUpdateSecurityToken() throws Exception {
    token.setUpdatedToken("updated");
    JSONObject request = new JSONObject("{method:http.post, id:req1, params : {"
        + "href:'http://www.example.org/somecontent',"
        + "body:'POSTBODY',"
        + "authz: 'signed' }"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("POST");
    httpRequest.setAuthType(AuthType.SIGNED);
    httpRequest.setOAuthArguments(
        new OAuthArguments(AuthType.SIGNED, ImmutableMap.<String, String>of()));
    httpRequest.setPostBody("POSTBODY".getBytes());
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();
    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals("{ headers : {}, status : 200, content : 'CONTENT', token : updated }}",
        converter.convertToString(httpApiResponse));

    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testOAuthRequest() throws Exception {
    JSONObject request = new JSONObject("{method:http.post, id:req1, params : {"
        + "href:'http://www.example.org/somecontent',"
        + "body:'POSTBODY',"
        + "authz: 'oauth' }"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("POST");
    httpRequest.setAuthType(AuthType.OAUTH);
    httpRequest.setOAuthArguments(
        new OAuthArguments(AuthType.OAUTH, ImmutableMap.<String, String>of()));
    httpRequest.setPostBody("POSTBODY".getBytes());
    expect(pipeline.execute(eqRequest(httpRequest))).andReturn(builder.create()).anyTimes();
    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    operation.execute(emptyFormItems, token, converter).get();
    verify();
  }

  @Test
  public void testOAuthRequestWithParameters() throws Exception {
    JSONObject request = new JSONObject("{method:http.post, id:req1, params : {"
        + "href:'http://www.example.org/somecontent',"
        + "body:'POSTBODY',"
        + "sign_owner:'false',"
        + "sign_viewer:'true',"
        + "oauth_service_name:'oauthService',"
        + "authz: 'oauth' }"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("POST");
    httpRequest.setAuthType(AuthType.OAUTH);
    OAuthArguments oauthArgs =
        new OAuthArguments(AuthType.OAUTH, ImmutableMap.<String, String>of());
    oauthArgs.setSignOwner(false);
    oauthArgs.setServiceName("oauthService");
    httpRequest.setOAuthArguments(oauthArgs);
    httpRequest.setPostBody("POSTBODY".getBytes());

    Capture<HttpRequest> requestCapture = new Capture<HttpRequest>();
    expect(pipeline.execute(capture(requestCapture))).andReturn(builder.create());
    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    operation.execute(emptyFormItems, token, converter).get();
    verify();

    assertEquals(httpRequest.getOAuthArguments(),
        requestCapture.getValue().getOAuthArguments());
  }

  @Test
  public void testInvalidSigningTypeTreatedAsNone() throws Exception {
    JSONObject request = new JSONObject("{method:http.post, id:req1, params : {"
        + "href:'http://www.example.org/somecontent',"
        + "body:'POSTBODY',"
        + "authz : 'rubbish' }"
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
        + "href:'http://www.example.org/somecontent',"
        + "authz : 'signed'}"
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
        + "href:'http://www.example.org/somecontent'"
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

    JsonAssert.assertJsonEquals("{ headers : {}, status : 500, content : 'I AM AN ERROR MESSAGE' }}",
        converter.convertToString(httpApiResponse));
  }

  @Test
  public void testMetadataCopied() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "href:'http://www.example.org/somecontent'"
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

    JsonAssert.assertJsonEquals("{ headers : {}, status : 200, content : 'CONTENT', metadata : { foo : 'CONTENT' }}",
        converter.convertToString(httpApiResponse));
  }

  @Test
  public void testSetCookiesReturned() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "href:'http://www.example.org/somecontent',"
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

    JsonAssert.assertJsonEquals(
        "{ headers : { 'set-cookie' : ['foo=bar; Secure','name=value'] },"
            + " status : 200, content : 'CONTENT' }",
        converter.convertToString(httpApiResponse));
  }

  @Test
  public void testLocationReturned() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "href:'http://www.example.org/somecontent',"
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

    JsonAssert.assertJsonEquals("{ headers : { 'location' : ['here'] },"
            + " status : 200, content : 'CONTENT' }",
        converter.convertToString(httpApiResponse));
  }

  @Test
  public void testSimpleGetVerifySecurityTokenPresent() throws Exception {
    JSONObject request = new JSONObject("{method:http.get, id:req1, params : {"
        + "href:'http://www.example.org/somecontent'"
        + "}}");
    HttpRequest httpRequest = new HttpRequest(Uri.parse("http://www.example.org/somecontent"));
    httpRequest.setMethod("GET");
    httpRequest.setSecurityToken( token );

    // check to make sure that the security token is being passed through to the pipeline, and not
    // stripped because this is not an auth request

    expect(pipeline.execute(eqRequest2(httpRequest))).andReturn(builder.create()).anyTimes();

    replay();
    RpcHandler operation = registry.getRpcHandler(request);

    HttpRequestHandler.HttpApiResponse httpApiResponse =
        (HttpRequestHandler.HttpApiResponse)operation.execute(emptyFormItems, token, converter).get();
    verify();

    JsonAssert.assertJsonEquals("{ headers : {}, status : 200, content : 'CONTENT' }}",
        converter.convertToString(httpApiResponse));
  }


  private static HttpRequest eqRequest(HttpRequest request) {
    reportMatcher(new RequestMatcher(request));
    return null;
  }

  private static HttpRequest eqRequest2(HttpRequest request) {
    reportMatcher(new RequestMatcherWithToken(request));
    return null;
  }

  private static class RequestMatcher implements IArgumentMatcher {

    protected final HttpRequest req;

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
          Objects.equal(match.getOAuthArguments(), req.getOAuthArguments()) &&
          match.getHeaders().equals(req.getHeaders()));
    }
  }

  private static class RequestMatcherWithToken extends RequestMatcher {

    public RequestMatcherWithToken(HttpRequest request) {
      super(request);
    }

    public boolean matches(Object obj) {
      HttpRequest match = (HttpRequest)obj;
      return super.matches(obj) &&
          match.getSecurityToken() != null &&
          match.getSecurityToken().equals( req.getSecurityToken() );
    }
  }

}
