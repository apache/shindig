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

import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

public class AppDataHandlerTest extends TestCase {
  private BeanJsonConverter converter;
  private HttpServletRequest servletRequest;
  private AppDataService appDataService;
  private AppDataHandler handler;
  private FakeGadgetToken token;


  @Override
  protected void setUp() throws Exception {
    token = new FakeGadgetToken();
    converter = EasyMock.createMock(BeanJsonConverter.class);
    servletRequest = EasyMock.createMock(HttpServletRequest.class);
    appDataService = EasyMock.createMock(AppDataService.class);

    handler = new AppDataHandler(appDataService);
    handler.setConverter(converter);
  }

  private void replay() {
    EasyMock.replay(converter);
    EasyMock.replay(servletRequest);
    EasyMock.replay(appDataService);
  }

  private void verify() {
    EasyMock.verify(converter);
    EasyMock.verify(servletRequest);
    EasyMock.verify(appDataService);
  }

  private void setPath(String path) {
    EasyMock.expect(servletRequest.getPathInfo()).andReturn(path);
  }

  private void assertHandleGetForGroup(GroupId.Type group) {
    setPath("/activities/john.doe/@" + group.toString() + "/appId");
    EasyMock.expect(servletRequest.getParameter("fields")).andReturn(null);

    ResponseItem<DataCollection> data = new ResponseItem<DataCollection>(null);
    EasyMock.expect(appDataService.getPersonData(new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(group, null),
        Lists.<String>newArrayList(), "appId", token)).andReturn(data);

    replay();
    assertEquals(data, handler.handleGet(servletRequest, token));
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

  public void testHandleGetWithoutFields() throws Exception {
    setPath("/appData/john.doe/@friends/appId");
    EasyMock.expect(servletRequest.getParameter("fields")).andReturn("pandas");

    ResponseItem<DataCollection> data = new ResponseItem<DataCollection>(null);
    EasyMock.expect(appDataService.getPersonData(new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(GroupId.Type.friends, null),
        Lists.newArrayList("pandas"), "appId", token)).andReturn(data);

    replay();
    assertEquals(data, handler.handleGet(servletRequest, token));
    verify();
  }

  private ResponseItem setupPostData() {
    setPath("/appData/john.doe/@self/appId");
    EasyMock.expect(servletRequest.getParameter("fields")).andReturn("pandas");

    String jsonAppData = "{pandas: 'are fuzzy'}";
    HashMap<String, String> values = Maps.newHashMap();
    EasyMock.expect(servletRequest.getParameter("entry")).andReturn(jsonAppData);
    EasyMock.expect(converter.convertToObject(jsonAppData, HashMap.class)).andReturn(values);

    ResponseItem data = new ResponseItem<Object>(null);
    EasyMock.expect(appDataService.updatePersonData(new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(GroupId.Type.self, null),
        Lists.newArrayList("pandas"), values, "appId", token)).andReturn(data);
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
    setPath("/appData/john.doe/@self/appId");
    EasyMock.expect(servletRequest.getParameter("fields")).andReturn("pandas");

    ResponseItem data = new ResponseItem<Object>(null);
    EasyMock.expect(appDataService.deletePersonData(new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(GroupId.Type.self, null),
        Lists.newArrayList("pandas"), "appId", token)).andReturn(data);

    replay();
    assertEquals(data, handler.handleDelete(servletRequest, token));
    verify();
  }
}