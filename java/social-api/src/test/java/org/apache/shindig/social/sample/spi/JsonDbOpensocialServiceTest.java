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
package org.apache.shindig.social.sample.spi;

import java.util.Collections;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.protocol.DataCollection;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.protocol.model.FilterOperation;
import org.apache.shindig.protocol.model.SortOrder;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.core.model.NameImpl;
import org.apache.shindig.social.core.model.PersonImpl;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.ActivityEntry;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Test the JSONOpensocialService
 */
public class JsonDbOpensocialServiceTest extends Assert {
  private JsonDbOpensocialService db;

  private static final UserId CANON_USER = new UserId(UserId.Type.userId, "canonical");
  private static final UserId JOHN_DOE = new UserId(UserId.Type.userId, "john.doe");
  private static final UserId JANE_DOE = new UserId(UserId.Type.userId, "jane.doe");
  private static final UserId ANONYMOUS = new UserId(UserId.Type.userId, AnonymousSecurityToken.ANONYMOUS_ID);

  private static final GroupId SELF_GROUP = new GroupId(GroupId.Type.self, null);
  private static final String APP_ID = "1";
  private static final String CANONICAL_USER_ID = "canonical";

  private SecurityToken token = new FakeGadgetToken();

  @Before
  public void setUp() throws Exception {
    Injector injector = Guice.createInjector(new SocialApiTestsGuiceModule());
    db = injector.getInstance(JsonDbOpensocialService.class);
  }

  @Test
  public void testGetPersonDefaultFields() throws Exception {
    Person person = db
        .getPerson(CANON_USER, Person.Field.DEFAULT_FIELDS, token).get();

    assertNotNull("Canonical user not found", person);
    assertNotNull("Canonical user has no id", person.getId());
    assertNotNull("Canonical user has no name", person.getName());
    assertNotNull("Canonical user has no thumbnail",
        person.getThumbnailUrl());
  }

  @Test
  public void testGetAnonymousUser() throws Exception {
    Person person = db.getPerson(ANONYMOUS, Person.Field.DEFAULT_FIELDS, token).get();
    assertEquals("-1", person.getId());
    assertEquals("Anonymous", person.getName().getFormatted());
    assertEquals("Anonymous", person.getNickname());
  }

  @Test
  public void testGetPersonAllFields() throws Exception {
    Person person = db
        .getPerson(CANON_USER, Person.Field.ALL_FIELDS, token).get();
    assertNotNull("Canonical user not found", person);
  }

  @Test
  public void testGetPersonAllAppData() throws Exception {
    Person person = db
        .getPerson(CANON_USER, ImmutableSet.of("id", "appData"), token).get();

    assertNotNull("Canonical user not found", person);
    assertEquals("Canonical user has wrong id", "canonical", person.getId());
    assertEquals("Canonical user has wrong app data",
        ImmutableMap.of("count", "2", "size", "100"), person.getAppData());
  }

  @Test
  public void testGetPersonOneAppDataField() throws Exception {
    Person person = db
        .getPerson(CANON_USER, ImmutableSet.of("id", "appData.size"), token).get();

    assertNotNull("Canonical user not found", person);
    assertEquals("Canonical user has wrong id", "canonical", person.getId());
    assertEquals("Canonical user has wrong app data",
        ImmutableMap.of("size", "100"), person.getAppData());
  }

  @Test
  public void testGetPersonMultipleAppDataFields() throws Exception {
    Person person = db
        .getPerson(CANON_USER,
            ImmutableSet.of("id", "appData.size", "appData.count", "appData.bogus"),
            token).get();

    assertNotNull("Canonical user not found", person);
    assertEquals("Canonical user has wrong id", "canonical", person.getId());
    assertEquals("Canonical user has wrong app data",
        ImmutableMap.of("count", "2", "size", "100"), person.getAppData());
  }

