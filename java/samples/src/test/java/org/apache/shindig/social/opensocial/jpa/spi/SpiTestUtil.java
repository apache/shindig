/* Licensed to the Apache Software Foundation (ASF) under one
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

package org.apache.shindig.social.opensocial.jpa.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.protocol.model.Enum;
import org.apache.shindig.protocol.model.EnumImpl;
import org.apache.shindig.social.core.model.ActivityImpl;
import org.apache.shindig.social.core.model.AddressImpl;
import org.apache.shindig.social.core.model.BodyTypeImpl;
import org.apache.shindig.social.core.model.ListFieldImpl;
import org.apache.shindig.social.core.model.NameImpl;
import org.apache.shindig.social.core.model.OrganizationImpl;
import org.apache.shindig.social.core.model.PersonImpl;
import org.apache.shindig.social.core.model.UrlImpl;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.BodyType;
import org.apache.shindig.social.opensocial.model.Drinker;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.LookingFor;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.NetworkPresence;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Smoker;
import org.apache.shindig.social.opensocial.model.Url;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.apache.shindig.social.opensocial.spi.UserId.Type;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


/**
 * 
 * Convenient build and assert methods used by spi unit tests
 *
 */
public class SpiTestUtil {
  
  public final static SecurityToken DEFAULT_TEST_SECURITY_TOKEN = (SecurityToken) new FakeGadgetToken();
  public final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
  
