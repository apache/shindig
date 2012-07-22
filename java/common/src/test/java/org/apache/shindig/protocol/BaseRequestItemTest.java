/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.protocol;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.protocol.model.SortOrder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Guice;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

/**
 * Test BaseRequestItem
 */
public class BaseRequestItemTest extends Assert {

  private static final FakeGadgetToken FAKE_TOKEN = new FakeGadgetToken();

  protected BaseRequestItem request;
  protected BeanJsonConverter converter;

  @Before
  public void setUp() throws Exception {
    FAKE_TOKEN.setAppId("12345");
    FAKE_TOKEN.setOwnerId("someowner");
    FAKE_TOKEN.setViewerId("someowner");
    converter = new BeanJsonConverter(Guice.createInjector());
    request = new BaseRequestItem(
        Maps.<String,String[]>newHashMap(),
        FAKE_TOKEN, converter, converter);
  }

  @Test
  public void testParseCommaSeparatedList() throws Exception {
    request.setParameter("fields", "huey,dewey,louie");
    assertEquals(Lists.newArrayList("huey", "dewey", "louie"), request.getListParameter("fields"));
  }

  @Test
  public void testGetAppId() throws Exception {
    request.setParameter("appId", "100");
    assertEquals("100", request.getAppId());

    request.setParameter("appId", "@app");
    assertEquals(FAKE_TOKEN.getAppId(), request.getAppId());
  }

  @Test
  public void testStartIndex() throws Exception {
    request.setParameter("startIndex", null);
    assertEquals(RequestItem.DEFAULT_START_INDEX, request.getStartIndex());

    request.setParameter("startIndex", "5");
    assertEquals(5, request.getStartIndex());
  }

  @Test
  public void testCount() throws Exception {
    request.setParameter("count", null);
    assertEquals(RequestItem.DEFAULT_COUNT, request.getCount());

    request.setParameter("count", "5");
    assertEquals(5, request.getCount());
  }

  @Test
  public void testSortOrder() throws Exception {
    request.setParameter("sortOrder", null);
    assertEquals(SortOrder.ascending, request.getSortOrder());

    request.setParameter("sortOrder", "descending");
    assertEquals(SortOrder.descending, request.getSortOrder());
  }

  @Test
  public void testFields() throws Exception {
    request.setParameter("fields", "");
    assertEquals(Sets.<String>newHashSet(), request.getFields());

    request.setParameter("fields", "happy,sad,grumpy");
    assertEquals(Sets.newHashSet("happy", "sad", "grumpy"), request.getFields());
  }

  @Test
  public void testGetTypedParameter() throws Exception {
    request.setParameter("anykey", "{name: 'Bob', id: '1234'}");
    InputData input = request.getTypedParameter("anykey", InputData.class);
    assertEquals("Bob", input.name);
    assertEquals(1234, input.id);
  }

  @Test(expected = ProtocolException.class)
  public void testGetTypedParameterEmpty() throws Exception {
    request.getTypedParameter("empty", InputData.class);
  }

  @Test
  public void testGetParameters() throws Exception {
    request.setParameter("anykey", "{name: 'Bob', id: '1234'}");
    Map<String, Object> params = request.getParameters();
    assertEquals(1, params.size());
    assertTrue(params.containsKey("anykey"));
    try {
      params.put("this", "is bad");
      fail("Params should be immutable");
    } catch (UnsupportedOperationException e) {
      // As expected
    }
  }

  @Test
  public void testGetInvalidJsonTypedParameter() throws Exception {
    request.setParameter("anykey", "{name: 'Bob");
    int code = 0;
    try {
      request.getTypedParameter("anykey", InputData.class);
    } catch(ProtocolException e) {
      code = e.getCode();
    }
    assertEquals(HttpServletResponse.SC_BAD_REQUEST, code);
  }

  @Test
  public void testJSONConstructor() throws Exception {
    request = new BaseRequestItem(new JSONObject('{' +
            "userId:john.doe," +
            "groupId:@self," +
            "fields:[huey,dewey,louie]" +
        '}'), null, FAKE_TOKEN, converter, converter);
    assertEquals(Lists.newArrayList("huey", "dewey", "louie"), request.getListParameter("fields"));
  }

  @Test
  public void testAttributes() throws Exception {
    assertNull(request.getAttribute("undefined"));
    request.setAttribute("test", "value");
    assertEquals("value", request.getAttribute("test"));
    request.setAttribute("test", null);
    assertNull(request.getAttribute("undefined"));
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
