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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.EasyMockTestCase;
import org.apache.shindig.social.core.model.ActivityImpl;
import org.apache.shindig.social.core.util.BeanJsonConverter;
import org.apache.shindig.social.core.util.ContainerConf;
import org.apache.shindig.social.core.util.JsonContainerConf;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.RestfulCollection;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.easymock.classextension.EasyMock;
import org.junit.Test;

import java.util.List;
import java.util.Set;

public class ActivityHandlerTest extends EasyMockTestCase {

  private BeanJsonConverter converter;

  private ActivityService activityService;

  private ActivityHandler handler;

  private FakeGadgetToken token;

  private RestfulRequestItem request;

  private static final Set<UserId> JOHN_DOE = Sets
      .newHashSet(new UserId(UserId.Type.userId, "john.doe"));

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    token = new FakeGadgetToken();
    token.setAppId("appId");
    converter = EasyMock.createMock(BeanJsonConverter.class);
    activityService = EasyMock.createMock(ActivityService.class);
    ContainerConf containerConf = new JsonContainerConf();
    handler = new ActivityHandler(activityService, containerConf);
  }

  @Override
  protected void replay() {
    EasyMock.replay(converter);
    EasyMock.replay(activityService);
  }

  @Override
  protected void verify() {
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

    List<Activity> activityList = ImmutableList.of();
    RestfulCollection<Activity> data = new RestfulCollection<Activity>(activityList);
    org.easymock.EasyMock.expect(activityService.getActivities(JOHN_DOE,
       new GroupId(group, null), null, Sets.<String>newHashSet(), new CollectionOptions(request), token)).andReturn(
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

    List<Activity> activities = ImmutableList.of();
    RestfulCollection<Activity> data = new RestfulCollection<Activity>(activities);
    Set<UserId> userIdSet = Sets.newLinkedHashSet(JOHN_DOE);
    userIdSet.add(new UserId(UserId.Type.userId, "jane.doe"));
    org.easymock.EasyMock.expect(activityService.getActivities(userIdSet,
        new GroupId(GroupId.Type.self, null), "appId", Sets.<String>newHashSet(), new CollectionOptions(request), token)).andReturn(
          ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleGetActivityById() throws Exception {
    setPath("/people/john.doe/@friends/@app/1");

    Activity activity = new ActivityImpl();
    org.easymock.EasyMock.expect(activityService.getActivity(JOHN_DOE.iterator().next(),
        new GroupId(GroupId.Type.friends, null),
        "appId", Sets.<String>newHashSet(), "1", token)).andReturn(
        ImmediateFuture.newInstance(activity));

    replay();
    assertEquals(activity, handler.handleGet(request).get());
    verify();
  }

  private void setupPostData() throws SocialSpiException {
    String jsonActivity = "{title: hi mom!, etc etc}";

    setPathAndPostData("/people/john.doe/@self/@app", jsonActivity);

    Activity activity = new ActivityImpl();
    org.easymock.EasyMock.expect(converter.convertToObject(jsonActivity, Activity.class)).andReturn(activity);

    org.easymock.EasyMock.expect(activityService.createActivity(JOHN_DOE.iterator().next(),
        new GroupId(GroupId.Type.self, null), "appId", Sets.<String>newHashSet(),
        activity, token)).andReturn(ImmediateFuture.newInstance((Void) null));
    replay();
  }

  public void testHandlePost() throws Exception {
    setupPostData();
    assertNull(handler.handlePost(request).get());
    verify();
  }

  public void testHandlePut() throws Exception {
    setupPostData();
    assertNull(handler.handlePut(request).get());
    verify();
  }

  public void testHandleDelete() throws Exception {
    setPath("/people/john.doe/@self/@app/1");

    org.easymock.EasyMock.expect(activityService.deleteActivities(JOHN_DOE.iterator().next(),
        new GroupId(GroupId.Type.self, null), "appId", Sets.newHashSet("1"), token)).andReturn(
        ImmediateFuture.newInstance((Void) null));

    replay();
    assertNull(handler.handleDelete(request).get());
    verify();
  }
}
