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
import org.apache.shindig.social.opensocial.model.*;
import org.apache.shindig.social.opensocial.model.Enum;

import com.google.inject.AbstractModule;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Provides social api component injection for all large tests
 */
public class SocialApiTestsGuiceModule extends AbstractModule {
  private static Logger logger =
      Logger.getLogger(SocialApiTestsGuiceModule.class.getName());

  @Override
  protected void configure() {
    bind(PeopleService.class).to(MockPeopleService.class);
    bind(DataService.class).to(MockDataService.class);
    bind(ActivitiesService.class).to(MockActivitiesService.class);

    bind(SecurityTokenDecoder.class).to(BasicSecurityTokenDecoder.class);
  }

  // TODO: These 3 static classes have too much logic in them. Once we clean up
  // the interfaces for restful hopefully we will be able to delete a lot of
  // this code.
  public static class MockPeopleService implements PeopleService {
    public static final Person johnDoe;
    public static final Person janeDoe;
    public static final Person simpleDoe;

    static {
      // setup John Doe
      johnDoe = new Person("john.doe", new Name("John Doe"));

      // John should have every field filled in
      johnDoe.setAboutMe("about me");
      johnDoe.setActivities(newList("activity"));
      johnDoe.setAddresses(newList(new Address("My home address")));
      johnDoe.setAge(5);
      johnDoe.setBodyType(new BodyType()); //TODO
      johnDoe.setBooks(newList("books"));
      johnDoe.setCars(newList("cars"));
      johnDoe.setChildren("children");
      johnDoe.setCurrentLocation(new Address("my location"));
      johnDoe.setDateOfBirth(new Date());
      johnDoe.setDrinker(new Enum<Enum.Drinker>(Enum.Drinker.HEAVILY));
      johnDoe.setEmails(newList(new Email("john.doe@work.bar", "work")));
      johnDoe.setEthnicity("purple");
      johnDoe.setFashion("so fashionable");
      johnDoe.setFood(newList("gruel"));
      johnDoe.setGender(new Enum<Enum.Gender>(Enum.Gender.MALE));
      johnDoe.setHappiestWhen("puppies");
      johnDoe.setHeroes(newList("the moon"));
      johnDoe.setHumor("not so good");
      johnDoe.setInterests(newList("kites"));
      johnDoe.setJobInterests("penguins");
      johnDoe.setJobs(newList(new Organization()));
      johnDoe.setLanguagesSpoken(newList("alligator"));
      johnDoe.setUpdated(new Date());
      johnDoe.setLivingArrangement("hammock");
      johnDoe.setLookingFor("jane doe");
      johnDoe.setMovies(newList("movies"));
      johnDoe.setMusic(newList("music"));
      johnDoe.setNickname("johnny boy");
      johnDoe.setPets("simple doe");
      johnDoe.setPhoneNumbers(newList(new Phone("+33H000000000", "home")));
      johnDoe.setPoliticalViews("none");
      johnDoe.setProfileSong(new Url("here", "i", "am"));
      johnDoe.setProfileUrl("http://niceness");
      johnDoe.setProfileVideo(new Url("here", "i", "am"));
      johnDoe.setQuotes(newList("quotes"));
      johnDoe.setRelationshipStatus("relationships");
      johnDoe.setReligion("religion");
      johnDoe.setRomance("romance");
      johnDoe.setScaredOf("scared of what");
      johnDoe.setSchools(newList(new Organization()));
      johnDoe.setSexualOrientation("sexy");
      johnDoe.setSmoker(new Enum<Enum.Smoker>(Enum.Smoker.REGULARLY));
      johnDoe.setSports(newList("ping pong"));
      johnDoe.setStatus("away");
      johnDoe.setTags(newList("tags"));
      johnDoe.setThumbnailUrl("http://beauty");
      johnDoe.setTimeZone(11L);
      johnDoe.setTurnOffs(newList("off"));
      johnDoe.setTurnOns(newList("on"));
      johnDoe.setTvShows(newList("no tv"));
      johnDoe.setUrls(newList(new Url("where", "are", "you")));
      johnDoe.setNetworkPresence(new Enum<Enum.NetworkPresence>(
          Enum.NetworkPresence.DND));


      // setup Jane Doe
      janeDoe = new Person("jane.doe", new Name("Jane Doe"));
      janeDoe.setUpdated(new Date());

      // setup Simple Doe
      simpleDoe = new Person("simple.doe", new Name("Simple Doe"));
      simpleDoe.setUpdated(new Date());
    }

    private static <T> List<T> newList(T item) {
      // TODO: Get from Google Collections
      List<T> items = new ArrayList<T>();
      items.add(item);
      return items;
    }

    public Map<String, Person> allPeople = new HashMap<String, Person>();
    public Map<String, List<String>> friendIds
        = new HashMap<String, List<String>>();

    public MockPeopleService() {
      allPeople.put(johnDoe.getId(), johnDoe);
      allPeople.put(janeDoe.getId(), janeDoe);
      allPeople.put(simpleDoe.getId(), simpleDoe);

      // Jane and Simple are John's friends.
      List<String> johnsFriends = new ArrayList<String>();
      johnsFriends.add(janeDoe.getId());
      johnsFriends.add(simpleDoe.getId());
      friendIds.put(johnDoe.getId(), johnsFriends);

      // John is Jane's friend.      
      List<String> janesFriends = new ArrayList<String>();
      janesFriends.add(johnDoe.getId());
      friendIds.put(janeDoe.getId(), janesFriends);
    }

