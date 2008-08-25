/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.shindig.social.dataservice.integration;

import org.apache.shindig.social.core.model.AddressImpl;
import org.apache.shindig.social.core.model.BodyTypeImpl;
import org.apache.shindig.social.core.model.EmailImpl;
import org.apache.shindig.social.core.model.EnumImpl;
import org.apache.shindig.social.core.model.NameImpl;
import org.apache.shindig.social.core.model.OrganizationImpl;
import org.apache.shindig.social.core.model.PersonImpl;
import org.apache.shindig.social.core.model.PhoneImpl;
import org.apache.shindig.social.core.model.UrlImpl;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.BodyType;
import org.apache.shindig.social.opensocial.model.Email;
import org.apache.shindig.social.opensocial.model.Enum;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Phone;
import org.apache.shindig.social.opensocial.model.Url;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class RestfulJsonPeopleTest extends AbstractLargeRestfulTests {
  private Person canonical;

  protected void setUp() throws Exception {
    super.setUp();
    NameImpl name = new NameImpl("Sir Shin H. Digg Social Butterfly");
    name.setAdditionalName("H");
    name.setFamilyName("Digg");
    name.setGivenName("Shin");
    name.setHonorificPrefix("Sir");
    name.setHonorificSuffix("Social Butterfly");
    canonical = new PersonImpl("canonical", name);

    canonical.setAboutMe("I have an example of every piece of data");
    canonical.setActivities(Lists.newArrayList("Coding Shindig"));

    Address address = new AddressImpl("PoBox 3565, 1 OpenStandards Way, Apache, CA");
    address.setCountry("US");
    address.setExtendedAddress("Next door");
    address.setLatitude(28.3043F);
    address.setLongitude(143.0859F);
    address.setLocality("who knows");
    address.setPoBox("3653");
    address.setPostalCode("12345");
    address.setRegion("Apache, CA");
    address.setStreetAddress("1 OpenStandards Way");
    address.setType("home");
    canonical.setAddresses(Lists.newArrayList(address));

    canonical.setAge(33);
    BodyTypeImpl bodyType = new BodyTypeImpl();
    bodyType.setBuild("svelte");
    bodyType.setEyeColor("blue");
    bodyType.setHairColor("black");
    bodyType.setHeight("1.84M");
    bodyType.setWeight("184lbs");
    canonical.setBodyType(bodyType);

    canonical.setBooks(Lists.newArrayList("The Cathedral & the Bazaar", "Catch 22"));
    canonical.setCars(Lists.newArrayList("beetle", "prius"));
    canonical.setChildren("3");
    AddressImpl location = new AddressImpl();
    location.setLatitude(48.858193F);
    location.setLongitude(2.29419F);
    canonical.setCurrentLocation(location);

    canonical.setDateOfBirth(new Date());
    canonical.setDrinker(new EnumImpl<Enum.Drinker>(Enum.Drinker.SOCIALLY));
    Email email = new EmailImpl("shindig-dev@incubator.apache.org", "work");
    canonical.setEmails(Lists.newArrayList(email));

    canonical.setEthnicity("developer");
    canonical.setFashion("t-shirts");
    canonical.setFood(Lists.newArrayList("sushi", "burgers"));
    canonical.setGender(new EnumImpl<Enum.Gender>(Enum.Gender.MALE));
    canonical.setHappiestWhen("coding");
    canonical.setHasApp(true);
    canonical.setHeroes(Lists.newArrayList("Doug Crockford", "Charles Babbage"));
    canonical.setHumor("none to speak of");
    canonical.setInterests(Lists.newArrayList("PHP", "Java"));
    canonical.setJobInterests("will work for beer");

    Organization job1 = new OrganizationImpl();
    job1.setAddress(new AddressImpl("1 Shindig Drive"));
    job1.setDescription("lots of coding");
    job1.setEndDate(new Date());
    job1.setField("Software Engineering");
    job1.setName("Apache.com");
    job1.setSalary("$1000000000");
    job1.setStartDate(new Date());
    job1.setSubField("Development");
    job1.setTitle("Grand PooBah");
    job1.setWebpage("http://incubator.apache.org/projects/shindig.html");

    Organization job2 = new OrganizationImpl();
    job2.setAddress(new AddressImpl("1 Skid Row"));
    job2.setDescription("");
    job2.setEndDate(new Date());
    job2.setField("College");
    job2.setName("School of hard Knocks");
    job2.setSalary("$100");
    job2.setStartDate(new Date());
    job2.setSubField("Lab Tech");
    job2.setTitle("Gopher");
    job2.setWebpage("");

    canonical.setJobs(Lists.newArrayList(job1, job2));

    canonical.setUpdated(new Date());
    canonical.setLanguagesSpoken(Lists.newArrayList("English", "Dutch", "Esperanto"));
    canonical.setLivingArrangement("in a house");
    canonical.setLookingFor("patches");
    canonical.setMovies(Lists.newArrayList("Iron Man", "Nosferatu"));
    canonical.setMusic(Lists.newArrayList("Chieftains", "Beck"));
    canonical.setNetworkPresence(new EnumImpl<Enum.NetworkPresence>(Enum.NetworkPresence.ONLINE));
    canonical.setNickname("diggy");
    canonical.setPets("dog,cat");
    canonical.setPhoneNumbers(Lists.<Phone>newArrayList(new PhoneImpl("111-111-111", "work"),
        new PhoneImpl("999-999-999", "mobile")));

    canonical.setPoliticalViews("open leaning");
    canonical.setProfileSong(new UrlImpl("http://www.example.org/songs/OnlyTheLonely.mp3",
        "Feelin' blue", "road"));
    canonical.setProfileUrl("http://www.example.org/?id=1");
    canonical.setProfileVideo(new UrlImpl("http://www.example.org/videos/Thriller.flv",
        "Thriller", "video"));

    canonical.setQuotes(Lists.newArrayList("I am therfore I code", "Doh!"));
    canonical.setRelationshipStatus("married to my job");
    canonical.setReligion("druidic");
    canonical.setRomance("twice a year");
    canonical.setScaredOf("COBOL");

    Organization school = new OrganizationImpl();
    school.setAddress(new AddressImpl("1 Edu St."));
    school.setDescription("High School");
    school.setEndDate(new Date());
    school.setField("");
    school.setName("");
    school.setSalary("");
    school.setStartDate(new Date());
    school.setSubField("");
    school.setTitle("");
    school.setWebpage("");
    canonical.setSchools(Lists.newArrayList(school));

    canonical.setSexualOrientation("north");
    canonical.setSmoker(new EnumImpl<Enum.Smoker>(Enum.Smoker.NO));
    canonical.setSports(Lists.newArrayList("frisbee", "rugby"));
    canonical.setStatus("happy");
    canonical.setTags(Lists.newArrayList("C#", "JSON", "template"));
    canonical.setThumbnailUrl("http://www.example.org/pic/?id=1");
    canonical.setTimeZone(-8L);
    canonical.setTurnOffs(Lists.newArrayList("lack of unit tests", "cabbage"));
    canonical.setTurnOns(Lists.newArrayList("well document code"));
    canonical.setTvShows(Lists.newArrayList("House", "Battlestar Galactica"));

    canonical.setUrls(Lists.<Url>newArrayList(
        new UrlImpl("http://www.example.org/?id=1", "Profile", "text/html"),
        new UrlImpl("http://www.example.org/pic/?id=1", "Thumbnail", "img/*")));
  }

  /**
   * Expected response for john.doe's json:
   *
   * {
   *   'id' : 'john.doe',
   *   'name' : {'unstructured' : 'John Doe'},
   *   'phoneNumbers' : [
   *     { 'number' : '+33H000000000', 'type' : 'home'},
   *   ],
   *   'addresses' : [
   *     {'unstructuredAddress' : 'My home address'}
   *   ],
   *   'emails' : [
   *     { 'address' : 'john.doe@work.bar', 'type' : 'work'},
   *   ]
   *
   *  ... etc, etc for all fields in the person object
   *
   * }
   * TODO: Finish up this test and make refactor so that it is easier to read
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetPersonJson() throws Exception {
    // TODO(doll): Test all of the date fields

    Map<String, String> extraParams = Maps.newHashMap();
    String allFieldsParam = "";
    for (String allField : Person.Field.ALL_FIELDS) {
      allFieldsParam += allField + ",";
    }
    extraParams.put("fields", allFieldsParam);

    // Currently, for Shindig {pid}/@all/{uid} == {uid}/@self
    String resp = getJsonResponse("/people/canonical/@self", "GET", extraParams);
    JSONObject result = getJson(resp);

    assertStringField(result, canonical.getAboutMe(), Person.Field.ABOUT_ME);
    assertStringListField(result, canonical.getActivities(),
        Person.Field.ACTIVITIES);

    JSONObject jsonAddress = result.getJSONArray(
        Person.Field.ADDRESSES.toString()).getJSONObject(0);
    assertAddressField(canonical.getAddresses().get(0), jsonAddress);

    assertEquals(canonical.getAge().intValue(), result.getInt(
        Person.Field.AGE.toString()));

    JSONObject jsonBody = result.getJSONObject(
        Person.Field.BODY_TYPE.toString());
    BodyType body = canonical.getBodyType();
    assertStringField(jsonBody, body.getBuild(), BodyType.Field.BUILD);
    assertStringField(jsonBody, body.getEyeColor(), BodyType.Field.EYE_COLOR);
    assertStringField(jsonBody, body.getHairColor(), BodyType.Field.HAIR_COLOR);
    assertStringField(jsonBody, body.getHeight(), BodyType.Field.HEIGHT);
    assertStringField(jsonBody, body.getWeight(), BodyType.Field.WEIGHT);

    assertStringListField(result, canonical.getBooks(), Person.Field.BOOKS);
    assertStringListField(result, canonical.getCars(), Person.Field.CARS);
    assertStringField(result, canonical.getChildren(), Person.Field.CHILDREN);

    JSONObject currentLocation = result.getJSONObject(Person.Field.CURRENT_LOCATION.toString());
    assertFloatField(currentLocation, canonical.getCurrentLocation().getLatitude(),
        Address.Field.LATITUDE);
    assertFloatField(currentLocation, canonical.getCurrentLocation().getLongitude(),
        Address.Field.LONGITUDE);

//    assertLongField(result, canonical.getDateOfBirth().getTime(),
//        Person.Field.DATE_OF_BIRTH);
//    assertEnumField(result, canonical.getDrinker(), Person.Field.DRINKER);

    JSONArray emailArray = result.getJSONArray(Person.Field.EMAILS.toString());
    assertEquals(1, emailArray.length());

    for (int i = 0; i < canonical.getEmails().size(); i++) {
      Email expectedEmail = canonical.getEmails().get(i);
      JSONObject actualEmail = emailArray.getJSONObject(i);
      assertEquals(expectedEmail.getType(),
          actualEmail.getString(Email.Field.TYPE.toString()));
      assertEquals(expectedEmail.getAddress(),
          actualEmail.getString(Email.Field.ADDRESS.toString()));
    }

    assertStringField(result, canonical.getEthnicity(), Person.Field.ETHNICITY);
    assertStringField(result, canonical.getFashion(), Person.Field.FASHION);
    assertStringListField(result, canonical.getFood(), Person.Field.FOOD);
    assertEnumField(result, canonical.getGender(), Person.Field.GENDER);
    assertStringField(result, canonical.getHappiestWhen(),
        Person.Field.HAPPIEST_WHEN);
    assertBooleanField(result, canonical.getHasApp(), Person.Field.HAS_APP);
    assertStringListField(result, canonical.getHeroes(), Person.Field.HEROES);
    assertStringField(result, canonical.getHumor(), Person.Field.HUMOR);
    assertStringField(result, canonical.getId(), Person.Field.ID);
    assertStringListField(result, canonical.getInterests(),
        Person.Field.INTERESTS);
    assertStringField(result, canonical.getJobInterests(),
        Person.Field.JOB_INTERESTS);

    assertOrganizationField(canonical.getJobs().get(0),
        result.getJSONArray(Person.Field.JOBS.toString()).getJSONObject(0));

    assertStringListField(result, canonical.getLanguagesSpoken(),
        Person.Field.LANGUAGES_SPOKEN);
//    assertDateField(result, canonical.getUpdated(), Person.Field.LAST_UPDATED);
    assertStringField(result, canonical.getLivingArrangement(),
        Person.Field.LIVING_ARRANGEMENT);
    assertStringField(result, canonical.getLookingFor(),
        Person.Field.LOOKING_FOR);
    assertStringListField(result, canonical.getMovies(), Person.Field.MOVIES);
    assertStringListField(result, canonical.getMusic(), Person.Field.MUSIC);

    assertEquals(canonical.getName().getUnstructured(),
        result.getJSONObject(Person.Field.NAME.toString()).getString(
            Name.Field.UNSTRUCTURED.toString()));

    assertEnumField(result, canonical.getNetworkPresence(),
        Person.Field.NETWORKPRESENCE);
    assertStringField(result, canonical.getNickname(), Person.Field.NICKNAME);
    assertStringField(result, canonical.getPets(), Person.Field.PETS);

    JSONArray phoneArray = result.getJSONArray(
        Person.Field.PHONE_NUMBERS.toString());
    assertEquals(canonical.getPhoneNumbers().size(), phoneArray.length());

    for (int i = 0; i < canonical.getPhoneNumbers().size(); i++) {
      Phone expectedPhone = canonical.getPhoneNumbers().get(i);
      JSONObject actualPhone = phoneArray.getJSONObject(i);
      assertEquals(expectedPhone.getType(), actualPhone.getString(
          Phone.Field.TYPE.toString()));
      assertEquals(expectedPhone.getNumber(), actualPhone.getString(
          Phone.Field.NUMBER.toString()));
    }

    assertStringField(result, canonical.getPoliticalViews(),
        Person.Field.POLITICAL_VIEWS);

    assertUrlField(canonical.getProfileSong(), result.getJSONObject(
        Person.Field.PROFILE_SONG.toString()));
    assertStringField(result, canonical.getProfileUrl(),
        Person.Field.PROFILE_URL);
    assertUrlField(canonical.getProfileVideo(), result.getJSONObject(
        Person.Field.PROFILE_VIDEO.toString()));

    assertStringListField(result, canonical.getQuotes(), Person.Field.QUOTES);
    assertStringField(result, canonical.getRelationshipStatus(),
        Person.Field.RELATIONSHIP_STATUS);
    assertStringField(result, canonical.getReligion(), Person.Field.RELIGION);
    assertStringField(result, canonical.getRomance(), Person.Field.ROMANCE);
    assertStringField(result, canonical.getScaredOf(), Person.Field.SCARED_OF);

    assertOrganizationField(canonical.getSchools().get(0),
        result.getJSONArray(Person.Field.SCHOOLS.toString()).getJSONObject(0));

    assertStringField(result, canonical.getSexualOrientation(), Person.Field.SEXUAL_ORIENTATION);
    assertEnumField(result, canonical.getSmoker(), Person.Field.SMOKER);
    assertStringListField(result, canonical.getSports(), Person.Field.SPORTS);
    assertStringField(result, canonical.getStatus(), Person.Field.STATUS);
    assertStringListField(result, canonical.getTags(), Person.Field.TAGS);
    assertStringField(result, canonical.getThumbnailUrl(),
        Person.Field.THUMBNAIL_URL);
    // TODO: time zone
    assertStringListField(result, canonical.getTurnOffs(),
        Person.Field.TURN_OFFS);
    assertStringListField(result, canonical.getTurnOns(), Person.Field.TURN_ONS);
    assertStringListField(result, canonical.getTvShows(), Person.Field.TV_SHOWS);
  }

  private void assertAddressField(Address expected, JSONObject actual)
      throws JSONException {
    assertStringField(actual, expected.getCountry(),
        Address.Field.COUNTRY);
    assertStringField(actual, expected.getExtendedAddress(),
        Address.Field.EXTENDED_ADDRESS);
    assertFloatField(actual, expected.getLatitude(), Address.Field.LATITUDE);
    assertStringField(actual, expected.getLocality(), Address.Field.LOCALITY);
    assertFloatField(actual, expected.getLongitude(), Address.Field.LONGITUDE);
    assertStringField(actual, expected.getPoBox(), Address.Field.PO_BOX);
    assertStringField(actual, expected.getPostalCode(),
        Address.Field.POSTAL_CODE);
    assertStringField(actual, expected.getRegion(), Address.Field.REGION);
    assertStringField(actual, expected.getStreetAddress(),
        Address.Field.STREET_ADDRESS);
    assertStringField(actual, expected.getType(), Address.Field.TYPE);
    assertStringField(actual, expected.getUnstructuredAddress(),
        Address.Field.UNSTRUCTURED_ADDRESS);
  }

  private void assertUrlField(Url expected, JSONObject actual)
      throws JSONException {
    assertStringField(actual, expected.getAddress(), Url.Field.ADDRESS);
    assertStringField(actual, expected.getLinkText(), Url.Field.LINK_TEXT);
    assertStringField(actual, expected.getType(), Url.Field.TYPE);
  }

  private void assertOrganizationField(Organization expected, JSONObject actual)
      throws JSONException {
    assertStringField(actual.getJSONObject(Organization.Field.ADDRESS.toString()),
        expected.getAddress().getUnstructuredAddress(), Address.Field.UNSTRUCTURED_ADDRESS);
    assertStringField(actual, expected.getDescription(),
        Organization.Field.DESCRIPTION);
//    assertDateField(actual, expected.getEndDate(), Organization.Field.END_DATE);
    assertStringField(actual, expected.getField(), Organization.Field.FIELD);
    assertStringField(actual, expected.getName(), Organization.Field.NAME);
    assertStringField(actual, expected.getSalary(), Organization.Field.SALARY);
//    assertDateField(actual, expected.getStartDate(), Organization.Field.START_DATE);
    assertStringField(actual, expected.getSubField(), Organization.Field.SUB_FIELD);
    assertStringField(actual, expected.getTitle(), Organization.Field.TITLE);
    assertStringField(actual, expected.getWebpage(), Organization.Field.WEBPAGE);
  }

  private void assertBooleanField(JSONObject result, boolean expected,
      Object field) throws JSONException {
    assertEquals(expected, result.getBoolean(field.toString()));
  }

  private void assertFloatField(JSONObject result, Float expected,
      Object field) throws JSONException {
    assertEquals(expected.intValue(), result.getInt(field.toString()));
  }

  private void assertStringField(JSONObject result, String expected,
      Object field) throws JSONException {
    assertEquals(expected, result.getString(field.toString()));
  }

  private void assertStringListField(JSONObject result, List<String> list,
      Person.Field field) throws JSONException {
    JSONArray actual = result.getJSONArray(field.toString());
    assertEquals(list.get(0), actual.getString(0));
  }

  private void assertEnumField(JSONObject result, Enum expected,
      Person.Field field) throws JSONException {
    JSONObject actual = result.getJSONObject(field.toString());
    assertEquals(expected.getDisplayValue(), actual.getString("displayValue"));
    assertEquals(expected.getKey().toString(), actual.getString("key"));
  }

  /**
   * Expected response for a list of people in json:
   *
   * {
   *  "totalResults" : 3,
   *  "startIndex" : 0
   *  "entry" : [
   *     {<jane doe>}, // layed out like above
   *     {<george doe>},
   *     {<maija m>},
   *  ]
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetPeople() throws Exception {
    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("sortBy", "name");
    extraParams.put("sortOrder", null);
    extraParams.put("filterBy", null);
    extraParams.put("startIndex", null);
    extraParams.put("count", "20");
    extraParams.put("fields", null);

    // Currently, for Shindig @all == @friends
    String resp = getJsonResponse("/people/john.doe/@friends", "GET", extraParams);
    JSONObject result = getJson(resp);

    assertEquals(3, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));

    JSONArray people = result.getJSONArray("entry");

    // The users should be in alphabetical order
    assertPerson(people.getJSONObject(0), "george.doe", "George Doe");
    assertPerson(people.getJSONObject(1), "jane.doe", "Jane Doe");
  }

  @Test
  public void testGetPeoplePagination() throws Exception {
    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("sortBy", "name");
    extraParams.put("sortOrder", null);
    extraParams.put("filterBy", null);
    extraParams.put("startIndex", "0");
    extraParams.put("count", "1");
    extraParams.put("fields", null);

    String resp = getJsonResponse("/people/john.doe/@friends", "GET", extraParams);
    JSONObject result = getJson(resp);

    assertEquals(3, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));

    JSONArray people = result.getJSONArray("entry");
    assertPerson(people.getJSONObject(0), "george.doe", "George Doe");

    // Get the second page
    extraParams.put("startIndex", "1");
    resp = getJsonResponse("/people/john.doe/@friends", "GET", extraParams);
    result = getJson(resp);

    assertEquals(3, result.getInt("totalResults"));
    assertEquals(1, result.getInt("startIndex"));

    people = result.getJSONArray("entry");
    assertPerson(people.getJSONObject(0), "jane.doe", "Jane Doe");
  }

  private void assertPerson(JSONObject person, String expectedId, String expectedName)
      throws Exception {
    assertEquals(expectedId, person.getString("id"));
    assertEquals(expectedName, person.getJSONObject("name").getString("unstructured"));
  }

  // TODO: Add tests for fields parameter
  // TODO: Add tests for networkDistance
}