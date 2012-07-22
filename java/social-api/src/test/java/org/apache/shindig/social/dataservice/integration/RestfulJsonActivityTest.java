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

import org.apache.shindig.protocol.ContentTypes;
import org.apache.shindig.social.core.model.ActivityImpl;
import org.apache.shindig.social.opensocial.model.Activity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class RestfulJsonActivityTest extends AbstractLargeRestfulTests {
  Activity johnsActivity;

  @Before
  public void restfulJsonActivityTestBefore() throws Exception {
    johnsActivity = new ActivityImpl("1", "john.doe");
    johnsActivity.setTitle("yellow");
    johnsActivity.setBody("what a color!");
  }

  /**
   * Expected response for an activity in json:
   * { 'entry' : {
   *     'id' : '1',
   *     'userId' : 'john.doe',
   *     'title' : 'yellow',
   *     'body' : 'what a color!'
   *   }
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetActivityJson() throws Exception {
    String resp = getResponse("/activities/john.doe/@self/@app/1", "GET", null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    JSONObject result = getJson(resp);
    assertActivitiesEqual(johnsActivity, result.getJSONObject("entry"));
  }

  /**
   * Expected response for a list of activities in json:
   *
   * {
   *  "totalResults" : 1,
   *  "startIndex" : 0
   *  "itemsPerPage" : 10 // Note: the js doesn't support paging. Should rest?
   *  "entry" : [
   *     {<activity>} // layed out like above
   *  ]
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetActivitiesJson() throws Exception {
    String resp = getResponse("/activities/john.doe/@self", "GET", null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    JSONObject result = getJson(resp);

    assertEquals(1, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));
    assertActivitiesEqual(johnsActivity, result.getJSONArray("list").getJSONObject(0));
  }

  /**
   * Expected response for a list of activities in json:
   *
   * {
   *  "totalResults" : 3,
   *  "startIndex" : 0
   *  "itemsPerPage" : 10 // Note: the js doesn't support paging. Should rest?
   *  "entry" : [
   *     {<activity>} // layed out like above, except for jane.doe
   *  ]
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetFriendsActivitiesJson() throws Exception {
    String resp = getResponse("/activities/john.doe/@friends", "GET", null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    JSONObject result = getJson(resp);

    assertEquals(2, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));
  }

  private void assertActivitiesEqual(Activity activity, JSONObject result)
      throws JSONException {
    assertEquals(activity.getId(), result.getString("id"));
    assertEquals(activity.getUserId(), result.getString("userId"));
    assertEquals(activity.getTitle(), result.getString("title"));
    assertEquals(activity.getBody(), result.getString("body"));
  }

  @Test
  public void testCreateActivity() throws Exception {
    String postData = "{title : 'hi mom!', body : 'and dad.'}";
    // Create the activity
    getResponse("/activities/john.doe/@self", "POST", postData, null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);

    // Verify it can be retrieved
    String resp = getResponse("/activities/john.doe/@self", "GET", null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    JSONObject result = getJson(resp);

    assertEquals(2, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));

    JSONArray activities = result.getJSONArray("list");
    int newActivityIndex = 0;
    if (activities.getJSONObject(0).has("id")) {
      newActivityIndex = 1;
    }

    JSONObject jsonActivity = activities.getJSONObject(newActivityIndex);
    assertEquals("hi mom!", jsonActivity.getString("title"));
    assertEquals("and dad.", jsonActivity.getString("body"));
  }

  // TODO: Add tests for the fields= parameter
}
