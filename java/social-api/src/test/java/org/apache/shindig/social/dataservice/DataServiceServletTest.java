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
import org.apache.shindig.common.SecurityTokenException;
import org.apache.shindig.common.testing.FakeGadgetToken;

import com.google.common.collect.Maps;
import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class DataServiceServletTest extends TestCase {
  private HttpServletRequest req;
  private HttpServletResponse res;
  private DataServiceServlet servlet;
  private PersonHandler peopleHandler;
  private ActivityHandler activityHandler;
  private AppDataHandler appDataHandler;
  private BasicSecurityTokenDecoder tokenDecoder;

  protected void setUp() throws Exception {
    req = EasyMock.createMock(HttpServletRequest.class);
    res = EasyMock.createMock(HttpServletResponse.class);

    Map<String, DataRequestHandler> handlers = Maps.newHashMap();
    peopleHandler = EasyMock.createMock(PersonHandler.class);
    activityHandler = EasyMock.createMock(ActivityHandler.class);
    appDataHandler = EasyMock.createMock(AppDataHandler.class);

    handlers.put(DataServiceServlet.PEOPLE_ROUTE, peopleHandler);
    handlers.put(DataServiceServlet.ACTIVITY_ROUTE, activityHandler);
    handlers.put(DataServiceServlet.APPDATA_ROUTE, appDataHandler);
    servlet = new DataServiceServlet();
    servlet.setHandlers(handlers);

    tokenDecoder = EasyMock.createMock(BasicSecurityTokenDecoder.class);
    servlet.setSecurityTokenDecoder(tokenDecoder);
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

  private void verifyHandlerWasFoundForPathInfo(String peoplePathInfo, DataRequestHandler handler)
      throws Exception {
    String post = "POST";
    verifyHandlerWasFoundForPathInfo(peoplePathInfo, handler, post, post, post);
  }

  private void verifyHandlerWasFoundForPathInfo(String pathInfo, DataRequestHandler handler,
      String actualMethod, String overrideMethod, String expectedMethod) throws Exception {
    req.setCharacterEncoding("UTF-8");

    EasyMock.expect(req.getPathInfo()).andReturn(pathInfo);
    EasyMock.expect(req.getMethod()).andReturn(actualMethod);
    EasyMock.expect(req.getMethod()).andReturn(actualMethod);
    EasyMock.expect(req.getParameter(DataServiceServlet.X_HTTP_METHOD_OVERRIDE)).andReturn(
        overrideMethod);

    String tokenString = "owner:viewer:app:container.com:foo:bar";
    FakeGadgetToken token = new FakeGadgetToken();
    EasyMock.expect(req.getParameter(DataServiceServlet.SECURITY_TOKEN_PARAM))
        .andReturn(tokenString);
    EasyMock.expect(tokenDecoder.createToken(tokenString)).andReturn(token);

    handler.handleMethod(expectedMethod, req, res, token);

    EasyMock.replay(req, res, handler, tokenDecoder);
    servlet.service(req, res);
    EasyMock.verify(req, res, handler, tokenDecoder);
    EasyMock.reset(req, res, handler, tokenDecoder);
  }

  public void testInvalidRoute() throws Exception {
    req.setCharacterEncoding("UTF-8");
    EasyMock.expect(req.getPathInfo()).andReturn("/ahhh!");

    EasyMock.replay(req);
    try {
      servlet.doPost(req, res);
      fail("The route should not have found a valid handler.");
    } catch (RuntimeException e) {
      // Yea!
      assertEquals("No handler for route: ahhh!", e.getMessage());
    }
    EasyMock.verify(req);
  }

  public void testSecurityTokenException() throws Exception {
    req.setCharacterEncoding("UTF-8");
    EasyMock.expect(req.getPathInfo()).andReturn("/" + DataServiceServlet.APPDATA_ROUTE);
    EasyMock.expect(req.getMethod()).andReturn("POST");
    EasyMock.expect(req.getParameter(DataServiceServlet.X_HTTP_METHOD_OVERRIDE)).andReturn("POST");

    String tokenString = "owner:viewer:app:container.com:foo:bar";
    EasyMock.expect(req.getParameter(DataServiceServlet.SECURITY_TOKEN_PARAM))
        .andReturn(tokenString);
    EasyMock.expect(tokenDecoder.createToken(tokenString)).andThrow(new SecurityTokenException(""));

    EasyMock.replay(req, tokenDecoder);
    try {
      servlet.doPost(req, res);
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

}
