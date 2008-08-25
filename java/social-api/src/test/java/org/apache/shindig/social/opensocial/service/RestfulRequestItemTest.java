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
import org.apache.shindig.social.core.oauth.AuthenticationServletFilter;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.easymock.classextension.EasyMock;

import junit.framework.TestCase;

import java.util.Collections;

public class RestfulRequestItemTest extends TestCase {

  private static final FakeGadgetToken FAKE_TOKEN = new FakeGadgetToken();

  private static final String DEFAULT_PATH = "/people/john.doe/@self";

  private RestfulRequestItem request;

  protected void setUp() throws Exception {
    super.setUp();
    request = new RestfulRequestItem(
        DEFAULT_PATH + "?fields=huey,dewey,louie", "GET",
        null, FAKE_TOKEN, null);
  }

  public void testParseUrl() throws Exception {
    assertEquals("people", request.getService());
    assertEquals(Lists.newArrayList("huey", "dewey", "louie"), request.getListParameter("fields"));

    // Try it without any params
    request = new RestfulRequestItem(DEFAULT_PATH, "GET", null, null, null);

    assertEquals("people", request.getService());
    assertEquals(null, request.getParameters().get("fields"));
  }

  public void testGetHttpMethodFromParameter() throws Exception {
    AuthenticationServletFilter.SecurityTokenRequest overridden =
        EasyMock.createMock(AuthenticationServletFilter.SecurityTokenRequest.class);
    EasyMock.expect(overridden.getParameter(RestfulRequestItem.X_HTTP_METHOD_OVERRIDE))
        .andReturn("DELETE");
    EasyMock.replay(overridden);
    assertEquals("DELETE", RestfulRequestItem.getMethod(overridden));
    EasyMock.verify(overridden);
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
    assertEquals(PersonService.SortOrder.topFriends, request.getSortBy());

    request.setParameter("sortBy", "name");
    assertEquals(PersonService.SortOrder.name, request.getSortBy());
  }

  public void testSortOrder() throws Exception {
    request.setParameter("sortOrder", null);
    assertEquals(PersonService.SortDirection.ascending, request.getSortOrder());

    request.setParameter("sortOrder", "descending");
    assertEquals(PersonService.SortDirection.descending, request.getSortOrder());
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

    request.setParameter("fields", "happy,sad,grumpy");
    assertEquals(Sets.newHashSet("happy", "sad", "grumpy"), request.getFields());
  }

  public void testRouteFromParameter() throws Exception {
    assertEquals("path", RestfulRequestItem.getServiceFromPath("/path"));
    assertEquals("path", RestfulRequestItem.getServiceFromPath("/path/fun"));
    assertEquals("path", RestfulRequestItem.getServiceFromPath("/path/fun/yes"));
  }
}
