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

import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.ActivityImpl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class RestfulJsonActivityTest extends AbstractLargeRestfulTests {
  Activity johnsActivity;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    johnsActivity = new ActivityImpl("1", "john.doe");
    johnsActivity.setTitle("yellow");
    johnsActivity.setBody("what a color!");
  }

  /**
   * Expected response for an activity in json:
   * {
   *   'id' : '1',
   *   'userId' : 'john.doe',
   *   'title' : 'yellow',
   *   'body' : 'what a color!'
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetActivityJson() throws Exception {
    String resp = getJsonResponse("/activities/john.doe/@self/1", "GET");
    JSONObject result = getJson(resp);
    assertActivitiesEqual(johnsActivity, result);
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
    String resp = getJsonResponse("/activities/john.doe/@self", "GET");
    JSONObject result = getJson(resp);

    assertEquals(1, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));
    assertActivitiesEqual(johnsActivity, result.getJSONArray("entry").getJSONObject(0));
  }

  /**
   * Expected response for a list of activities in json:
   * TODO: Fix the question marks...
   *
   * {
   *  "author" : "<???>",
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
    String resp = getJsonResponse("/activities/john.doe/@friends", "GET");
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
    getJsonResponse("/activities/john.doe/@self", "POST", postData);

    String resp = getJsonResponse("/activities/john.doe/@self", "GET");
    JSONObject result = getJson(resp);

    assertEquals(2, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));

    JSONArray activities = result.getJSONArray("entry");
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