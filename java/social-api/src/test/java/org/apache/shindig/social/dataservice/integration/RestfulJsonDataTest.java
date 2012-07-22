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

import com.google.common.collect.Maps;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Map;


public class RestfulJsonDataTest extends AbstractLargeRestfulTests {

  /**
   * Expected response for app data in json:
   *
   * {
   *  "entry" : {
   *    "jane.doe" : {"count" : "7"},
   *    "george.doe" : {"count" : "2"},
   *    "maija.m" : {}, // TODO: Should this entry really be included if she doesn't have any data?
   *  }
   * }
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetFriendsAppDataJson() throws Exception {
    // app id is mocked out
    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("fields", "count");
    String resp = getResponse("/appdata/john.doe/@friends/app", "GET", extraParams, null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);

    JSONObject data = getJson(resp).getJSONObject("entry");
    assertEquals(3, data.length());

    JSONObject janesEntries = data.getJSONObject("jane.doe");
    assertEquals(1, janesEntries.length());
    assertEquals("7", janesEntries.getString("count"));

    JSONObject georgesEntries = data.getJSONObject("george.doe");
    assertEquals(1, georgesEntries.length());
    assertEquals("2", georgesEntries.getString("count"));
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
  public void testGetSelfAppDataJson() throws Exception {
    // app id is mocked out
    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("fields", null);
    String resp = getResponse("/appdata/john.doe/@self/app", "GET", extraParams, null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);

    JSONObject data = getJson(resp).getJSONObject("entry");
    assertEquals(1, data.length());

    JSONObject johnsEntries = data.getJSONObject("john.doe");
    assertEquals(1, johnsEntries.length());
    assertEquals("0", johnsEntries.getString("count"));
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
    // app id is mocked out
    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("fields", "count");
    String resp = getResponse("/appdata/john.doe/@self/app", "GET", extraParams, null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);

    JSONObject data = getJson(resp).getJSONObject("entry");
    assertEquals(1, data.length());

    JSONObject johnsEntries = data.getJSONObject("john.doe");
    assertEquals(1, johnsEntries.length());
    assertEquals("0", johnsEntries.getString("count"));
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
  public void testGetSelfAppDataJsonWithInvalidKeys() throws Exception {
    // app id is mocked out
    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("fields", "peabody");
    String resp = getResponse("/appdata/john.doe/@self/app", "GET", extraParams, null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);

    JSONObject data = getJson(resp).getJSONObject("entry");
    assertEquals(1, data.length());

    JSONObject johnsEntries = data.getJSONObject("john.doe");
    assertEquals(0, johnsEntries.length());
  }

  @Test
  public void testDeleteAppData() throws Exception {
    assertCount("0");

    // With the wrong field
    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("fields", "peabody");
    getResponse("/appdata/john.doe/@self/app", "DELETE", extraParams, null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);

    assertCount("0");

    extraParams.put("fields", "count");
    getResponse("/appdata/john.doe/@self/app", "DELETE", extraParams, null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);

    assertCount(null);
  }

  @Test
  public void testUpdateAppData() throws Exception {
    assertCount("0");

    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("fields", "count");
    String postData = "{count : 5}";
    getResponse("/appdata/john.doe/@self/app", "POST", extraParams, postData, null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);

    assertCount("5");
  }

  private void assertCount(String expectedCount) throws Exception {
    String resp = getResponse("/appdata/john.doe/@self/app", "GET", null,
        ContentTypes.OUTPUT_JSON_CONTENT_TYPE);
    JSONObject data = getJson(resp).getJSONObject("entry");
    assertEquals(1, data.length());

    JSONObject johnsEntries = data.getJSONObject("john.doe");

    if (expectedCount != null) {
      assertEquals(1, johnsEntries.length());
      assertEquals(expectedCount, johnsEntries.getString("count"));
    } else {
      assertEquals(0, johnsEntries.length());
    }
  }

  // TODO: support for indexBy??

}
