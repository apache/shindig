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

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class RestfulJsonActivityTests extends AbstractLargeRestfulTests {

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @After
  public void tearDown() throws Exception {
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
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetActivitiesJson() throws Exception {
    resp = client.get(BASEURL + "/activities/john.doe/@self");
    checkForGoodJsonResponse(resp);
    JSONObject result = getJson(resp);
  }
}
