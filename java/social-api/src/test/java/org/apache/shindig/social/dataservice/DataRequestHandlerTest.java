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

import org.apache.shindig.social.ResponseItem;

import junit.framework.TestCase;

public class DataRequestHandlerTest extends TestCase {
  private DataRequestHandler drh;
  private RequestItem request;

  @Override
  protected void setUp() throws Exception {
    drh = new DataRequestHandler() {
      protected ResponseItem handleDelete(RequestItem request) {
        return new ResponseItem<String>("DELETE");
      }

      protected ResponseItem handlePut(RequestItem request) {
        return new ResponseItem<String>("PUT");
      }

      protected ResponseItem handlePost(RequestItem request) {
        return new ResponseItem<String>("POST");
      }

      protected ResponseItem handleGet(RequestItem request) {
        return new ResponseItem<String>("GET");
      }
    };

    request = new RequestItem();
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
}