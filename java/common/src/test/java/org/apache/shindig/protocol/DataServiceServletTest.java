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
package org.apache.shindig.protocol;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.auth.AuthInfo;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.testing.FakeHttpServletRequest;
import org.apache.shindig.protocol.conversion.BeanConverter;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import junit.framework.TestCase;

import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DataServiceServletTest extends TestCase {

  private static final FakeGadgetToken FAKE_GADGET_TOKEN = new FakeGadgetToken()
      .setOwnerId("john.doe").setViewerId("john.doe");

  private HttpServletRequest req;
  private HttpServletResponse res;
  private DataServiceServlet servlet;
  private BeanJsonConverter jsonConverter;
  private BeanConverter xmlConverter;
  private BeanConverter atomConverter;


  private IMocksControl mockControl = EasyMock.createNiceControl();

  private final ServletInputStream dummyPostData = new ServletInputStream() {
    @Override public int read()  {
      return -1;
    }
  };

  @Override protected void setUp() throws Exception {
    servlet = new DataServiceServlet();
    req = mockControl.createMock(HttpServletRequest.class);
    res = mockControl.createMock(HttpServletResponse.class);
    jsonConverter = mockControl.createMock(BeanJsonConverter.class);
    xmlConverter = mockControl.createMock(BeanConverter.class);
    atomConverter = mockControl.createMock(BeanConverter.class);

    EasyMock.expect(jsonConverter.getContentType()).andReturn("application/json").anyTimes();
    EasyMock.expect(xmlConverter.getContentType()).andReturn("application/xml").anyTimes();
    EasyMock.expect(atomConverter.getContentType()).andReturn("application/atom+xml").anyTimes();

    HandlerRegistry registry = new DefaultHandlerRegistry(null,
        Sets.newHashSet(new TestHandler()), jsonConverter);

    servlet.setHandlerRegistry(registry);

    servlet.setBeanConverters(jsonConverter, xmlConverter, atomConverter);
  }

  public void testUriRecognition() throws Exception {
    verifyHandlerWasFoundForPathInfo("/test/5/@self");
  }

  private void verifyHandlerWasFoundForPathInfo(String peoplePathInfo)
      throws Exception {
    String post = "POST";
    verifyHandlerWasFoundForPathInfo(peoplePathInfo, post, post);
  }

  private void verifyHandlerWasFoundForPathInfo(String pathInfo,
    String actualMethod, String overrideMethod) throws Exception {
    setupRequest(pathInfo, actualMethod, overrideMethod);

    String method = StringUtils.isEmpty(overrideMethod) ? actualMethod : overrideMethod;

    EasyMock.expect(jsonConverter.convertToString(
        ImmutableMap.of("entry", TestHandler.REST_RESULTS.get(method))))
        .andReturn("{ 'entry' : " + TestHandler.REST_RESULTS.get(method) + " }");

    PrintWriter writerMock = EasyMock.createMock(PrintWriter.class);
    EasyMock.expect(res.getWriter()).andReturn(writerMock);
    writerMock.write(TestHandler.GET_RESPONSE);
    EasyMock.expectLastCall();
    res.setCharacterEncoding("UTF-8");
    res.setContentType("application/json");

    mockControl.replay();
    servlet.service(req, res);
    mockControl.verify();
    mockControl.reset();
  }

  public void testOverridePostWithGet() throws Exception {
    String route = "/test";
    verifyHandlerWasFoundForPathInfo(route, "POST", "GET");
  }

  public void  testOverrideGetWithPost() throws Exception {
    String route = "/test";
    verifyHandlerWasFoundForPathInfo(route, "GET", "POST");
  }

  /**
   * Tests a data handler that returns a failed Future
   */
  public void testFailedRequest() throws Exception {
    String route = "/test";
    setupRequest(route, "DELETE", null);

    // Shouldnt these be expectations
    res.sendError(ResponseError.BAD_REQUEST.getHttpErrorCode(), TestHandler.FAILURE_MESSAGE);
    res.setCharacterEncoding("UTF-8");
    res.setContentType("application/json");

    mockControl.replay();
    servlet.service(req, res);
    mockControl.verify();
    mockControl.reset();
  }

  private void setupRequest(String pathInfo, String actualMethod, String overrideMethod)
      throws IOException {
    FakeHttpServletRequest fakeReq = new FakeHttpServletRequest("/social/rest", pathInfo, "");
    fakeReq.setPathInfo(pathInfo);
    fakeReq.setParameter(DataServiceServlet.X_HTTP_METHOD_OVERRIDE, overrideMethod);
    fakeReq.setCharacterEncoding("UTF-8");
    if (!("GET").equals(actualMethod) && !("HEAD").equals(actualMethod)) {
      fakeReq.setPostData("", "UTF-8");
    }
    fakeReq.setMethod(actualMethod);
    fakeReq.setAttribute(AuthInfo.Attribute.SECURITY_TOKEN.getId(), FAKE_GADGET_TOKEN);
    req = fakeReq;
  }

  public void testGetConverterForRequest() throws Exception {
    assertConverter(atomConverter, "atom");
    assertConverter(xmlConverter, "xml");
    assertConverter(jsonConverter, "");
    assertConverter(jsonConverter, null);
    assertConverter(jsonConverter, "ahhhh!");
  }

  public void testGetConverterForRequestContentType() throws Exception {
    assertConverterForContentType(atomConverter, "application/atom+xml");
    assertConverterForContentType(xmlConverter, "application/xml");
    assertConverterForContentType(jsonConverter, "");
    assertConverterForContentType(jsonConverter, null);
    assertConverterForContentType(jsonConverter, "abcd!");
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
