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


public class RestfulJsonDataTests extends AbstractLargeRestfulTests {

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @After
  public void tearDown() throws Exception {
    super.tearDown();
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
   * 
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetAppDataJson() throws Exception {
    // app id is mocked out
    resp = client.get(BASEURL + "/appdata/john.doe/@friends/appId");
    checkForGoodJsonResponse(resp);
    JSONObject result = getJson(resp);
  }

}
