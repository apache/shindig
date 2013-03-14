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
import static org.easymock.EasyMock.isNull;

import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.config.JsonContainerConfig;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.protocol.DefaultHandlerRegistry;
import org.apache.shindig.protocol.HandlerExecutionListener;
import org.apache.shindig.protocol.HandlerRegistry;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestHandler;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.social.core.model.ActivityEntryImpl;
import org.apache.shindig.social.opensocial.model.ActivityEntry;
import org.apache.shindig.social.opensocial.spi.ActivityStreamService;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;

/**
 * Tests the ActivityStreamsHandler.
 */
public class ActivityStreamHandlerTest extends EasyMockTestCase {

  private BeanJsonConverter converter;

  private ActivityStreamService service;

  private ActivityStreamHandler handler;

  private FakeGadgetToken token;

  private static final Set<UserId> JOHN_DOE = ImmutableSet.of(new UserId(
      UserId.Type.userId, "john.doe"));

  protected HandlerRegistry registry;
  protected ContainerConfig containerConfig;

  @Before
  public void setUp() throws Exception {
    token = new FakeGadgetToken();
    token.setAppId("appId");

    converter = mock(BeanJsonConverter.class);
    service = mock(ActivityStreamService.class);

    JSONObject config = new JSONObject('{' + ContainerConfig.DEFAULT_CONTAINER + ':' +
        "{'gadgets.container': ['default']," +
         "'gadgets.features':{opensocial:" +
           "{supportedFields: {activityEntry: ['id', 'title']}}" +
         "}}}");

    containerConfig = new JsonContainerConfig(config, Expressions.forTesting());
    handler = new ActivityStreamHandler(service, containerConfig);
    registry = new DefaultHandlerRegistry(null, converter,
        new HandlerExecutionListener.NoOpHandler());
    registry.addHandlers(ImmutableSet.<Object>of(handler));
  }

  /* Helper for retrieving groups. */
  private void assertHandleGetForGroup(GroupId.Type group) throws Exception {
    String path = "/activitystreams/john.doe/@" + group.toString();
    RestHandler operation = registry.getRestHandler(path, "GET");

    List<ActivityEntry> entries = ImmutableList.of();
    RestfulCollection<ActivityEntry> data = new RestfulCollection<ActivityEntry>(entries);
    org.easymock.EasyMock.expect(service.getActivityEntries(eq(JOHN_DOE),
       eq(new GroupId(group, null)), (String)isNull(), eq(ImmutableSet.<String>of()),
        org.easymock.EasyMock.isA(CollectionOptions.class), eq(token))).
        andReturn(Futures.immediateFuture(data));

    replay();
    assertEquals(data, operation.execute(Maps.<String, String[]>newHashMap(),
        null, token, converter).get());
    verify();
    reset();
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
    String path = "/activitystreams/john.doe,jane.doe/@self/@app";
    RestHandler operation = registry.getRestHandler(path, "GET");

    List<ActivityEntry> entries = ImmutableList.of();
    RestfulCollection<ActivityEntry> data = new RestfulCollection<ActivityEntry>(entries);
    Set<UserId> userIdSet = Sets.newLinkedHashSet(JOHN_DOE);
    userIdSet.add(new UserId(UserId.Type.userId, "jane.doe"));
    org.easymock.EasyMock.expect(service.getActivityEntries(eq(userIdSet),
        eq(new GroupId(GroupId.Type.self, null)), eq("appId"),eq(ImmutableSet.<String>of()),
        org.easymock.EasyMock.isA((CollectionOptions.class)), eq(token))).andReturn(
          Futures.immediateFuture(data));

    replay();
    assertEquals(data, operation.execute(Maps.<String, String[]>newHashMap(),
        null, token, converter).get());
    verify();
    reset();
  }

  @Test
  public void testHandleGetActivityEntryById() throws Exception {
    String path = "/activitystreams/john.doe/@friends/@app/myObjectId123";  // TODO: change id=1 in DB for consistency
    RestHandler operation = registry.getRestHandler(path, "GET");

    ActivityEntry entry = new ActivityEntryImpl();
    org.easymock.EasyMock.expect(service.getActivityEntry(eq(JOHN_DOE.iterator().next()),
        eq(new GroupId(GroupId.Type.friends, null)),
        eq("appId"), eq(ImmutableSet.<String>of()), eq("myObjectId123"), eq(token))).andReturn(
        Futures.immediateFuture(entry));

    replay();
    assertEquals(entry, operation.execute(Maps.<String, String[]>newHashMap(),
        null, token, converter).get());
    verify();
    reset();
  }

