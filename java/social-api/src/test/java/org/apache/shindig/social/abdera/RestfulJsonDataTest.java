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

import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.ResponseItem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


public class RestfulJsonDataTest extends AbstractLargeRestfulTests {
  private Map<String, Map<String, String>> data;
  private Map<String, String> janeData;
  private Map<String, String> simpleData;
  private Map<String, String> johnData;

  @Before
  public void setUp() throws Exception {
    super.setUp();

    data = new HashMap<String, Map<String, String>>();
    janeData = new HashMap<String, String>();
    janeData.put("count", "5");

    simpleData = new HashMap<String, String>();
    simpleData.put("count", "7");

    johnData = new HashMap<String, String>();
    johnData.put("count", "0");
  }

  @After
  public void tearDown() throws Exception {
    SocialApiTestsGuiceModule.MockDataService.setPersonData(null);
    super.tearDown();
  }

  private void updateAppData() {
    SocialApiTestsGuiceModule.MockDataService.setPersonData(
        new ResponseItem<Map<String, Map<String, String>>>(data));
  }

  /**
   * Expected response for app data in json:
   *
   * {
   *  "entry" : {
   *    "jane.doe" : {"count" : "5"},
   *    "simple.doe" : {"count" : "7"},
   *  }
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetFriendsAppDataJson() throws Exception {
    data.put("jane.doe", janeData);
    data.put("simple.doe", simpleData);
    updateAppData();

    // app id is mocked out
    resp = client.get(BASEURL + "/appdata/john.doe/@friends/app?fields=count");
    // checkForGoodJsonResponse(resp);
    // JSONObject result = getJson(resp);
  }

  /**
   * Expected response for app data in json:
   *
   * {
   *  "entry" : {
   *    "john.doe" : {"count" : "0"},
   *  }
   * }
   *
   * @throws Exception if test encounters an error
   */
   // TODO: Shindig currently throws an exception when no keys are specified
   // @Test
  public void testGetSelfAppDataJson() throws Exception {
    data.put("john.doe", johnData);
    updateAppData();

    // app id is mocked out
    resp = client.get(BASEURL + "/appdata/john.doe/@self/app");
    // checkForGoodJsonResponse(resp);
    // JSONObject result = getJson(resp);
  }

  /**
   * Expected response for app data in json:
   *
   * {
   *  "entry" : {
   *    "john.doe" : {"count" : "0"},
   *  }
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetSelfAppDataJsonWithKey() throws Exception {
    data.put("john.doe", johnData);
    updateAppData();

    // app id is mocked out
    resp = client.get(BASEURL + "/appdata/john.doe/@self/app?fields=count");
    // checkForGoodJsonResponse(resp);
    // JSONObject result = getJson(resp);
  }

  /**
   * Expected response for app data in json with non-existant key:
   * TODO: Double check this output with the spec
   *
   * {
   *  "entry" : {
   *    "john.doe" : {},
   *  }
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetSelfAppDataJsonWithoutKeys() throws Exception {
    data.put("john.doe", johnData);
    updateAppData();

    // app id is mocked out
    resp = client.get(BASEURL + "/appdata/john.doe/@self/app?fields=peabody");
    // checkForGoodJsonResponse(resp);
    // JSONObject result = getJson(resp);
  }

  // TODO: support for indexBy??
  // TODO: support for post and delete

}
