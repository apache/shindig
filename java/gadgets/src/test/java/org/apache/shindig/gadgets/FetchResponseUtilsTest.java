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
package org.apache.shindig.gadgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Test of FetchResponseUtils
 */
public class FetchResponseUtilsTest {

  @Test
  public void testSimpleResponse() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setHttpStatusCode(999)
        .create();
    JSONObject obj = FetchResponseUtils.getResponseAsJson(response, "key", "body");
    assertEquals(999, obj.getInt("rc"));
    assertEquals("key", obj.getString("id"));
    assertEquals("body", obj.getString("body"));
    assertEquals(0, obj.getJSONObject("headers").length());
  }
  
  @Test
  public void testMetadata() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setHttpStatusCode(999)
        .setMetadata("metaname", "metavalue")
        .setMetadata("more meta", "more value")
        .create();
    JSONObject obj = FetchResponseUtils.getResponseAsJson(response, null, "body");
    assertEquals(999, obj.getInt("rc"));
    assertFalse(obj.has("id"));
    assertEquals("body", obj.getString("body"));
    assertEquals("metavalue", obj.getString("metaname"));
    assertEquals("more value", obj.getString("more meta"));
  }
  
  @Test
  public void testHeaders() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setHttpStatusCode(999)
        .setHeader("Set-Cookie", "cookie")
        .setHeader("location", "somewhere")
        .create();
    JSONObject obj = FetchResponseUtils.getResponseAsJson(response, "key", "body");
    assertEquals(999, obj.getInt("rc"));
    assertEquals("key", obj.getString("id"));
    assertEquals("body", obj.getString("body"));
    assertEquals(1, obj.getJSONObject("headers").getJSONArray("set-cookie").length());
    assertEquals("cookie", obj.getJSONObject("headers").getJSONArray("set-cookie").get(0));
    assertEquals(1, obj.getJSONObject("headers").getJSONArray("location").length());
    assertEquals("somewhere", obj.getJSONObject("headers").getJSONArray("location").get(0));
  }
  
  @Test
  public void testMultiValuedHeaders() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setHttpStatusCode(999)
        .addHeader("Set-Cookie", "cookie")
        .addHeader("Set-Cookie", "cookie2")
        .addHeader("Set-Cookie", "cookie3")
        .create();
    JSONObject obj = FetchResponseUtils.getResponseAsJson(response, "key", "body");
    assertEquals(999, obj.getInt("rc"));
    assertEquals("key", obj.getString("id"));
    assertEquals("body", obj.getString("body"));
    assertEquals(3, obj.getJSONObject("headers").getJSONArray("set-cookie").length());
    assertEquals("cookie", obj.getJSONObject("headers").getJSONArray("set-cookie").get(0));
    assertEquals("cookie2", obj.getJSONObject("headers").getJSONArray("set-cookie").get(1));
    assertEquals("cookie3", obj.getJSONObject("headers").getJSONArray("set-cookie").get(2));
  }
}
