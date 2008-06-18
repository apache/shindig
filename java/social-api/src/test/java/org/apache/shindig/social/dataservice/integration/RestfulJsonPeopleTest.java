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

import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.BodyType;
import org.apache.shindig.social.opensocial.model.Email;
import org.apache.shindig.social.opensocial.model.Enum;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Phone;
import org.apache.shindig.social.opensocial.model.Url;

import com.google.common.collect.Maps;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class RestfulJsonPeopleTest extends AbstractLargeRestfulTests {

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
    // Currently, for Shindig {pid}/@all/{uid} == {uid}/@self
    String resp = getJsonResponse("/people/john.doe/@self", "GET");
    JSONObject result = getJson(resp);

    Person johnDoe = SocialApiTestsGuiceModule.MockXmlStateFileFetcher.johnDoe;
    assertStringField(result, johnDoe.getAboutMe(), Person.Field.ABOUT_ME);
    assertStringListField(result, johnDoe.getActivities(),
        Person.Field.ACTIVITIES);

    JSONObject jsonAddress = result.getJSONArray(
        Person.Field.ADDRESSES.toString()).getJSONObject(0);
    assertAddressField(johnDoe.getAddresses().get(0), jsonAddress);

    assertEquals(johnDoe.getAge().intValue(), result.getInt(
        Person.Field.AGE.toString()));

    JSONObject jsonBody = result.getJSONObject(
        Person.Field.BODY_TYPE.toString());
    BodyType body = johnDoe.getBodyType();
    assertStringField(jsonBody, body.getBuild(), BodyType.Field.BUILD);
    assertStringField(jsonBody, body.getEyeColor(), BodyType.Field.EYE_COLOR);
    assertStringField(jsonBody, body.getHairColor(), BodyType.Field.HAIR_COLOR);
    assertStringField(jsonBody, body.getHeight(), BodyType.Field.HEIGHT);
    assertStringField(jsonBody, body.getWeight(), BodyType.Field.WEIGHT);

    assertStringListField(result, johnDoe.getBooks(), Person.Field.BOOKS);
    assertStringListField(result, johnDoe.getCars(), Person.Field.CARS);
    assertStringField(result, johnDoe.getChildren(), Person.Field.CHILDREN);

    assertStringField(result.getJSONObject(
        Person.Field.CURRENT_LOCATION.toString()),
        johnDoe.getCurrentLocation().getUnstructuredAddress(),
        Address.Field.UNSTRUCTURED_ADDRESS);

    assertStringField(result, johnDoe.getDateOfBirth().toString(),
        Person.Field.DATE_OF_BIRTH);
    assertEnumField(result, johnDoe.getDrinker(), Person.Field.DRINKER);

    JSONArray emailArray = result.getJSONArray(Person.Field.EMAILS.toString());
    assertEquals(1, emailArray.length());

    for (int i = 0; i < johnDoe.getEmails().size(); i++) {
      Email expectedEmail = johnDoe.getEmails().get(i);
      JSONObject actualEmail = emailArray.getJSONObject(i);
      assertEquals(expectedEmail.getType(),
          actualEmail.getString(Email.Field.TYPE.toString()));
      assertEquals(expectedEmail.getAddress(),
          actualEmail.getString(Email.Field.ADDRESS.toString()));
    }

    assertStringField(result, johnDoe.getEthnicity(), Person.Field.ETHNICITY);
    assertStringField(result, johnDoe.getFashion(), Person.Field.FASHION);
    assertStringListField(result, johnDoe.getFood(), Person.Field.FOOD);
    assertEnumField(result, johnDoe.getGender(), Person.Field.GENDER);
    assertStringField(result, johnDoe.getHappiestWhen(),
        Person.Field.HAPPIEST_WHEN);
    assertBooleanField(result, johnDoe.getHasApp(), Person.Field.HAS_APP);
    assertStringListField(result, johnDoe.getHeroes(), Person.Field.HEROES);
    assertStringField(result, johnDoe.getHumor(), Person.Field.HUMOR);
    assertStringField(result, johnDoe.getId(), Person.Field.ID);
    assertStringListField(result, johnDoe.getInterests(),
        Person.Field.INTERESTS);
    assertStringField(result, johnDoe.getJobInterests(),
        Person.Field.JOB_INTERESTS);

    assertOrganizationField(johnDoe.getJobs().get(0),
        result.getJSONArray(Person.Field.JOBS.toString()).getJSONObject(0));

    assertStringListField(result, johnDoe.getLanguagesSpoken(),
        Person.Field.LANGUAGES_SPOKEN);
    assertDateField(result, johnDoe.getUpdated(), Person.Field.LAST_UPDATED);
    assertStringField(result, johnDoe.getLivingArrangement(),
        Person.Field.LIVING_ARRANGEMENT);
    assertStringField(result, johnDoe.getLookingFor(),
        Person.Field.LOOKING_FOR);
    assertStringListField(result, johnDoe.getMovies(), Person.Field.MOVIES);
    assertStringListField(result, johnDoe.getMusic(), Person.Field.MUSIC);

    assertEquals(johnDoe.getName().getUnstructured(),
        result.getJSONObject(Person.Field.NAME.toString()).getString(
            Name.Field.UNSTRUCTURED.toString()));

    assertEnumField(result, johnDoe.getNetworkPresence(),
        Person.Field.NETWORKPRESENCE);
    assertStringField(result, johnDoe.getNickname(), Person.Field.NICKNAME);
    assertStringField(result, johnDoe.getPets(), Person.Field.PETS);

    JSONArray phoneArray = result.getJSONArray(
        Person.Field.PHONE_NUMBERS.toString());
    assertEquals(1, phoneArray.length());

    for (int i = 0; i < johnDoe.getPhoneNumbers().size(); i++) {
      Phone expectedPhone = johnDoe.getPhoneNumbers().get(i);
      JSONObject actualPhone = phoneArray.getJSONObject(i);
      assertEquals(expectedPhone.getType(), actualPhone.getString(
          Phone.Field.TYPE.toString()));
      assertEquals(expectedPhone.getNumber(), actualPhone.getString(
          Phone.Field.NUMBER.toString()));
    }

    assertStringField(result, johnDoe.getPoliticalViews(),
        Person.Field.POLITICAL_VIEWS);

    assertUrlField(johnDoe.getProfileSong(), result.getJSONObject(
        Person.Field.PROFILE_SONG.toString()));
    assertStringField(result, johnDoe.getProfileUrl(),
        Person.Field.PROFILE_URL);
    assertUrlField(johnDoe.getProfileVideo(), result.getJSONObject(
        Person.Field.PROFILE_VIDEO.toString()));

    assertStringListField(result, johnDoe.getQuotes(), Person.Field.QUOTES);
    assertStringField(result, johnDoe.getRelationshipStatus(),
        Person.Field.RELATIONSHIP_STATUS);
    assertStringField(result, johnDoe.getReligion(), Person.Field.RELIGION);
    assertStringField(result, johnDoe.getRomance(), Person.Field.ROMANCE);
    assertStringField(result, johnDoe.getScaredOf(), Person.Field.SCARED_OF);

    assertOrganizationField(johnDoe.getJobs().get(0),
        result.getJSONArray(Person.Field.JOBS.toString()).getJSONObject(0));

    assertStringField(result, johnDoe.getSexualOrientation(),
        Person.Field.SEXUAL_ORIENTATION);
    assertEnumField(result, johnDoe.getSmoker(), Person.Field.SMOKER);
    assertStringListField(result, johnDoe.getSports(), Person.Field.SPORTS);
    assertStringField(result, johnDoe.getStatus(), Person.Field.STATUS);
    assertStringListField(result, johnDoe.getTags(), Person.Field.TAGS);
    assertStringField(result, johnDoe.getThumbnailUrl(),
        Person.Field.THUMBNAIL_URL);
    // TODO: time zone
    assertStringListField(result, johnDoe.getTurnOffs(),
        Person.Field.TURN_OFFS);
    assertStringListField(result, johnDoe.getTurnOns(), Person.Field.TURN_ONS);
    assertStringListField(result, johnDoe.getTvShows(), Person.Field.TV_SHOWS);
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
    assertAddressField(expected.getAddress(), actual.getJSONObject(
        Organization.Field.ADDRESS.toString()));
    assertStringField(actual, expected.getDescription(),
        Organization.Field.DESCRIPTION);
    assertDateField(actual, expected.getEndDate(), Organization.Field.END_DATE);
    assertStringField(actual, expected.getField(), Organization.Field.FIELD);
    assertStringField(actual, expected.getName(), Organization.Field.NAME);
    assertStringField(actual, expected.getSalary(), Organization.Field.SALARY);
    assertDateField(actual, expected.getStartDate(),
        Organization.Field.START_DATE);
    assertStringField(actual, expected.getSubField(),
        Organization.Field.SUB_FIELD);
    assertStringField(actual, expected.getTitle(), Organization.Field.TITLE);
    assertStringField(actual, expected.getWebpage(),
        Organization.Field.WEBPAGE);
  }

  private void assertBooleanField(JSONObject result, boolean expected,
      Object field) throws JSONException {
    assertEquals(expected, result.getBoolean(field.toString()));
  }

  private void assertFloatField(JSONObject result, Float expected,
      Object field) throws JSONException {
    assertEquals(expected.intValue(), result.getInt(field.toString()));
  }

  private void assertDateField(JSONObject result, Date expected,
      Object field) throws JSONException {
    assertEquals(expected.toString(), result.getString(field.toString()));
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
   * TODO: Fix the question marks...
   *
   * {
   *  "author" : "<???>",
   *  "link" : {"rel" : "next", "href" : "<???>"},
   *  "totalResults" : 2,
   *  "startIndex" : 0
   *  "itemsPerPage" : 10
   *  "entry" : [
   *     {<jane doe>}, // layed out like above
   *     {<simple doe>},
   *  ]
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetPeopleJson() throws Exception {
    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("orderBy", null);
    extraParams.put("filterBy", null);
    extraParams.put("startIndex", null);
    extraParams.put("count", null);
    extraParams.put("fields", null);

    // Currently, for Shindig @all == @friends
    String resp = getJsonResponse("/people/john.doe/@friends", "GET", extraParams);
    JSONObject result = getJson(resp);

    assertEquals(2, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));
    // TODO: Paging not handled yet
    // assertEquals(10, result.getInt("itemsPerPage"));

    JSONArray people = result.getJSONArray("entry");

    for (int i = 0; i < people.length(); i++) {
      JSONObject person = people.getJSONObject(i);
      String id = person.getString("id");
      String name = person.getJSONObject("name").getString("unstructured");

      // TODO: Clean this after we support sorting
      if (id.equals("jane.doe")) {
        assertEquals("Jane Doe", name);
      } else {
        assertEquals("simple.doe", id);
        assertEquals("Simple Doe", name);
      }
    }
  }

  // TODO: Add tests for paging, sorting
  // TODO: Add tests for fields parameter
  // TODO: Add tests for networkDistance
}