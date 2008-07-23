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
import org.apache.shindig.social.core.util.BeanJsonConverter;
import org.apache.shindig.social.opensocial.service.AppDataHandler;
import org.apache.shindig.social.opensocial.service.RequestItem;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.DataCollection;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;

import java.util.HashMap;
import java.util.Map;

public class AppDataHandlerTest extends TestCase {
  private BeanJsonConverter converter;
  private AppDataService appDataService;
  private AppDataHandler handler;
  private FakeGadgetToken token;
  private RequestItem request;


  @Override
  protected void setUp() throws Exception {
    token = new FakeGadgetToken();
    converter = EasyMock.createMock(BeanJsonConverter.class);
    appDataService = EasyMock.createMock(AppDataService.class);

    handler = new AppDataHandler(appDataService);
  }

  private void replay() {
    EasyMock.replay(converter);
    EasyMock.replay(appDataService);
  }

  private void verify() {
    EasyMock.verify(converter);
    EasyMock.verify(appDataService);
  }

  private void setPath(String path) {
    Map<String, String> params = Maps.newHashMap();
    params.put("fields", null);
    this.setPathAndParams(path, params);
  }

  private void setPathAndParams(String path, Map<String, String> params) {
    setPathAndParams(path, params, null);
  }

  private void setPathAndParams(String path, Map<String, String> params, String postData) {
    request = new RequestItem();
    request.setUrl(path);
    request.setParameters(params);
    request.setToken(token);
    request.setConverter(converter);
    request.setPostData(postData);
  }

  private void assertHandleGetForGroup(GroupId.Type group) throws Exception {
    setPath("/activities/john.doe/@" + group.toString() + "/appId");

    ResponseItem<DataCollection> data = new ResponseItem<DataCollection>(null);
    EasyMock.expect(appDataService.getPersonData(new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(group, null),
        "appId", Sets.<String>newHashSet(), token)).andReturn(ImmediateFuture.newInstance(data));

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

  public void testHandleGetWithoutFields() throws Exception {
    Map<String, String> params = Maps.newHashMap();
    params.put("fields", "pandas");
    setPathAndParams("/appData/john.doe/@friends/appId", params);

    ResponseItem<DataCollection> data = new ResponseItem<DataCollection>(null);
    EasyMock.expect(appDataService.getPersonData(new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(GroupId.Type.friends, null),
        "appId", Sets.newHashSet("pandas"), token)).andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  private ResponseItem<Object> setupPostData() {
    String jsonAppData = "{pandas: 'are fuzzy'}";

    Map<String, String> params = Maps.newHashMap();
    params.put("fields", "pandas");
    setPathAndParams("/appData/john.doe/@self/appId", params, jsonAppData);

    HashMap<String, String> values = Maps.newHashMap();
    EasyMock.expect(converter.convertToObject(jsonAppData, HashMap.class)).andReturn(values);

    ResponseItem<Object> data = new ResponseItem<Object>(null);
    EasyMock.expect(appDataService.updatePersonData(new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(GroupId.Type.self, null),
        "appId", Sets.newHashSet("pandas"), values, token))
        .andReturn(ImmediateFuture.newInstance(data));
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
    Map<String, String> params = Maps.newHashMap();
    params.put("fields", "pandas");
    setPathAndParams("/appData/john.doe/@self/appId", params);

    ResponseItem<Object> data = new ResponseItem<Object>(null);
    EasyMock.expect(appDataService.deletePersonData(new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(GroupId.Type.self, null),
        "appId", Sets.newHashSet("pandas"), token))
        .andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleDelete(request).get());
    verify();
  }
}