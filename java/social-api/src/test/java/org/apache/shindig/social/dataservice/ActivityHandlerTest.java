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
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;
import org.apache.shindig.common.testing.FakeGadgetToken;

import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;

import javax.servlet.http.HttpServletRequest;

public class ActivityHandlerTest extends TestCase {
  private BeanJsonConverter converter;
  private HttpServletRequest servletRequest;
  private ActivityService activityService;
  private ActivityHandler handler;
  private FakeGadgetToken token;


  @Override
  protected void setUp() throws Exception {
    token = new FakeGadgetToken();
    converter = EasyMock.createMock(BeanJsonConverter.class);
    servletRequest = EasyMock.createMock(HttpServletRequest.class);
    activityService = EasyMock.createMock(ActivityService.class);

    handler = new ActivityHandler(activityService, converter);
  }

  private void replay() {
    EasyMock.replay(converter);
    EasyMock.replay(servletRequest);
    EasyMock.replay(activityService);
  }

  private void verify() {
    EasyMock.verify(converter);
    EasyMock.verify(servletRequest);
    EasyMock.verify(activityService);
  }

  private void setPath(String path) {
    EasyMock.expect(servletRequest.getPathInfo()).andReturn(path);
  }

  private void assertHandleGetForGroup(DataServiceServlet.GroupId group) {
    setPath("/activities/john.doe/" + group.getJsonString());

    ResponseItem<RestfulCollection<Activity>> data
        = new ResponseItem<RestfulCollection<Activity>>(null);
    EasyMock.expect(activityService.getActivities("john.doe",
        group, token)).andReturn(data);

    replay();
    assertEquals(data, handler.handleGet(servletRequest, token));
    verify();
  }

  public void testHandleGetAll() throws Exception {
    assertHandleGetForGroup(DataServiceServlet.GroupId.ALL);
  }

  public void testHandleGetFriends() throws Exception {
    assertHandleGetForGroup(DataServiceServlet.GroupId.FRIENDS);
  }

  public void testHandleGetSelf() throws Exception {
    assertHandleGetForGroup(DataServiceServlet.GroupId.SELF);
  }

  public void testHandleGetActivityById() throws Exception {
    setPath("/people/john.doe/@friends/jane.doe");

    ResponseItem<Activity> data = new ResponseItem<Activity>(null);
    EasyMock.expect(activityService.getActivity("john.doe", DataServiceServlet.GroupId.FRIENDS,
        "jane.doe", token)).andReturn(data);

    replay();
    assertEquals(data, handler.handleGet(servletRequest, token));
    verify();
  }

  private ResponseItem setupPostData() {
    setPath("/people/john.doe/@self");

    String jsonActivity = "{title: hi mom!, etc etc}";
    Activity activity = new Activity();
    EasyMock.expect(servletRequest.getParameter("entry")).andReturn(jsonActivity);
    EasyMock.expect(converter.convertToObject(jsonActivity, Activity.class)).andReturn(activity);

    ResponseItem data = new ResponseItem<Object>(null);
    EasyMock.expect(activityService.createActivity("john.doe", activity, token)).andReturn(data);
    replay();
    return data;
  }

  public void testHandlePost() throws Exception {
    ResponseItem data = setupPostData();
    assertEquals(data, handler.handlePost(servletRequest, token));
    verify();
  }

  public void testHandlePut() throws Exception {
    ResponseItem data = setupPostData();
    assertEquals(data, handler.handlePut(servletRequest, token));
    verify();
  }

  public void testHandleDelete() throws Exception {
    replay();
    assertEquals(ResponseError.BAD_REQUEST, handler.handleDelete(servletRequest, token).getError());
    verify();
  }
}