  /*
   * Build canonical test person instance
   */
  public static Person buildCanonicalPerson() {    
    NameImpl name = new NameImpl("Sir Shin H. Digg Social Butterfly");
    name.setAdditionalName("H");
    name.setFamilyName("Digg");
    name.setGivenName("Shin");
    name.setHonorificPrefix("Sir");
    name.setHonorificSuffix("Social Butterfly");
    
    Person canonical = new PersonImpl("canonical", "Shin Digg", name);

    canonical.setAboutMe("I have an example of every piece of data");
    canonical.setActivities(Lists.newArrayList("Coding Shindig"));

    Address address = new AddressImpl("PoBox 3565, 1 OpenStandards Way, Apache, CA");
    address.setCountry("US");
    address.setLatitude(28.3043F);
    address.setLongitude(143.0859F);
    address.setLocality("who knows");
    address.setPostalCode("12345");
    address.setRegion("Apache, CA");
    address.setStreetAddress("1 OpenStandards Way");
    address.setType("home");
    address.setFormatted("PoBox 3565, 1 OpenStandards Way, Apache, CA");
    canonical.setAddresses(Lists.newArrayList(address));

    canonical.setAge(33);
    BodyTypeImpl bodyType = new BodyTypeImpl();
    bodyType.setBuild("svelte");
    bodyType.setEyeColor("blue");
    bodyType.setHairColor("black");
    bodyType.setHeight(1.84F);
    bodyType.setWeight(74F);
    canonical.setBodyType(bodyType);

    canonical.setBooks(Lists.newArrayList("The Cathedral & the Bazaar", "Catch 22"));
    canonical.setCars(Lists.newArrayList("beetle", "prius"));
    canonical.setChildren("3");
    AddressImpl location = new AddressImpl();
    location.setLatitude(48.858193F);
    location.setLongitude(2.29419F);
    canonical.setCurrentLocation(location);

    canonical.setBirthday(buildDate("1975-01-01"));
    canonical.setDrinker(new EnumImpl<Drinker>(Drinker.SOCIALLY));
    ListField email = new ListFieldImpl("work", "dev@shindig.apache.org");
    canonical.setEmails(Lists.newArrayList(email));

    canonical.setEthnicity("developer");
    canonical.setFashion("t-shirts");
    canonical.setFood(Lists.newArrayList("sushi", "burgers"));
    canonical.setGender(Person.Gender.male);
    canonical.setHappiestWhen("coding");
    canonical.setHasApp(true);
    canonical.setHeroes(Lists.newArrayList("Doug Crockford", "Charles Babbage"));
    canonical.setHumor("none to speak of");
    canonical.setInterests(Lists.newArrayList("PHP", "Java"));
    canonical.setJobInterests("will work for beer");

    Organization job1 = new OrganizationImpl();
    job1.setAddress(new AddressImpl("1 Shindig Drive"));
    job1.setDescription("lots of coding");
    job1.setEndDate(buildDate("2010-10-10"));
    job1.setField("Software Engineering");
    job1.setName("Apache.com");
    job1.setSalary("$1000000000");
    job1.setStartDate(buildDate("1995-01-01"));
    job1.setSubField("Development");
    job1.setTitle("Grand PooBah");
    job1.setWebpage("http://shindig.apache.org/");
    job1.setType("job");

    Organization job2 = new OrganizationImpl();
    job2.setAddress(new AddressImpl("1 Skid Row"));
    job2.setDescription("");
    job2.setEndDate(buildDate("1995-01-01"));
    job2.setField("College");
    job2.setName("School of hard Knocks");
    job2.setSalary("$100");
    job2.setStartDate(buildDate("1991-01-01"));
    job2.setSubField("Lab Tech");
    job2.setTitle("Gopher");
    job2.setWebpage("");
    job2.setType("job");

    canonical.setOrganizations(Lists.newArrayList(job1, job2));

    canonical.setUpdated(new Date());
    canonical.setLanguagesSpoken(Lists.newArrayList("English", "Dutch", "Esperanto"));
    canonical.setLivingArrangement("in a house");
    List<Enum<LookingFor>> lookingFor = Lists.newArrayList();
    Enum<LookingFor> lookingForOne = new EnumImpl<LookingFor>(LookingFor.RANDOM);
    Enum<LookingFor> lookingForTwo = new EnumImpl<LookingFor>(LookingFor.NETWORKING);
    lookingFor.add(lookingForOne);
    lookingFor.add(lookingForTwo);
    canonical.setLookingFor(lookingFor);
    canonical.setMovies(Lists.newArrayList("Iron Man", "Nosferatu"));
    canonical.setMusic(Lists.newArrayList("Chieftains", "Beck"));
    canonical.setNetworkPresence(new EnumImpl<NetworkPresence>(NetworkPresence.ONLINE));
    canonical.setNickname("diggy");
    canonical.setPets("dog,cat");
    canonical.setPhoneNumbers(Lists.<ListField> newArrayList(new ListFieldImpl("work",
        "111-111-111"), new ListFieldImpl("mobile", "999-999-999")));

    canonical.setPoliticalViews("open leaning");
    canonical.setProfileSong(new UrlImpl("http://www.example.org/songs/OnlyTheLonely.mp3",
        "Feelin' blue", "road"));
    canonical.setProfileVideo(new UrlImpl("http://www.example.org/videos/Thriller.flv",
        "Thriller", "video"));

    canonical.setQuotes(Lists.newArrayList("I am therfore I code", "Doh!"));
    canonical.setRelationshipStatus("married to my job");
    canonical.setReligion("druidic");
    canonical.setRomance("twice a year");
    canonical.setScaredOf("COBOL");
    canonical.setSexualOrientation("north");
    canonical.setSmoker(new EnumImpl<Smoker>(Smoker.NO));
    canonical.setSports(Lists.newArrayList("frisbee", "rugby"));
    canonical.setStatus("happy");
    canonical.setTags(Lists.newArrayList("C#", "JSON", "template"));
    canonical.setThumbnailUrl("http://www.example.org/pic/?id=1");
    canonical.setUtcOffset(-8L);
    canonical.setTurnOffs(Lists.newArrayList("lack of unit tests", "cabbage"));
    canonical.setTurnOns(Lists.newArrayList("well document code"));
    canonical.setTvShows(Lists.newArrayList("House", "Battlestar Galactica"));

    canonical.setUrls(Lists.<Url>newArrayList(
        new UrlImpl("http://www.example.org/?id=1", "my profile", "Profile"),
        new UrlImpl("http://www.example.org/pic/?id=1", "my awesome picture", "Thumbnail")));
    
    return canonical;
  }
  
  /*
   * Build userId set
   */
  public static Set<UserId> buildUserIds(String... userIds) {
    // Set user id list
    Set<UserId> userIdSet = Sets.newHashSet();
    for (String userId: userIds) {
      userIdSet.add(new UserId(Type.userId, userId));
    }
    return userIdSet;
  }
  
  /*
   * Asserts actual person instance has the expected person id and (formatted) name.
   * 
   */
  public static void assertPersonEquals(Person actual, String expectedId, String expectedName) {
    assertEquals(actual.getId(), expectedId);
    assertNotNull(actual.getName());
    assertEquals(actual.getName().getFormatted(), expectedName);
  }

