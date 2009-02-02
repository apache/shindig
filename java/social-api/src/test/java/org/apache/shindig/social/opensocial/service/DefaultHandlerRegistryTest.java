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
package org.apache.shindig.social.opensocial.service;

import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import junit.framework.TestCase;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Tests BasicHandleRregistry
 */
public class DefaultHandlerRegistryTest extends TestCase {

  private DefaultHandlerRegistry registry;

  @Override
  @SuppressWarnings("unchecked")
  protected void setUp() throws Exception {
    super.setUp();
    registry = new DefaultHandlerRegistry(null,
        Lists.newArrayList(new TestHandler()));
  }

  public void testGetHandlerRPC() throws Exception {
    assertNotNull(registry.getRpcHandler(new JSONObject("{method : test.get}")));
  }

  public void testGetHandlerRest() throws Exception {
    assertNotNull(registry.getRestHandler("/test/", "GET"));
  }

  public void testOverrideHandlerRPC() throws Exception {
    assertNotNull(registry.getRpcHandler(new JSONObject("{method : test.overidden}")));
  }

  public void testOverrideHandlerRest() throws Exception {
    assertNotNull(registry.getRestHandler("/test/overidden/method/", "GET"));
  }

  public void testGetForAliasHandler() {
    assertNotNull(registry.getRestHandler("/test", "GET"));
  }

  public void testRpcHandler_serviceDoesntExist() throws Exception {
    JSONObject rpc = new JSONObject("{method : makebelieve.get}");
    RpcHandler rpcHandler = registry.getRpcHandler(rpc);
    try {
      Future future = rpcHandler.execute(rpc, null, null);
      future.get();
      fail("Expect exception for missing method");
    } catch (ExecutionException t) {
      assertEquals(t.getCause().getClass(), SocialSpiException.class);
      assertEquals(((SocialSpiException) t.getCause()).getError(), ResponseError.NOT_IMPLEMENTED);
    } catch (Throwable t) {
      fail("Unexpected exception " + t.toString());
    }
  }

  public void testRestHandler_serviceDoesntExist() {
    RestHandler restHandler = registry.getRestHandler("/makebelieve", "GET");
    try {
      Future future = restHandler.execute("/makebelieve", Maps.<String, String[]>newHashMap(),
          null, null, null);
      future.get();
      fail("Expect exception for missing method");
    } catch (ExecutionException t) {
      assertEquals(t.getCause().getClass(), SocialSpiException.class);
      assertEquals(((SocialSpiException) t.getCause()).getError(), ResponseError.NOT_IMPLEMENTED);
    } catch (Throwable t) {
      fail("Unexpected exception " + t.toString());
    }
  }

  public void testNonFutureDispatch() throws Exception {
    // Test calling a handler method which does not return a future
    RestHandler handler = registry.getRestHandler("/test", "GET");
    Future future = handler.execute("/test", Maps.<String, String[]>newHashMap(), null, null, null);
    assertEquals(future.get(), TestHandler.GET_RESPONSE);
  }

  public void testFutureDispatch() throws Exception {
    // Test calling a handler method which does not return a future
    RestHandler handler = registry.getRestHandler("/test", "POST");
    Future future = handler.execute("/test", Maps.<String, String[]>newHashMap(), null, null, null);
    assertEquals(future.get(), TestHandler.CREATE_RESPONSE);
  }

  public void testNonFutureException() throws Exception {
    // Test calling a handler method which does not return a future
    JSONObject rpc = new JSONObject("{ method : test.exception }");
    RpcHandler handler = registry.getRpcHandler(rpc);
    Future future = handler.execute(rpc, null, null);
    try {
      future.get();
      fail("Service method did not produce NullPointerException from Future");
    } catch (ExecutionException ee) {
      assertEquals(ee.getCause().getClass(), NullPointerException.class);
    }
  }

  public void testFutureException() throws Exception {
    // Test calling a handler method which does not return a future
    JSONObject rpc = new JSONObject("{ method : test.futureException }");
    RpcHandler handler = registry.getRpcHandler(rpc);
    Future future = handler.execute(rpc, null, null);
    try {
      future.get();
      fail("Service method did not produce ExecutionException from Future");
    } catch (ExecutionException ee) {
      assertEquals(ee.getCause().getClass(), SocialSpiException.class);
    }
  }

  public void testSupportedRpcServices() throws Exception {
    assertEquals(registry.getSupportedRpcServices(),
        Sets.newHashSet("test.create", "test.get", "test.overridden", "test.exception",
            "test.futureException"));
  }

  public void testSupportedRestServices() throws Exception {
    assertEquals(registry.getSupportedRestServices(),
        Sets.newHashSet("GET /test", "PUT /test", "DELETE /test", "POST /test",
            "GET /test/overridden/method"));
  }

  public void testAddNonService() {
    try {
      registry.addHandlers(new Object());
      fail("Adding an invalid service object succeded");
    } catch (IllegalStateException ise) {

    }
  }
}
