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


import org.apache.shindig.protocol.conversion.BeanJsonConverter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableSortedSet;
import com.google.inject.Guice;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

/**
 * Tests BasicHandleRregistry
 */
public class DefaultHandlerRegistryTest extends Assert {

  private DefaultHandlerRegistry registry;
  private BeanJsonConverter converter;

  @Before
  public void setUp() throws Exception {
    converter = new BeanJsonConverter(Guice.createInjector());
    registry = new DefaultHandlerRegistry(null, converter,
        new HandlerExecutionListener.NoOpHandler());
    registry.addHandlers(Sets.<Object>newHashSet(new TestHandler()));
  }

  @Test
  public void testGetHandlerRPC() throws Exception {
    assertNotNull(registry.getRpcHandler(new JSONObject("{method : test.get}")));
  }

  @Test
  public void testGetHandlerRest() throws Exception {
    assertNotNull(registry.getRestHandler("/test/", "GET"));
  }

  @Test
  public void testOverrideHandlerRPC() throws Exception {
    assertNotNull(registry.getRpcHandler(new JSONObject("{method : test.overidden}")));
  }

  @Test
  public void testOverrideHandlerRPCName() throws Exception {
    assertNotNull(registry.getRpcHandler(new JSONObject("{method : test.override.rpcname}")));
  }

  @Test
  public void testOverrideHandlerRest() throws Exception {
    assertNotNull(registry.getRestHandler("/test/overidden/method/", "GET"));
  }

  @Test
  public void testGetForAliasHandler() {
    assertNotNull(registry.getRestHandler("/test", "GET"));
  }

  @Test
  public void testRpcHandler_serviceDoesntExist() throws Exception {
    JSONObject rpc = new JSONObject("{method : makebelieve.get}");
    RpcHandler rpcHandler = registry.getRpcHandler(rpc);
    try {
      Future<?> future = rpcHandler.execute(null, null, null);
      future.get();
      fail("Expect exception for missing method");
    } catch (ExecutionException t) {
      assertSame(t.getCause().getClass(), ProtocolException.class);
      Assert.assertEquals(HttpServletResponse.SC_NOT_IMPLEMENTED, ((ProtocolException) t.getCause()).getCode());
    } catch (Throwable t) {
      fail("Unexpected exception " + t.toString());
    }
  }

  @Test
  public void testRestHandler_serviceDoesntExist() {
    RestHandler restHandler = registry.getRestHandler("/makebelieve", "GET");
    try {
      Future<?> future = restHandler.execute(Maps.<String, String[]>newHashMap(), null, null, null);
      future.get();
      fail("Expect exception for missing method");
    } catch (ExecutionException t) {
      assertSame(t.getCause().getClass(), ProtocolException.class);
      Assert.assertEquals(HttpServletResponse.SC_NOT_IMPLEMENTED, ((ProtocolException) t.getCause()).getCode());
    } catch (Throwable t) {
      fail("Unexpected exception " + t.toString());
    }
  }

  @Test
  public void testNonFutureDispatch() throws Exception {
    // Test calling a handler method which does not return a future
    RestHandler handler = registry.getRestHandler("/test", "GET");
    Future<?> future = handler.execute(Maps.<String, String[]>newHashMap(), null, null, null);
    assertEquals(TestHandler.GET_RESPONSE, future.get());
  }

  @Test
  public void testFutureDispatch() throws Exception {
    // Test calling a handler method which does not return a future
    RestHandler handler = registry.getRestHandler("/test", "POST");
    Future<?> future = handler.execute(Maps.<String, String[]>newHashMap(), null, null, null);
    assertEquals(TestHandler.CREATE_RESPONSE, future.get());
  }

  @Test
  public void testRpcWithInputClassThatIsntRequestItem() throws Exception {
    JSONObject rpc = new JSONObject("{ method : test.echo, params: {value: 'Bob' }}");
    RpcHandler handler = registry.getRpcHandler(rpc);
    Future<?> future = handler.execute(null, null, converter);
    assertEquals(future.get(), TestHandler.ECHO_PREFIX + "Bob");
  }

  @Test
  public void testRestWithInputClassThatIsntRequestItem() throws Exception {
    RestHandler handler = registry.getRestHandler("/test/echo", "GET");
    String[] value = {"Bob"};
    Future<?> future = handler.execute(ImmutableMap.of("value", value), null, null, converter);
    assertEquals(future.get(), TestHandler.ECHO_PREFIX + "Bob");
  }

  @Test
  public void testNoArgumentClass() throws Exception {
    JSONObject rpc = new JSONObject("{ method : test.noArg }");
    RpcHandler handler = registry.getRpcHandler(rpc);
    Future<?> future = handler.execute(null, null, converter);
    assertEquals(TestHandler.NO_ARG_RESPONSE, future.get());
  }

  @Test
  public void testNonFutureException() throws Exception {
    // Test calling a handler method which does not return a future
    JSONObject rpc = new JSONObject("{ method : test.exception }");
    RpcHandler handler = registry.getRpcHandler(rpc);
    Future<?> future = handler.execute(null, null, null);
    try {
      future.get();
      fail("Service method did not produce NullPointerException from Future");
    } catch (ExecutionException ee) {
      assertSame(ee.getCause().getClass(), NullPointerException.class);
    }
  }

  @Test
  public void testFutureException() throws Exception {
    // Test calling a handler method which does not return a future
    JSONObject rpc = new JSONObject("{ method : test.futureException }");
    RpcHandler handler = registry.getRpcHandler(rpc);
    Future<?> future = handler.execute(null, null, null);
    try {
      future.get();
      fail("Service method did not produce ExecutionException from Future");
    } catch (ExecutionException ee) {
      assertSame(ee.getCause().getClass(), ProtocolException.class);
    }
  }

  @Test
  public void testSupportedRpcServices() throws Exception {
    assertEquals(registry.getSupportedRpcServices(),
        Sets.newHashSet("test.create", "test.get", "test.overridden", "test.exception",
            "test.futureException", "test.override.rpcname", "test.echo", "test.noArg"));
  }

  @Test
  public void testSupportedRestServices() throws Exception {
    assertEquals(registry.getSupportedRestServices(),
        Sets.newHashSet("GET /test/{someParam}/{someOtherParam}",
            "PUT /test/{someParam}/{someOtherParam}",
            "DELETE /test/{someParam}/{someOtherParam}",
            "POST /test/{someParam}/{someOtherParam}",
            "GET /test/overridden/method",
            "GET /test/echo"));
  }

  @Test(expected = IllegalStateException.class)
  public void testAddNonService() {
    registry.addHandlers(Sets.newHashSet(new Object()));
  }

  @Test
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

  @Test
  public void testRestPathOrdering() {
    DefaultHandlerRegistry.RestPath restPath1 =
        new DefaultHandlerRegistry.RestPath("/service/const1/{p1}/{p2}+/const2/{p3}", null);
    DefaultHandlerRegistry.RestPath restPath2 =
        new DefaultHandlerRegistry.RestPath("/service/{p1}/{p2}+/const2/{p3}", null);
    DefaultHandlerRegistry.RestPath restPath3 =
        new DefaultHandlerRegistry.RestPath("/service/const1/const2/{p1}/{p2}+/{p3}", null);
    Set<DefaultHandlerRegistry.RestPath> sortedSet = ImmutableSortedSet.of(restPath1, restPath2, restPath3);
    Iterator<DefaultHandlerRegistry.RestPath> itr = sortedSet.iterator();
    assertEquals(itr.next(), restPath3);
    assertEquals(itr.next(), restPath1);
    assertEquals(itr.next(), restPath2);
  }
}