  /*
   * Asserts actual person instance equals expected person instance
   * 
   * Verified each individual variable so to know which variable is causing it to fail.
   * Note that person.updated isn't verified as we can't expect this to be equal.
   * 
   */
  public static void assertPersonEquals(Person actual, Person expected) {
    
    assertEquals(actual.getAboutMe(), expected.getAboutMe());
    assertEquals(actual.getActivities(), expected.getActivities());    
    assertCollectionSizeEquals(actual.getAddresses(), expected.getAddresses());
    for (int i = 0; i < actual.getAddresses().size(); i++) {
      assertAddressEquals(actual.getAddresses().get(i), expected.getAddresses().get(i));
    }    
    assertEquals(actual.getAge(), expected.getAge());    
    assertBodyTypeEquals(actual.getBodyType(), expected.getBodyType());
    assertEquals(actual.getBooks(), expected.getBooks());
    assertEquals(actual.getCars(), expected.getCars());
    assertEquals(actual.getChildren(), expected.getChildren());    
    assertAddressEquals(actual.getCurrentLocation(), expected.getCurrentLocation());    
    assertEquals(actual.getDisplayName(), expected.getDisplayName());
    assertEquals(actual.getBirthday(), expected.getBirthday());    
    assertCollectionSizeEquals(actual.getEmails(), expected.getEmails());
    for (int i = 0; i < actual.getEmails().size(); i++) {
      assertListFieldEquals(actual.getEmails().get(i), expected.getEmails().get(i));
    }    
    assertEquals(actual.getEthnicity(), expected.getEthnicity());
    assertEquals(actual.getFashion(), expected.getFashion());
    assertEquals(actual.getFood(), expected.getFood());
    assertEquals(actual.getGender(), expected.getGender());
    assertEquals(actual.getHappiestWhen(), expected.getHappiestWhen());
    assertEquals(actual.getHasApp(), expected.getHasApp());
    assertEquals(actual.getHeroes(), expected.getHeroes());
    assertEquals(actual.getHumor(), expected.getHumor());
    assertEquals(actual.getId(), expected.getId());
    assertEquals(actual.getInterests(), expected.getInterests());
    assertEquals(actual.getJobInterests(), expected.getJobInterests());    
    assertCollectionSizeEquals(actual.getOrganizations(), expected.getOrganizations());
    for (int i = 0; i < actual.getOrganizations().size(); i++) {
      assertOrganizationEquals(actual.getOrganizations().get(i), expected.getOrganizations().get(i));
    }    
    assertEquals(actual.getLanguagesSpoken(), expected.getLanguagesSpoken());
    assertEquals(actual.getLivingArrangement(), expected.getLivingArrangement());    
    assertCollectionSizeEquals(actual.getLookingFor(), expected.getLookingFor());
    for (int i = 0; i < actual.getLookingFor().size(); i++) {
      assertEquals(actual.getLookingFor().get(i).getValue(), expected.getLookingFor().get(i).getValue());
    }    
    assertEquals(actual.getMovies(), expected.getMovies());
    assertEquals(actual.getMusic(), expected.getMusic());    
    assertNameEquals(actual.getName(), expected.getName());
    assertEquals(actual.getNetworkPresence().getValue(), expected.getNetworkPresence().getValue());
    assertEquals(actual.getNickname(), expected.getNickname());
    assertEquals(actual.getPets(), expected.getPets());    
    assertCollectionSizeEquals(actual.getPhoneNumbers(), expected.getPhoneNumbers());
    for (int i = 0; i < actual.getPhoneNumbers().size(); i++) {
      assertListFieldEquals(actual.getPhoneNumbers().get(i), expected.getPhoneNumbers().get(i));
    }    
    assertEquals(actual.getPoliticalViews(), expected.getPoliticalViews());    
    assertUrlEquals(actual.getProfileSong(), expected.getProfileSong());    
    assertEquals(actual.getProfileUrl(), expected.getProfileUrl());
    assertUrlEquals(actual.getProfileVideo(), expected.getProfileVideo());
    assertEquals(actual.getQuotes(), expected.getQuotes());
    assertEquals(actual.getRelationshipStatus(), expected.getRelationshipStatus());
    assertEquals(actual.getReligion(), expected.getReligion());
    assertEquals(actual.getRomance(), expected.getRomance());
    assertEquals(actual.getScaredOf(), expected.getScaredOf());
    assertEquals(actual.getSexualOrientation(), expected.getSexualOrientation());
    assertEquals(actual.getSmoker().getValue(), expected.getSmoker().getValue());
    assertEquals(actual.getSports(), expected.getSports());
    assertEquals(actual.getStatus(), expected.getStatus());
    assertEquals(actual.getTags(), expected.getTags());
    assertEquals(actual.getThumbnailUrl(), expected.getThumbnailUrl());
    assertEquals(actual.getUtcOffset(), expected.getUtcOffset());
    assertEquals(actual.getTurnOffs(), expected.getTurnOffs());
    assertEquals(actual.getTurnOns(), expected.getTurnOns());
    assertEquals(actual.getTvShows(), expected.getTvShows());    
  }
  
