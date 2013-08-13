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

import static org.easymock.EasyMock.eq;

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.protocol.DataCollection;
import org.apache.shindig.protocol.DefaultHandlerRegistry;
import org.apache.shindig.protocol.HandlerExecutionListener;
import org.apache.shindig.protocol.HandlerRegistry;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestHandler;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.easymock.EasyMock;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;

public class AppDataHandlerTest extends EasyMockTestCase {

  private BeanJsonConverter converter;

  private AppDataService appDataService;

  private FakeGadgetToken token;

  protected HandlerRegistry registry;


  private static final Set<UserId> JOHN_DOE = Collections.unmodifiableSet(
      ImmutableSet.of(new UserId(UserId.Type.userId, "john.doe")));


  @Before
  public void setUp() throws Exception {
    token = new FakeGadgetToken();
    converter = mock(BeanJsonConverter.class);
    appDataService = mock(AppDataService.class);
    AppDataHandler handler = new AppDataHandler(appDataService);
    registry = new DefaultHandlerRegistry(null, converter,
        new HandlerExecutionListener.NoOpHandler());
    registry.addHandlers(ImmutableSet.<Object>of(handler));
  }

  private void assertHandleGetForGroup(GroupId.Type group) throws Exception {
    String path = "/appdata/john.doe/@" + group.toString() + "/appId";
    RestHandler operation = registry.getRestHandler(path, "GET");

    DataCollection data = new DataCollection(null);
    org.easymock.EasyMock.expect(appDataService.getPersonData(eq(JOHN_DOE),
        eq(new GroupId(group, null)),
        eq("appId"), eq(ImmutableSet.<String>of()), eq(token)))
        .andReturn(Futures.immediateFuture(data));

    replay();
    assertEquals(data, operation.execute(Maps.<String, String[]>newHashMap(),
        null, token, converter).get());
    verify();
  }

  @Test
  public void testHandleGetAll() throws Exception {
    assertHandleGetForGroup(GroupId.Type.all);
  }

  @Test
  public void testHandleGetFriends() throws Exception {
    assertHandleGetForGroup(GroupId.Type.friends);
  }

  @Test
  public void testHandleGetSelf() throws Exception {
    assertHandleGetForGroup(GroupId.Type.self);
  }

  @Test
  public void testHandleGetPlural() throws Exception {
    String path = "/appdata/john.doe,jane.doe/@self/appId";
    RestHandler operation = registry.getRestHandler(path, "GET");

    DataCollection data = new DataCollection(null);
    Set<UserId> userIdSet = Sets.newLinkedHashSet(JOHN_DOE);
    userIdSet.add(new UserId(UserId.Type.userId, "jane.doe"));
    org.easymock.EasyMock.expect(appDataService.getPersonData(eq(userIdSet),
        eq(new GroupId(GroupId.Type.self, null)),
        eq("appId"), eq(ImmutableSet.<String>of()), eq(token)))
        .andReturn(Futures.immediateFuture(data));

    replay();
    assertEquals(data, operation.execute(Maps.<String, String[]>newHashMap(),
        null, token, converter).get());
    verify();
  }

  @Test
  public void testHandleGetWithoutFields() throws Exception {
    String path = "/appdata/john.doe/@friends/appId";
    RestHandler operation = registry.getRestHandler(path, "GET");

    Map<String, String[]> params = Maps.newHashMap();
    params.put("fields", new String[]{"pandas"});

    DataCollection data = new DataCollection(null);
    org.easymock.EasyMock.expect(appDataService.getPersonData(eq(JOHN_DOE),
        eq(new GroupId(GroupId.Type.friends, null)),
        eq("appId"), eq(ImmutableSet.of("pandas")), eq(token)))
        .andReturn(Futures.immediateFuture(data));

    replay();
    assertEquals(data, operation.execute(params, null, token, converter).get());
    verify();
  }

