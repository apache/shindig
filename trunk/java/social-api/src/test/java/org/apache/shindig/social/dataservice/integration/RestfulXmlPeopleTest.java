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
package org.apache.shindig.social.dataservice.integration;

import org.apache.shindig.protocol.model.Enum;
import org.apache.shindig.protocol.model.EnumImpl;
import org.apache.shindig.social.core.model.AddressImpl;
import org.apache.shindig.social.core.model.BodyTypeImpl;
import org.apache.shindig.social.core.model.ListFieldImpl;
import org.apache.shindig.social.core.model.NameImpl;
import org.apache.shindig.social.core.model.OrganizationImpl;
import org.apache.shindig.social.core.model.PersonImpl;
import org.apache.shindig.social.core.model.UrlImpl;
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
import org.apache.shindig.social.opensocial.util.XSDValidator;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.custommonkey.xmlunit.XMLUnit;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class RestfulXmlPeopleTest extends AbstractLargeRestfulTests {
  private Person canonical;

  @Before
  public void restfulxXmlPeopleTestBefore() throws Exception {

    NameImpl name = new NameImpl("Sir Shin H. Digg Social Butterfly");
    name.setAdditionalName("H");
    name.setFamilyName("Digg");
    name.setGivenName("Shin");
    name.setHonorificPrefix("Sir");
    name.setHonorificSuffix("Social Butterfly");
    canonical = new PersonImpl("canonical", "Shin Digg", name);

    canonical.setAboutMe("I have an example of every piece of data");
    canonical.setActivities(Lists.newArrayList("Coding Shindig"));

    Address address = new AddressImpl(
        "PoBox 3565, 1 OpenStandards Way, Apache, CA");
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
    bodyType.setHeight(1.84F); // meters as per spec
    bodyType.setWeight(74F); // kg as per spec
    canonical.setBodyType(bodyType);

    canonical.setBooks(Lists.newArrayList("The Cathedral & the Bazaar",
        "Catch 22"));
    canonical.setCars(Lists.newArrayList("beetle", "prius"));
    canonical.setChildren("3");
    AddressImpl location = new AddressImpl();
    location.setLatitude(48.858193F);
    location.setLongitude(2.29419F);
    canonical.setCurrentLocation(location);

    canonical.setBirthday(new Date());
    canonical.setDrinker(new EnumImpl<Drinker>(Drinker.SOCIALLY));
    ListField email = new ListFieldImpl("work",
        "dev@shindig.apache.org");
    canonical.setEmails(Lists.newArrayList(email));

    canonical.setEthnicity("developer");
    canonical.setFashion("t-shirts");
    canonical.setFood(Lists.newArrayList("sushi", "burgers"));
    canonical.setGender(Person.Gender.male);
    canonical.setHappiestWhen("coding");
    canonical.setHasApp(true);
    canonical
        .setHeroes(Lists.newArrayList("Doug Crockford", "Charles Babbage"));
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
    job1.setWebpage("http://shindig.apache.org/");
    job1.setType("job");

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
    job2.setType("job");

    canonical.setOrganizations(Lists.newArrayList(job1, job2));

    canonical.setUpdated(new Date());
    canonical.setLanguagesSpoken(Lists.newArrayList("English", "Dutch",
        "Esperanto"));
    canonical.setLivingArrangement("in a house");
    org.apache.shindig.protocol.model.Enum<LookingFor> lookingForRandom = new EnumImpl<LookingFor>(
        LookingFor.RANDOM, "Random");
    Enum<LookingFor> lookingForNetworking = new EnumImpl<LookingFor>(
        LookingFor.NETWORKING, "Networking");
    canonical.setLookingFor(Lists.newArrayList(lookingForRandom,
        lookingForNetworking));
    canonical.setMovies(Lists.newArrayList("Iron Man", "Nosferatu"));
    canonical.setMusic(Lists.newArrayList("Chieftains", "Beck"));
    canonical.setNetworkPresence(new EnumImpl<NetworkPresence>(
        NetworkPresence.ONLINE));
    canonical.setNickname("diggy");
    canonical.setPets("dog,cat");
    canonical.setPhoneNumbers(Lists.<ListField> newArrayList(new ListFieldImpl(
        "work", "111-111-111"), new ListFieldImpl("mobile", "999-999-999")));

    canonical.setPoliticalViews("open leaning");
    canonical.setProfileSong(new UrlImpl(
        "http://www.example.org/songs/OnlyTheLonely.mp3", "Feelin' blue",
        "road"));
    canonical.setProfileVideo(new UrlImpl(
        "http://www.example.org/videos/Thriller.flv", "Thriller", "video"));

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
    canonical.setThumbnailUrl("/images/nophoto.gif");
    canonical.setUtcOffset(-8L);
    canonical.setTurnOffs(Lists.newArrayList("lack of unit tests", "cabbage"));
    canonical.setTurnOns(Lists.newArrayList("well document code"));
    canonical.setTvShows(Lists.newArrayList("House", "Battlestar Galactica"));

    canonical
        .setUrls(Lists.<Url> newArrayList(new UrlImpl(
            "http://www.example.org/?id=1", "my profile", "Profile"),
            new UrlImpl("/images/nophoto.gif",
                "my awesome picture", "Thumbnail")));

  }

  /**
   * Expected response for john.doe's json:
   *
   * { 'entry' :
   * { 'id' : 'john.doe',
   * 'name' : {'formatted' : 'John Doe'},
   * 'phoneNumbers' : [ { 'number' : '+33H000000000', 'type' : 'home'}, ],
   * 'addresses' : [ {'formatted' : 'My home address'} ],
   * 'emails' : [
   *    { 'value' : 'john.doe@work.bar', 'type' : 'work'}, ]
   *
   * ... etc, etc for all fields in the person object } } TODO: Finish up this
   * test and make refactor so that it is easier to read
   *
   * @throws Exception
   *           if test encounters an error
   */
  @SuppressWarnings("boxing")
  @Test
  public void testGetPersonJson() throws Exception {
    // TODO(doll): Test all of the date fields

    Map<String, String> extraParams = Maps.newHashMap();
    StringBuilder allFieldsParam = new StringBuilder();
    for (String allField : Person.Field.ALL_FIELDS) {
      allFieldsParam.append(allField).append(',');
    }
    extraParams.put("fields", allFieldsParam.toString());

    // Currently, for Shindig {pid}/@all/{uid} == {uid}/@self
    String resp = getResponse("/people/canonical/@self", "GET", extraParams,
        "xml", "application/xml");

    XSDValidator.validateOpenSocial(resp);

    NodeList resultNodeList = xp.getMatchingNodes("/:response/:person", XMLUnit.buildTestDocument(resp));
    assertEquals(1, resultNodeList.getLength());

    Node personNode = resultNodeList.item(0);

    Map<String, List<Node>> childNodeMap = childNodesToNodeMap(personNode);
    Map<String, List<String>> result = childNodesToMap(personNode);

    assertStringField(result, canonical.getAboutMe(), Person.Field.ABOUT_ME);
    assertStringListField(result, canonical.getActivities(),
        Person.Field.ACTIVITIES);

    List<Node> addressNodes = childNodeMap.get(Person.Field.ADDRESSES
        .toString());
    assertEquals(addressNodes.size(), canonical.getAddresses().size());
    for (int i = 0; i < addressNodes.size(); i++) {
      assertAddressField(canonical.getAddresses().get(i),
          childNodesToMap(addressNodes.get(i)));
    }

    assertEquals(canonical.getAge().intValue(), Integer.parseInt(result.get(
        Person.Field.AGE.toString()).get(0)));

    Map<String, List<String>> bodyMap = childNodesToMap(childNodeMap.get(
        Person.Field.BODY_TYPE.toString()).get(0));
    BodyType body = canonical.getBodyType();

    assertStringField(bodyMap, body.getBuild(), BodyType.Field.BUILD);
    assertStringField(bodyMap, body.getEyeColor(), BodyType.Field.EYE_COLOR);
    assertStringField(bodyMap, body.getHairColor(), BodyType.Field.HAIR_COLOR);
    assertFloatField(bodyMap, body.getHeight(), BodyType.Field.HEIGHT);
    assertFloatField(bodyMap, body.getWeight(), BodyType.Field.WEIGHT);

    assertStringListField(result, canonical.getBooks(), Person.Field.BOOKS);
    assertStringListField(result, canonical.getCars(), Person.Field.CARS);
    assertStringField(result, canonical.getChildren(), Person.Field.CHILDREN);

    Map<String, List<String>> currentLocation = childNodesToMap(childNodeMap
        .get(Person.Field.CURRENT_LOCATION.toString()).get(0));
    assertFloatField(currentLocation, canonical.getCurrentLocation()
        .getLatitude(), Address.Field.LATITUDE);
    assertFloatField(currentLocation, canonical.getCurrentLocation()
        .getLongitude(), Address.Field.LONGITUDE);

    assertStringField(result, canonical.getDisplayName(),
        Person.Field.DISPLAY_NAME);

    // assertLongField(result, canonical.getBirthday().getTime(),
    // Person.Field.BIRTHDAY);
    // assertEnumField(result, canonical.getDrinker(), Person.Field.DRINKER);

    List<Node> emailArray = childNodeMap.get(Person.Field.EMAILS.toString());
    assertEquals(1, emailArray.size());

    for (int i = 0; i < canonical.getEmails().size(); i++) {
      ListField expectedEmail = canonical.getEmails().get(i);
      Map<String, List<String>> actualEmail = childNodesToMap(emailArray.get(i));

      assertStringField(actualEmail, expectedEmail.getType(),
          ListField.Field.TYPE);
      assertStringField(actualEmail, expectedEmail.getValue(),
          ListField.Field.VALUE);
    }

    assertStringField(result, canonical.getEthnicity(), Person.Field.ETHNICITY);
    assertStringField(result, canonical.getFashion(), Person.Field.FASHION);
    assertStringListField(result, canonical.getFood(), Person.Field.FOOD);
    assertStringField(result, canonical.getGender().toString(),
        Person.Field.GENDER);
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

    assertOrganizationField(canonical.getOrganizations().get(0), childNodeMap
        .get(Person.Field.ORGANIZATIONS.toString()).get(0));

    assertStringListField(result, canonical.getLanguagesSpoken(),
        Person.Field.LANGUAGES_SPOKEN);
    // assertDateField(result, canonical.getUpdated(),
    // Person.Field.LAST_UPDATED);
    assertStringField(result, canonical.getLivingArrangement(),
        Person.Field.LIVING_ARRANGEMENT);
    assertListEnumField(childNodeMap, canonical.getLookingFor(),
        Person.Field.LOOKING_FOR);
    assertStringListField(result, canonical.getMovies(), Person.Field.MOVIES);
    assertStringListField(result, canonical.getMusic(), Person.Field.MUSIC);

    assertEquals(canonical.getName().getFormatted(), childNodesToMap(
        childNodeMap.get(Person.Field.NAME.toString()).get(0)).get(
        Name.Field.FORMATTED.toString()).get(0));

    assertEnumField(childNodeMap, canonical.getNetworkPresence(),
        Person.Field.NETWORKPRESENCE);
    assertStringField(result, canonical.getNickname(), Person.Field.NICKNAME);
    assertStringField(result, canonical.getPets(), Person.Field.PETS);

    List<Node> phoneArray = childNodeMap.get(Person.Field.PHONE_NUMBERS
        .toString());
    assertEquals(canonical.getPhoneNumbers().size(), phoneArray.size());

    for (int i = 0; i < canonical.getPhoneNumbers().size(); i++) {
      ListField expectedPhone = canonical.getPhoneNumbers().get(i);
      Map<String, List<String>> actualPhone = childNodesToMap(phoneArray.get(i));
      assertEquals(expectedPhone.getType(), actualPhone.get(
          ListField.Field.TYPE.toString()).get(0));
      assertEquals(expectedPhone.getValue(), actualPhone.get(
          ListField.Field.VALUE.toString()).get(0));
    }

    assertStringField(result, canonical.getPoliticalViews(),
        Person.Field.POLITICAL_VIEWS);

    assertUrlField(canonical.getProfileSong(), childNodesToMap(childNodeMap
        .get(Person.Field.PROFILE_SONG.toString()).get(0)));
    assertStringField(result, canonical.getProfileUrl(),
        Person.Field.PROFILE_URL);
    assertUrlField(canonical.getProfileVideo(), childNodesToMap(childNodeMap
        .get(Person.Field.PROFILE_VIDEO.toString()).get(0)));

    assertStringListField(result, canonical.getQuotes(), Person.Field.QUOTES);
    assertStringField(result, canonical.getRelationshipStatus(),
        Person.Field.RELATIONSHIP_STATUS);
    assertStringField(result, canonical.getReligion(), Person.Field.RELIGION);
    assertStringField(result, canonical.getRomance(), Person.Field.ROMANCE);
    assertStringField(result, canonical.getScaredOf(), Person.Field.SCARED_OF);

    assertStringField(result, canonical.getSexualOrientation(),
        Person.Field.SEXUAL_ORIENTATION);
    assertEnumField(childNodeMap, canonical.getSmoker(), Person.Field.SMOKER);
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

  private void assertAddressField(Address expected,
      Map<String, List<String>> actual) {
    assertStringField(actual, expected.getCountry(), Address.Field.COUNTRY);
    assertFloatField(actual, expected.getLatitude(), Address.Field.LATITUDE);
    assertStringField(actual, expected.getLocality(), Address.Field.LOCALITY);
    assertFloatField(actual, expected.getLongitude(), Address.Field.LONGITUDE);
    assertStringField(actual, expected.getPostalCode(),
        Address.Field.POSTAL_CODE);
    assertStringField(actual, expected.getRegion(), Address.Field.REGION);
    assertStringField(actual, expected.getStreetAddress(),
        Address.Field.STREET_ADDRESS);
    assertStringField(actual, expected.getType(), Address.Field.TYPE);
    assertStringField(actual, expected.getFormatted(), Address.Field.FORMATTED);
  }

  private void assertUrlField(Url expected, Map<String, List<String>> actual) {
    assertStringField(actual, expected.getValue(), Url.Field.VALUE);
    assertStringField(actual, expected.getLinkText(), Url.Field.LINK_TEXT);
    assertStringField(actual, expected.getType(), Url.Field.TYPE);
  }

  private void assertOrganizationField(Organization expected, Node orgNode) {
    Map<String, List<String>> actual = childNodesToMap(orgNode);
    Map<String, List<Node>> actualNode = childNodesToNodeMap(orgNode);
    assertStringField(childNodesToMap(actualNode.get(
        Organization.Field.ADDRESS.toString()).get(0)), expected.getAddress()
        .getFormatted(), Address.Field.FORMATTED);
    assertStringField(actual, expected.getDescription(),
        Organization.Field.DESCRIPTION);
    // assertDateField(actual, expected.getEndDate(),
    // Organization.Field.END_DATE);
    assertStringField(actual, expected.getField(), Organization.Field.FIELD);
    assertStringField(actual, expected.getName(), Organization.Field.NAME);
    assertStringField(actual, expected.getSalary(), Organization.Field.SALARY);
    // assertDateField(actual, expected.getStartDate(),
    // Organization.Field.START_DATE);
    assertStringField(actual, expected.getSubField(),
        Organization.Field.SUB_FIELD);
    assertStringField(actual, expected.getTitle(), Organization.Field.TITLE);
    assertStringField(actual, expected.getWebpage(), Organization.Field.WEBPAGE);
    assertStringField(actual, expected.getType(), Organization.Field.TYPE);
  }

  private void assertBooleanField(Map<String, List<String>> result,
      boolean expected, Object field) {
    assertEquals(expected, Boolean.parseBoolean(result.get(field.toString())
        .get(0)));
  }

  @SuppressWarnings("boxing")
  private void assertFloatField(Map<String, List<String>> result, Float expected, Object field) {
    assertEquals(expected.floatValue(), Float.valueOf(result.get(field.toString()).get(0)), 0);
  }

  private void assertStringField(Map<String, List<String>> result,
      String expected, Object field) {
    List<String> v = result.get(field.toString());
    String t;
    if ( v == null || v.isEmpty()) {
      if (expected == null ) {
        return;
      }
      t = "";
    } else {
      t = v.get(0);
    }
    assertEquals(expected, t);
  }

  private void assertStringListField(Map<String, List<String>> result,
      List<String> list, Person.Field field) {
    assertEquals(list.size(), result.get(field.toString()).size());
    for (int i = 0; i < list.size(); i++) {
      assertEquals(list.get(i), result.get(field.toString()).get(i));
    }
  }

  private void assertEnumField(Map<String, List<Node>> result, Enum<?> expected,
      Person.Field field) {
    Map<String, List<String>> actual = childNodesToMap(result.get(
        field.toString()).get(0));
    assertEquals(expected.getDisplayValue(), actual.get("displayValue").get(0));
    assertEquals(expected.getValue().toString(), actual.get("value").get(0));
  }

  private void assertListEnumField(Map<String, List<Node>> result,
      List<? extends Enum<? extends Enum.EnumKey>> expected, Person.Field field) {
    List<Node> actual = result.get(field.toString());
    for (int i = 0; i < actual.size(); i++) {
      Map<String, List<String>> nm = childNodesToMap(actual.get(i));
      assertEquals(expected.get(i).getDisplayValue(), nm.get("displayValue")
          .get(0));
      assertEquals(expected.get(i).getValue().toString(), nm.get("value")
          .get(0));
    }
  }

  /**
   * Expected response for a list of people in json:
   *
   * { "totalResults" : 3,
   *     "startIndex" : 0
   *     "entry" : [ {<jane doe>}, // layed out like above
   * {<george doe>}, {<maija m>}, ] }
   *
   * @throws Exception
   *           if test encounters an error
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
    String resp = getResponse("/people/john.doe/@friends", "GET", extraParams,
        "xml", "application/xml");

    XSDValidator.validateOpenSocial(resp);

    NodeList resultNodeList = xp.getMatchingNodes("/:response", XMLUnit.buildTestDocument(resp));
    assertEquals(1, resultNodeList.getLength());

    Map<String, List<String>> result = childNodesToMap(resultNodeList.item(0));
    Map<String, List<Node>> resultNodes = childNodesToNodeMap(resultNodeList
        .item(0));

    assertEquals("3", result.get("totalResults").get(0));
    assertEquals("0", result.get("startIndex").get(0));

    // The users should be in alphabetical order
    List<Node> listNodes = resultNodes.get("list");
    Map<String, List<Node>> listNodesChildMap = childNodesToNodeMap(listNodes.get(0));
    List<Node> entries = listNodesChildMap.get("entry");

    Map<String, List<Node>> entryOne = childNodesToNodeMap(entries.get(0));
    assertPerson(childNodesToNodeMap(entryOne.get("person").get(0)),
        "george.doe", "George Doe");

    Map<String, List<Node>> entryTwo = childNodesToNodeMap(entries.get(1));
    assertPerson(childNodesToNodeMap(entryTwo.get("person").get(0)),
        "jane.doe", "Jane Doe");
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

    String resp = getResponse("/people/john.doe/@friends", "GET", extraParams,
        "xml", "application/xml");

    XSDValidator.validateOpenSocial(resp);

    NodeList resultNodeList = xp.getMatchingNodes("/:response", XMLUnit.buildTestDocument(resp));
    assertEquals(1, resultNodeList.getLength());

    Map<String, List<String>> result = childNodesToMap(resultNodeList.item(0));
    Map<String, List<Node>> resultNodes = childNodesToNodeMap(resultNodeList
        .item(0));

    assertEquals("3", result.get("totalResults").get(0));
    assertEquals("0", result.get("startIndex").get(0));

    List<Node> listNodes = resultNodes.get("list");
    Map<String, List<Node>> listNodesChildMap = childNodesToNodeMap(listNodes.get(0));
    List<Node> entries = listNodesChildMap.get("entry");

    Map<String, List<Node>> entryOne = childNodesToNodeMap(entries.get(0));

    assertPerson(childNodesToNodeMap(entryOne.get("person").get(0)),
        "george.doe", "George Doe");

    // Get the second page
    extraParams.put("startIndex", "1");
    resp = getResponse("/people/john.doe/@friends", "GET", extraParams, "xml",
        "application/xml");

    XSDValidator.validateOpenSocial(resp);

    resultNodeList = xp.getMatchingNodes("/:response", XMLUnit.buildTestDocument(resp));
    assertEquals(1, resultNodeList.getLength());

    result = childNodesToMap(resultNodeList.item(0));
    resultNodes = childNodesToNodeMap(resultNodeList.item(0));

    assertEquals("3", result.get("totalResults").get(0));
    assertEquals("1", result.get("startIndex").get(0));

    listNodes = resultNodes.get("list");
    listNodesChildMap = childNodesToNodeMap(listNodes.get(0));
    entries = listNodesChildMap.get("entry");

    Map<String, List<Node>> entryTwo = childNodesToNodeMap(entries.get(0));
    assertPerson(childNodesToNodeMap(entryTwo.get("person").get(0)),
        "jane.doe", "Jane Doe");
  }

  private void assertPerson(Map<String, List<Node>> person, String expectedId,
      String expectedName) throws Exception {
    assertEquals(expectedId, person.get("id").get(0).getTextContent());
    assertEquals(expectedName, childNodesToMap(person.get("name").get(0)).get(
        "formatted").get(0));
  }

  // TODO: Add tests for fields parameter
  // TODO: Add tests for networkDistance
}
