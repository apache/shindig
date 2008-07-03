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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import org.apache.shindig.common.BasicSecurityTokenDecoder;
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.servlet.ParameterFetcher;
import org.apache.shindig.social.abdera.SocialRouteManager;
import org.apache.shindig.social.dataservice.ActivityService;
import org.apache.shindig.social.dataservice.AppDataService;
import org.apache.shindig.social.dataservice.DataServiceServletFetcher;
import org.apache.shindig.social.dataservice.PersonService;
import org.apache.shindig.social.opensocial.ActivitiesService;
import org.apache.shindig.social.opensocial.DataService;
import org.apache.shindig.social.opensocial.DefaultModelGuiceModule;
import org.apache.shindig.social.opensocial.PeopleService;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.ActivityImpl;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.AddressImpl;
import org.apache.shindig.social.opensocial.model.BodyType;
import org.apache.shindig.social.opensocial.model.BodyTypeImpl;
import org.apache.shindig.social.opensocial.model.Email;
import org.apache.shindig.social.opensocial.model.EmailImpl;
import org.apache.shindig.social.opensocial.model.Enum;
import org.apache.shindig.social.opensocial.model.EnumImpl;
import org.apache.shindig.social.opensocial.model.NameImpl;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.OrganizationImpl;
import org.apache.shindig.social.opensocial.model.PersonImpl;
import org.apache.shindig.social.opensocial.model.Phone;
import org.apache.shindig.social.opensocial.model.PhoneImpl;
import org.apache.shindig.social.opensocial.model.Url;
import org.apache.shindig.social.opensocial.model.UrlImpl;
import org.apache.shindig.social.samplecontainer.BasicActivitiesService;
import org.apache.shindig.social.samplecontainer.BasicDataService;
import org.apache.shindig.social.samplecontainer.BasicPeopleService;
import org.apache.shindig.social.samplecontainer.SampleContainerRouteManager;
import org.apache.shindig.social.samplecontainer.XmlStateFileFetcher;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Provides social api component injection for all large tests
 */
public class SocialApiTestsGuiceModule extends DefaultModelGuiceModule {
  private static Logger logger =
      Logger.getLogger(SocialApiTestsGuiceModule.class.getName());

  @Override
  protected void configure() {
    super.configure();
    bind(PeopleService.class).to(BasicPeopleService.class);
    bind(DataService.class).to(BasicDataService.class);
    bind(ActivitiesService.class).to(BasicActivitiesService.class);

    bind(PersonService.class).to(BasicPeopleService.class);
    bind(ActivityService.class).to(BasicActivitiesService.class);
    bind(AppDataService.class).to(BasicDataService.class);

    bind(XmlStateFileFetcher.class).to(MockXmlStateFileFetcher.class);
    bind(SocialRouteManager.class).to(SampleContainerRouteManager.class);

    bind(SecurityTokenDecoder.class).to(BasicSecurityTokenDecoder.class);

    bind(ParameterFetcher.class).annotatedWith(Names.named("GadgetDataServlet")).to(GadgetDataServletFetcher.class);
    bind(ParameterFetcher.class).annotatedWith(Names.named("DataServiceServlet")).to(DataServiceServletFetcher.class);
  }

  @Singleton
  public static class MockXmlStateFileFetcher extends XmlStateFileFetcher {
    public static final PersonImpl johnDoe;
    public static final PersonImpl janeDoe;
    public static final PersonImpl simpleDoe;

    public static ActivityImpl johnActivity;
    public static ActivityImpl janeActivity;

