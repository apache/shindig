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

import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.social.ResponseItem;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import junit.framework.TestCase;

import java.util.Map;

public class DataRequestHandlerTest extends TestCase {
  private DataRequestHandler drh;
  private RequestItem request;

  @Override
  protected void setUp() throws Exception {
    drh = new DataRequestHandler() {
      ResponseItem handleDelete(RequestItem request) {
        return new ResponseItem<String>("DELETE");
      }

      ResponseItem handlePut(RequestItem request) {
        return new ResponseItem<String>("PUT");
      }

      ResponseItem handlePost(RequestItem request) {
        return new ResponseItem<String>("POST");
      }

      ResponseItem handleGet(RequestItem request) {
        return new ResponseItem<String>("GET");
      }
    };

    request = new RequestItem(null, null, null, null);
  }

  public void testHandleMethodSuccess() throws Exception {
    verifyDispatchMethodCalled("DELETE");
    verifyDispatchMethodCalled("PUT");
    verifyDispatchMethodCalled("POST");
    verifyDispatchMethodCalled("GET");
  }

  private void verifyDispatchMethodCalled(String methodName) throws Exception {
    request.setMethod(methodName);
    assertEquals(methodName, drh.handleMethod(request).getResponse());
  }

  public void testHandleMethodWithInvalidMethod() throws Exception {
    verifyExceptionThrown(null);
    verifyExceptionThrown("  ");
    verifyExceptionThrown("HEAD");
  }

  private void verifyExceptionThrown(String methodName) throws Exception {
    request.setMethod(methodName);
    try {
      drh.handleMethod(request);
      fail("The invalid method " + methodName + " should throw an exception.");
    } catch (IllegalArgumentException e) {
      // Yea! We like exeptions
      assertEquals("Unserviced Http method type", e.getMessage());
    }
  }

  public void testGetParamsFromRequest() throws Exception {
    String[] params = DataRequestHandler.getParamsFromRequest(
        new RequestItem("/people/5/@self", null, null, null));
    assertEquals("5", params[0]);
    assertEquals("@self", params[1]);
  }

  public void testGetQueryPath() throws Exception {
    assertEquals("5/@self", DataRequestHandler.getQueryPath(
        new RequestItem("/people/5/@self", null, null, null)));
  }

  public void testGetEnumParam() throws Exception {
    Map<String, String> parameters = Maps.newHashMap();
    parameters.put("field", "name");

    assertEquals(PersonService.SortOrder.name, DataRequestHandler.getEnumParam(
        new RequestItem(null, parameters, null, null), "field",
        PersonService.SortOrder.topFriends, PersonService.SortOrder.class));

    // Should return the default value if the parameter is null
    parameters = Maps.newHashMap();
    parameters.put("field", null);

    assertEquals(PersonService.SortOrder.topFriends, DataRequestHandler.getEnumParam(
        new RequestItem(null, parameters, null, null), "field",
        PersonService.SortOrder.topFriends, PersonService.SortOrder.class));
  }

  public void testGetIntegerParam() throws Exception {
    Map<String, String> parameters = Maps.newHashMap();
    parameters.put("field", "5");

    assertEquals(5, DataRequestHandler.getIntegerParam(
        new RequestItem(null, parameters, null, null), "field", 100));

    // Should return the default value if the parameter is null
    parameters = Maps.newHashMap();
    parameters.put("field", null);

    assertEquals(100, DataRequestHandler.getIntegerParam(
        new RequestItem(null, parameters, null, null), "field", 100));
  }

  public void testGetListParam() throws Exception {
    Map<String, String> parameters = Maps.newHashMap();
    parameters.put("field", "happy,sad,grumpy");

    assertEquals(Lists.newArrayList("happy", "sad", "grumpy"),
        DataRequestHandler.getListParam(
            new RequestItem(null, parameters, null, null),
            "field", Lists.newArrayList("alpha")));

    // Should return the default value if the parameter is null
    parameters = Maps.newHashMap();
    parameters.put("field", null);

    assertEquals(Lists.newArrayList("alpha"),
        DataRequestHandler.getListParam(
            new RequestItem(null, parameters, null, null),
            "field", Lists.newArrayList("alpha")));
  }

  public void testGetAppId() throws Exception {
    FakeGadgetToken token = new FakeGadgetToken();
    assertEquals(token.getAppId(), DataRequestHandler.getAppId(
        DataRequestHandler.APP_SUBSTITUTION_TOKEN, token));

    assertEquals("676", DataRequestHandler.getAppId("676", token));
  }
}