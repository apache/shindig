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
package org.apache.shindig.social.opensocial.service;

import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.testing.FakeHttpServletRequest;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.core.util.BeanAtomConverter;
import org.apache.shindig.social.core.util.BeanJsonConverter;
import org.apache.shindig.social.core.util.BeanXStreamAtomConverter;
import org.apache.shindig.social.core.util.BeanXStreamConverter;
import org.apache.shindig.social.core.util.BeanXmlConverter;
import org.apache.shindig.social.core.util.xstream.GuiceBeanProvider;
import org.apache.shindig.social.core.util.xstream.XStream081Configuration;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DataServiceServletTest extends TestCase {

  private static final FakeGadgetToken FAKE_GADGET_TOKEN = new FakeGadgetToken()
      .setOwnerId("john.doe").setViewerId("john.doe");

  private HttpServletRequest req;
  private HttpServletResponse res;
  private DataServiceServlet servlet;
  private PersonHandler peopleHandler;
  private ActivityHandler activityHandler;
  private AppDataHandler appDataHandler;
  private BeanJsonConverter jsonConverter;

  private final ServletInputStream dummyPostData = new ServletInputStream() {
    @Override public int read()  {
      return -1;
    }
  };

  @Override protected void setUp() throws Exception {
    servlet = new DataServiceServlet();
    req = EasyMock.createMock(HttpServletRequest.class);
    res = EasyMock.createMock(HttpServletResponse.class);
    jsonConverter = EasyMock.createMock(BeanJsonConverter.class);
    BeanXStreamConverter xmlConverter = EasyMock.createMock(BeanXStreamConverter.class);
    BeanXStreamAtomConverter atomConverter = EasyMock.createMock(BeanXStreamAtomConverter.class);
    peopleHandler = EasyMock.createMock(PersonHandler.class);
    activityHandler = EasyMock.createMock(ActivityHandler.class);
    appDataHandler = EasyMock.createMock(AppDataHandler.class);

    EasyMock.expect(jsonConverter.getContentType()).andReturn("application/json");
    EasyMock.expect(xmlConverter.getContentType()).andReturn("application/xml");
    EasyMock.expect(atomConverter.getContentType()).andReturn("application/atom+xml");

    HandlerDispatcher dispatcher = new StandardHandlerDispatcher(constant(peopleHandler),
        constant(activityHandler), constant(appDataHandler));
    servlet.setHandlerDispatcher(dispatcher);

    servlet.setBeanConverters(jsonConverter, xmlConverter, atomConverter);
  }

  // TODO: replace with Providers.of() when Guice version is upgraded
  private static <T> Provider<T> constant(final T value) {
    return new Provider<T>() {
      public T get() {
        return value;
      }
    };
  }

  public void testPeopleUriRecognition() throws Exception {
    verifyHandlerWasFoundForPathInfo('/'
        + DataServiceServlet.PEOPLE_ROUTE + "/5/@self", peopleHandler);
  }

  public void testActivitiesUriRecognition() throws Exception {
    verifyHandlerWasFoundForPathInfo('/'
        + DataServiceServlet.ACTIVITY_ROUTE + "/5/@self", activityHandler);
  }

  public void testAppDataUriRecognition() throws Exception {
    verifyHandlerWasFoundForPathInfo('/'
        + DataServiceServlet.APPDATA_ROUTE + "/5/@self", appDataHandler);
  }

  public void testMethodOverride() throws Exception {
    String route = '/' + DataServiceServlet.APPDATA_ROUTE;
    verifyHandlerWasFoundForPathInfo(route, appDataHandler, "POST", "GET", "GET");
    verifyHandlerWasFoundForPathInfo(route, appDataHandler, "POST", "", "POST");
    verifyHandlerWasFoundForPathInfo(route, appDataHandler, "POST", null, "POST");
    verifyHandlerWasFoundForPathInfo(route, appDataHandler, "POST", "POST", "POST");
    verifyHandlerWasFoundForPathInfo(route, appDataHandler, "GET", null, "GET");
    verifyHandlerWasFoundForPathInfo(route, appDataHandler, "DELETE", null, "DELETE");
    verifyHandlerWasFoundForPathInfo(route, appDataHandler, "PUT", null, "PUT");
  }

  /**
   * Tests a data handler that returns a failed Future
   */
  public void testFailedRequest() throws Exception {
    String route = '/' + DataServiceServlet.APPDATA_ROUTE;
    setupRequest(route, "GET", null);

    EasyMock.expect(appDataHandler.handleItem(EasyMock.isA(RestfulRequestItem.class)));
    EasyMock.expectLastCall().andReturn(
        ImmediateFuture.errorInstance(new RuntimeException("FAILED")));

    res.sendError(500, "FAILED");
    res.setCharacterEncoding("UTF-8");
    res.setContentType("application/json");

    EasyMock.replay(req, res, appDataHandler, jsonConverter);
    servlet.service(req, res);
    EasyMock.verify(req, res, appDataHandler, jsonConverter);
    EasyMock.reset(req, res, appDataHandler, jsonConverter);
  }

  private void verifyHandlerWasFoundForPathInfo(String peoplePathInfo, DataRequestHandler handler)
      throws Exception {
    String post = "POST";
    verifyHandlerWasFoundForPathInfo(peoplePathInfo, handler, post, post, post);
  }

  private void verifyHandlerWasFoundForPathInfo(String pathInfo, DataRequestHandler handler,
      String actualMethod, String overrideMethod, String expectedMethod) throws Exception {
    setupRequest(pathInfo, actualMethod, overrideMethod);

    String jsonObject = "my lovely json";

    EasyMock.expect(handler.handleItem(EasyMock.isA(RequestItem.class)));
    EasyMock.expectLastCall().andReturn(ImmediateFuture.newInstance(jsonObject));

    EasyMock.expect(jsonConverter.convertToString(ImmutableMap.of("entry", jsonObject)))
        .andReturn("{ 'entry' : " + jsonObject + " }");

    PrintWriter writerMock = EasyMock.createMock(PrintWriter.class);
    EasyMock.expect(res.getWriter()).andReturn(writerMock);
    writerMock.write(jsonObject);
    res.setCharacterEncoding("UTF-8");
    res.setContentType("application/json");

    EasyMock.replay(req, res, handler, jsonConverter);
    servlet.service(req, res);
    EasyMock.verify(req, res, handler, jsonConverter);
    EasyMock.reset(req, res, handler, jsonConverter);
    // ick, this resets for the next call...
    EasyMock.expect(jsonConverter.getContentType()).andReturn("application/json");
  }

  private void setupRequest(String pathInfo, String actualMethod, String overrideMethod)
      throws IOException {
    EasyMock.expect(req.getCharacterEncoding()).andStubReturn("UTF-8");
    if (!("GET").equals(overrideMethod) && !("HEAD").equals(overrideMethod)) {
      EasyMock.expect(req.getInputStream()).andStubReturn(dummyPostData);
    }
    EasyMock.expect(req.getPathInfo()).andStubReturn(pathInfo);
    EasyMock.expect(req.getMethod()).andStubReturn(actualMethod);
    EasyMock.expect(req.getParameterNames()).andStubReturn(new StringTokenizer(""));
    EasyMock.expect(req.getParameter(RestfulRequestItem.X_HTTP_METHOD_OVERRIDE)).andReturn(
        overrideMethod).times(2);
    EasyMock.expect(req.getParameter(DataServiceServlet.FORMAT_PARAM)).andReturn(null);

    EasyMock.expect(req.getAttribute(EasyMock.isA(String.class))).andReturn(FAKE_GADGET_TOKEN);
  }

  public void testInvalidRoute() throws Exception {
    RestfulRequestItem requestItem = new RestfulRequestItem("/ahhh!", "GET", null,
        FAKE_GADGET_TOKEN, jsonConverter);
    try {
      servlet.handleRequestItem(requestItem, new FakeHttpServletRequest()).get();
      fail();
    } catch (ExecutionException ee) {
      assertTrue(ee.getCause() instanceof SocialSpiException);
      assertEquals(ResponseError.NOT_IMPLEMENTED, ((SocialSpiException) ee.getCause()).getError());
    }
  }

  public void testGetConverterForRequest() throws Exception {

    Injector injector = Guice.createInjector(new SocialApiTestsGuiceModule());
    BeanJsonConverter json = new BeanJsonConverter(injector);
    BeanXStreamConverter xml = new BeanXStreamConverter(new XStream081Configuration(injector));
    BeanXStreamAtomConverter atom = new BeanXStreamAtomConverter(new XStream081Configuration(injector));
    servlet.setBeanConverters(json, xml, atom);

    assertConverter(atom, "atom");
    assertConverter(xml, "xml");
    assertConverter(json, "");
    assertConverter(json, null);
    assertConverter(json, "ahhhh!");
  }

  public void testGetConverterForRequestContentType() throws Exception {
    Injector injector = Guice.createInjector(new SocialApiTestsGuiceModule());
    BeanJsonConverter json = new BeanJsonConverter(injector);
    BeanXStreamConverter xml = new BeanXStreamConverter(new XStream081Configuration(injector));
    BeanXStreamAtomConverter atom = new BeanXStreamAtomConverter(new XStream081Configuration(injector));
    servlet.setBeanConverters(json, xml, atom);

    assertConverterForContentType(atom, "application/atom+xml");
    assertConverterForContentType(xml, "application/xml");
    assertConverterForContentType(json, "");
    assertConverterForContentType(json, null);
    assertConverterForContentType(json, "abcd!");

  }

  private void assertConverter(BeanConverter converter, String format) {
    EasyMock.expect(req.getParameter(DataServiceServlet.FORMAT_PARAM))
        .andReturn(format);
    EasyMock.replay(req);
    assertEquals(converter, servlet.getConverterForRequest(req));
    EasyMock.verify(req);
    EasyMock.reset(req);
  }

  private void assertConverterForContentType(BeanConverter converter,
      String contentType) {
    EasyMock.expect(req.getHeader(DataServiceServlet.CONTENT_TYPE)).andReturn(
        contentType);
    EasyMock.replay(req);
    assertEquals(converter, servlet.getConverterForRequest(req));
    EasyMock.verify(req);
    EasyMock.reset(req);
  }
}