    public List<String> getIds(IdSpec idSpec, SecurityToken token)
        throws JSONException {
      List<String> ids = new ArrayList<String>();
      switch (idSpec.getType()) {
        case VIEWER :
          ids.add(token.getViewerId());
          break;
        case OWNER :
          ids.add(token.getOwnerId());
          break;
        case OWNER_FRIENDS:
          ids.addAll(friendIds.get(token.getOwnerId()));
          break;
        case VIEWER_FRIENDS:
          ids.addAll(friendIds.get(token.getViewerId()));
          break;
        case USER_IDS:
          ids.addAll(idSpec.fetchUserIds());
          break;
      }
      return ids;
    }

    public ResponseItem<ApiCollection<Person>> getPeople(List<String> ids,
        SortOrder sortOrder, FilterType filter, int first, int max,
        Set<String> profileDetails, SecurityToken token) {
      List<Person> people = new ArrayList<Person>();
      for (String id : ids) {
        people.add(allPeople.get(id));
      }
      // TODO: paginaton, sorting etc
      return new ResponseItem<ApiCollection<Person>>(
          new ApiCollection<Person>(people));
    }

    public ResponseItem<Person> getPerson(String id, SecurityToken token) {
      Person person = allPeople.get(id);
      return new ResponseItem<Person>(person);
    }
  }

  public static class MockDataService implements DataService {
    private Map<String, Map<String, String>> data
        = new HashMap<String, Map<String, String>>();

    public MockDataService() {
      Map<String, String> johnData = new HashMap<String, String>();
      johnData.put("count", "0");

      Map<String, String> janeData = new HashMap<String, String>();
      janeData.put("count", "5");

      Map<String, String> simpleData = new HashMap<String, String>();
      simpleData.put("count", "7");

      data.put(MockPeopleService.johnDoe.getId(), johnData);
      data.put(MockPeopleService.janeDoe.getId(), janeData);
      data.put(MockPeopleService.simpleDoe.getId(), simpleData);
    }

    public ResponseItem<Map<String, Map<String, String>>> getPersonData(
        List<String> ids, List<String> keys, SecurityToken token) {
      Map<String, Map<String, String>> dataToReturn
          = new HashMap<String, Map<String, String>>();
      for (String id : ids) {
        Map<String, String> allPersonData = data.get(id);

        // Filter by keys
        Map<String, String> personDataToReturn = new HashMap<String, String>();
        if (keys != null) {
          for (String key : keys) {
            personDataToReturn.put(key, allPersonData.get(key));
          }
        } else {
          personDataToReturn = allPersonData;
        }

        dataToReturn.put(id, personDataToReturn);
      }
      return new ResponseItem<Map<String, Map<String, String>>>(dataToReturn);
    }

    public ResponseItem updatePersonData(String id, String key, String value,
        SecurityToken token) {
      data.get(id).put(key, value);
      return new ResponseItem<Object>(null);
    }
  }

  public static class MockActivitiesService implements ActivitiesService {
    public static Activity johnActivity;
    public static Activity janeActivity;

    static {
      johnActivity = new Activity("1", MockPeopleService.johnDoe.getId());
      johnActivity.setTitle("yellow");
      johnActivity.setBody("what a color!");
      johnActivity.setUpdated(new Date());

      janeActivity = new Activity("2", MockPeopleService.janeDoe.getId());
      janeActivity.setTitle("green");
      janeActivity.setBody("a better color!");
      janeActivity.setUpdated(new Date());
    }

    public Map<String, List<Activity>> activities
        = new HashMap<String, List<Activity>>();

    public MockActivitiesService() {
      List<Activity> johnsActivities = new ArrayList<Activity>();
      johnsActivities.add(johnActivity);

      List<Activity> janesActivities = new ArrayList<Activity>();
      janesActivities.add(janeActivity);

      List<Activity> simplesActivities = new ArrayList<Activity>();

      activities.put(MockPeopleService.johnDoe.getId(), johnsActivities);
      activities.put(MockPeopleService.janeDoe.getId(), janesActivities);
      activities.put(MockPeopleService.simpleDoe.getId(), simplesActivities);
    }

    public ResponseItem<List<Activity>> getActivities(List<String> ids,
        SecurityToken token) {
      List<Activity> allActivities = new ArrayList<Activity>();
      for (String id : ids) {
        allActivities.addAll(activities.get(id));
      }
      return new ResponseItem<List<Activity>>(allActivities);
    }

    public ResponseItem createActivity(String personId, Activity activity,
        SecurityToken token) {
      activities.get(personId).add(activity);
      return new ResponseItem<Object>(null);
    }

    public ResponseItem<Activity> getActivity(String id, String activityId,
        SecurityToken token) {
      List<Activity> allActivities = activities.get(id);
      for (Activity activity : allActivities) {
        if (activity.getId().equals(activityId)) {
          return new ResponseItem<Activity>(activity);
        }
      }
      return new ResponseItem<Activity>(ResponseError.BAD_REQUEST, "", null);
    }
  }
}