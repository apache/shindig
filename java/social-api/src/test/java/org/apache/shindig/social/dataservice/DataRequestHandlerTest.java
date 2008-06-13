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

import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;
import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.testing.FakeGadgetToken;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;

import java.io.PrintWriter;
import java.io.IOException;

public class DataRequestHandlerTest extends TestCase {
  private DataRequestHandler drh;
  private HttpServletRequest req;
  private HttpServletResponse resp;
  private BeanJsonConverter converter;

  @Override
  protected void setUp() throws Exception {
    req = EasyMock.createMock(HttpServletRequest.class);
    resp = EasyMock.createMock(HttpServletResponse.class);
    converter = EasyMock.createMock(BeanJsonConverter.class);

    drh = new DataRequestHandler(converter) {
      ResponseItem handleDelete(HttpServletRequest servletRequest,
          SecurityToken token) {
        return new ResponseItem<String>("DELETE");
      }

      ResponseItem handlePut(HttpServletRequest servletRequest,
          SecurityToken token) {
        return new ResponseItem<String>("PUT");
      }

      ResponseItem handlePost(HttpServletRequest servletRequest,
          SecurityToken token) {
        return new ResponseItem<String>("POST");
      }

      ResponseItem handleGet(HttpServletRequest servletRequest,
          SecurityToken token) {
        return new ResponseItem<String>("GET");
      }
    };
  }

  public void testHandleMethodSuccess() throws Exception {
    verifyDispatchMethodCalled("DELETE", converter);
    verifyDispatchMethodCalled("PUT", converter);
    verifyDispatchMethodCalled("POST", converter);
    verifyDispatchMethodCalled("GET", converter);
  }

  private void verifyDispatchMethodCalled(String methodName, BeanJsonConverter converter)
      throws IOException {
    String jsonObject = "my lovely json";
    EasyMock.expect(converter.convertToJson(methodName)).andReturn(jsonObject);

    PrintWriter writerMock = EasyMock.createMock(PrintWriter.class);
    EasyMock.expect(resp.getWriter()).andReturn(writerMock);
    writerMock.write(jsonObject);

    EasyMock.replay(req, resp, converter, writerMock);
    drh.handleMethod(methodName, req, resp, null);
    EasyMock.verify(req, resp, converter, writerMock);
    EasyMock.reset(req, resp, converter, writerMock);
  }

  public void testHandleMethodWithInvalidMethod() throws Exception {
    verifyExceptionThrown(null);
    verifyExceptionThrown("  ");
    verifyExceptionThrown("HEAD");
  }

  private void verifyExceptionThrown(String methodName) throws IOException {
    try {
      drh.handleMethod(methodName, req, resp, null);
      fail("The invalid method " + methodName + " should throw an exception.");
    } catch (IllegalArgumentException e) {
      // Yea! We like exeptions
      assertEquals("Unserviced Http method type", e.getMessage());
    }
  }

  public void testHandleMethodFailure() throws Exception {
    drh = new DataRequestHandler(null) {
      ResponseItem handleDelete(HttpServletRequest servletRequest,
          SecurityToken token) {
        return null;
      }

      ResponseItem handlePut(HttpServletRequest servletRequest,
          SecurityToken token) {
        return null;
      }

      ResponseItem handlePost(HttpServletRequest servletRequest,
          SecurityToken token) {
        return null;
      }

      ResponseItem handleGet(HttpServletRequest servletRequest,
          SecurityToken token) {
        return new ResponseItem<String>(ResponseError.INTERNAL_ERROR, "", "");
      }
    };

    EasyMock.replay(req, resp);
    drh.handleMethod("GET", req, resp, null);
    // TODO: Assert the right actions occur based on the error type
    EasyMock.verify(req, resp);
  }

  public void testGetParamsFromRequest() throws Exception {
    EasyMock.expect(req.getPathInfo()).andReturn("/people/5/@self");

    EasyMock.replay(req);
    String[] params = DataRequestHandler.getParamsFromRequest(req);
    assertEquals("5", params[0]);
    assertEquals("@self", params[1]);
    EasyMock.verify(req);
  }

  public void testGetQueryPath() throws Exception {
    EasyMock.expect(req.getPathInfo()).andReturn("/people/5/@self");

    EasyMock.replay(req);
    assertEquals("5/@self", DataRequestHandler.getQueryPath(req));
    EasyMock.verify(req);
  }

  public void testGetEnumParam() throws Exception {
    EasyMock.expect(req.getParameter("field")).andReturn("name");

    EasyMock.replay(req);
    assertEquals(PersonService.SortOrder.name, DataRequestHandler.getEnumParam(req, "field",
        PersonService.SortOrder.topFriends, PersonService.SortOrder.class));
    EasyMock.verify(req);

    EasyMock.reset(req);

    // Should return the default value if the parameter is null
    EasyMock.expect(req.getParameter("field")).andReturn(null);

    EasyMock.replay(req);
    assertEquals(PersonService.SortOrder.topFriends, DataRequestHandler.getEnumParam(req, "field",
        PersonService.SortOrder.topFriends, PersonService.SortOrder.class));
    EasyMock.verify(req);
  }

  public void testGetIntegerParam() throws Exception {
    EasyMock.expect(req.getParameter("field")).andReturn("5");

    EasyMock.replay(req);
    assertEquals(5, DataRequestHandler.getIntegerParam(req, "field", 100));
    EasyMock.verify(req);

    EasyMock.reset(req);

    // Should return the default value if the parameter is null
    EasyMock.expect(req.getParameter("field")).andReturn(null);

    EasyMock.replay(req);
    assertEquals(100, DataRequestHandler.getIntegerParam(req, "field", 100));
    EasyMock.verify(req);
  }

  public void testGetListParam() throws Exception {
    EasyMock.expect(req.getParameter("field")).andReturn("happy,sad,grumpy");

    EasyMock.replay(req);
    assertEquals(Lists.newArrayList("happy", "sad", "grumpy"),
        DataRequestHandler.getListParam(req, "field", Lists.newArrayList("alpha")));
    EasyMock.verify(req);

    EasyMock.reset(req);

    // Should return the default value if the parameter is null
    EasyMock.expect(req.getParameter("field")).andReturn(null);

    EasyMock.replay(req);
    assertEquals(Lists.newArrayList("alpha"),
        DataRequestHandler.getListParam(req, "field", Lists.newArrayList("alpha")));
    EasyMock.verify(req);
  }

  public void testGetAppId() throws Exception {
    FakeGadgetToken token = new FakeGadgetToken();
    assertEquals(token.getAppId(), DataRequestHandler.getAppId(
        DataRequestHandler.APP_SUBSTITUTION_TOKEN, token));

    assertEquals("676", DataRequestHandler.getAppId("676", token));
  }
}