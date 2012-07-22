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
package org.apache.shindig.gadgets.http;

import org.apache.shindig.auth.AuthenticationMode;
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.protocol.DefaultHandlerRegistry;
import org.apache.shindig.protocol.HandlerExecutionListener;
import org.apache.shindig.protocol.HandlerRegistry;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestHandler;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Basic test of invalidation handler
 */
public class InvalidationHandlerTest extends EasyMockTestCase {

  private BeanJsonConverter converter;

  private InvalidationService invalidationService;

  private InvalidationHandler handler;

  private FakeGadgetToken token;

  private Map<String, String[]> params;

  protected HandlerRegistry registry;
  protected ContainerConfig containerConfig;

  @Before
  public void setUp() throws Exception {
    token = new FakeGadgetToken();
    token.setAppId("appId");
    token.setViewerId("userX");

    converter = mock(BeanJsonConverter.class);
    invalidationService = mock(InvalidationService.class);

    handler = new InvalidationHandler(invalidationService);
    registry = new DefaultHandlerRegistry(null, converter,
        new HandlerExecutionListener.NoOpHandler());
    registry.addHandlers(Sets.<Object>newHashSet(handler));

    params = Maps.newHashMap();
  }

  @Test
  public void testHandleSimpleGetInvalidateViewer() throws Exception {
    String path = "/cache/invalidate";
    RestHandler operation = registry.getRestHandler(path, "GET");

    invalidationService.invalidateUserResources(
        eq(ImmutableSet.of("userX")),
        eq(token));
    expectLastCall();

    replay();
    operation.execute(params, null, token, converter).get();
    verify();
    reset();
  }

  @Test
  public void testAllowConsumerAuthInvalidateAppResource() throws Exception {
    String path = "/cache/invalidate";
    RestHandler operation = registry.getRestHandler(path, "POST");
    params.put(InvalidationHandler.KEYS_PARAM, new String[]{"http://www.example.org/gadget.xml"});
    token.setAuthenticationMode(AuthenticationMode.OAUTH_CONSUMER_REQUEST.name());
    invalidationService.invalidateApplicationResources(
        eq(ImmutableSet.of(Uri.parse("http://www.example.org/gadget.xml"))),
        eq(token));
    expectLastCall();

    replay();
    operation.execute(params, null, token, converter).get();
    verify();
    reset();
  }

  @Test
  public void testFailTokenAuthInvalidateAppResource() throws Exception {
    String path = "/cache/invalidate";
    RestHandler operation = registry.getRestHandler(path, "POST");
    params.put(InvalidationHandler.KEYS_PARAM, new String[]{"http://www.example.org/gadget.xml"});

    try {
      operation.execute(params, null, token, converter).get();
      fail("Expected error");
    } catch (ExecutionException ee) {
      assertTrue(ee.getCause() instanceof ProtocolException);
    }
  }

  @Test
  public void testFailInvalidateNoApp() throws Exception {
    String path = "/cache/invalidate";
    RestHandler operation = registry.getRestHandler(path, "POST");
    params.put(InvalidationHandler.KEYS_PARAM, new String[]{"http://www.example.org/gadget.xml"});

    try {
      token.setAppId("");
      token.setAppUrl("");
      operation.execute(params, null, token, converter).get();
      fail("Expected error");
    } catch (ExecutionException ee) {
      assertTrue(ee.getCause() instanceof ProtocolException);
    }
  }
}
