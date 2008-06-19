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
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.opensocial.model.ActivityImpl;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;
import org.apache.shindig.common.testing.FakeGadgetToken;

import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;

import java.util.Map;

import com.google.common.collect.Maps;

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
    handler.setConverter(converter);
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
    this.setPathAndParams(path, null);
  }

  private void setPathAndParams(String path, Map<String, String> params) {
    request = new RequestItem(path, params, token, null);
  }

  private void assertHandleGetForGroup(GroupId.Type group) {
    setPath("/activities/john.doe/@" + group.toString());

    ResponseItem<RestfulCollection<Activity>> data
        = new ResponseItem<RestfulCollection<Activity>>(null);
    EasyMock.expect(activityService.getActivities(new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(group, null), token)).andReturn(data);

    replay();
    assertEquals(data, handler.handleGet(request));
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
        "jane.doe", token)).andReturn(data);

    replay();
    assertEquals(data, handler.handleGet(request));
    verify();
  }

  private ResponseItem setupPostData() {
    String jsonActivity = "{title: hi mom!, etc etc}";

    Map<String, String> params = Maps.newHashMap();
    params.put("entry", jsonActivity);
    setPathAndParams("/people/john.doe/@self", params);

    ActivityImpl activity = new ActivityImpl();
    EasyMock.expect(converter.convertToObject(jsonActivity, ActivityImpl.class)).andReturn(activity);

    ResponseItem data = new ResponseItem<Object>(null);
    EasyMock.expect(activityService.createActivity(new UserId(UserId.Type.userId, "john.doe"),
        activity, token)).andReturn(data);
    replay();
    return data;
  }

  public void testHandlePost() throws Exception {
    ResponseItem data = setupPostData();
    assertEquals(data, handler.handlePost(request));
    verify();
  }

  public void testHandlePut() throws Exception {
    ResponseItem data = setupPostData();
    assertEquals(data, handler.handlePut(request));
    verify();
  }

  public void testHandleDelete() throws Exception {
    replay();
    assertEquals(ResponseError.BAD_REQUEST, handler.handleDelete(request).getError());
    verify();
  }
}