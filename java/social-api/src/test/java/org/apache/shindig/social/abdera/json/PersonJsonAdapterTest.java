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
package org.apache.shindig.social.abdera.json;

//import static org.easymock.EasyMock.expect;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

import junit.framework.TestCase;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.abdera.RequestUrlTemplate;
import org.apache.shindig.social.abdera.SocialRequestContext;
import org.apache.shindig.social.dataservice.GroupId;
import org.apache.shindig.social.dataservice.PersonService;
import org.apache.shindig.social.dataservice.RestfulCollection;
import org.apache.shindig.social.dataservice.UserId;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;

import org.apache.abdera.i18n.templates.Route;
import org.apache.abdera.protocol.server.impl.SimpleTarget;

import java.util.concurrent.Future;

public class PersonJsonAdapterTest extends TestCase {
  private BeanJsonConverter converter;
  private SocialRequestContext request;
  private PersonService personService;
  private PersonJsonAdapter adapter;
  private SimpleTarget target;
  private Route route;
  private SecurityToken token;
  private SecurityTokenDecoder decoder;

  @Override
  protected void setUp() throws Exception {
    converter = createMock(BeanJsonConverter.class);
    request = createMock(SocialRequestContext.class);
    personService = createMock(PersonService.class);
    target = createMock(SimpleTarget.class);
    route = createMock(Route.class);
    token = createMock(SecurityToken.class);
    decoder = createMock(SecurityTokenDecoder.class);

    adapter = new PersonJsonAdapter(converter);
    adapter.setPersonService(personService);
    //this expectation is just for method chaining.
    expect(request.getTarget()).andReturn(target).anyTimes();
  }

  private void replayAll() {
    replay(converter);
    replay(request);
    replay(personService);
    replay(target);
    replay(route);
    replay(token);
    replay(decoder);
  }

  private void verifyAll() {
    verify(converter);
    verify(request);
    verify(personService);
    verify(target);
    verify(route);
    verify(token);
    verify(decoder);
  }

  private void setTarget(RequestUrlTemplate template) {
    // have to mock out the full route object because SocialRouteManager.getUrlTemplate is static.
    expect(route.getName()).andReturn(template.name()).anyTimes();
    expect(target.getMatcher()).andReturn(route);
  }

  private void setUpPersonRequest(){
    expect(target.getParameter("uid")).andReturn("john.doe");
    expect(request.getFields()).andReturn(SocialRequestContext.DEFAULT_PERSON_FIELDS);
  }

  private void setUpDefaultParameters(){
    expect(request.getOrderBy()).andReturn(PersonService.SortOrder.topFriends);
    expect(request.getCount()).andReturn(SocialRequestContext.DEFAULT_COUNT);
    expect(request.getStartIndex()).andReturn(SocialRequestContext.DEFAULT_START_INDEX);
    expect(request.getFilterBy()).andReturn(PersonService.FilterType.all);
    }

  public void testGetEntities_ProfilesOfConnectionsOfUser() throws Exception {
    setTarget(RequestUrlTemplate.JSON_PROFILES_OF_CONNECTIONS_OF_USER);
    setUpPersonRequest();
    setUpDefaultParameters();

    Future<ResponseItem<RestfulCollection<Person>>> data =
      ImmediateFuture.newInstance(new ResponseItem<RestfulCollection<Person>>(null));

    expect(
        personService.getPeople(new UserId(UserId.Type.userId, "john.doe"), new GroupId(
            GroupId.Type.all, "all"), PersonService.SortOrder.topFriends,
            PersonService.FilterType.all, SocialRequestContext.DEFAULT_START_INDEX,
            SocialRequestContext.DEFAULT_COUNT, SocialRequestContext.DEFAULT_PERSON_FIELDS, token))
        .andReturn(data);

    replayAll();
    assertEquals(data, adapter.getEntities(request, token));
    verifyAll();
  }

  public void testGetEntities_ProfilesOfFriendsOfUser() throws Exception {
    setTarget(RequestUrlTemplate.JSON_PROFILES_OF_FRIENDS_OF_USER);
    setUpPersonRequest();
    setUpDefaultParameters();

    Future<ResponseItem<RestfulCollection<Person>>> data =
        ImmediateFuture.newInstance(new ResponseItem<RestfulCollection<Person>>(null));

    expect(
        personService.getPeople(new UserId(UserId.Type.userId, "john.doe"), new GroupId(
            GroupId.Type.friends, "friends"), PersonService.SortOrder.topFriends,
            PersonService.FilterType.all, SocialRequestContext.DEFAULT_START_INDEX,
            SocialRequestContext.DEFAULT_COUNT, SocialRequestContext.DEFAULT_PERSON_FIELDS, token))
        .andReturn(data);

    replayAll();
    assertEquals(data, adapter.getEntities(request, token));
    verifyAll();
  }


  public void testGetEntities_ProfilesInGroupOfUser() throws Exception {
    setTarget(RequestUrlTemplate.JSON_PROFILES_IN_GROUP_OF_USER);
    setUpPersonRequest();
    setUpDefaultParameters();

    expect(target.getParameter("gid")).andReturn("losers");

    Future<ResponseItem<RestfulCollection<Person>>> data =
        ImmediateFuture.newInstance(new ResponseItem<RestfulCollection<Person>>(null));

    expect(
        personService.getPeople(new UserId(UserId.Type.userId, "john.doe"), new GroupId(
            GroupId.Type.groupId, "losers"), PersonService.SortOrder.topFriends,
            PersonService.FilterType.all, SocialRequestContext.DEFAULT_START_INDEX,
            SocialRequestContext.DEFAULT_COUNT, SocialRequestContext.DEFAULT_PERSON_FIELDS, token))
        .andReturn(data);

    replayAll();
    assertEquals(data, adapter.getEntities(request, token));
    verifyAll();
  }

  public void testGetEntity() throws Exception {
    Future<ResponseItem<Person>> data = ImmediateFuture.newInstance(new ResponseItem<Person>(null));
    expect(
        personService.getPerson(new UserId(UserId.Type.userId, "john.doe"),
            SocialRequestContext.DEFAULT_PERSON_FIELDS, token)).andReturn(data);
    setUpPersonRequest();

    replayAll();
    assertEquals(data, adapter.getEntity(request, token));
    verifyAll();
  }


  // public void testHandleGetFriendById() throws Exception {
  // setPath("/people/john.doe/@friends/jane.doe");
  //
  // ResponseItem<Person> data = new ResponseItem<Person>(null);
  // // TODO: This isn't right! We should be passing both john.doe and jane.doe to the service
  // // We probably need to either change the getPerson parameters or add a new method to
  // // the interface
  // expect(personService.getPerson(
  // new UserId(UserId.Type.userId, "john.doe"),
  // token)).andReturn(data);
  //
  // replayAll();
  // assertEquals(data, handler.handleGet(request, token));
  // verifyAll();
  // }

  // public void testHandleDelete() throws Exception {
  // replayAll();
  // assertEquals(ResponseError.BAD_REQUEST,
  // handler.handleDelete(request, token).getError());
  // verifyAll();
  // }
  //
  // public void testHandlePut() throws Exception {
  // replayAll();
  // assertEquals(ResponseError.NOT_IMPLEMENTED,
  // handler.handlePut(request, token).getError());
  // verifyAll();
  // }
  //
  // public void testHandlePost() throws Exception {
  // replayAll();
  // assertEquals(ResponseError.NOT_IMPLEMENTED,
  // handler.handlePost(request, token).getError());
  // verifyAll();
  // }
}
