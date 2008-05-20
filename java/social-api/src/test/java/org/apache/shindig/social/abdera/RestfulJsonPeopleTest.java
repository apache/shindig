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
package org.apache.shindig.social.abdera;

import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.opensocial.model.ApiCollection;
import org.apache.shindig.social.opensocial.model.Email;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Phone;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;


public class RestfulJsonPeopleTest extends AbstractLargeRestfulTests {

  @Before
  public void setUp() throws Exception {
    super.setUp();

    List<Person> people = new ArrayList<Person>();
    people.add(SocialApiTestsGuiceModule.MockPeopleService.janeDoe);
    people.add(SocialApiTestsGuiceModule.MockPeopleService.simpleDoe);

    SocialApiTestsGuiceModule.MockPeopleService.setPeople(
        new ResponseItem<ApiCollection<Person>>(
            new ApiCollection<Person>(people)));

    SocialApiTestsGuiceModule.MockPeopleService.setPerson(
        new ResponseItem<Person>(SocialApiTestsGuiceModule
            .MockPeopleService.johnDoe));
  }

  @After
  public void tearDown() throws Exception {
    SocialApiTestsGuiceModule.MockPeopleService.setPeople(null);
    SocialApiTestsGuiceModule.MockPeopleService.setPerson(null);

    super.tearDown();
  }

  /**
   * Expected response for john.doe's json:
   *
   * {
   *   'id' : 'john.doe',
   *   'name' : {'unstructured' : 'John Doe'},
   *   'phoneNumbers' : [
   *     { 'number' : '+33H000000000', 'type' : 'home'},
   *     { 'number' : '+33M000000000', 'type' : 'mobile'},
   *     { 'number' : '+33W000000000', 'type' : 'work'}
   *   ],
   *   'addresses' : [
   *     {'unstructuredAddress' : 'My home address'}
   *   ],
   *   'emails' : [
   *     { 'address' : 'john.doe@work.bar', 'type' : 'work'},
   *     { 'address' : 'john.doe@home.bar', 'type' : 'home'}
   *   ]
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetPersonJson() throws Exception {
    // Currently, for Shindig {pid}/@all/{uid} == {uid}/@self
    resp = client.get(BASEURL + "/people/john.doe/@self");
    checkForGoodJsonResponse(resp);
    JSONObject result = getJson(resp);

    Person johnDoe = SocialApiTestsGuiceModule.MockPeopleService.johnDoe;
    assertEquals(johnDoe.getId(), result.getString("id"));

    assertEquals(johnDoe.getName().getUnstructured(),
        result.getJSONObject("name").getString("unstructured"));

    assertEquals(johnDoe.getAddresses().get(0).getUnstructuredAddress(),
        result.getJSONArray("addresses").getJSONObject(0)
            .getString("unstructuredAddress"));

    JSONArray phoneArray = result.getJSONArray("phoneNumbers");
    assertEquals(3, phoneArray.length());

    for (int i = 0; i < johnDoe.getPhoneNumbers().size(); i++) {
      Phone expectedPhone = johnDoe.getPhoneNumbers().get(i);
      JSONObject actualPhone = phoneArray.getJSONObject(i);
      assertEquals(expectedPhone.getType(), actualPhone.getString("type"));
      assertEquals(expectedPhone.getNumber(), actualPhone.getString("number"));
    }

    JSONArray emailArray = result.getJSONArray("emails");
    assertEquals(2, emailArray.length());

    for (int i = 0; i < johnDoe.getEmails().size(); i++) {
      Email expectedEmail = johnDoe.getEmails().get(i);
      JSONObject actualEmail = emailArray.getJSONObject(i);
      assertEquals(expectedEmail.getType(), actualEmail.getString("type"));
      assertEquals(expectedEmail.getAddress(),
          actualEmail.getString("address"));
    }
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
    // Currently, for Shindig @all == @friends
    resp = client.get(BASEURL + "/people/john.doe/@friends");
    checkForGoodJsonResponse(resp);
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
