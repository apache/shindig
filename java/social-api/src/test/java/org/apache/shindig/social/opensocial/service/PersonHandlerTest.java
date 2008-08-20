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
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.RestfulCollection;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.easymock.classextension.EasyMock;

import junit.framework.TestCase;

import java.util.Map;
import java.util.Set;

public class PersonHandlerTest extends TestCase {

  private PersonService personService;

  private PersonHandler handler;

  private FakeGadgetToken token;

  private RestfulRequestItem request;

  private static final Set<String> DEFAULT_FIELDS = Sets.newHashSet(Person.Field.ID.toString(),
      Person.Field.NAME.toString(),
      Person.Field.THUMBNAIL_URL.toString());

  private static final Set<UserId> JOHN_DOE = Sets
      .newHashSet(new UserId(UserId.Type.userId, "john.doe"));

  @Override
  protected void setUp() throws Exception {
    token = new FakeGadgetToken();
    personService = EasyMock.createMock(PersonService.class);

    handler = new PersonHandler(personService);
  }

  private void replay() {
    EasyMock.replay(personService);
  }

  private void verify() {
    EasyMock.verify(personService);
  }

  private void setPath(String path) {
    Map<String, String> params = Maps.newHashMap();
    params.put("orderBy", null);
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

    ResponseItem<RestfulCollection<Person>> data
        = new ResponseItem<RestfulCollection<Person>>(null);
    EasyMock.expect(personService.getPeople(
        JOHN_DOE,
        new GroupId(GroupId.Type.all, null),
        PersonService.SortOrder.topFriends,
        PersonService.FilterType.all, 0, 20,
        DEFAULT_FIELDS,
        token))
        .andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleGetFriendsNoParams() throws Exception {
    setPath("/people/john.doe/@friends");

    ResponseItem<RestfulCollection<Person>> data
        = new ResponseItem<RestfulCollection<Person>>(null);
    EasyMock.expect(personService.getPeople(
        JOHN_DOE,
        new GroupId(GroupId.Type.friends, null),
        PersonService.SortOrder.topFriends,
        PersonService.FilterType.all, 0, 20,
        DEFAULT_FIELDS,
        token))
        .andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleGetFriendsWithParams() throws Exception {
    PersonService.SortOrder order = PersonService.SortOrder.name;
    PersonService.FilterType filter = PersonService.FilterType.topFriends;

    Map<String, String> params = Maps.newHashMap();
    params.put("orderBy", order.toString());
    params.put("filterBy", filter.toString());
    params.put("startIndex", "5");
    params.put("count", "10");
    params.put("fields", "money,fame,fortune");

    setPathAndParams("/people/john.doe/@friends", params);

    ResponseItem<RestfulCollection<Person>> data
        = new ResponseItem<RestfulCollection<Person>>(null);
    EasyMock.expect(personService.getPeople(
        JOHN_DOE,
        new GroupId(GroupId.Type.friends, null), order,
        filter, 5, 10, Sets.newLinkedHashSet("money", "fame", "fortune"), token))
        .andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleGetFriendById() throws Exception {
    setPath("/people/john.doe/@friends/jane.doe");

    ResponseItem<Person> data = new ResponseItem<Person>(null);
    // TODO: We aren't passing john.doe to the service yet.
    EasyMock.expect(personService.getPerson(new UserId(UserId.Type.userId, "jane.doe"),
        DEFAULT_FIELDS, token)).andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleGetSelf() throws Exception {
    setPath("/people/john.doe/@self");

    ResponseItem<Person> data = new ResponseItem<Person>(null);
    EasyMock.expect(personService.getPerson(JOHN_DOE.iterator().next(),
        DEFAULT_FIELDS, token)).andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleGetPlural() throws Exception {
    setPath("/people/john.doe,jane.doe/@self");

    ResponseItem<RestfulCollection<Person>> data
        = new ResponseItem<RestfulCollection<Person>>(null);
    Set<UserId> userIdSet = Sets.newLinkedHashSet(JOHN_DOE);
    userIdSet.add(new UserId(UserId.Type.userId, "jane.doe"));
    EasyMock.expect(personService.getPeople(userIdSet,
        new GroupId(GroupId.Type.self, null),
        PersonService.SortOrder.topFriends,
        PersonService.FilterType.all, 0, 20,
        DEFAULT_FIELDS,
        token)).andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, handler.handleGet(request).get());
    verify();
  }

  public void testHandleDelete() throws Exception {
    replay();
    assertEquals(ResponseError.BAD_REQUEST, handler.handleDelete(request).get().getError());
    verify();
  }

  public void testHandlePut() throws Exception {
    replay();
    assertEquals(ResponseError.NOT_IMPLEMENTED, handler.handlePut(request).get().getError());
    verify();
  }

  public void testHandlePost() throws Exception {
    replay();
    assertEquals(ResponseError.NOT_IMPLEMENTED, handler.handlePost(request).get().getError());
    verify();
  }
}
