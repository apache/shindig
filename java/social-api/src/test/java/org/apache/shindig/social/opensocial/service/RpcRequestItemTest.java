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
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.json.JSONObject;

import junit.framework.TestCase;

import java.util.Collections;

public class RpcRequestItemTest extends TestCase {

  private static final FakeGadgetToken FAKE_TOKEN = new FakeGadgetToken();


  private RpcRequestItem request;

  private JSONObject baseRpc;

  protected void setUp() throws Exception {
    super.setUp();
    baseRpc = new JSONObject(
        "{method:people.get,id:id,params:{"
            + "userId:john.doe,"
            + "groupId:@self,"
            + "fields:[huey,dewey,louie]"
            + "}}");
    request = new RpcRequestItem(baseRpc, FAKE_TOKEN, null);
  }

  public void testParseMethod() throws Exception {
    assertEquals("people", request.getService());
    assertEquals(Lists.newArrayList("huey", "dewey", "louie"), request.getListParameter("fields"));

    // Try it without any params
    JSONObject noParams = new JSONObject(baseRpc.toString());
    noParams.remove("params");
    request = new RpcRequestItem(noParams, FAKE_TOKEN, null);

    assertEquals("people", request.getService());
    assertEquals(Collections.<String>emptyList(), request.getListParameter("fields"));
  }

  public void testGetAppId() throws Exception {
    request.setParameter("appId", "100");
    assertEquals("100", request.getAppId());

    request.setParameter("appId", "@app");
    assertEquals(FAKE_TOKEN.getAppId(), request.getAppId());
  }

  public void testGetUser() throws Exception {
    request.setParameter("userId", "@owner");
    assertEquals(UserId.Type.owner, request.getUsers().iterator().next().getType());
  }

  public void testGetGroup() throws Exception {
    request.setParameter("groupId", "@self");
    assertEquals(GroupId.Type.self, request.getGroup().getType());
  }

  public void testStartIndex() throws Exception {
    request.setParameter("startIndex", null);
    assertEquals(0, request.getStartIndex());

    request.setParameter("startIndex", "5");
    assertEquals(5, request.getStartIndex());
  }

  public void testCount() throws Exception {
    request.setParameter("count", null);
    assertEquals(20, request.getCount());

    request.setParameter("count", "5");
    assertEquals(5, request.getCount());
  }

  public void testOrderBy() throws Exception {
    request.setParameter("orderBy", null);
    assertEquals(PersonService.SortOrder.topFriends, request.getOrderBy());

    request.setParameter("orderBy", "name");
    assertEquals(PersonService.SortOrder.name, request.getOrderBy());
  }

  public void testFilterBy() throws Exception {
    request.setParameter("filterBy", null);
    assertEquals(PersonService.FilterType.all, request.getFilterBy());

    request.setParameter("filterBy", "hasApp");
    assertEquals(PersonService.FilterType.hasApp, request.getFilterBy());
  }

  public void testFields() throws Exception {
    request.setListParameter("fields", Collections.<String>emptyList());
    assertEquals(Sets.<String>newHashSet(), request.getFields());

    request.setListParameter("fields", Lists.newArrayList("happy","sad","grumpy"));
    assertEquals(Sets.newHashSet("happy", "sad", "grumpy"), request.getFields());
  }
}