  @Test
  public void testGetExpectedFriends() throws Exception {
    CollectionOptions options = new CollectionOptions();
    options.setSortBy(PersonService.TOP_FRIENDS_SORT);
    options.setSortOrder(SortOrder.ascending);
    options.setFilter(null);
    options.setFilterOperation(FilterOperation.contains);
    options.setFilterValue("");
    options.setFirst(0);
    options.setMax(20);

    RestfulCollection<Person> responseItem = db.getPeople(
        ImmutableSet.of(CANON_USER), new GroupId(GroupId.Type.friends, null),
        options, Collections.<String>emptySet(), token).get();
    assertNotNull(responseItem);
    assertEquals(4, responseItem.getTotalResults());
    // Test a couple of users
    assertEquals("john.doe", responseItem.getList().get(0).getId());
    assertEquals("jane.doe", responseItem.getList().get(1).getId());
  }

  @Test
  public void testGetExpectedUsersForPlural() throws Exception {
    CollectionOptions options = new CollectionOptions();
    options.setSortBy(PersonService.TOP_FRIENDS_SORT);
    options.setSortOrder(SortOrder.ascending);
    options.setFilter(null);
    options.setFilterOperation(FilterOperation.contains);
    options.setFilterValue("");
    options.setFirst(0);
    options.setMax(20);

    RestfulCollection<Person> responseItem = db.getPeople(
        ImmutableSet.of(JOHN_DOE, JANE_DOE), new GroupId(GroupId.Type.friends, null),
       options, Collections.<String>emptySet(), token).get();
    assertNotNull(responseItem);
    assertEquals(4, responseItem.getTotalResults());
    // Test a couple of users
    assertEquals("john.doe", responseItem.getList().get(0).getId());
    assertEquals("jane.doe", responseItem.getList().get(1).getId());
  }

  @Test
  public void testGetExpectedActivities() throws Exception {
    RestfulCollection<Activity> responseItem = db.getActivities(
        ImmutableSet.of(CANON_USER), SELF_GROUP, APP_ID, Collections.<String>emptySet(), null,
        new FakeGadgetToken()).get();
    assertSame(2, responseItem.getTotalResults());
  }

  @Test
  public void testGetExpectedActivitiesForPlural() throws Exception {
    RestfulCollection<Activity> responseItem = db.getActivities(
        ImmutableSet.of(CANON_USER, JOHN_DOE), SELF_GROUP, APP_ID, Collections.<String>emptySet(), null,
        new FakeGadgetToken()).get();
    assertSame(3, responseItem.getTotalResults());
  }

  @Test
  public void testGetExpectedActivity() throws Exception {
    Activity activity = db.getActivity(
        CANON_USER, SELF_GROUP, APP_ID,
        ImmutableSet.of("appId", "body", "mediaItems"), APP_ID, new FakeGadgetToken()).get();
    assertNotNull(activity);
    // Check that some fields are fetched and others are not
    assertNotNull(activity.getBody());
    assertNull(activity.getBodyId());
  }

  @Test
  public void testDeleteExpectedActivity() throws Exception {
    db.deleteActivities(CANON_USER, SELF_GROUP, APP_ID, ImmutableSet.of(APP_ID),
        new FakeGadgetToken());

    // Try to fetch the activity
    try {
      db.getActivity(
          CANON_USER, SELF_GROUP, APP_ID,
          ImmutableSet.of("appId", "body", "mediaItems"), APP_ID, new FakeGadgetToken()).get();
      fail();
    } catch (ProtocolException sse) {
      assertEquals(HttpServletResponse.SC_BAD_REQUEST, sse.getCode());
    }
  }

  @Test
  public void testGetExpectedAppData() throws Exception {
    DataCollection responseItem = db.getPersonData(
        ImmutableSet.of(CANON_USER), SELF_GROUP, APP_ID, Collections.<String>emptySet(),
        new FakeGadgetToken()).get();
    assertFalse(responseItem.getEntry().isEmpty());
    assertFalse(responseItem.getEntry().get(CANONICAL_USER_ID).isEmpty());

    assertSame(2, responseItem.getEntry().get(CANONICAL_USER_ID).size());
    assertTrue(responseItem.getEntry().get(CANONICAL_USER_ID).containsKey("count"));
    assertTrue(responseItem.getEntry().get(CANONICAL_USER_ID).containsKey("size"));
  }

