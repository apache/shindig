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
import org.apache.shindig.social.core.model.ActivityImpl;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.CollectionOptionsFactory;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.isNull;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

public class ActivityHandlerTest extends EasyMockTestCase {

  private BeanJsonConverter converter;

  private ActivityService activityService;

  private ActivityHandler handler;

  private FakeGadgetToken token;

  private static final Set<UserId> JOHN_DOE =
      ImmutableSet.of(new UserId(UserId.Type.userId, "john.doe"));

  protected HandlerRegistry registry;
  protected ContainerConfig containerConfig;

  @Before
  public void setUp() throws Exception {
    token = new FakeGadgetToken();
    token.setAppId("appId");

    converter = mock(BeanJsonConverter.class);
    activityService = mock(ActivityService.class);

    JSONObject config = new JSONObject('{' + ContainerConfig.DEFAULT_CONTAINER + ':' +
        "{'gadgets.container': ['default']," +
         "'gadgets.features':{opensocial:" +
           "{supportedFields: {activity: ['id', 'title']}}" +
         "}}}");

    containerConfig = new JsonContainerConfig(config, Expressions.forTesting());
    handler = new ActivityHandler(activityService, containerConfig, new CollectionOptionsFactory());
    registry = new DefaultHandlerRegistry(null, converter,
        new HandlerExecutionListener.NoOpHandler());
    registry.addHandlers(ImmutableSet.<Object>of(handler));
  }

  private void assertHandleGetForGroup(GroupId.Type group) throws Exception {
    String path = "/activities/john.doe/@" + group.toString();
    RestHandler operation = registry.getRestHandler(path, "GET");

    List<Activity> activityList = ImmutableList.of();
    RestfulCollection<Activity> data = new RestfulCollection<Activity>(activityList);
    org.easymock.EasyMock.expect(activityService.getActivities(eq(JOHN_DOE),
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
    String path = "/activities/john.doe,jane.doe/@self/@app";
    RestHandler operation = registry.getRestHandler(path, "GET");

    List<Activity> activities = ImmutableList.of();
    RestfulCollection<Activity> data = new RestfulCollection<Activity>(activities);
    Set<UserId> userIdSet = Sets.newLinkedHashSet(JOHN_DOE);
    userIdSet.add(new UserId(UserId.Type.userId, "jane.doe"));
    org.easymock.EasyMock.expect(activityService.getActivities(eq(userIdSet),
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
  public void testHandleGetActivityById() throws Exception {
    String path = "/activities/john.doe/@friends/@app/1";
    RestHandler operation = registry.getRestHandler(path, "GET");

    Activity activity = new ActivityImpl();
    org.easymock.EasyMock.expect(activityService.getActivity(eq(JOHN_DOE.iterator().next()),
        eq(new GroupId(GroupId.Type.friends, null)),
        eq("appId"), eq(ImmutableSet.<String>of()), eq("1"), eq(token))).andReturn(
        Futures.immediateFuture(activity));

    replay();
    assertEquals(activity, operation.execute(Maps.<String, String[]>newHashMap(),
        null, token, converter).get());
    verify();
    reset();
  }

  private Future<?> setupBodyRequest(String method) throws ProtocolException {
    String jsonActivity = "{title: hi mom!, etc etc}";

    String path = "/activities/john.doe/@self/@app";
    RestHandler operation = registry.getRestHandler(path, method);

    Activity activity = new ActivityImpl();
    org.easymock.EasyMock.expect(converter.convertToObject(eq(jsonActivity), eq(Activity.class)))
        .andReturn(activity);

    org.easymock.EasyMock.expect(activityService.createActivity(eq(JOHN_DOE.iterator().next()),
        eq(new GroupId(GroupId.Type.self, null)), eq("appId"), eq(ImmutableSet.<String>of()),
        eq(activity), eq(token))).andReturn(Futures.immediateFuture((Void) null));
    replay();

    return operation.execute(Maps.<String, String[]>newHashMap(),
        new StringReader(jsonActivity), token, converter);
  }

  @Test
  public void testHandlePost() throws Exception {
    Future<?> future = setupBodyRequest("POST");
    assertNull(future.get());
    verify();
    reset();
  }

  @Test
  public void testHandlePut() throws Exception {
    Future<?> future = setupBodyRequest("PUT");
    assertNull(future.get());
    verify();
    reset();
  }

  @Test
  public void testHandleDelete() throws Exception {
    String path = "/activities/john.doe/@self/@app/1";
    RestHandler operation = registry.getRestHandler(path, "DELETE");


    org.easymock.EasyMock.expect(activityService.deleteActivities(eq(JOHN_DOE.iterator().next()),
        eq(new GroupId(GroupId.Type.self, null)), eq("appId"), eq(ImmutableSet.of("1")),
        eq(token))).andReturn(Futures.immediateFuture((Void) null));

    replay();
    assertNull(operation.execute(Maps.<String, String[]>newHashMap(), null,
        token, converter).get());
    verify();
    reset();
  }

  @Test
  public void testHandleGetSuportedFields() throws Exception {
    String path = "/activities/@supportedFields";
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
