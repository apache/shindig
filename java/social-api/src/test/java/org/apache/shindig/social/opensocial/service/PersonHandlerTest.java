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

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.config.JsonContainerConfig;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.protocol.DefaultHandlerRegistry;
import org.apache.shindig.protocol.HandlerExecutionListener;
import org.apache.shindig.protocol.HandlerRegistry;
import org.apache.shindig.protocol.RestHandler;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.protocol.model.FilterOperation;
import org.apache.shindig.protocol.model.SortOrder;
import org.apache.shindig.social.core.model.PersonImpl;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.*;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PersonHandlerTest extends EasyMockTestCase {
  private PersonService personService;
  private PersonHandler handler;
  private FakeGadgetToken token;
  protected HandlerRegistry registry;
  private BeanJsonConverter converter;

  private static final Set<String> DEFAULT_FIELDS = Sets.newHashSet(Person.Field.ID.toString(),
      Person.Field.NAME.toString(),
      Person.Field.THUMBNAIL_URL.toString());

  private static final Set<UserId> JOHN_DOE = Sets
      .newHashSet(new UserId(UserId.Type.userId, "john.doe"));

  private static CollectionOptions DEFAULT_OPTIONS = new CollectionOptions();
  protected ContainerConfig containerConfig;

  static {
    DEFAULT_OPTIONS.setSortBy(PersonService.TOP_FRIENDS_SORT);
    DEFAULT_OPTIONS.setSortOrder(SortOrder.ascending);
    DEFAULT_OPTIONS.setFilter(null);
    DEFAULT_OPTIONS.setFilterOperation(FilterOperation.contains);
    DEFAULT_OPTIONS.setFilterValue("");
    DEFAULT_OPTIONS.setFirst(0);
    DEFAULT_OPTIONS.setMax(20);
  }

  @Override
  protected void setUp() throws Exception {
    token = new FakeGadgetToken();
    converter = mock(BeanJsonConverter.class);
    personService = mock(PersonService.class);
    JSONObject config = new JSONObject("{"  + ContainerConfig.DEFAULT_CONTAINER + ":" +
            "{'gadgets.features':{'opensocial-0.8':" +
               "{supportedFields: {person: ['id', {name: 'familyName'}]}}" +
             "}}}");

    containerConfig = new JsonContainerConfig(config, new Expressions());
    handler = new PersonHandler(personService, containerConfig);
    registry = new DefaultHandlerRegistry(null, Sets.<Object>newHashSet(handler), converter,
        new HandlerExecutionListener.NoOpHandlerExecutionListener());
  }

  public void testHandleGetAllNoParams() throws Exception {
    String path = "/people/john.doe/@all";
    RestHandler operation = registry.getRestHandler(path, "GET");

    List<Person> personList = ImmutableList.of();
    RestfulCollection<Person> data = new RestfulCollection<Person>(personList);

    expect(personService.getPeople(
        eq(JOHN_DOE),
        eq(new GroupId(GroupId.Type.all, null)),
        eq(DEFAULT_OPTIONS),
        eq(DEFAULT_FIELDS),
        eq(token)))
        .andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, operation.execute(Maps.<String, String[]>newHashMap(), null,
        token, converter).get());
    verify();
  }

  public void testHandleGetFriendsNoParams() throws Exception {
    String path = "/people/john.doe/@friends";
    RestHandler operation = registry.getRestHandler(path, "GET");

    List<Person> personList = ImmutableList.of();
    RestfulCollection<Person> data = new RestfulCollection<Person>(personList);
    expect(personService.getPeople(
        eq(JOHN_DOE),
        eq(new GroupId(GroupId.Type.friends, null)),
        eq(DEFAULT_OPTIONS),
        eq(DEFAULT_FIELDS),
        eq(token)))
        .andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, operation.execute(Maps.<String, String[]>newHashMap(),
        null, token, converter).get());
    verify();
  }

  public void testHandleGetFriendsWithParams() throws Exception {
    String path = "/people/john.doe/@friends";
    RestHandler operation = registry.getRestHandler(path, "GET");

    CollectionOptions options = new CollectionOptions();
    options.setSortBy(Person.Field.NAME.toString());
    options.setSortOrder(SortOrder.descending);
    options.setFilter(PersonService.TOP_FRIENDS_FILTER);
    options.setFilterOperation(FilterOperation.present);
    options.setFilterValue("cassie");
    options.setFirst(5);
    options.setMax(10);

    Map<String, String[]> params = Maps.newHashMap();
    params.put("sortBy", new String[]{options.getSortBy()});
    params.put("sortOrder", new String[]{options.getSortOrder().toString()});
    params.put("filterBy", new String[]{options.getFilter()});
    params.put("filterOp", new String[]{options.getFilterOperation().toString()});
    params.put("filterValue", new String[]{options.getFilterValue()});
    params.put("startIndex", new String[]{"5"});
    params.put("count", new String[]{"10"});
    params.put("fields", new String[]{"money,fame,fortune"});


    List<Person> people = ImmutableList.of();
    RestfulCollection<Person> data = new RestfulCollection<Person>(people);
    expect(personService.getPeople(
        eq(JOHN_DOE),
        eq(new GroupId(GroupId.Type.friends, null)), eq(options),
        eq(ImmutableSortedSet.of("money", "fame", "fortune")), eq(token)))
        .andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, operation.execute(params, null, token, converter).get());
    verify();
  }

  public void testHandleGetFriendById() throws Exception {
    String path = "/people/john.doe/@friends/jane.doe";
    RestHandler operation = registry.getRestHandler(path, "GET");

    Person person = new PersonImpl();
    List<Person> people = Lists.newArrayList(person);
    RestfulCollection<Person> data = new RestfulCollection<Person>(people);
    // TODO: We aren't passing john.doe to the service yet.
    expect(personService.getPeople(
        eq(Sets.newHashSet(new UserId(UserId.Type.userId, "jane.doe"))),
        eq(new GroupId(GroupId.Type.self, null)), eq(DEFAULT_OPTIONS),
        eq(DEFAULT_FIELDS), eq(token)))
        .andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(person, operation.execute(Maps.<String, String[]>newHashMap(),
        null, token, converter).get());
    verify();
  }

  public void testHandleGetSelf() throws Exception {
    String path = "/people/john.doe/@self";
    RestHandler operation = registry.getRestHandler(path, "GET");

    Person data = new PersonImpl();
    expect(personService.getPerson(eq(JOHN_DOE.iterator().next()),
        eq(DEFAULT_FIELDS), eq(token))).andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, operation.execute(Maps.<String, String[]>newHashMap(),
        null, token, converter).get());
    verify();
  }

  public void testHandleGetPlural() throws Exception {
    String path = "/people/john.doe,jane.doe/@self";
    RestHandler operation = registry.getRestHandler(path, "GET");

    List<Person> people = ImmutableList.of();
    RestfulCollection<Person> data = new RestfulCollection<Person>(people);
    Set<UserId> userIdSet = Sets.newLinkedHashSet(JOHN_DOE);
    userIdSet.add(new UserId(UserId.Type.userId, "jane.doe"));
    expect(personService.getPeople(eq(userIdSet),
        eq(new GroupId(GroupId.Type.self, null)),
        eq(DEFAULT_OPTIONS),
        eq(DEFAULT_FIELDS),
        eq(token))).andReturn(ImmediateFuture.newInstance(data));

    replay();
    assertEquals(data, operation.execute(Maps.<String, String[]>newHashMap(),
        null, token, converter).get());
    verify();
  }

  public void testHandleGetSupportedFields() throws Exception {
    String path = "/people/@supportedFields";
    RestHandler operation = registry.getRestHandler(path, "GET");

    replay();
    @SuppressWarnings("unchecked")
    List<Object> received = (List<Object>) operation.execute(Maps.<String, String[]>newHashMap(),
        null, token, converter).get();
    assertEquals(2, received.size());
    assertEquals("id", received.get(0).toString());
    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) received.get(1);
    assertEquals("familyName", map.get("name").toString());

    verify();
  }
}