  private Future<?> setupPostData(String method) throws ProtocolException {
    String path = "/appdata/john.doe/@self/appId";
    RestHandler operation = registry.getRestHandler(path, method);

    String jsonAppData = "{pandas: 'are fuzzy'}";

    Map<String, String[]> params = Maps.newHashMap();
    params.put("fields", new String[]{"pandas"});

    HashMap<String, Object> values = Maps.newHashMap();
    org.easymock.EasyMock.expect(converter.convertToObject(eq(jsonAppData), eq(Map.class)))
        .andReturn(values);

    org.easymock.EasyMock.expect(appDataService.updatePersonData(eq(JOHN_DOE.iterator().next()),
        eq(new GroupId(GroupId.Type.self, null)),
        eq("appId"), eq(ImmutableSet.of("pandas")), eq(values), eq(token)))
        .andReturn(Futures.immediateFuture((Void) null));
    replay();
    return operation.execute(params, new StringReader(jsonAppData), token, converter);
  }

  @Test
  public void testHandlePost() throws Exception {
    assertNull(setupPostData("POST").get());
    verify();
  }

  @Test
  public void testHandlePut() throws Exception {
    assertNull(setupPostData("PUT").get());
    verify();
  }

  /**
   * Test that the handler correctly recognizes null keys in the data.
   * @throws Exception if the test fails
   */
  @Test
  public void testHandleNullPostDataKeys() throws Exception {
    String path = "/appdata/john.doe/@self/appId";
    RestHandler operation = registry.getRestHandler(path, "POST");
    String jsonAppData = "{pandas: 'are fuzzy'}";

    Map<String, String[]> params = Maps.newHashMap();
    params.put("fields", new String[]{"pandas"});

    HashMap<String, String> values = Maps.newHashMap();
    // create an invalid set of app data and inject
    values.put("Aokkey", "an ok key");
    values.put("", "an empty value");
    org.easymock.EasyMock.expect(converter.convertToObject(eq(jsonAppData), eq(Map.class)))
        .andReturn(values);

    replay();
    try {
      operation.execute(params, new StringReader(jsonAppData), token, converter).get();
      fail();
    } catch (ExecutionException ee) {
      assertEquals(HttpServletResponse.SC_BAD_REQUEST,
          ((ProtocolException) ee.getCause()).getCode());
      // was expecting an Exception
    }
    verify();
  }
  /**
   * Test that the handler correctly recognizes invalid keys in the data.
   * @throws Exception if the test fails
   */
  @Test
  public void testHandleInvalidPostDataKeys() throws Exception {
    String path = "/appdata/john.doe/@self/appId";
    RestHandler operation = registry.getRestHandler(path, "POST");
    String jsonAppData = "{pandas: 'are fuzzy'}";

    Map<String, String[]> params = Maps.newHashMap();
    params.put("fields", new String[]{"pandas"});

    HashMap<String, String> values = Maps.newHashMap();
    // create an invalid set of app data and inject
    values.put("Aokkey", "an ok key");
    values.put("a bad key", "a good value");
    org.easymock.EasyMock.expect(converter.convertToObject(eq(jsonAppData), eq(Map.class)))
        .andReturn(values);

    replay();
    try {
      operation.execute(params, new StringReader(jsonAppData), token, converter).get();
      fail();
    } catch (ExecutionException ee) {
      assertEquals(HttpServletResponse.SC_BAD_REQUEST,
          ((ProtocolException) ee.getCause()).getCode());
    }
    verify();
  }


  @Test
  public void testHandleDelete() throws Exception {
    Map<String, String[]> params = Maps.newHashMap();
    params.put("fields", new String[]{"pandas"});
    String path = "/appdata/john.doe/@self/appId";
    RestHandler operation = registry.getRestHandler(path, "DELETE");

    EasyMock.expect(appDataService.deletePersonData(eq(JOHN_DOE.iterator().next()),
        eq(new GroupId(GroupId.Type.self, null)),
        eq("appId"), eq(ImmutableSet.of("pandas")), eq(token)))
        .andReturn(Futures.immediateFuture((Void) null));

    replay();
    assertNull(operation.execute(params, null, token, converter).get());
    verify();
  }
}
