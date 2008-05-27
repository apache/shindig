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
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.social.opensocial.ActivitiesService;
import org.apache.shindig.social.opensocial.DataService;
import org.apache.shindig.social.opensocial.PeopleService;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.BodyType;
import org.apache.shindig.social.opensocial.model.Email;
import org.apache.shindig.social.opensocial.model.Enum;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Phone;
import org.apache.shindig.social.opensocial.model.Url;
import org.apache.shindig.social.samplecontainer.BasicActivitiesService;
import org.apache.shindig.social.samplecontainer.BasicDataService;
import org.apache.shindig.social.samplecontainer.BasicPeopleService;
import org.apache.shindig.social.samplecontainer.XmlStateFileFetcher;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Provides social api component injection for all large tests
 */
public class SocialApiTestsGuiceModule extends AbstractModule {
  private static Logger logger =
      Logger.getLogger(SocialApiTestsGuiceModule.class.getName());

  @Override
  protected void configure() {
    bind(PeopleService.class).to(BasicPeopleService.class);
    bind(DataService.class).to(BasicDataService.class);
    bind(ActivitiesService.class).to(BasicActivitiesService.class);

    bind(XmlStateFileFetcher.class).to(MockXmlStateFileFetcher.class);

    bind(SecurityTokenDecoder.class).to(BasicSecurityTokenDecoder.class);
  }

  public static class MockXmlStateFileFetcher extends XmlStateFileFetcher {
    public static final Person johnDoe;
    public static final Person janeDoe;
    public static final Person simpleDoe;

    public static Activity johnActivity;
    public static Activity janeActivity;

    static {
      // setup John Doe
      johnDoe = new Person("john.doe", new Name("John Doe"));

      // John should have every field filled in
      johnDoe.setAboutMe("about me");
      johnDoe.setActivities(Lists.newArrayList("activity"));
      johnDoe.setAddresses(Lists.newArrayList(new Address("My home address")));
      johnDoe.setAge(5);
      johnDoe.setBodyType(new BodyType()); //TODO
      johnDoe.setBooks(Lists.newArrayList("books"));
      johnDoe.setCars(Lists.newArrayList("cars"));
      johnDoe.setChildren("children");
      johnDoe.setCurrentLocation(new Address("my location"));
      johnDoe.setDateOfBirth(new Date());
      johnDoe.setDrinker(new Enum<Enum.Drinker>(Enum.Drinker.HEAVILY));
      johnDoe.setEmails(Lists.newArrayList(
          new Email("john.doe@work.bar", "work")));
      johnDoe.setEthnicity("purple");
      johnDoe.setFashion("so fashionable");
      johnDoe.setFood(Lists.newArrayList("gruel"));
      johnDoe.setGender(new Enum<Enum.Gender>(Enum.Gender.MALE));
      johnDoe.setHappiestWhen("puppies");
      johnDoe.setHasApp(true);
      johnDoe.setHeroes(Lists.newArrayList("the moon"));
      johnDoe.setHumor("not so good");
      johnDoe.setInterests(Lists.newArrayList("kites"));
      johnDoe.setJobInterests("penguins");
      johnDoe.setJobs(Lists.newArrayList(new Organization()));
      johnDoe.setLanguagesSpoken(Lists.newArrayList("alligator"));
      johnDoe.setUpdated(new Date());
      johnDoe.setLivingArrangement("hammock");
      johnDoe.setLookingFor("jane doe");
      johnDoe.setMovies(Lists.newArrayList("movies"));
      johnDoe.setMusic(Lists.newArrayList("music"));
      johnDoe.setNetworkPresence(new Enum<Enum.NetworkPresence>(
          Enum.NetworkPresence.DND));
      johnDoe.setNickname("johnny boy");
      johnDoe.setPets("simple doe");
      johnDoe.setPhoneNumbers(Lists.newArrayList(
          new Phone("+33H000000000", "home")));
      johnDoe.setPoliticalViews("none");
      johnDoe.setProfileSong(new Url("here", "i", "am"));
      johnDoe.setProfileUrl("http://niceness");
      johnDoe.setProfileVideo(new Url("here", "i", "am"));
      johnDoe.setQuotes(Lists.newArrayList("quotes"));
      johnDoe.setRelationshipStatus("relationships");
      johnDoe.setReligion("religion");
      johnDoe.setRomance("romance");
      johnDoe.setScaredOf("scared of what");
      johnDoe.setSchools(Lists.newArrayList(new Organization()));
      johnDoe.setSexualOrientation("sexy");
      johnDoe.setSmoker(new Enum<Enum.Smoker>(Enum.Smoker.REGULARLY));
      johnDoe.setSports(Lists.newArrayList("ping pong"));
      johnDoe.setStatus("away");
      johnDoe.setTags(Lists.newArrayList("tags"));
      johnDoe.setThumbnailUrl("http://beauty");
      johnDoe.setTimeZone(11L);
      johnDoe.setTurnOffs(Lists.newArrayList("off"));
      johnDoe.setTurnOns(Lists.newArrayList("on"));
      johnDoe.setTvShows(Lists.newArrayList("no tv"));
      johnDoe.setUrls(Lists.newArrayList(new Url("where", "are", "you")));

      // setup Jane Doe
      janeDoe = new Person("jane.doe", new Name("Jane Doe"));
      janeDoe.setUpdated(new Date());

      // setup Simple Doe
      simpleDoe = new Person("simple.doe", new Name("Simple Doe"));
      simpleDoe.setUpdated(new Date());

      // setup activities
      johnActivity = new Activity("1", johnDoe.getId());
      johnActivity.setTitle("yellow");
      johnActivity.setBody("what a color!");
      johnActivity.setUpdated(new Date());

      janeActivity = new Activity("2", janeDoe.getId());
      janeActivity.setTitle("green");
      janeActivity.setBody("a better color!");
      janeActivity.setUpdated(new Date());
    }

    public MockXmlStateFileFetcher() {
      allPeople = Maps.newHashMap();
      allPeople.put(johnDoe.getId(), johnDoe);
      allPeople.put(janeDoe.getId(), janeDoe);
      allPeople.put(simpleDoe.getId(), simpleDoe);

      // Jane and Simple are John's friends.
      friendIdMap = Maps.newHashMap();
      friendIdMap.put(johnDoe.getId(), Lists.newArrayList(janeDoe.getId(),
          simpleDoe.getId()));

      // John is Jane's friend.
      friendIdMap.put(janeDoe.getId(), Lists.newArrayList(johnDoe.getId()));

      Map<String, String> johnData = new HashMap<String, String>();
      johnData.put("count", "0");

      Map<String, String> janeData = new HashMap<String, String>();
      janeData.put("count", "5");

      Map<String, String> simpleData = new HashMap<String, String>();
      simpleData.put("count", "7");

      allData = Maps.newHashMap();
      allData.put(johnDoe.getId(), johnData);
      allData.put(janeDoe.getId(), janeData);
      allData.put(simpleDoe.getId(), simpleData);

      List<Activity> simplesActivities = new ArrayList<Activity>();

      allActivities = Maps.newHashMap();
      allActivities.put(johnDoe.getId(), Lists.newArrayList(johnActivity));
      allActivities.put(janeDoe.getId(), Lists.newArrayList(janeActivity));
      allActivities.put(simpleDoe.getId(), simplesActivities);
    }

    public void resetStateFile(URI stateFile) {
      // Ignore
    }

    public void loadDefaultStateFileIfNoneLoaded() {
      // Ignore
    }

    public void setEvilness(boolean doEvil) {
      throw new UnsupportedOperationException();
    }

  }
}