  private static void assertUrlEquals(Url actual, Url expected) {
    assertEquals(actual.getLinkText(), expected.getLinkText());
    assertEquals(actual.getPrimary(), expected.getPrimary());
    assertEquals(actual.getType(), expected.getType());
    assertEquals(actual.getValue(), expected.getValue());    
  }

  private static void assertNameEquals(Name actual, Name expected) {
    assertEquals(actual.getAdditionalName(), expected.getAdditionalName());
    assertEquals(actual.getFamilyName(), expected.getFamilyName());
    assertEquals(actual.getGivenName(), expected.getGivenName());
    assertEquals(actual.getHonorificPrefix(), expected.getHonorificPrefix());
    assertEquals(actual.getHonorificSuffix(), expected.getHonorificSuffix());
    assertEquals(actual.getFormatted(), expected.getFormatted());    
  }

  private static void assertOrganizationEquals(Organization actual, Organization expected) {
    assertAddressEquals(actual.getAddress(), expected.getAddress());
    assertEquals(actual.getDescription(), expected.getDescription());
    assertEquals(actual.getEndDate(), expected.getEndDate());
    assertEquals(actual.getField(), expected.getField());
    assertEquals(actual.getName(), expected.getName());
    assertEquals(actual.getPrimary(), expected.getPrimary());
    assertEquals(actual.getSalary(), expected.getSalary());
    assertEquals(actual.getStartDate(), expected.getStartDate());
    assertEquals(actual.getSubField(), expected.getSubField());
    assertEquals(actual.getTitle(), expected.getTitle());
    assertEquals(actual.getType(), expected.getType());
    assertEquals(actual.getWebpage(), expected.getWebpage());    
  }

  private static void assertCollectionSizeEquals(Collection<?> actual, Collection<?> expected) {
    assertTrue(actual != null && expected != null);
    assertEquals(actual.size(), expected.size());
  }
  
  private static void assertAddressEquals(Address actual, Address expected) {    
    assertEquals(actual.getCountry(), expected.getCountry());
    assertEquals(actual.getLatitude(), expected.getLatitude());
    assertEquals(actual.getLocality(), expected.getLocality());
    assertEquals(actual.getLongitude(), expected.getLongitude());
    assertEquals(actual.getPostalCode(), expected.getPostalCode());
    assertEquals(actual.getRegion(), expected.getRegion());
    assertEquals(actual.getStreetAddress(), expected.getStreetAddress());
    assertEquals(actual.getType(), expected.getType());
    assertEquals(actual.getFormatted(), expected.getFormatted());    
  }

  private static void assertBodyTypeEquals(BodyType actual, BodyType expected) {
    assertEquals(actual.getBuild(), expected.getBuild());
    assertEquals(actual.getEyeColor(), expected.getEyeColor());
    assertEquals(actual.getHairColor(), expected.getHairColor());
    assertEquals(actual.getHeight(), expected.getHeight());
    assertEquals(actual.getWeight(), expected.getWeight());
  }
  
  private static void assertListFieldEquals(ListField actual, ListField expected) {
    assertEquals(actual.getPrimary(), expected.getPrimary());
    assertEquals(actual.getType(), expected.getType());
    assertEquals(actual.getValue(), expected.getValue());
  }
  
  public static void assertActivityEquals(Activity actual, Activity expected) {
    assertEquals(actual.getId(), expected.getId());
    assertEquals(actual.getUserId(), expected.getUserId());
    assertEquals(actual.getTitle(), expected.getTitle());
    assertEquals(actual.getBody(), expected.getBody());
  }
  
  public static Activity buildTestActivity(String id, String userId, String title, String body) {
    Activity activity = new ActivityImpl(id, userId); 
    activity.setTitle(title);
    activity.setBody(body);
    return activity;
  }
  
  public static Date buildDate(String dateAsString) {    
    try {
      return DATE_FORMATTER.parse(dateAsString);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse date - " + dateAsString, e);
    }
  }
  
  public static Set<String> asSet(String... items) {
    return Sets.newHashSet(Arrays.asList(items));
  }
  
}
