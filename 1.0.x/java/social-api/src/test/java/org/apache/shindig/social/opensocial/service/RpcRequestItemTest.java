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
import org.apache.shindig.social.core.util.BeanJsonConverter;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Guice;

import org.json.JSONObject;

import junit.framework.TestCase;

import java.util.Collections;

public class RpcRequestItemTest extends TestCase {

  private static final FakeGadgetToken FAKE_TOKEN = new FakeGadgetToken();

  private RpcRequestItem request;

  private JSONObject baseRpc;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    baseRpc = new JSONObject(
        "{method:people.get,id:id,params:{"
            + "userId:john.doe,"
            + "groupId:@self,"
            + "fields:[huey,dewey,louie]"
            + "}}");
    request = new RpcRequestItem(baseRpc, FAKE_TOKEN, new BeanJsonConverter(Guice.createInjector()));
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

  public void testSortBy() throws Exception {
    request.setParameter("sortBy", null);
    assertEquals("topFriends", request.getSortBy());

    request.setParameter("sortBy", "name");
    assertEquals("name", request.getSortBy());
  }

  public void testSortOrder() throws Exception {
    request.setParameter("sortOrder", null);
    assertEquals(PersonService.SortOrder.ascending, request.getSortOrder());

    request.setParameter("sortOrder", "descending");
    assertEquals(PersonService.SortOrder.descending, request.getSortOrder());
  }

  public void testFilterBy() throws Exception {
    request.setParameter("filterBy", null);
    assertEquals(null, request.getFilterBy());

    request.setParameter("filterBy", "hasApp");
    assertEquals("hasApp", request.getFilterBy());
  }

  public void testFilterOperation() throws Exception {
    request.setParameter("filterOp", null);
    assertEquals(PersonService.FilterOperation.contains, request.getFilterOperation());

    request.setParameter("filterOp", "equals");
    assertEquals(PersonService.FilterOperation.equals, request.getFilterOperation());
  }

  public void testFilterValue() throws Exception {
    request.setParameter("filterValue", null);
    assertEquals("", request.getFilterValue());

    request.setParameter("filterValue", "cassie");
    assertEquals("cassie", request.getFilterValue());
  }

  public void testFields() throws Exception {
    request.setListParameter("fields", Collections.<String>emptyList());
    assertEquals(Sets.<String>newHashSet(), request.getFields());

    request.setListParameter("fields", Lists.newArrayList("happy", "sad", "grumpy"));
    assertEquals(Sets.newHashSet("happy", "sad", "grumpy"), request.getFields());
  }
  
  public static class InputData {
    String name;
    int id;
    
    public void setName(String name) {
      this.name = name;
    }
    
    public void setId(int id) {
      this.id = id;
    }
  }
  
  public void testGetTypedParameter() throws Exception {
    JSONObject obj = new JSONObject();
    obj.put("name", "Bob");
    obj.put("id", "1234");
    
    request.setJsonParameter("tp", obj);
    
    InputData input = request.getTypedParameter("tp", InputData.class);
    assertEquals("Bob", input.name);
    assertEquals(1234, input.id);
  }
  
  public void testGetTypedParameters() throws Exception {
    request.setParameter("name", "Bob");
    request.setParameter("id", "1234");
    
    InputData input = request.getTypedParameters(InputData.class);
    assertEquals("Bob", input.name);
    assertEquals(1234, input.id);
  }
}
