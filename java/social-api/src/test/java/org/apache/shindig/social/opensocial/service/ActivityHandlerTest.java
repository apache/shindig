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
package org.apache.shindig.social.opensocial.service;

import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.core.model.ActivityImpl;
import org.apache.shindig.social.core.util.BeanJsonConverter;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.service.ActivityHandler;
import org.apache.shindig.social.opensocial.service.RequestItem;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.RestfulCollection;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.util.ImmediateFuture;

import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ActivityHandlerTest extends TestCase {
  private BeanJsonConverter converter;
  private ActivityService activityService;
  private ActivityHandler handler;
  private FakeGadgetToken token;
  private RequestItem request;


  @Override
  protected void setUp() throws Exception {
    token = new FakeGadgetToken();
    converter = EasyMock.createMock(BeanJsonConverter.class);
    activityService = EasyMock.createMock(ActivityService.class);

    handler = new ActivityHandler(activityService);
  }

  private void replay() {
    EasyMock.replay(converter);
    EasyMock.replay(activityService);
  }

  private void verify() {
    EasyMock.verify(converter);
    EasyMock.verify(activityService);
  }

  private void setPath(String path) {
    this.setPathAndPostData(path, null);
  }

  private void setPathAndPostData(String path, String postData) {
    Map<String, String> params = Maps.newHashMap();
    params.put("fields", null);

    request = new RequestItem();
    request.setUrl(path);
    request.setParameters(params);
    request.setToken(token);
    request.setConverter(converter);
    request.setPostData(postData);
  }

  private void assertHandleGetForGroup(GroupId.Type group) throws Exception {
    setPath("/activities/john.doe/@" + group.toString());

    ResponseItem<RestfulCollection<Activity>> data
        = new ResponseItem<RestfulCollection<Activity>>(null);
    EasyMock.expect(activityService.getActivities(new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(group, null), null, Sets.<String>newHashSet(), token)).andReturn(
        ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleGetAll() throws Exception {
    assertHandleGetForGroup(GroupId.Type.all);
  }

  public void testHandleGetFriends() throws Exception {
    assertHandleGetForGroup(GroupId.Type.friends);
  }

  public void testHandleGetSelf() throws Exception {
    assertHandleGetForGroup(GroupId.Type.self);
  }

  public void testHandleGetActivityById() throws Exception {
    setPath("/people/john.doe/@friends/jane.doe");

    ResponseItem<Activity> data = new ResponseItem<Activity>(null);
    EasyMock.expect(activityService.getActivity(new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(GroupId.Type.friends, null),
        null, Sets.<String>newHashSet(), "jane.doe", token)).andReturn(
        ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  private ResponseItem<Object> setupPostData() {
    String jsonActivity = "{title: hi mom!, etc etc}";

    setPathAndPostData("/people/john.doe/@self", jsonActivity);

    Activity activity = new ActivityImpl();
    EasyMock.expect(converter.convertToObject(jsonActivity, Activity.class)).andReturn(activity);

    ResponseItem<Object> data = new ResponseItem<Object>(null);
    EasyMock.expect(activityService.createActivity(new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(GroupId.Type.self, null), null, Sets.<String>newHashSet(),
        activity, token)).andReturn(ImmediateFuture.newInstance(data));
    replay();
    return data;
  }

  public void testHandlePost() throws Exception {
    ResponseItem data = setupPostData();
    assertEquals(data, handler.handlePost(request).get());
    verify();
  }

  public void testHandlePut() throws Exception {
    ResponseItem data = setupPostData();
    assertEquals(data, handler.handlePut(request).get());
    verify();
  }

  public void testHandleDelete() throws Exception {
    setPath("/people/john.doe/@self/1");

    ResponseItem<Object> data = new ResponseItem<Object>(null);
    EasyMock.expect(activityService.deleteActivity(new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(GroupId.Type.self, null), null, "1", token)).andReturn(
        ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleDelete(request).get());
    verify();
  }
}