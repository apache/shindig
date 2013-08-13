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
package org.apache.shindig.protocol;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;
import org.apache.shindig.auth.AuthInfoUtil;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.testing.FakeHttpServletRequest;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.protocol.conversion.BeanConverter;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.easymock.IMocksControl;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class DataServiceServletTest extends Assert {

  private static final FakeGadgetToken FAKE_GADGET_TOKEN = new FakeGadgetToken()
      .setOwnerId("john.doe").setViewerId("john.doe");

  private HttpServletRequest req;
  private HttpServletResponse res;
  private DataServiceServlet servlet;
  private BeanJsonConverter jsonConverter;
  private BeanConverter xmlConverter;
  private BeanConverter atomConverter;
  private ContainerConfig containerConfig;

  private IMocksControl mockControl = EasyMock.createNiceControl();

  @Before
  public void setUp() throws Exception {
    servlet = new DataServiceServlet();
    req = mockControl.createMock(HttpServletRequest.class);
    res = mockControl.createMock(HttpServletResponse.class);
    jsonConverter = mockControl.createMock(BeanJsonConverter.class);
    xmlConverter = mockControl.createMock(BeanConverter.class);
    atomConverter = mockControl.createMock(BeanConverter.class);
    containerConfig = mockControl.createMock(ContainerConfig.class);

    EasyMock.expect(jsonConverter.getContentType()).andReturn(
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE).anyTimes();
    EasyMock.expect(xmlConverter.getContentType()).andReturn(
        ContentTypes.OUTPUT_XML_CONTENT_TYPE).anyTimes();
    EasyMock.expect(atomConverter.getContentType()).andReturn(
        ContentTypes.OUTPUT_ATOM_CONTENT_TYPE).anyTimes();

    HandlerRegistry registry = new DefaultHandlerRegistry(null, jsonConverter,
        new HandlerExecutionListener.NoOpHandler());
    registry.addHandlers(Sets.<Object>newHashSet(new TestHandler()));

    servlet.setHandlerRegistry(registry);
    servlet.setContainerConfig(containerConfig);
    servlet.setJSONPAllowed(true);

    servlet.setBeanConverters(jsonConverter, xmlConverter, atomConverter);
  }

  @Test
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

    String method = Strings.isNullOrEmpty(overrideMethod) ? actualMethod : overrideMethod;

    EasyMock.expect(jsonConverter.convertToString(
        ImmutableMap.of("entry", TestHandler.REST_RESULTS.get(method))))
        .andReturn("{ 'entry' : " + TestHandler.REST_RESULTS.get(method) + " }");

    PrintWriter writerMock = EasyMock.createMock(PrintWriter.class);
    EasyMock.expect(res.getWriter()).andReturn(writerMock);
    writerMock.write(TestHandler.GET_RESPONSE);
    EasyMock.expectLastCall();
    res.setCharacterEncoding("UTF-8");
    res.setContentType(ContentTypes.OUTPUT_JSON_CONTENT_TYPE);

    mockControl.replay();
    servlet.service(req, res);
    mockControl.verify();
    mockControl.reset();
  }

  @Test
  public void testDisallowJSONP() throws Exception {
    servlet.setJSONPAllowed(false);
    String route = "/test";
    verifyHandlerWasFoundForPathInfo(route, "POST", "GET");
    servlet.setJSONPAllowed(true);
  }

  @Test
  public void testOverridePostWithGet() throws Exception {
    String route = "/test";
    verifyHandlerWasFoundForPathInfo(route, "POST", "GET");
  }

  @Test
  public void  testOverrideGetWithPost() throws Exception {
    String route = "/test";
    verifyHandlerWasFoundForPathInfo(route, "GET", "POST");
  }

  /**
   * Tests a data handler that returns a failed Future
   */
  @Test
  public void testFailedRequest() throws Exception {
    String route = "/test";
    setupRequest(route, "DELETE", null);

    // Shouldnt these be expectations
    res.sendError(HttpServletResponse.SC_BAD_REQUEST, TestHandler.FAILURE_MESSAGE);
    res.setCharacterEncoding("UTF-8");
    res.setContentType(ContentTypes.OUTPUT_JSON_CONTENT_TYPE);

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
    fakeReq.setAttribute(AuthInfoUtil.Attribute.SECURITY_TOKEN.getId(), FAKE_GADGET_TOKEN);
    fakeReq.setContentType(ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    req = fakeReq;
  }

  @Test
  public void testGetConverterForFormat() throws Exception {
    assertConverterForFormat(atomConverter, "atom");
    assertConverterForFormat(xmlConverter, "xml");
    assertConverterForFormat(jsonConverter, "");
    assertConverterForFormat(jsonConverter, null);
    assertConverterForFormat(jsonConverter, "ahhhh!");
  }

  @Test
  public void testGetConverterForContentType() throws Exception {
    assertConverterForContentType(atomConverter, ContentTypes.OUTPUT_ATOM_CONTENT_TYPE);
    assertConverterForContentType(xmlConverter, ContentTypes.OUTPUT_XML_CONTENT_TYPE);
    assertConverterForContentType(xmlConverter, "text/xml");
    assertConverterForContentType(jsonConverter, ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    assertConverterForContentType(jsonConverter, "application/json");
    assertConverterForContentType(jsonConverter, "");
    assertConverterForContentType(jsonConverter, null);
    assertConverterForContentType(jsonConverter, "abcd!");
  }

  private void assertConverterForFormat(BeanConverter converter, String format) {
    assertEquals(converter, servlet.getConverterForFormat(format));
  }

  private void assertConverterForContentType(BeanConverter converter, String contentType) {
    assertEquals(converter, servlet.getConverterForContentType(contentType));
  }
}
