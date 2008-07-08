/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social.dataservice;

import org.apache.shindig.common.BasicSecurityTokenDecoder;
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.SecurityTokenException;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.opensocial.util.BeanConverter;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;
import org.apache.shindig.social.opensocial.util.BeanXmlConverter;

import com.google.inject.Guice;
import com.google.inject.Injector;
import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DataServiceServletTest extends TestCase {
  private HttpServletRequest req;
  private HttpServletResponse res;
  private DataServiceServlet servlet;
  private PersonHandler peopleHandler;
  private ActivityHandler activityHandler;
  private AppDataHandler appDataHandler;
  private BasicSecurityTokenDecoder tokenDecoder;
  private Injector injector;
  private BeanJsonConverter jsonConverter;
  private BeanXmlConverter xmlConverter;

  private final ServletInputStream dummyPostData = new ServletInputStream() {
    public int read() throws IOException {
      return -1;
    }
  };

  protected void setUp() throws Exception {
    servlet = new DataServiceServlet();
    req = EasyMock.createMock(HttpServletRequest.class);
    res = EasyMock.createMock(HttpServletResponse.class);
    jsonConverter = EasyMock.createMock(BeanJsonConverter.class);
    xmlConverter = EasyMock.createMock(BeanXmlConverter.class);

    peopleHandler = EasyMock.createMock(PersonHandler.class);
    activityHandler = EasyMock.createMock(ActivityHandler.class);
    appDataHandler = EasyMock.createMock(AppDataHandler.class);

    injector = EasyMock.createMock(Injector.class);
    servlet.setInjector(injector);

    servlet.setHandlers(new HandlerProvider(new PersonHandler(null), new ActivityHandler(null),
        new AppDataHandler(null)));

    servlet.setBeanConverters(jsonConverter, xmlConverter);

    servlet.setParameterFetcher(new DataServiceServletFetcher());

    tokenDecoder = EasyMock.createMock(BasicSecurityTokenDecoder.class);
    servlet.setSecurityTokenDecoder(tokenDecoder);
  }

  private void setupInjector() {
    EasyMock.expect(injector.getInstance(PersonHandler.class)).andStubReturn(peopleHandler);
    EasyMock.expect(injector.getInstance(ActivityHandler.class)).andStubReturn(activityHandler);
    EasyMock.expect(injector.getInstance(AppDataHandler.class)).andStubReturn(appDataHandler);
  }

  public void testPeopleUriRecognition() throws Exception {
    verifyHandlerWasFoundForPathInfo("/"
        + DataServiceServlet.PEOPLE_ROUTE + "/5/@self", peopleHandler);
  }

  public void testActivitiesUriRecognition() throws Exception {
    verifyHandlerWasFoundForPathInfo("/"
        + DataServiceServlet.ACTIVITY_ROUTE + "/5/@self", activityHandler);
  }

  public void testAppDataUriRecognition() throws Exception {
    verifyHandlerWasFoundForPathInfo("/"
        + DataServiceServlet.APPDATA_ROUTE + "/5/@self", appDataHandler);
  }

  public void testMethodOverride() throws Exception {
    String route = "/" + DataServiceServlet.APPDATA_ROUTE;
    verifyHandlerWasFoundForPathInfo(route, appDataHandler, "POST", "GET", "GET");
    verifyHandlerWasFoundForPathInfo(route, appDataHandler, "POST", "", "POST");
    verifyHandlerWasFoundForPathInfo(route, appDataHandler, "POST", null, "POST");
    verifyHandlerWasFoundForPathInfo(route, appDataHandler, "POST", "POST", "POST");
    verifyHandlerWasFoundForPathInfo(route, appDataHandler, "GET", null, "GET");
    verifyHandlerWasFoundForPathInfo(route, appDataHandler, "DELETE", null, "DELETE");
    verifyHandlerWasFoundForPathInfo(route, appDataHandler, "PUT", null, "PUT");
  }

  /** Tests a data handler that returns a failed Future */
  public void testFailedRequest() throws Exception {
    String route = "/" + DataServiceServlet.APPDATA_ROUTE;
    setupRequest(route, "GET", null);
    EasyMock.expect(injector.getInstance(AppDataHandler.class)).andStubReturn(appDataHandler);    
    setupInjector();

    EasyMock.expect(appDataHandler.handleItem(EasyMock.isA(RequestItem.class)));
    EasyMock.expectLastCall().andReturn(new FailingFuture());

    res.sendError(500, "FAILED");

    EasyMock.replay(req, res, appDataHandler, tokenDecoder, injector, jsonConverter);
    servlet.service(req, res);
    EasyMock.verify(req, res, appDataHandler, tokenDecoder, injector, jsonConverter);
    EasyMock.reset(req, res, appDataHandler, tokenDecoder, injector, jsonConverter);
  }

  private void verifyHandlerWasFoundForPathInfo(String peoplePathInfo, DataRequestHandler handler)
      throws Exception {
    String post = "POST";
    verifyHandlerWasFoundForPathInfo(peoplePathInfo, handler, post, post, post);
  }

  private void verifyHandlerWasFoundForPathInfo(String pathInfo, DataRequestHandler handler,
      String actualMethod, String overrideMethod, String expectedMethod) throws Exception {
    setupRequest(pathInfo, actualMethod, overrideMethod);
    setupInjector();

    String jsonObject = "my lovely json";
    ResponseItem<String> response = new ResponseItem<String>(jsonObject);
    
    EasyMock.expect(handler.handleItem(EasyMock.isA(RequestItem.class)));
    EasyMock.expectLastCall().andReturn(ImmediateFuture.newInstance(response));

    EasyMock.expect(jsonConverter.convertToString(jsonObject)).andReturn(jsonObject);

    PrintWriter writerMock = EasyMock.createMock(PrintWriter.class);
    EasyMock.expect(res.getWriter()).andReturn(writerMock);
    writerMock.write(jsonObject);

    EasyMock.replay(req, res, handler, tokenDecoder, injector, jsonConverter);
    servlet.service(req, res);
    EasyMock.verify(req, res, handler, tokenDecoder, injector, jsonConverter);
    EasyMock.reset(req, res, handler, tokenDecoder, injector, jsonConverter);
  }

  private void setupRequest(String pathInfo, String actualMethod, String overrideMethod)
      throws IOException, SecurityTokenException {
    req.setCharacterEncoding("UTF-8");

    EasyMock.expect(req.getInputStream()).andStubReturn(dummyPostData);
    EasyMock.expect(req.getPathInfo()).andStubReturn(pathInfo);
    EasyMock.expect(req.getMethod()).andStubReturn(actualMethod);
    EasyMock.expect(req.getParameterNames()).andStubReturn(new StringTokenizer(""));
    EasyMock.expect(req.getParameter(DataServiceServlet.X_HTTP_METHOD_OVERRIDE)).andReturn(
        overrideMethod);
    EasyMock.expect(req.getParameter(DataServiceServlet.FORMAT_PARAM)).andReturn(null);

    String tokenString = "owner:viewer:app:container.com:foo:bar";
    EasyMock.expect(req.getParameter(DataServiceServlet.SECURITY_TOKEN_PARAM))
        .andReturn(tokenString);

    FakeGadgetToken token = new FakeGadgetToken();
    EasyMock.expect(tokenDecoder.createToken(Collections.singletonMap(SecurityTokenDecoder.SECURITY_TOKEN_NAME, tokenString))).andReturn(token);
  }

  public void testInvalidRoute() throws Exception {
    RequestItem requestItem = new RequestItem();
    requestItem.setUrl("/ahhh!");
    try {
      servlet.handleRequestItem(requestItem);
      fail("The route should not have found a valid handler.");
    } catch (RuntimeException e) {
      // Yea!
      assertEquals("No handler for route: ahhh!", e.getMessage());
    }
  }

  public void testSecurityTokenException() throws Exception {
    String tokenString = "owner:viewer:app:container.com:foo:bar";
    EasyMock.expect(req.getParameter(DataServiceServlet.SECURITY_TOKEN_PARAM))
        .andReturn(tokenString);
    EasyMock.expect(tokenDecoder.createToken(
        Collections.singletonMap(SecurityTokenDecoder.SECURITY_TOKEN_NAME, tokenString)))
        .andThrow(new SecurityTokenException(""));

    EasyMock.replay(req, tokenDecoder);
    try {
      servlet.getSecurityToken(req);
      fail("The route should have thrown an exception due to the invalid security token.");
    } catch (RuntimeException e) {
      // Yea!
      // TODO: The impl being tested here is not finished. We should return a proper error
      // instead of just throwing an exception.
      assertEquals("Implement error return for bad security token.", e.getMessage());
    }
    EasyMock.verify(req, tokenDecoder);
  }

  public void testGetHttpMethodFromParameter() throws Exception {
    String method = "POST";
    assertEquals(method, servlet.getHttpMethodFromParameter(method, null));
    assertEquals(method, servlet.getHttpMethodFromParameter(method, ""));
    assertEquals(method, servlet.getHttpMethodFromParameter(method, "  "));
    assertEquals("DELETE", servlet.getHttpMethodFromParameter(method, "DELETE"));
  }

  public void testRouteFromParameter() throws Exception {
    assertEquals("path", servlet.getRouteFromParameter("/path"));
    assertEquals("path", servlet.getRouteFromParameter("/path/fun"));
    assertEquals("path", servlet.getRouteFromParameter("/path/fun/yes"));
  }

  public void testIsBatchUrl() throws Exception {
    assertBatchUrl("/jsonBatch", true);
    assertBatchUrl("/path/to/the/jsonBatch", true);
    assertBatchUrl("/people/normalpath", false);
    assertBatchUrl("/notjsonBatch", false);
  }

  private void assertBatchUrl(String url, boolean isBatch) {
    EasyMock.expect(req.getPathInfo()).andReturn(url);
    EasyMock.replay(req);
    assertEquals(isBatch, servlet.isBatchUrl(req));
    EasyMock.verify(req);
    EasyMock.reset(req);
  }

  public void testGetConverterForRequest() throws Exception {
    BeanJsonConverter json = new BeanJsonConverter(
        Guice.createInjector(new SocialApiTestsGuiceModule()));
    BeanXmlConverter xml = new BeanXmlConverter();
    servlet.setBeanConverters(json, xml);

    assertConverter(xml, "atom");
    assertConverter(json, "");
    assertConverter(json, null);
    assertConverter(json, "ahhhh!");
  }

  private void assertConverter(BeanConverter converter, String format) {
    EasyMock.expect(req.getParameter(DataServiceServlet.FORMAT_PARAM)).andReturn(format);
    EasyMock.replay(req);
    assertEquals(converter, servlet.getConverterForRequest(req));
    EasyMock.verify(req);
    EasyMock.reset(req);
  }

  /**
   * Future implementation that fails with an exception.
   */
  private static class FailingFuture implements Future<ResponseItem> {
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    public boolean isCancelled() {
      return false;
    }

    public boolean isDone() {
      return true;
    }

    public ResponseItem get() throws InterruptedException, ExecutionException {
      throw new ExecutionException(new RuntimeException("FAILED"));
    }

    public ResponseItem get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
  }
}
