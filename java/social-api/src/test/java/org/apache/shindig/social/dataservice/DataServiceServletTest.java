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

import com.google.common.collect.Maps;
import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

public class DataServiceServletTest extends TestCase {
  private HttpServletRequest req;
  private HttpServletResponse res;
  private DataServiceServlet servlet ;
  private PersonHandler peopleHandler;
  private ActivityHandler activityHandler;
  private AppDataHandler appDataHandler;

  protected void setUp() throws Exception {
    req = EasyMock.createMock(HttpServletRequest.class);
    res = EasyMock.createMock(HttpServletResponse.class);

    HashMap<String, DataRequestHandler> handlers = Maps.newHashMap();
    peopleHandler = EasyMock.createMock(PersonHandler.class);
    activityHandler = EasyMock.createMock(ActivityHandler.class);
    appDataHandler = EasyMock.createMock(AppDataHandler.class);

    handlers.put(DataServiceServlet.PEOPLE_ROUTE, peopleHandler);
    handlers.put(DataServiceServlet.ACTIVITY_ROUTE, activityHandler);
    handlers.put(DataServiceServlet.APPDATA_ROUTE, appDataHandler);
    servlet = new DataServiceServlet();
    servlet.setHandlers(handlers);
    BasicSecurityTokenDecoder tokenDecoder
        = EasyMock.createMock(BasicSecurityTokenDecoder.class);

    servlet.setSecurityTokenDecoder(tokenDecoder);

    EasyMock.expect(req.getMethod()).andReturn("POST");
    req.setCharacterEncoding("UTF-8");
  }


  public void testPeopleUriRecognition() throws Exception {
    verifyHandlerWasFoundForPathInfo("/"
        + DataServiceServlet.PEOPLE_ROUTE+"/5/@self", peopleHandler);
  }

  public void testActivitiesUriRecognition() throws Exception {
    verifyHandlerWasFoundForPathInfo("/"
        + DataServiceServlet.ACTIVITY_ROUTE +"/5/@self", activityHandler);
  }

  public void testAppDataUriRecognition() throws Exception {
    verifyHandlerWasFoundForPathInfo("/"
        + DataServiceServlet.APPDATA_ROUTE+"/5/@self", appDataHandler);
  }

  public void testMethodOverride() throws Exception {
    EasyMock.expect(req.getHeader("X-HTTP-Method-Override")).andReturn("GET");
  }

  private void verifyHandlerWasFoundForPathInfo(String peoplePathInfo,
      DataRequestHandler handler) throws ServletException, IOException,
      SecurityTokenException {
    EasyMock.expect(req.getPathInfo()).andReturn(peoplePathInfo);
    EasyMock.expect(req.getMethod()).andReturn("POST");
    EasyMock.expect(req.getParameter(
        DataServiceServlet.X_HTTP_METHOD_OVERRIDE)).andReturn("POST");
    String tokenStr = "owner:viewer:app:container.com:foo:bar";
    EasyMock.expect(req.getParameter(
        DataServiceServlet.SECURITY_TOKEN_PARAM)).andReturn(tokenStr);
    handler.handleMethod("POST", req, res, null);
    EasyMock.replay(req, res, handler);
    servlet.service(req, res);
    EasyMock.verify(req, res, handler);
  }

}