  @Test
  public void testGetExpectedAppDataForPlural() throws Exception {
    DataCollection responseItem = db.getPersonData(
        ImmutableSet.of(CANON_USER, JOHN_DOE), SELF_GROUP, APP_ID, Collections.<String>emptySet(),
        new FakeGadgetToken()).get();
    assertFalse(responseItem.getEntry().isEmpty());
    assertFalse(responseItem.getEntry().get(CANONICAL_USER_ID).isEmpty());

    assertSame(2, responseItem.getEntry().get(CANONICAL_USER_ID).size());
    assertTrue(responseItem.getEntry().get(CANONICAL_USER_ID).containsKey("count"));
    assertTrue(responseItem.getEntry().get(CANONICAL_USER_ID).containsKey("size"));

    assertFalse(responseItem.getEntry().get(JOHN_DOE.getUserId()).isEmpty());
    assertSame(1, responseItem.getEntry().get(JOHN_DOE.getUserId()).size());
    assertTrue(responseItem.getEntry().get(JOHN_DOE.getUserId()).containsKey("count"));
  }

  @Test
  public void testDeleteExpectedAppData() throws Exception {
    // Delete the data
    db.deletePersonData(CANON_USER, SELF_GROUP, APP_ID,
        ImmutableSet.of("count"), new FakeGadgetToken());

    // Fetch the remaining and test
    DataCollection responseItem = db.getPersonData(
        ImmutableSet.of(CANON_USER), SELF_GROUP, APP_ID, Collections.<String>emptySet(),
        new FakeGadgetToken()).get();
    assertFalse(responseItem.getEntry().isEmpty());
    assertFalse(responseItem.getEntry().get(CANONICAL_USER_ID).isEmpty());

    assertSame(1, responseItem.getEntry().get(CANONICAL_USER_ID).size());
    assertFalse(responseItem.getEntry().get(CANONICAL_USER_ID).containsKey("count"));
    assertTrue(responseItem.getEntry().get(CANONICAL_USER_ID).containsKey("size"));
  }

  @Test
  public void testUpdateExpectedAppData() throws Exception {
    // Delete the data
    db.updatePersonData(CANON_USER, SELF_GROUP, APP_ID,
        null, ImmutableMap.of("count", (Object)"10", "newvalue", (Object)"20", "isValid", new Boolean(true)), new FakeGadgetToken());

    // Fetch the remaining and test
    DataCollection responseItem = db.getPersonData(
        ImmutableSet.of(CANON_USER), SELF_GROUP, APP_ID, Collections.<String>emptySet(),
        new FakeGadgetToken()).get();

    assertFalse(responseItem.getEntry().isEmpty());
    assertFalse(responseItem.getEntry().get(CANONICAL_USER_ID).isEmpty());

    assertSame(4, responseItem.getEntry().get(CANONICAL_USER_ID).size());
    assertTrue(responseItem.getEntry().get(CANONICAL_USER_ID).containsKey("count"));
    assertEquals("10", responseItem.getEntry().get(CANONICAL_USER_ID).get("count"));
    assertTrue(responseItem.getEntry().get(CANONICAL_USER_ID).containsKey("newvalue"));
    assertEquals("20", responseItem.getEntry().get(CANONICAL_USER_ID).get("newvalue"));
    assertTrue(responseItem.getEntry().get(CANONICAL_USER_ID).containsKey("isValid"));
    assertEquals(true, Boolean.valueOf(responseItem.getEntry().get(CANONICAL_USER_ID).get("isValid").toString()));
  }

