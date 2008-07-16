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

import org.json.JSONObject;
import org.junit.Test;

public class RestfulBatchTest extends AbstractLargeRestfulTests {

  /**
   * Batch format:
   *   POST to /jsonBatch
   *   {request :
   *     {friends : {url : /people/john.doe/@friends, method : GET}}
   *     {john : {url : /people/john.doe/@self, method : GET}}
   *     {updateData : {url : /appdata/john.doe/@self/appId, method : POST, postData : {count : 1}}}
   *   }
   *
   *
   * Expected response
   *
   *  {error : false,
   *   responses : {
   *     {friends : {response : {<friend collection>}}}
   *     {john : {response : {<john.doe>}}}
   *     {updateData : {response : {}}}
   *  }
   *
   * Each response can possibly have .error and .errorMessage properties as well.
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetBatchRequest() throws Exception {
    String postData = '{'
        + "friends : {url : '/people/john.doe/@friends', method : 'GET'}, "
        + "john : {url : '/people/john.doe/@self', method : 'GET'}, "
        + "updateData : {url : '/appdata/john.doe/@self/a', method : 'POST', postData : {count : 1}}}"
        + '}';

    String resp = getJsonResponse("jsonBatch", "POST", postData);
    JSONObject result = getJson(resp);

    assertEquals(false, result.getBoolean("error"));

    JSONObject jsonResponses = result.getJSONObject("responses");
    assertEquals(3, jsonResponses.length());

    // friends response
    JSONObject jsonFriends = jsonResponses.getJSONObject("friends");
    assertFalse(jsonFriends.has("error"));
    assertFalse(jsonFriends.has("errorMessage"));

    JSONObject jsonFriendsResponse = jsonFriends.getJSONObject("response");
    assertEquals(2, jsonFriendsResponse.getInt("totalResults"));
    assertEquals(0, jsonFriendsResponse.getInt("startIndex"));

    // john.doe response
    JSONObject jsonJohn = jsonResponses.getJSONObject("john");
    assertFalse(jsonJohn.has("error"));
    assertFalse(jsonJohn.has("errorMessage"));

    JSONObject jsonJohnResponse = jsonJohn.getJSONObject("response");
    assertEquals("john.doe", jsonJohnResponse.getString("id"));
    assertEquals("John Doe", jsonJohnResponse.getJSONObject("name").getString("unstructured"));

    // john.doe response
    JSONObject jsonData = jsonResponses.getJSONObject("updateData");
    assertFalse(jsonData.has("error"));
    assertFalse(jsonData.has("errorMessage"));
    assertTrue(jsonData.has("response"));
  }

}