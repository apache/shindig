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

  @Override
  protected void setUp() throws Exception {
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

  public void testHandleGetAllNoParams() throws Exception {
    EasyMock.expect(servletRequest.getPathInfo()).andReturn("/people/john.doe/@all");

    EasyMock.expect(servletRequest.getParameter("orderBy")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("filterBy")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("startIndex")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("count")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("fields")).andReturn(null);

    FakeGadgetToken token = new FakeGadgetToken();
    ResponseItem<RestfulCollection<Person>> data
        = new ResponseItem<RestfulCollection<Person>>(null);
    EasyMock.expect(personService.getPeople("john.doe",
        DataServiceServlet.GroupId.ALL, PersonService.SortOrder.topFriends,
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
    EasyMock.expect(servletRequest.getPathInfo()).andReturn("/people/john.doe/@friends");

    EasyMock.expect(servletRequest.getParameter("orderBy")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("filterBy")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("startIndex")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("count")).andReturn(null);
    EasyMock.expect(servletRequest.getParameter("fields")).andReturn(null);

    FakeGadgetToken token = new FakeGadgetToken();
    ResponseItem<RestfulCollection<Person>> data
        = new ResponseItem<RestfulCollection<Person>>(null);
    EasyMock.expect(personService.getPeople("john.doe",
        DataServiceServlet.GroupId.FRIENDS, PersonService.SortOrder.topFriends,
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
    EasyMock.expect(servletRequest.getPathInfo()).andReturn("/people/john.doe/@friends");

    PersonService.SortOrder order = PersonService.SortOrder.name;
    PersonService.FilterType filter = PersonService.FilterType.topFriends;

    EasyMock.expect(servletRequest.getParameter("orderBy")).andReturn(order.toString());
    EasyMock.expect(servletRequest.getParameter("filterBy")).andReturn(filter.toString());
    EasyMock.expect(servletRequest.getParameter("startIndex")).andReturn("5");
    EasyMock.expect(servletRequest.getParameter("count")).andReturn("10");
    EasyMock.expect(servletRequest.getParameter("fields")).andReturn("money,fame,fortune");

    FakeGadgetToken token = new FakeGadgetToken();
    ResponseItem<RestfulCollection<Person>> data
        = new ResponseItem<RestfulCollection<Person>>(null);
    EasyMock.expect(personService.getPeople("john.doe", DataServiceServlet.GroupId.FRIENDS, order,
        filter, 5, 10, Sets.newHashSet("money", "fame", "fortune"), token)).andReturn(data);

    replay();
    assertEquals(data, handler.handleGet(servletRequest, token));
    verify();
  }

  public void testHandleGetFriendById() throws Exception {
    EasyMock.expect(servletRequest.getPathInfo()).andReturn("/people/john.doe/@friends/jane.doe");

    FakeGadgetToken token = new FakeGadgetToken();
    ResponseItem<Person> data = new ResponseItem<Person>(null);
    // TODO: This isn't right! We should be passing both john.doe and jane.doe to the service
    // We probably need to either change the getPerson parameters or add a new method to
    // the interface
    EasyMock.expect(personService.getPerson("john.doe", token)).andReturn(data);

    replay();
    assertEquals(data, handler.handleGet(servletRequest, token));
    verify();
  }

  public void testHandleGetSelf() throws Exception {
    EasyMock.expect(servletRequest.getPathInfo()).andReturn("/people/john.doe/@self");

    FakeGadgetToken token = new FakeGadgetToken();
    ResponseItem<Person> data = new ResponseItem<Person>(null);
    EasyMock.expect(personService.getPerson("john.doe", token)).andReturn(data);

    replay();
    assertEquals(data, handler.handleGet(servletRequest, token));
    verify();
  }
}