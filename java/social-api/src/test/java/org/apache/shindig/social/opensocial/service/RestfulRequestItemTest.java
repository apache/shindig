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

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;

import java.util.Map;

public class RestfulRequestItemTest extends TestCase {

  public void testParseUrl() throws Exception {
    String path = "/people/john.doe/@self";

    RestfulRequestItem request = new RestfulRequestItem();
    request.setUrl(path + "?fields=huey,dewey,louie");

    request.putUrlParamsIntoParameters();

    assertEquals(path, request.getUrl());
    assertEquals("huey,dewey,louie", request.getParameters().get("fields"));

    // Try it without any params
    request = new RestfulRequestItem();
    request.setUrl(path);

    request.putUrlParamsIntoParameters();

    assertEquals(path, request.getUrl());
    assertEquals(null, request.getParameters().get("fields"));
  }

  public void testBasicFunctions() throws Exception {
    String url = "url";
    Map<String, String> params = Maps.newHashMap();
    SecurityToken token = null;
    String method = "method";
    RestfulRequestItem request = new RestfulRequestItem();

    request.setUrl(url);
    assertEquals(url, request.getUrl());

    request.setParameters(params);
    assertEquals(params, request.getParameters());

    request.setMethod(method);
    assertEquals(method, request.getMethod());

    request.setToken(token);
    assertEquals(token, request.getToken());
  }

  public void testGetAppId() throws Exception {
    RestfulRequestItem request = new RestfulRequestItem();

    request.setParameters(Maps.immutableMap("appId", "100"));
    assertEquals("100", request.getAppId());

    FakeGadgetToken token = new FakeGadgetToken();
    request.setToken(token);
    request.setParameters(Maps.immutableMap("appId", "@app"));
    assertEquals(token.getAppId(), request.getAppId());
  }

  public void testGetUser() throws Exception {
    RestfulRequestItem request = new RestfulRequestItem();

    request.setParameters(Maps.immutableMap("userId", "@owner"));
    assertEquals(UserId.Type.owner, request.getUser().getType());
  }

  public void testGetGroup() throws Exception {
    RestfulRequestItem request = new RestfulRequestItem();

    request.setParameters(Maps.immutableMap("groupId", "@self"));
    assertEquals(GroupId.Type.self, request.getGroup().getType());
  }

  public void testStartIndex() throws Exception {
    RestfulRequestItem request = new RestfulRequestItem();

    request.setParameters(Maps.<String, String>immutableMap("startIndex", null));
    assertEquals(0, request.getStartIndex());

    request.setParameters(Maps.immutableMap("startIndex", "5"));
    assertEquals(5, request.getStartIndex());
  }

  public void testCount() throws Exception {
    RestfulRequestItem request = new RestfulRequestItem();

    request.setParameters(Maps.<String, String>immutableMap("count", null));
    assertEquals(20, request.getCount());

    request.setParameters(Maps.immutableMap("count", "5"));
    assertEquals(5, request.getCount());
  }

  public void testOrderBy() throws Exception {
    RestfulRequestItem request = new RestfulRequestItem();

    request.setParameters(Maps.<String, String>immutableMap("orderBy", null));
    assertEquals(PersonService.SortOrder.topFriends, request.getOrderBy());

    request.setParameters(Maps.immutableMap("orderBy", "name"));
    assertEquals(PersonService.SortOrder.name, request.getOrderBy());
  }

  public void testFilterBy() throws Exception {
    RestfulRequestItem request = new RestfulRequestItem();

    request.setParameters(Maps.<String, String>immutableMap("filterBy", null));
    assertEquals(PersonService.FilterType.all, request.getFilterBy());

    request.setParameters(Maps.immutableMap("filterBy", "hasApp"));
    assertEquals(PersonService.FilterType.hasApp, request.getFilterBy());
  }

  public void testFields() throws Exception {
    RestfulRequestItem request = new RestfulRequestItem();

    request.setParameters(Maps.<String, String>immutableMap("fields", null));
    assertEquals(Sets.<String>newHashSet(), request.getFields());

    request.setParameters(Maps.immutableMap("fields", "happy,sad,grumpy"));
    assertEquals(Sets.newHashSet("happy", "sad", "grumpy"), request.getFields());
  }

  public void testRouteFromParameter() throws Exception {
    assertEquals("path", RestfulRequestItem.getServiceFromPath("/path"));
    assertEquals("path", RestfulRequestItem.getServiceFromPath("/path/fun"));
    assertEquals("path", RestfulRequestItem.getServiceFromPath("/path/fun/yes"));
  }

}