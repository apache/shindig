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

import org.apache.shindig.social.JettyServer;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.opensocial.model.ApiCollection;
import org.apache.shindig.social.opensocial.model.Email;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Phone;

import junit.framework.Assert;
import org.apache.abdera.Abdera;
import org.apache.abdera.model.Base;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.Response;
import org.apache.abdera.protocol.Response.ResponseType;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.util.Constants;
import org.apache.abdera.util.MimeTypeHelper;
import org.apache.abdera.writer.Writer;
import org.apache.abdera.writer.WriterFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class SocialApiProviderLargeTest extends Assert {
  private static Logger logger =
      Logger.getLogger(SocialApiProviderLargeTest.class.getName());

  private static JettyServer server;
  private static Abdera abdera = Abdera.getInstance();
  private static AbderaClient client = new AbderaClient();

  private static int JETTY_PORT = 9002;
  private static String BASE = "/social/rest";
  private static String BASEURL = "http://localhost:" + JETTY_PORT + BASE;

  private List<Person> people;
  private ClientResponse resp;

  @BeforeClass
  public static void setUpOnce() throws Exception {
    try {
      server = new JettyServer();
      server.start(JETTY_PORT, BASE + "/*");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownOnce() throws Exception {
    server.stop();
  }

  @Before
  public void setUp() throws Exception {
    people = new ArrayList<Person>();
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
    resp.release();
  }


  // Json tests
  // TODO: split into sub files
  // Note: most of these aren't annotated as tests because they don't pass yet

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
   */
  @Test
  public void testGetPersonJson() throws Exception {
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
   */
  public void testGetPeopleJson() throws Exception {
    resp = client.get(BASEURL + "/people/john.doe/@friends");
    checkForGoodJsonResponse(resp);
    JSONObject result = getJson(resp);

    assertEquals(2, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));
    assertEquals(10, result.getInt("itemsPerPage"));

    JSONArray people = result.getJSONArray("entry");

    JSONObject janeDoe = people.getJSONObject(0);
    assertEquals("jane.doe", janeDoe.getString("id"));

    JSONObject simpleDoe = people.getJSONObject(1);
    assertEquals("simple.doe", simpleDoe.getString("id"));
  }

  /**
   * Expected response for an activity in json:
   * {
   *   'id' : '1',
   *   'userId' : 'john.doe',
   *   'title' : 'yellow',
   *   'body' : 'what a color!'
   * }
   */
  public void testGetActivityJson() throws Exception {
    resp = client.get(BASEURL + "/activities/john.doe/@self/1");
    checkForGoodJsonResponse(resp);
    JSONObject result = getJson(resp);
  }

  /**
   * Expected response for a list of activities in json:
   * TODO: Fix the question marks...
   *
   * {
   *  "author" : "<???>",
   *  "link" : {"rel" : "next", "href" : "<???>"},
   *  "totalResults" : 1,
   *  "startIndex" : 0
   *  "itemsPerPage" : 10
   *  "entry" : [
   *     {<activity>} // layed out like above
   *  ]
   * }
   */
  public void testGetActivitiesJson() throws Exception {
    resp = client.get(BASEURL + "/activities/john.doe/@self");
    checkForGoodJsonResponse(resp);
    JSONObject result = getJson(resp);
  }

  /**
   * Expected response for app data in json:
   *
   * {
   *  "entry" : {
   *    "jane.doe" : {"count" : 5},
   *    "simple.doe" : {"count" : 7},
   *  }
   * }
   */
  public void testGetAppDataJson() throws Exception {
    // app id is mocked out
    resp = client.get(BASEURL + "/appdata/john.doe/@friends/appId");
    checkForGoodJsonResponse(resp);
    JSONObject result = getJson(resp);
  }


  // Atom tests

  @Test
  public void testGetPeopleAtom() throws IOException {
    resp = client.get(BASEURL + "/people/john.doe/@all?format=atom");
    checkForGoodAtomResponse(resp);

    Document<Feed> doc = resp.getDocument();
    prettyPrint(doc);
    Feed feed = doc.getRoot();
    assertEquals(2, feed.getEntries().size());
  }

  @Test
  public void testGetIndirectPersonAtom() throws IOException {
    resp = client.get(BASEURL + "/people/john.doe/@all/jane.doe?format=atom");
    checkForGoodAtomResponse(resp);

    Document<Entry> doc = resp.getDocument();
    Entry entry = doc.getRoot();
    prettyPrint(entry);

    Person expectedJaneDoe = people.get(0);
    assertEquals(expectedJaneDoe.getName().getUnstructured(), entry.getTitle());
  }

  @Test
  public void testGetInvalidPersonAtom() throws IOException {
    resp = client.get(BASEURL + "/people/john.doe/@all/nobody?format=atom");
    checkForBadResponse(resp);
  }

  protected void checkForGoodResponse(ClientResponse response,
      String mimeType) {
    assertNotNull(response);
    assertEquals(ResponseType.SUCCESS, response.getType());
    assertTrue(MimeTypeHelper.isMatch(response.getContentType().toString(),
        mimeType));
  }

  protected void checkForGoodJsonResponse(ClientResponse response){
    checkForGoodResponse(response, "application/json");
  }

  protected void checkForGoodAtomResponse(ClientResponse response){
    checkForGoodResponse(response, Constants.ATOM_MEDIA_TYPE);
  }

  protected void checkForBadResponse(ClientResponse response){
    assertNotNull(response);
    assertEquals(ResponseType.CLIENT_ERROR, response.getType());
  }

  private JSONObject getJson(ClientResponse resp) throws IOException,
      JSONException {
    BufferedReader reader = new BufferedReader(resp.getReader());

    StringBuffer json = new StringBuffer();
    String line = reader.readLine();
    while (line != null) {
      json.append(line);
      line = reader.readLine();
    }

    return new JSONObject(json.toString());
  }

  protected void prettyPrint(Base doc) throws IOException {
    WriterFactory writerFactory = abdera.getWriterFactory();
    Writer writer = writerFactory.getWriter("prettyxml");
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    writer.writeTo(doc, os);
    logger.fine(os.toString("utf8"));
  }

}
