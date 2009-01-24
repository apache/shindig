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
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.core.model.PersonImpl;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.RestfulCollection;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.easymock.classextension.EasyMock;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PersonHandlerTest extends EasyMockTestCase {
  private PersonService personService;
  private PersonHandler handler;
  private FakeGadgetToken token;
  private RestfulRequestItem request;

  private static final Set<String> DEFAULT_FIELDS = Sets.newHashSet(Person.Field.ID.toString(),
      Person.Field.NAME.toString(),
      Person.Field.THUMBNAIL_URL.toString());

  private static final Set<UserId> JOHN_DOE = Sets
      .newHashSet(new UserId(UserId.Type.userId, "john.doe"));

  private static CollectionOptions DEFAULT_OPTIONS = new CollectionOptions();

  static {
    DEFAULT_OPTIONS.setSortBy(PersonService.TOP_FRIENDS_SORT);
    DEFAULT_OPTIONS.setSortOrder(PersonService.SortOrder.ascending);
    DEFAULT_OPTIONS.setFilter(null);
    DEFAULT_OPTIONS.setFilterOperation(PersonService.FilterOperation.contains);
    DEFAULT_OPTIONS.setFilterValue("");
    DEFAULT_OPTIONS.setFirst(0);
    DEFAULT_OPTIONS.setMax(20);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    token = new FakeGadgetToken();
    personService = EasyMock.createMock(PersonService.class);
    handler = new PersonHandler(personService);
  }

  @Override
  protected void replay() {
    EasyMock.replay(personService);
  }

  @Override
  protected void verify() {
    EasyMock.verify(personService);
  }

  private void setPath(String path) {
    Map<String, String> params = Maps.newHashMap();
    params.put("sortBy", null);
    params.put("sortOrder", null);
    params.put("filterBy", null);
    params.put("startIndex", null);
    params.put("count", null);
    params.put("fields", null);
    this.setPathAndParams(path, params);
  }

  private void setPathAndParams(String path, Map<String, String> params) {
    request = new RestfulRequestItem(path, "GET", null, token, null);
    for (Map.Entry<String, String> entry : params.entrySet()) {
      request.setParameter(entry.getKey(), entry.getValue());
    }
  }

  public void testHandleGetAllNoParams() throws Exception {
    setPath("/people/john.doe/@all");

    List<Person> personList = ImmutableList.of();
    RestfulCollection<Person> data = new RestfulCollection<Person>(personList);

    EasyMock.expect(personService.getPeople(
        JOHN_DOE,
        new GroupId(GroupId.Type.all, null),
        DEFAULT_OPTIONS,
        DEFAULT_FIELDS,
        token))
        .andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleGetFriendsNoParams() throws Exception {
    setPath("/people/john.doe/@friends");

    List<Person> personList = ImmutableList.of();
    RestfulCollection<Person> data = new RestfulCollection<Person>(personList);
    EasyMock.expect(personService.getPeople(
        JOHN_DOE,
        new GroupId(GroupId.Type.friends, null),
        DEFAULT_OPTIONS,
        DEFAULT_FIELDS,
        token))
        .andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleGetFriendsWithParams() throws Exception {
    CollectionOptions options = new CollectionOptions();
    options.setSortBy(Person.Field.NAME.toString());
    options.setSortOrder(PersonService.SortOrder.descending);
    options.setFilter(PersonService.TOP_FRIENDS_FILTER);
    options.setFilterOperation(PersonService.FilterOperation.present);
    options.setFilterValue("cassie");
    options.setFirst(5);
    options.setMax(10);

    Map<String, String> params = Maps.newHashMap();
    params.put("sortBy", options.getSortBy());
    params.put("sortOrder", options.getSortOrder().toString());
    params.put("filterBy", options.getFilter());
    params.put("filterOp", options.getFilterOperation().toString());
    params.put("filterValue", options.getFilterValue());
    params.put("startIndex", "5");
    params.put("count", "10");
    params.put("fields", "money,fame,fortune");

    setPathAndParams("/people/john.doe/@friends", params);

    List<Person> people = ImmutableList.of();
    RestfulCollection<Person> data = new RestfulCollection<Person>(people);
    EasyMock.expect(personService.getPeople(
        JOHN_DOE,
        new GroupId(GroupId.Type.friends, null), options,
        ImmutableSortedSet.of("money", "fame", "fortune"), token))
        .andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleGetFriendById() throws Exception {
    setPath("/people/john.doe/@friends/jane.doe");

    Person data = new PersonImpl();
    // TODO: We aren't passing john.doe to the service yet.
    EasyMock.expect(personService.getPerson(new UserId(UserId.Type.userId, "jane.doe"),
        DEFAULT_FIELDS, token)).andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleGetSelf() throws Exception {
    setPath("/people/john.doe/@self");

    Person data = new PersonImpl();
    EasyMock.expect(personService.getPerson(JOHN_DOE.iterator().next(),
        DEFAULT_FIELDS, token)).andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleGetPlural() throws Exception {
    setPath("/people/john.doe,jane.doe/@self");

    List<Person> people = ImmutableList.of();
    RestfulCollection<Person> data = new RestfulCollection<Person>(people);
    Set<UserId> userIdSet = Sets.newLinkedHashSet(JOHN_DOE);
    userIdSet.add(new UserId(UserId.Type.userId, "jane.doe"));
    EasyMock.expect(personService.getPeople(userIdSet,
        new GroupId(GroupId.Type.self, null),
        DEFAULT_OPTIONS,
        DEFAULT_FIELDS,
        token)).andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleDelete() throws Exception {
    replay();
    try {
      handler.handleDelete(request);
      fail();
    } catch (SocialSpiException spe) {
      assertEquals(ResponseError.BAD_REQUEST, spe.getError());
    }

    verify();
  }

  public void testHandlePut() throws Exception {
    replay();

    try {
      handler.handlePut(request).get();
      fail();
    } catch (SocialSpiException spe) {
      assertEquals(ResponseError.NOT_IMPLEMENTED, spe.getError());
    }

    verify();
  }

  public void testHandlePost() throws Exception {
    replay();

    try {
      handler.handlePost(request).get();
      fail();
    } catch (SocialSpiException spe) {
      assertEquals(ResponseError.NOT_IMPLEMENTED, spe.getError());
    }

    verify();
  }
}
