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
package org.apache.shindig.social.opensocial.spi;

import junit.framework.TestCase;

import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.opensocial.service.DataRequestHandler;
import org.apache.shindig.social.opensocial.service.RequestItem;
import org.apache.shindig.social.opensocial.service.RestfulRequestItem;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class DataRequestHandlerTest extends TestCase {

  private DataRequestHandler drh;

  @Override
  protected void setUp() throws Exception {
    drh = new DataRequestHandler() {
      @Override
      protected Future<?> handleDelete(RequestItem request) {
        return ImmediateFuture.newInstance("DELETE");
      }

      @Override
      protected Future<?> handlePut(RequestItem request) {
        return ImmediateFuture.newInstance("PUT");
      }

      @Override
      protected Future<?> handlePost(RequestItem request) {
        return ImmediateFuture.newInstance("POST");
      }

      @Override
      protected Future<?> handleGet(RequestItem request) {
        return ImmediateFuture.newInstance("GET");
      }
    };
  }

  public void testHandleItemSuccess() throws Exception {
    verifyItemDispatchMethodCalled("DELETE");
    verifyItemDispatchMethodCalled("PUT");
    verifyItemDispatchMethodCalled("POST");
    verifyItemDispatchMethodCalled("GET");
  }

  private void verifyItemDispatchMethodCalled(String methodName) throws Exception {
    RestfulRequestItem request = new RestfulRequestItem(null, methodName, null, null);
    assertEquals(methodName, drh.handleItem(request).get());
  }

  public void testHandleMethodWithInvalidMethod() throws Exception {
    verifyExceptionThrown(null);
    verifyExceptionThrown("  ");
    verifyExceptionThrown("HEAD");
  }

  private void verifyExceptionThrown(String methodName) throws Exception {
    RestfulRequestItem request = new RestfulRequestItem(null, methodName, null, null);
    Future<?> err = drh.handleItem(request);
    try {
      err.get();
    } catch (ExecutionException ee) {
      assertTrue(ee.getCause() instanceof SocialSpiException);
      SocialSpiException spe = (SocialSpiException) ee.getCause();
      assertEquals(ResponseError.NOT_IMPLEMENTED, spe.getError());
    }
  }
}
