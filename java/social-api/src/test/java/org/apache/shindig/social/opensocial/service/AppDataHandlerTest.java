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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import junit.framework.TestCase;

import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.core.util.BeanJsonConverter;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.DataCollection;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.easymock.classextension.EasyMock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AppDataHandlerTest extends TestCase {

  private BeanJsonConverter converter;

  private AppDataService appDataService;

  private AppDataHandler handler;

  private FakeGadgetToken token;

  private RestfulRequestItem request;


  private static final Set<UserId> JOHN_DOE = Collections.unmodifiableSet(Sets
      .newHashSet(new UserId(UserId.Type.userId, "john.doe")));


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
    request = new RestfulRequestItem(path, "GET", postData, token, converter);
    for (Map.Entry<String, String> entry : params.entrySet()) {
      request.setParameter(entry.getKey(), entry.getValue());
    }
  }

  private void assertHandleGetForGroup(GroupId.Type group) throws Exception {
    setPath("/activities/john.doe/@" + group.toString() + "/appId");

    DataCollection data = new DataCollection(null);
    EasyMock.expect(appDataService.getPersonData(JOHN_DOE,
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

  public void testHandleGetPlural() throws Exception {
    setPath("/activities/john.doe,jane.doe/@self/appId");

    DataCollection data = new DataCollection(null);
    Set<UserId> userIdSet = Sets.newLinkedHashSet(JOHN_DOE);
    userIdSet.add(new UserId(UserId.Type.userId, "jane.doe"));
    EasyMock.expect(appDataService.getPersonData(userIdSet,
        new GroupId(GroupId.Type.self, null),
        "appId", Sets.<String>newHashSet(), token)).andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleGetWithoutFields() throws Exception {
    Map<String, String> params = Maps.newHashMap();
    params.put("fields", "pandas");
    setPathAndParams("/appData/john.doe/@friends/appId", params);

    DataCollection data = new DataCollection(null);
    EasyMock.expect(appDataService.getPersonData(JOHN_DOE,
        new GroupId(GroupId.Type.friends, null),
        "appId", Sets.newHashSet("pandas"), token)).andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  private void setupPostData() throws SocialSpiException {
    String jsonAppData = "{pandas: 'are fuzzy'}";

    Map<String, String> params = Maps.newHashMap();
    params.put("fields", "pandas");
    setPathAndParams("/appData/john.doe/@self/appId", params, jsonAppData);

    HashMap<String, String> values = Maps.newHashMap();
    EasyMock.expect(converter.convertToObject(jsonAppData, HashMap.class)).andReturn(values);

    EasyMock.expect(appDataService.updatePersonData(JOHN_DOE.iterator().next(),
        new GroupId(GroupId.Type.self, null),
        "appId", Sets.newHashSet("pandas"), values, token))
        .andReturn(ImmediateFuture.newInstance((Void) null));
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
    Map<String, String> params = Maps.newHashMap();
    params.put("fields", "pandas");
    setPathAndParams("/appData/john.doe/@self/appId", params);

    EasyMock.expect(appDataService.deletePersonData(JOHN_DOE.iterator().next(),
        new GroupId(GroupId.Type.self, null),
        "appId", Sets.newHashSet("pandas"), token))
        .andReturn(ImmediateFuture.newInstance((Void) null));

    replay();
    assertNull(handler.handleDelete(request).get());
    verify();
  }
}
