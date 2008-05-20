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
package org.apache.shindig.social;

import org.apache.shindig.common.BasicSecurityTokenDecoder;
import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.social.opensocial.ActivitiesService;
import org.apache.shindig.social.opensocial.DataService;
import org.apache.shindig.social.opensocial.PeopleService;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.ApiCollection;
import org.apache.shindig.social.opensocial.model.Email;
import org.apache.shindig.social.opensocial.model.IdSpec;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Phone;

import com.google.inject.AbstractModule;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides social api component injection for all large tests
 */
public class SocialApiTestsGuiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(PeopleService.class).to(MockPeopleService.class);
    bind(DataService.class).to(MockDataService.class);
    bind(ActivitiesService.class).to(MockActivitiesService.class);

    bind(SecurityTokenDecoder.class).to(BasicSecurityTokenDecoder.class);
  }

  // TODO: These 3 static classes with static params are definitely not the
  // best way to do things. We need to make this integration with Jetty
  // cleaner and easier.
  public static class MockPeopleService implements PeopleService {
    public static Person johnDoe;
    public static Person janeDoe;
    public static Person simpleDoe;
    private static ResponseItem<ApiCollection<Person>> people;
    private static ResponseItem<Person> person;

    static {
      // setup John Doe
      johnDoe = new Person("john.doe", new Name("John Doe"));
      List<Phone> phones = new ArrayList<Phone>();
      phones.add(new Phone("+33H000000000", "home"));
      phones.add(new Phone("+33M000000000", "mobile"));
      phones.add(new Phone("+33W000000000", "work"));
      johnDoe.setPhoneNumbers(phones);

      List<Address> addresses = new ArrayList<Address>();
      addresses.add(new Address("My home address"));
      johnDoe.setAddresses(addresses);

      List<Email> emails = new ArrayList<Email>();
      emails.add(new Email("john.doe@work.bar", "work"));
      emails.add(new Email("john.doe@home.bar", "home"));
      johnDoe.setEmails(emails);
      johnDoe.setUpdated(new Date());

      // setup Jane Doe
      janeDoe = new Person("jane.doe", new Name("Jane Doe"));
      janeDoe.setUpdated(new Date());

      // setup Simple Doe
      simpleDoe = new Person("simple.doe", new Name("Simple Doe"));
      simpleDoe.setUpdated(new Date());
    }

    public static void setPeople(ResponseItem<ApiCollection<Person>>
        peopleVal) {
      people = peopleVal;
    }

    public static void setPerson(ResponseItem<Person> personVal) {
      person = personVal;
    }

    public List<String> getIds(IdSpec idSpec, SecurityToken token)
        throws JSONException {
      // Not needed yet
      return null;
    }

    public ResponseItem<ApiCollection<Person>> getPeople(List<String> ids,
        SortOrder sortOrder, FilterType filter, int first, int max,
        Set<String> profileDetails, SecurityToken token) {
      return people;
    }

    public ResponseItem<Person> getPerson(String id, SecurityToken token) {
      return person;
    }
  }

  public static class MockDataService implements DataService {
    private static ResponseItem<Map<String, Map<String, String>>> personData;

    public static void setPersonData(ResponseItem<Map<String,
        Map<String, String>>> personDataVal) {
      personData = personDataVal;
    }

    public ResponseItem<Map<String, Map<String, String>>> getPersonData(
        List<String> ids, List<String> keys, SecurityToken token) {
      return personData;
    }

    public ResponseItem updatePersonData(String id, String key, String value,
        SecurityToken token) {
      // Not needed yet
      return null;
    }
  }

  public static class MockActivitiesService implements ActivitiesService {
    public static ResponseItem<List<Activity>> activities;
    public static ResponseItem<Activity> activity;

    public static Activity basicActivity;

    static {
      basicActivity = new Activity("1", "john.doe");
      basicActivity.setTitle("yellow");
      basicActivity.setBody("what a color!");
      basicActivity.setUpdated(new Date());
    }

    public static void setActivity(ResponseItem<Activity> activityVal) {
      activity = activityVal;
    }

    public static void setActivities(ResponseItem<List<Activity>>
        activitiesVal) {
      activities = activitiesVal;
    }

    public ResponseItem<List<Activity>> getActivities(List<String> ids,
        SecurityToken token) {
      return activities;
    }

    public ResponseItem createActivity(String personId, Activity activity,
        SecurityToken token) {
      // Not needed yet
      return null;
    }

    public ResponseItem<Activity> getActivity(String id, String activityId,
        SecurityToken token) {
      return activity;
    }
  }
}