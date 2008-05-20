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
import org.apache.shindig.social.opensocial.model.Activity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


public class RestfulJsonActivityTest extends AbstractLargeRestfulTests {
  private Activity activity;

  @Before
  public void setUp() throws Exception {
    super.setUp();

    activity = SocialApiTestsGuiceModule.MockActivitiesService.basicActivity;
    List<Activity> activities = new ArrayList<Activity>();
    activities.add(activity);

    SocialApiTestsGuiceModule.MockActivitiesService.setActivities(
        new ResponseItem<List<Activity>>(activities));
    SocialApiTestsGuiceModule.MockActivitiesService.setActivity(
        new ResponseItem<Activity>(activity));
  }

  @After
  public void tearDown() throws Exception {
    SocialApiTestsGuiceModule.MockActivitiesService.setActivities(null);
    SocialApiTestsGuiceModule.MockActivitiesService.setActivity(null);

    super.tearDown();
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
    resp = client.get(BASEURL + "/activities/john.doe/@self/1");
    // checkForGoodJsonResponse(resp);
    // JSONObject result = getJson(resp);
    // assertActivitiesEqual(activity, result);
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
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetActivitiesJson() throws Exception {
    resp = client.get(BASEURL + "/activities/john.doe/@self");
    // checkForGoodJsonResponse(resp);
    // JSONObject result = getJson(resp);
    // assertActivitiesEqual(activity,
    //     result.getJSONArray("entry").getJSONObject(0));
  }

  // TODO: Add tests for the fields= parameter
  // TODO: Add tests for post
  // TODO: Add tests for @friends
}
