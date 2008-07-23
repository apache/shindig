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
package org.apache.shindig.social.canonical;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.spi.DataCollection;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.RestfulCollection;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.apache.shindig.social.sample.service.JsonDbOpensocialService;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import junit.framework.TestCase;

/**
 * Test the JSONOpensocialService
 */
public class JsonDbOpensocialServiceTest extends TestCase {

  private JsonDbOpensocialService db;

  private static final UserId CANON_USER = new UserId(UserId.Type.userId, "canonical");
  private static final GroupId SELF_GROUP = new GroupId(GroupId.Type.self, null);
  private static final String APP_ID = "1";

  private static final String CANONICAL_USER_ID = "canonical";
  private SecurityToken token = new FakeGadgetToken();


  @Override
  protected void setUp() throws Exception {
    Injector injector = Guice.createInjector(new SocialApiTestsGuiceModule());
    db = injector.getInstance(JsonDbOpensocialService.class);
  }

  public void testGetPersonDefaultFields() throws Exception {
    ResponseItem<Person> personResponseItem = db
        .getPerson(CANON_USER, Person.Field.DEFAULT_FIELDS, token).get();

    assertNotNull("Canonical user not found", personResponseItem.getResponse());
    assertNotNull("Canonical user has no id", personResponseItem.getResponse().getId());
    assertNotNull("Canonical user has no name", personResponseItem.getResponse().getName());
    assertNotNull("Canonical user has no thumbnail",
        personResponseItem.getResponse().getThumbnailUrl());
  }

  public void testGetPersonAllFields() throws Exception {
    ResponseItem<Person> personResponseItem = db
        .getPerson(CANON_USER, Person.Field.ALL_FIELDS, token).get();
    assertNotNull("Canonical user not found", personResponseItem.getResponse());
  }

  public void testGetExpectedFriends() throws Exception {
    ResponseItem<RestfulCollection<Person>> responseItem = db.getPeople(
        CANON_USER, new GroupId(GroupId.Type.friends, null), PersonService.SortOrder.topFriends,
        PersonService.FilterType.all, 0, Integer.MAX_VALUE, null, token).get();
    assertNotNull(responseItem.getResponse());
    assertEquals(responseItem.getResponse().getTotalResults(), 4);
    // Test a couple of users
    assertEquals(responseItem.getResponse().getEntry().get(0).getId(), "john.doe");
    assertEquals(responseItem.getResponse().getEntry().get(1).getId(), "jane.doe");
  }

  public void testGetExpectedActivities() throws Exception {
    ResponseItem<RestfulCollection<Activity>> responseItem = db.getActivities(
        CANON_USER, SELF_GROUP, APP_ID, null, new FakeGadgetToken()).get();
    assertTrue(responseItem.getResponse().getTotalResults() == 2);
  }

  public void testGetExpectedActivity() throws Exception {
    ResponseItem<Activity> responseItem = db.getActivity(
        CANON_USER, SELF_GROUP, APP_ID,
        Sets.newHashSet("appId", "body", "mediaItems"), APP_ID, new FakeGadgetToken()).get();
    assertTrue(responseItem != null);
    assertTrue(responseItem.getResponse() != null);
    // Check that some fields are fetched and others are not
    assertTrue(responseItem.getResponse().getBody() != null);
    assertTrue(responseItem.getResponse().getBodyId() == null);
  }

  public void testDeleteExpectedActivity() throws Exception {
    db.deleteActivity(CANON_USER, SELF_GROUP, APP_ID, APP_ID,
        new FakeGadgetToken());

    // Try to fetch the activity
    ResponseItem<Activity> responseItem = db.getActivity(
        CANON_USER, SELF_GROUP, APP_ID,
        Sets.newHashSet("appId", "body", "mediaItems"), APP_ID, new FakeGadgetToken()).get();
    assertTrue(responseItem.getResponse() == null);
  }

  public void testGetExpectedAppData() throws Exception {
    ResponseItem<DataCollection> responseItem = db.getPersonData(
        CANON_USER, SELF_GROUP, APP_ID, null, new FakeGadgetToken()).get();
    assertTrue(!responseItem.getResponse().getEntry().isEmpty());
    assertTrue(!responseItem.getResponse().getEntry().get(CANONICAL_USER_ID).isEmpty());
    assertTrue(responseItem.getResponse().getEntry().get(CANONICAL_USER_ID).size() == 2);
    assertTrue(responseItem.getResponse().getEntry().get(CANONICAL_USER_ID).containsKey("count"));
    assertTrue(responseItem.getResponse().getEntry().get(CANONICAL_USER_ID).containsKey("size"));
  }

  public void testDeleteExpectedAppData() throws Exception {
    // Delete the data
    db.deletePersonData(CANON_USER, SELF_GROUP, APP_ID,
        Sets.newHashSet("count"), new FakeGadgetToken());

    //Fetch the remaining and test
    ResponseItem<DataCollection> responseItem = db.getPersonData(
        CANON_USER, SELF_GROUP, APP_ID, null, new FakeGadgetToken()).get();
    assertTrue(!responseItem.getResponse().getEntry().isEmpty());
    assertTrue(!responseItem.getResponse().getEntry().get(CANONICAL_USER_ID).isEmpty());
    assertTrue(responseItem.getResponse().getEntry().get(CANONICAL_USER_ID).size() == 1);
    assertTrue(!responseItem.getResponse().getEntry().get(CANONICAL_USER_ID).containsKey("count"));
    assertTrue(responseItem.getResponse().getEntry().get(CANONICAL_USER_ID).containsKey("size"));
  }

  public void testUpdateExpectedAppData() throws Exception {
    // Delete the data
    db.updatePersonData(CANON_USER, SELF_GROUP, APP_ID,
        null, Maps.immutableMap("count", "10", "newvalue", "20"), new FakeGadgetToken());

    //Fetch the remaining and test
    ResponseItem<DataCollection> responseItem = db.getPersonData(
        CANON_USER, SELF_GROUP, APP_ID, null, new FakeGadgetToken()).get();
    assertTrue(!responseItem.getResponse().getEntry().isEmpty());
    assertTrue(!responseItem.getResponse().getEntry().get(CANONICAL_USER_ID).isEmpty());
    assertTrue(responseItem.getResponse().getEntry().get(CANONICAL_USER_ID).size() == 3);
    assertTrue(responseItem.getResponse().getEntry().get(CANONICAL_USER_ID).containsKey("count"));
    assertTrue(
        responseItem.getResponse().getEntry().get(CANONICAL_USER_ID).get("count").equals("10"));
    assertTrue(
        responseItem.getResponse().getEntry().get(CANONICAL_USER_ID).containsKey("newvalue"));
    assertTrue(
        responseItem.getResponse().getEntry().get(CANONICAL_USER_ID).get("newvalue").equals("20"));
  }
}