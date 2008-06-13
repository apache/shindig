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
package org.apache.shindig.social.dataservice;

import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;
import org.apache.shindig.common.testing.FakeGadgetToken;

import com.google.common.collect.Sets;
import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;

import javax.servlet.http.HttpServletRequest;

public class PersonHandlerTest extends TestCase {
  private BeanJsonConverter converter;
  private HttpServletRequest servletRequest;
  private PersonService personService;
  private PersonHandler handler;
  private FakeGadgetToken token;

  @Override
  protected void setUp() throws Exception {
    token = new FakeGadgetToken();
    converter = EasyMock.createMock(BeanJsonConverter.class);
    servletRequest = EasyMock.createMock(HttpServletRequest.class);
    personService = EasyMock.createMock(PersonService.class);

    handler = new PersonHandler(personService, converter);
  }

  private void replay() {
    EasyMock.replay(converter);
    EasyMock.replay(servletRequest);
    EasyMock.replay(personService);
  }

  private void verify() {
    EasyMock.verify(converter);
    EasyMock.verify(servletRequest);
    EasyMock.verify(personService);
  }

  // TODO: Make super class and pull this up
  private void setPath(String path) {
    EasyMock.expect(servletRequest.getPathInfo()).andReturn(path);
  }

  public void testHandleGetAllNoParams() throws Exception {
    setPath("/people/john.doe/@all");

    EasyMock.expect(servletRequest.getParameter("orderBy")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("filterBy")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("startIndex")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("count")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("fields")).andReturn(null);

    ResponseItem<RestfulCollection<Person>> data
        = new ResponseItem<RestfulCollection<Person>>(null);
    EasyMock.expect(personService.getPeople(
        new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(GroupId.Type.all, null),
        PersonService.SortOrder.topFriends,
        PersonService.FilterType.all, 0, 20,
        Sets.newHashSet(Person.Field.ID.toString(),
            Person.Field.NAME.toString(),
            Person.Field.THUMBNAIL_URL.toString()),
        token)).andReturn(data);

    replay();
    assertEquals(data, handler.handleGet(servletRequest, token));
    verify();
  }

  public void testHandleGetFriendsNoParams() throws Exception {
    setPath("/people/john.doe/@friends");

    EasyMock.expect(servletRequest.getParameter("orderBy")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("filterBy")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("startIndex")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("count")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("fields")).andReturn(null);

    ResponseItem<RestfulCollection<Person>> data
        = new ResponseItem<RestfulCollection<Person>>(null);
    EasyMock.expect(personService.getPeople(
        new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(GroupId.Type.friends, null),
        PersonService.SortOrder.topFriends,
        PersonService.FilterType.all, 0, 20,
        Sets.newHashSet(Person.Field.ID.toString(),
            Person.Field.NAME.toString(),
            Person.Field.THUMBNAIL_URL.toString()),
        token)).andReturn(data);

    replay();
    assertEquals(data, handler.handleGet(servletRequest, token));
    verify();
  }

  public void testHandleGetFriendsWithParams() throws Exception {
    setPath("/people/john.doe/@friends");

    PersonService.SortOrder order = PersonService.SortOrder.name;
    PersonService.FilterType filter = PersonService.FilterType.topFriends;

    EasyMock.expect(servletRequest.getParameter("orderBy")).andReturn(order.toString());
    EasyMock.expect(servletRequest.getParameter("filterBy")).andReturn(filter.toString());
    EasyMock.expect(servletRequest.getParameter("startIndex")).andReturn("5");
    EasyMock.expect(servletRequest.getParameter("count")).andReturn("10");
    EasyMock.expect(servletRequest.getParameter("fields")).andReturn("money,fame,fortune");

    ResponseItem<RestfulCollection<Person>> data
        = new ResponseItem<RestfulCollection<Person>>(null);
    EasyMock.expect(personService.getPeople(
        new UserId(UserId.Type.userId, "john.doe"),
        new GroupId(GroupId.Type.friends, null), order,
        filter, 5, 10, Sets.newHashSet("money", "fame", "fortune"), token)).andReturn(data);

    replay();
    assertEquals(data, handler.handleGet(servletRequest, token));
    verify();
  }

  public void testHandleGetFriendById() throws Exception {
    setPath("/people/john.doe/@friends/jane.doe");

    ResponseItem<Person> data = new ResponseItem<Person>(null);
    // TODO: This isn't right! We should be passing both john.doe and jane.doe to the service
    // We probably need to either change the getPerson parameters or add a new method to
    // the interface
    EasyMock.expect(personService.getPerson(
        new UserId(UserId.Type.userId, "john.doe"),
        token)).andReturn(data);

    replay();
    assertEquals(data, handler.handleGet(servletRequest, token));
    verify();
  }

  public void testHandleGetSelf() throws Exception {
    setPath("/people/john.doe/@self");

    ResponseItem<Person> data = new ResponseItem<Person>(null);
    EasyMock.expect(personService.getPerson(
        new UserId(UserId.Type.userId, "john.doe"),
        token)).andReturn(data);

    replay();
    assertEquals(data, handler.handleGet(servletRequest, token));
    verify();
  }

  public void testHandleDelete() throws Exception {
    replay();
    assertEquals(ResponseError.BAD_REQUEST,
        handler.handleDelete(servletRequest, token).getError());
    verify();
  }

  public void testHandlePut() throws Exception {
    replay();
    assertEquals(ResponseError.NOT_IMPLEMENTED,
        handler.handlePut(servletRequest, token).getError());
    verify();
  }

  public void testHandlePost() throws Exception {
    replay();
    assertEquals(ResponseError.NOT_IMPLEMENTED,
        handler.handlePost(servletRequest, token).getError());
    verify();
  }
}