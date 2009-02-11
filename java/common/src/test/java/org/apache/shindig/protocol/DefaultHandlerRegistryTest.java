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


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.json.JSONObject;
import static org.junit.Assert.assertArrayEquals;

import java.util.Iterator;
import java.util.TreeSet;
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
        Lists.newArrayList(new TestHandler()), null);
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

  public void testOverrideHandlerRPCName() throws Exception {
    assertNotNull(registry.getRpcHandler(new JSONObject("{method : test.override.rpcname}")));
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
      Future future = rpcHandler.execute(null, null);
      future.get();
      fail("Expect exception for missing method");
    } catch (ExecutionException t) {
      assertEquals(t.getCause().getClass(), ProtocolException.class);
      Assert.assertEquals(((ProtocolException) t.getCause()).getError(), ResponseError.NOT_IMPLEMENTED);
    } catch (Throwable t) {
      fail("Unexpected exception " + t.toString());
    }
  }

  public void testRestHandler_serviceDoesntExist() {
    RestHandler restHandler = registry.getRestHandler("/makebelieve", "GET");
    try {
      Future future = restHandler.execute(Maps.<String, String[]>newHashMap(), null, null, null);
      future.get();
      fail("Expect exception for missing method");
    } catch (ExecutionException t) {
      assertEquals(t.getCause().getClass(), ProtocolException.class);
      Assert.assertEquals(((ProtocolException) t.getCause()).getError(), ResponseError.NOT_IMPLEMENTED);
    } catch (Throwable t) {
      fail("Unexpected exception " + t.toString());
    }
  }

  public void testNonFutureDispatch() throws Exception {
    // Test calling a handler method which does not return a future
    RestHandler handler = registry.getRestHandler("/test", "GET");
    Future future = handler.execute(Maps.<String, String[]>newHashMap(), null, null, null);
    assertEquals(future.get(), TestHandler.GET_RESPONSE);
  }

  public void testFutureDispatch() throws Exception {
    // Test calling a handler method which does not return a future
    RestHandler handler = registry.getRestHandler("/test", "POST");
    Future future = handler.execute(Maps.<String, String[]>newHashMap(), null, null, null);
    assertEquals(future.get(), TestHandler.CREATE_RESPONSE);
  }

  public void testNonFutureException() throws Exception {
    // Test calling a handler method which does not return a future
    JSONObject rpc = new JSONObject("{ method : test.exception }");
    RpcHandler handler = registry.getRpcHandler(rpc);
    Future future = handler.execute(null, null);
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
    Future future = handler.execute(null, null);
    try {
      future.get();
      fail("Service method did not produce ExecutionException from Future");
    } catch (ExecutionException ee) {
      assertEquals(ee.getCause().getClass(), ProtocolException.class);
    }
  }

  public void testSupportedRpcServices() throws Exception {
    assertEquals(registry.getSupportedRpcServices(),
        Sets.newHashSet("test.create", "test.get", "test.overridden", "test.exception",
            "test.futureException", "test.override.rpcname"));
  }

  public void testSupportedRestServices() throws Exception {
    assertEquals(registry.getSupportedRestServices(),
        Sets.newHashSet("GET /test/{someParam}/{someOtherParam}",
            "PUT /test/{someParam}/{someOtherParam}",
            "DELETE /test/{someParam}/{someOtherParam}",
            "POST /test/{someParam}/{someOtherParam}",
            "GET /test/overridden/method"));
  }

  public void testAddNonService() {
    try {
      registry.addHandlers(new Object());
      fail("Adding an invalid service object succeded");
    } catch (IllegalStateException ise) {

    }
  }

  public void testRestPath() {
    DefaultHandlerRegistry.RestPath restPath =
        new DefaultHandlerRegistry.RestPath("/service/const1/{p1}/{p2}+/const2/{p3}", null);
    DefaultHandlerRegistry.RestInvocationWrapper wrapper =
        restPath.accept("service/const1/a/b,c/const2/d".split("/"));
    assertArrayEquals(wrapper.pathParams.get("p1"), new String[]{"a"});
    assertArrayEquals(wrapper.pathParams.get("p2"), new String[]{"b","c"});
    assertArrayEquals(wrapper.pathParams.get("p3"), new String[]{"d"});
    wrapper = restPath.accept("service/const1/a/b/const2".split("/"));
    assertArrayEquals(wrapper.pathParams.get("p1"), new String[]{"a"});
    assertArrayEquals(wrapper.pathParams.get("p2"), new String[]{"b"});
    assertNull(wrapper.pathParams.get("p3"));
    assertNull(restPath.accept("service/const1/{p1}/{p2}+".split("/")));
    assertNull(restPath.accept("service/constmiss/{p1}/{p2}+/const2".split("/")));
  }

  public void testRestPathOrdering() {
    DefaultHandlerRegistry.RestPath restPath1 =
        new DefaultHandlerRegistry.RestPath("/service/const1/{p1}/{p2}+/const2/{p3}", null);
    DefaultHandlerRegistry.RestPath restPath2 =
        new DefaultHandlerRegistry.RestPath("/service/{p1}/{p2}+/const2/{p3}", null);
    DefaultHandlerRegistry.RestPath restPath3 =
        new DefaultHandlerRegistry.RestPath("/service/const1/const2/{p1}/{p2}+/{p3}", null);
    TreeSet<DefaultHandlerRegistry.RestPath> sortedSet =
        Sets.newTreeSet(restPath1, restPath2, restPath3);
    Iterator<DefaultHandlerRegistry.RestPath> itr = sortedSet.iterator();
    assertEquals(itr.next(), restPath3);
    assertEquals(itr.next(), restPath1);
    assertEquals(itr.next(), restPath2);
  }
}
