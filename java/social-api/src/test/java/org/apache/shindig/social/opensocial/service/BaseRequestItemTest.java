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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import junit.framework.TestCase;
import org.json.JSONObject;

/**
 * Test BaseRequestItem
 */
public class BaseRequestItemTest extends TestCase {

  private static final FakeGadgetToken FAKE_TOKEN = new FakeGadgetToken();

  private static final String DEFAULT_PATH = "/people/john.doe/@self";
  protected BaseRequestItem request;
  protected BeanJsonConverter converter;

  @Override protected void setUp() throws Exception {
    FAKE_TOKEN.setAppId("12345");
    FAKE_TOKEN.setOwnerId("someowner");
    FAKE_TOKEN.setViewerId("someowner");
    converter = new BeanJsonConverter(Guice.createInjector());
    request = new BaseRequestItem(
        Maps.<String,String[]>newHashMap(),
        FAKE_TOKEN, converter);
  }


  public void testParseCommaSeparatedList() throws Exception {
    request.setParameter("fields", "huey,dewey,louie");
    assertEquals(Lists.newArrayList("huey", "dewey", "louie"), request.getListParameter("fields"));
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
    assertEquals(RequestItem.DEFAULT_START_INDEX, request.getStartIndex());

    request.setParameter("startIndex", "5");
    assertEquals(5, request.getStartIndex());
  }

  public void testCount() throws Exception {
    request.setParameter("count", null);
    assertEquals(RequestItem.DEFAULT_COUNT, request.getCount());

    request.setParameter("count", "5");
    assertEquals(5, request.getCount());
  }

  public void testSortOrder() throws Exception {
    request.setParameter("sortOrder", null);
    assertEquals(PersonService.SortOrder.ascending, request.getSortOrder());

    request.setParameter("sortOrder", "descending");
    assertEquals(PersonService.SortOrder.descending, request.getSortOrder());
  }

  public void testFields() throws Exception {
    request.setParameter("fields", "");
    assertEquals(Sets.<String>newHashSet(), request.getFields());

    request.setParameter("fields", "happy,sad,grumpy");
    assertEquals(Sets.newHashSet("happy", "sad", "grumpy"), request.getFields());
  }

  public void testGetTypedParameter() throws Exception {
    request.setParameter("anykey", "{name: 'Bob', id: '1234'}");
    InputData input = request.getTypedParameter("anykey", InputData.class);
    assertEquals("Bob", input.name);
    assertEquals(1234, input.id);
  }

  public void testJSONConstructor() throws Exception {
    request = new BaseRequestItem(new JSONObject("{" +
            "userId:john.doe," +
            "groupId:@self," +
            "fields:[huey,dewey,louie]" +
            "}"), FAKE_TOKEN, converter);
    assertEquals(Lists.newArrayList("huey", "dewey", "louie"), request.getListParameter("fields"));
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
}

