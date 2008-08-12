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

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Sets;

import junit.framework.TestCase;

public class RequestItemTest extends TestCase {

  public void testParseUrl() throws Exception {
    String path = "/people/john.doe/@self";

    RequestItem request = new RequestItem();
    request.setUrl(path + "?fields=huey,dewey,louie");

    request.putUrlParamsIntoParameters();

    assertEquals(path, request.getUrl());
    assertEquals("huey,dewey,louie", request.getParameter("fields"));

    // Try it without any params
    request = new RequestItem();
    request.setUrl(path);

    request.putUrlParamsIntoParameters();

    assertEquals(path, request.getUrl());
    assertEquals(null, request.getParameter("fields"));
  }

  public void testBasicFunctions() throws Exception {
    String url = "url";
    SecurityToken token = null;
    String method = "method";
    RequestItem request = new RequestItem();

    request.setUrl(url);
    assertEquals(url, request.getUrl());

    request.setMethod(method);
    assertEquals(method, request.getMethod());

    request.setToken(token);
    assertEquals(token, request.getToken());
  }

  public void testGetAppId() throws Exception {
    RequestItem request = new RequestItem();

    request.setParameter("appId", "100");
    assertEquals("100", request.getAppId());

    FakeGadgetToken token = new FakeGadgetToken();
    request.setToken(token);
    request.setParameter("appId", "@app");
    assertEquals(token.getAppId(), request.getAppId());
  }

  public void testGetUser() throws Exception {
    RequestItem request = new RequestItem();

    request.setParameter("userId", "@owner");
    assertEquals(UserId.Type.owner, request.getUsers().iterator().next().getType());
  }

  public void testGetGroup() throws Exception {
    RequestItem request = new RequestItem();

    request.setParameter("groupId", "@self");
    assertEquals(GroupId.Type.self, request.getGroup().getType());
  }

  public void testStartIndex() throws Exception {
    RequestItem request = new RequestItem();

    request.setParameter("startIndex", null);
    assertEquals(0, request.getStartIndex());

    request.setParameter("startIndex", "5");
    assertEquals(5, request.getStartIndex());
  }

  public void testCount() throws Exception {
    RequestItem request = new RequestItem();

    request.setParameter("count", null);
    assertEquals(20, request.getCount());

    request.setParameter("count", "5");
    assertEquals(5, request.getCount());
  }

  public void testOrderBy() throws Exception {
    RequestItem request = new RequestItem();

    request.setParameter("orderBy", null);
    assertEquals(PersonService.SortOrder.topFriends, request.getOrderBy());

    request.setParameter("orderBy", "name");
    assertEquals(PersonService.SortOrder.name, request.getOrderBy());
  }

  public void testFilterBy() throws Exception {
    RequestItem request = new RequestItem();

    request.setParameter("filterBy", null);
    assertEquals(PersonService.FilterType.all, request.getFilterBy());

    request.setParameter("filterBy", "hasApp");
    assertEquals(PersonService.FilterType.hasApp, request.getFilterBy());
  }

  public void testFields() throws Exception {
    RequestItem request = new RequestItem();

    request.setParameter("fields", null);
    assertEquals(Sets.<String>newHashSet(), request.getFields());

    request.setParameter("fields", "happy,sad,grumpy");
    assertEquals(Sets.newHashSet("happy", "sad", "grumpy"), request.getFields());
  }

}