  @Test
  public void testGetExpectedActivityEntries() throws Exception {
    RestfulCollection<ActivityEntry> responseItem = db.getActivityEntries(
        ImmutableSet.of(JOHN_DOE), SELF_GROUP, APP_ID, Collections.<String>emptySet(), null,
        new FakeGadgetToken()).get();
    assertSame(3, responseItem.getTotalResults());
  }

  @Test
  public void testGetExpectedActivityEntriesForPlural() throws Exception {
    RestfulCollection<ActivityEntry> responseItem = db.getActivityEntries(
        ImmutableSet.of(CANON_USER, JOHN_DOE), SELF_GROUP, APP_ID, Collections.<String>emptySet(), null,
        new FakeGadgetToken()).get();
    assertSame(3, responseItem.getTotalResults());
  }

  @Test
  public void testGetExpectedActivityEntry() throws Exception {
    ActivityEntry entry = db.getActivityEntry(JOHN_DOE, SELF_GROUP, APP_ID,
        ImmutableSet.of("title"), "activity2", new FakeGadgetToken()).get();
    assertNotNull(entry);
    // Check that some fields are fetched and others are not
    assertNotNull(entry.getTitle());
    assertNull(entry.getPublished());
  }

  @Test
  public void testDeleteExpectedActivityEntry() throws Exception {
    db.deleteActivityEntries(JOHN_DOE, SELF_GROUP, APP_ID, ImmutableSet.of(APP_ID),
        new FakeGadgetToken());

    // Try to fetch the activity
    try {
      db.getActivityEntry(
          JOHN_DOE, SELF_GROUP, APP_ID,
          ImmutableSet.of("body"), APP_ID, new FakeGadgetToken()).get();
      fail();
    } catch (ProtocolException sse) {
      assertEquals(HttpServletResponse.SC_BAD_REQUEST, sse.getCode());
    }
  }

  @Test
  public void testViewerCanUpdatePerson() throws Exception {
    // Create new user
    JSONArray people = db.getDb().getJSONArray("people");
    JSONObject jsonPerson = new JSONObject();
    jsonPerson.put("id", "updatePerson");
    people.put(people.length(),jsonPerson);

    SecurityToken updateToken = new FakeGadgetToken("appId", "appUrl", "domain", "updatePerson", "trustedJson", "updatePerson", "20");

    // Get user
    UserId userId = new UserId(UserId.Type.userId, "updatePerson");
    Person person = db
        .getPerson(userId, Person.Field.ALL_FIELDS, token).get();
    assertNotNull("User 'updatePerson' not found", person);

    // update a field in user object
    person.setThumbnailUrl("http://newthumbnail.url");
    // Save user to db
    db.updatePerson(userId, person, updateToken);
    // Get user again from db and check if the fields were properly updated
    person = db.getPerson(userId, Person.Field.ALL_FIELDS, token).get();
    assertNotNull("User 'updatePerson' not found", person);

    assertEquals("http://newthumbnail.url", person.getThumbnailUrl());
  }

  @Test
  public void testViewerNotAllowedUpdatePerson() throws Exception {
    // Create new user
    JSONArray people = db.getDb().getJSONArray("people");
    JSONObject jsonPerson = new JSONObject();
    jsonPerson.put("id", "updatePerson");
    people.put(people.length(),jsonPerson);

    SecurityToken updateToken = new FakeGadgetToken("appId", "appUrl", "domain", "viewer", "trustedJson", "viewer", "20");

    // Get user
    UserId userId = new UserId(UserId.Type.userId, "updatePerson");
    Person person = db
        .getPerson(userId, Person.Field.ALL_FIELDS, token).get();

    // update a field in user object
    person.setThumbnailUrl("http://newthumbnail.url");
    // Save user to db, should throw an exception
    try {
      db.updatePerson(userId, person, updateToken);
      fail();
    } catch (ProtocolException sse) {
      assertEquals(HttpServletResponse.SC_FORBIDDEN, sse.getCode());
    }
  }

}
