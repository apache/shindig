/*
 * Licensed to the Apache Software Foundation (ASF) under one
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
package org.apache.shindig.social.dataservice.integration;

import org.apache.shindig.protocol.ContentTypes;
import org.apache.shindig.social.core.model.ActivityEntryImpl;
import org.apache.shindig.social.core.model.ActivityObjectImpl;
import org.apache.shindig.social.opensocial.model.ActivityEntry;
import org.apache.shindig.social.opensocial.model.ActivityObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class RestfulJsonActivityEntryTest extends AbstractLargeRestfulTests {
  ActivityEntry johnsEntry;

  @Before
  public void restfulJsonActivityEntryTestBefore() throws Exception {
    ActivityObject actor = new ActivityObjectImpl();
    actor.setId("john.doe");
    actor.setDisplayName("John Doe");
    
    ActivityObject object = new ActivityObjectImpl();
    object.setId("object1");
    object.setDisplayName("Frozen Eric");
    
    johnsEntry = new ActivityEntryImpl();
    johnsEntry.setTitle("John posted a photo");
    johnsEntry.setBody("John Doe posted a photo to the album Germany 2009");
    johnsEntry.setActor(actor);
    johnsEntry.setObject(object);
  }

  /**
   * Expected response for an activity in json:
   * { 'entry' : {
   *     'title' : 'John posted a photo',
   *     'body' : 'John Doe posted a photo to the album German 2009',
   *     'actor' : {
   *       'id' : 'john.doe',
   *       'displayName' : 'John Doe'
   *     },
   *     'object' : {
   *       'id' : 'object1',
   *       'displayName' : 'Frozen Eric'
   *     }
   *   }
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetActivityEntryJson() throws Exception {
    String resp = getResponse("/activitystreams/john.doe/@self/@app/object1", "GET", null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    JSONObject result = getJson(resp);
    assertActivityEntriesEqual(johnsEntry, result.getJSONObject("entry"));
  }

  /**
   * Expected response for a list of activities in json:
   *
   * {
   *  "totalResults" : 2,
   *  "startIndex" : 0
   *  "itemsPerPage" : 10 // Note: the js doesn't support paging. Should rest?
   *  "entry" : [
   *     {<entry>} // layed out like above
   *  ]
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetActivityEntriesJson() throws Exception {
    String resp = getResponse("/activitystreams/john.doe/@self", "GET", null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    JSONObject result = getJson(resp);

    assertEquals(2, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));
    assertActivityEntriesEqual(johnsEntry, result.getJSONArray("entry").getJSONObject(0));
  }

  /**
   * Expected response for a list of activities in json:
   *
   * {
   *  "totalResults" : 2,
   *  "startIndex" : 0
   *  "itemsPerPage" : 10 // Note: the js doesn't support paging. Should rest?
   *  "entry" : [
   *     {<entry>} // layed out like above, except for jane.doe
   *  ]
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetFriendsActivityEntriesJson() throws Exception {
    String resp = getResponse("/activitystreams/jane.doe/@friends", "GET", null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    JSONObject result = getJson(resp);

    assertEquals(2, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));
  }

  private void assertActivityEntriesEqual(ActivityEntry entry, JSONObject result)
      throws JSONException {
    assertEquals(entry.getTitle(), result.getString("title"));
    assertEquals(entry.getBody(), result.getString("body"));
    assertEquals(entry.getActor().getId(), result.getJSONObject("actor").getString("id"));
    assertEquals(entry.getActor().getDisplayName(), result.getJSONObject("actor").getString("displayName"));
    assertEquals(entry.getObject().getId(), result.getJSONObject("object").getString("id"));
    assertEquals(entry.getObject().getDisplayName(), result.getJSONObject("object").getString("displayName"));
  }

  @Test
  public void testCreateActivityEntry() throws Exception {
    // Create the activity entry
    String postData = "{title : 'hi mom!', body : 'and dad.', object : {id: '1'}}";
    getResponse("/activitystreams/john.doe/@self", "POST", postData, null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);

    // Verify it can be retrieved
    String resp = getResponse("/activitystreams/john.doe/@self", "GET", null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    JSONObject result = getJson(resp);
    assertEquals(3, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));

    JSONArray entries = result.getJSONArray("entry");
    int newEntryIndex = 0;
    if (entries.getJSONObject(1).getJSONObject("object").getString("id").equals("1")) {
      newEntryIndex = 1;
    }
    if (entries.getJSONObject(2).getJSONObject("object").getString("id").equals("1")) {
      newEntryIndex = 2;
    }

    JSONObject jsonEntry = entries.getJSONObject(newEntryIndex);
    assertEquals("hi mom!", jsonEntry.getString("title"));
    assertEquals("and dad.", jsonEntry.getString("body"));
    assertEquals("1", jsonEntry.getJSONObject("object").getString("id"));
  }
  
  @Test
  public void testUpdateActivityEntry() throws Exception {
    // Create an activity entry
    String postData = "{title : 'hi mom!', body : 'and dad.', object : {id: '1'}}";
    getResponse("/activitystreams/john.doe/@self", "POST", postData, null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    
    // Verify it can be retrieved
    String resp = getResponse("/activitystreams/john.doe/@self", "GET", null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    JSONObject result = getJson(resp);
    assertEquals(3, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));
    
    // Update activity entry
    postData = "{title: 'hi mom2!', body: 'and dad2.', object: {id: '1'}}";
    getResponse("/activitystreams/john.doe/@self", "PUT", postData, null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    
    // Retrieve activities and verify PUT was successful
    resp = getResponse("/activitystreams/john.doe/@self", "GET", null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    result = getJson(resp);
    assertEquals(3, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));
    
    // Find updated entry and test
    JSONArray entries = result.getJSONArray("entry");
    for(int i = 0; i < entries.length(); i++) {
      JSONObject entry = entries.getJSONObject(i);
      if (entry.getJSONObject("object").getString("id").equals("1")) {
        assertEquals("hi mom2!", entry.getString("title"));
        assertEquals("and dad2.", entry.getString("body"));
      }
    }
  }

  // TODO: Add tests for the fields= parameter
}
