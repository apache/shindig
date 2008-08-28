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

import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.core.model.ActivityImpl;
import org.apache.shindig.social.core.util.BeanJsonConverter;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.RestfulCollection;
import org.apache.shindig.social.opensocial.spi.RestfulItem;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Sets;
import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;

import java.util.Set;

public class ActivityHandlerTest extends TestCase {

  private BeanJsonConverter converter;

  private ActivityService activityService;

  private ActivityHandler handler;

  private FakeGadgetToken token;

  private RestfulRequestItem request;

  private static final Set<UserId> JOHN_DOE = Sets
      .newHashSet(new UserId(UserId.Type.userId, "john.doe"));

  @Override
  protected void setUp() throws Exception {
    token = new FakeGadgetToken();
    token.setAppId("appId");
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
    request = new RestfulRequestItem(path, "GET", postData, token, converter);
  }

  private void assertHandleGetForGroup(GroupId.Type group) throws Exception {
    setPath("/activities/john.doe/@" + group.toString());

    RestfulCollection<Activity> data = new RestfulCollection<Activity>(null, null);
    EasyMock.expect(activityService.getActivities(JOHN_DOE,
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

  public void testHandleGetPlural() throws Exception {
    setPath("/activities/john.doe,jane.doe/@self/@app");

    RestfulCollection<Activity> data = new RestfulCollection<Activity>(null, null);
    Set<UserId> userIdSet = Sets.newLinkedHashSet(JOHN_DOE);
    userIdSet.add(new UserId(UserId.Type.userId, "jane.doe"));
    EasyMock.expect(activityService.getActivities(userIdSet,
        new GroupId(GroupId.Type.self, null), "appId", Sets.<String>newHashSet(), token)).andReturn(
        ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleGetActivityById() throws Exception {
    setPath("/people/john.doe/@friends/@app/1");

    RestfulItem<Activity> data = new RestfulItem<Activity>(null, null);
    EasyMock.expect(activityService.getActivity(JOHN_DOE.iterator().next(),
        new GroupId(GroupId.Type.friends, null),
        "appId", Sets.<String>newHashSet(), "1", token)).andReturn(
        ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  private ResponseItem setupPostData() {
    String jsonActivity = "{title: hi mom!, etc etc}";

    setPathAndPostData("/people/john.doe/@self/@app", jsonActivity);

    Activity activity = new ActivityImpl();
    EasyMock.expect(converter.convertToObject(jsonActivity, Activity.class)).andReturn(activity);

    ResponseItem data = new ResponseItem(null, null);
    EasyMock.expect(activityService.createActivity(JOHN_DOE.iterator().next(),
        new GroupId(GroupId.Type.self, null), "appId", Sets.<String>newHashSet(),
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
    setPath("/people/john.doe/@self/@app/1");

    ResponseItem data = new ResponseItem(null, null);
    EasyMock.expect(activityService.deleteActivities(JOHN_DOE.iterator().next(),
        new GroupId(GroupId.Type.self, null), "appId", Sets.newHashSet("1"), token)).andReturn(
        ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleDelete(request).get());
    verify();
  }
}