    static {
      // setup John Doe
      johnDoe = new PersonImpl("john.doe", new NameImpl("John Doe"));

      // John should have every field filled in
      johnDoe.setAboutMe("about me");
      johnDoe.setActivities(Lists.newArrayList("activity"));

      AddressImpl homeAddress = new AddressImpl("My home address");
      homeAddress.setCountry("super");
      homeAddress.setExtendedAddress("cali");
      homeAddress.setLatitude(new Float(1.0));
      homeAddress.setLocality("fragi");
      homeAddress.setLongitude(new Float(1.0));
      homeAddress.setPoBox("listic");
      homeAddress.setPostalCode("559");
      homeAddress.setRegion("expi");
      homeAddress.setStreetAddress("ali");
      homeAddress.setType("docious");
      homeAddress.setUnstructuredAddress("supercalifragilisticexpialidocious");
      johnDoe.setAddresses(Lists.<Address>newArrayList(homeAddress));

      johnDoe.setAge(5);

      BodyType bodyType = new BodyTypeImpl();
      bodyType.setBuild("flying purple people eater");
      bodyType.setEyeColor("one eyed");
      bodyType.setHairColor("one horned");
      bodyType.setHeight("8675309");
      bodyType.setWeight("90210");
      johnDoe.setBodyType(bodyType);

      johnDoe.setBooks(Lists.newArrayList("books"));
      johnDoe.setCars(Lists.newArrayList("cars"));
      johnDoe.setChildren("children");
      johnDoe.setCurrentLocation(new AddressImpl("my location"));
      johnDoe.setDateOfBirth(new Date());
      johnDoe.setDrinker(new EnumImpl<Enum.Drinker>(Enum.Drinker.HEAVILY));
      johnDoe.setEmails(Lists.<Email>newArrayList(
          new EmailImpl("john.doe@work.bar", "work")));
      johnDoe.setEthnicity("purple");
      johnDoe.setFashion("so fashionable");
      johnDoe.setFood(Lists.newArrayList("gruel"));
      johnDoe.setGender(new EnumImpl<Enum.Gender>(Enum.Gender.MALE));
      johnDoe.setHappiestWhen("puppies");
      johnDoe.setHasApp(true);
      johnDoe.setHeroes(Lists.newArrayList("the moon"));
      johnDoe.setHumor("not so good");
      johnDoe.setInterests(Lists.newArrayList("kites"));
      johnDoe.setJobInterests("penguins");

      OrganizationImpl job = new OrganizationImpl();
      job.setAddress(homeAddress);
      job.setDescription("um");
      job.setEndDate(new Date());
      job.setField("diddle");
      job.setName("diddle");
      job.setSalary("um");
      job.setStartDate(new Date());
      job.setSubField("diddleye");
      job.setTitle("Suoicodilaipxecitsiligarfilacrepus!");
      job.setWebpage("http://en.wikipedia.org/wiki/" +
          "Supercalifragilisticexpialidocious");
      johnDoe.setJobs(Lists.<Organization>newArrayList(job));

      johnDoe.setLanguagesSpoken(Lists.newArrayList("alligator"));
      johnDoe.setUpdated(new Date());
      johnDoe.setLivingArrangement("hammock");
      johnDoe.setLookingFor("jane doe");
      johnDoe.setMovies(Lists.newArrayList("movies"));
      johnDoe.setMusic(Lists.newArrayList("music"));
      johnDoe.setNetworkPresence(new EnumImpl<Enum.NetworkPresence>(
          Enum.NetworkPresence.DND));
      johnDoe.setNickname("johnny boy");
      johnDoe.setPets("simple doe");
      johnDoe.setPhoneNumbers(Lists.<Phone>newArrayList(
          new PhoneImpl("+33H000000000", "home")));
      johnDoe.setPoliticalViews("none");
      johnDoe.setProfileSong(new UrlImpl("here", "i", "am"));
      johnDoe.setProfileUrl("http://niceness");
      johnDoe.setProfileVideo(new UrlImpl("here", "i", "am"));
      johnDoe.setQuotes(Lists.newArrayList("quotes"));
      johnDoe.setRelationshipStatus("relationships");
      johnDoe.setReligion("religion");
      johnDoe.setRomance("romance");
      johnDoe.setScaredOf("scared of what");

      OrganizationImpl school = new OrganizationImpl();
      school.setAddress(homeAddress);
      school.setDescription("gummy");
      school.setEndDate(new Date());
      school.setField("bears");
      school.setName("bouncing");
      school.setSalary("here");
      school.setStartDate(new Date());
      school.setSubField("and there");
      school.setTitle("and everywhere");
      school.setWebpage("http://en.wikipedia.org/wiki/" +
          "Disney's_Adventures_of_the_Gummi_Bears");
      johnDoe.setSchools(Lists.<Organization>newArrayList(school));

      johnDoe.setSexualOrientation("sexy");
      johnDoe.setSmoker(new EnumImpl<Enum.Smoker>(Enum.Smoker.REGULARLY));
      johnDoe.setSports(Lists.newArrayList("ping pong"));
      johnDoe.setStatus("away");
      johnDoe.setTags(Lists.newArrayList("tags"));
      johnDoe.setThumbnailUrl("http://beauty");
      johnDoe.setTimeZone(11L);
      johnDoe.setTurnOffs(Lists.newArrayList("off"));
      johnDoe.setTurnOns(Lists.newArrayList("on"));
      johnDoe.setTvShows(Lists.newArrayList("no tv"));
      johnDoe.setUrls(Lists.<Url>newArrayList(new UrlImpl("where", "are", "you")));

      // setup Jane Doe
      janeDoe = new PersonImpl("jane.doe", new NameImpl("Jane Doe"));
      janeDoe.setUpdated(new Date());

      // setup Simple Doe
      simpleDoe = new PersonImpl("simple.doe", new NameImpl("Simple Doe"));
      simpleDoe.setUpdated(new Date());

      // setup activities
      johnActivity = new ActivityImpl("1", johnDoe.getId());
      johnActivity.setTitle("yellow");
      johnActivity.setBody("what a color!");
      johnActivity.setUpdated(new Date());

      janeActivity = new ActivityImpl("2", janeDoe.getId());
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

      List<Activity> simplesActivities = Lists.newArrayList();

      allActivities = Maps.newHashMap();
      allActivities.put(johnDoe.getId(), Lists.<Activity>newArrayList(johnActivity));
      allActivities.put(janeDoe.getId(), Lists.<Activity>newArrayList(janeActivity));
      allActivities.put(simpleDoe.getId(), simplesActivities);
    }

    public void resetStateFile(URI stateFile) {
      // Ignore
    }

    public void loadDefaultStateFileIfNoneLoaded() {
      // Ignore
    }

    public void setEvilness(boolean doEvil) {
      // Ignore
    }

  }
}