  /* Helper for testing PUT and POST */
  private Future<?> setupBodyRequest(String method) throws ProtocolException {
    String jsonActivityEntry = "{title: 'hi mom!', object: {id: 'testObject'}}";

    String path = "/activitystreams/john.doe/@self/@app";
    RestHandler operation = registry.getRestHandler(path, method);

    ActivityEntry entry = new ActivityEntryImpl();
    org.easymock.EasyMock.expect(converter.convertToObject(eq(jsonActivityEntry), eq(ActivityEntry.class)))
        .andReturn(entry);

    org.easymock.EasyMock.expect(service.createActivityEntry(eq(JOHN_DOE.iterator().next()),
        eq(new GroupId(GroupId.Type.self, null)), eq("appId"), eq(ImmutableSet.<String>of()),
        eq(entry), eq(token))).andReturn(Futures.immediateFuture((ActivityEntry) null));
    replay();

    return operation.execute(Maps.<String, String[]>newHashMap(),
        new StringReader(jsonActivityEntry), token, converter);
  }

  @Test
  public void testHandlePost() throws Exception {
    String jsonActivityEntry = "{title: 'hi mom!', object: {id: 'testObject'}}";

    String path = "/activitystreams/john.doe/@self/@app";
    RestHandler operation = registry.getRestHandler(path, "POST");

    ActivityEntry entry = new ActivityEntryImpl();
    org.easymock.EasyMock.expect(converter.convertToObject(eq(jsonActivityEntry), eq(ActivityEntry.class)))
        .andReturn(entry);

    org.easymock.EasyMock.expect(service.createActivityEntry(eq(JOHN_DOE.iterator().next()),
        eq(new GroupId(GroupId.Type.self, null)), eq("appId"), eq(ImmutableSet.<String>of()),
        eq(entry), eq(token))).andReturn(Futures.immediateFuture((ActivityEntry) null));
    replay();

    Future<?> future = operation.execute(Maps.<String, String[]>newHashMap(),
        new StringReader(jsonActivityEntry), token, converter);
    assertNull(future.get());
    verify();
    reset();
  }

  @Test
  public void testHandlePut() throws Exception {
    String jsonActivityEntry = "{title: 'hi mom!', object: {id: 'testObject'}}";

    String path = "/activitystreams/john.doe/@self/@app/testObject";
    RestHandler operation = registry.getRestHandler(path, "PUT");

    ActivityEntry entry = new ActivityEntryImpl();
    org.easymock.EasyMock.expect(converter.convertToObject(eq(jsonActivityEntry), eq(ActivityEntry.class)))
        .andReturn(entry);

    org.easymock.EasyMock.expect(service.updateActivityEntry(eq(JOHN_DOE.iterator().next()),
        eq(new GroupId(GroupId.Type.self, null)), eq("appId"), eq(ImmutableSet.<String>of()),
        eq(entry), eq("testObject"), eq(token))).andReturn(Futures.immediateFuture((ActivityEntry) null));
    replay();

    Future<?> future = operation.execute(Maps.<String, String[]>newHashMap(),
        new StringReader(jsonActivityEntry), token, converter);
    assertNull(future.get());
    verify();
    reset();
  }

  @Test
  public void testHandleDelete() throws Exception {
    String path = "/activitystreams/john.doe/@self/@app/myObjectId123";
    RestHandler operation = registry.getRestHandler(path, "DELETE");

    org.easymock.EasyMock.expect(service.deleteActivityEntries(eq(JOHN_DOE.iterator().next()),
        eq(new GroupId(GroupId.Type.self, null)), eq("appId"), eq(ImmutableSet.of("myObjectId123")),
        eq(token))).andReturn(Futures.immediateFuture((Void) null));

    replay();
    assertNull(operation.execute(Maps.<String, String[]>newHashMap(), null,
        token, converter).get());
    verify();
    reset();
  }

  @Test
  public void testHandleGetSupportedFields() throws Exception {
    String path = "/activitystreams/@supportedFields";
    RestHandler operation = registry.getRestHandler(path, "GET");

    replay();
    @SuppressWarnings("unchecked")
    List<Object> received = (List<Object>) operation.execute(Maps.<String, String[]>newHashMap(),
        null, token, converter).get();
    assertEquals(2, received.size());
    assertEquals("id", received.get(0).toString());
    assertEquals("title", received.get(1).toString());

    verify();
  }
}
