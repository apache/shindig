/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shindig.gadgets.http;

import org.apache.shindig.common.uri.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.*;
import org.junit.Test;

public class HttpCacheKeyTest {

  private Uri target = Uri.parse("http://www.example.com/");

  @Test
  public void testGet() throws Exception {
    HttpRequest request = new HttpRequest(target);
    HttpCacheKey key = new HttpCacheKey(request);
    assertTrue(key.isCacheable());
    assertEquals(
        "[{\"method\":\"GET\"},{\"url\":\"http://www.example.com/\"}]",
        key.toString());
  }

  @Test
  public void testNonCacheableOptions() throws Exception {
    HttpRequest request = new HttpRequest(target).setIgnoreCache(true);
    HttpCacheKey key = new HttpCacheKey(request);
    assertFalse(key.isCacheable());
    assertEquals(
        "[{\"method\":\"GET\"},{\"url\":\"http://www.example.com/\"}]",
        key.toString());
  }

  @Test
  public void testNonGet() throws Exception {
    HttpRequest request = new HttpRequest(target).setMethod("POST");
    HttpCacheKey key = new HttpCacheKey(request);
    assertFalse(key.isCacheable());
    assertEquals(
        "[{\"method\":\"POST\"},{\"url\":\"http://www.example.com/\"}]",
        key.toString());
  }

  @Test
  public void testNonGetWithOverride() throws Exception {
    HttpRequest request = new HttpRequest(target).setMethod("POST")
        .addHeader("X-Method-Override", "GET");
    HttpCacheKey key = new HttpCacheKey(request);
    assertTrue(key.isCacheable());
    assertEquals(
        "[{\"method\":\"POST\"},{\"url\":\"http://www.example.com/\"}]",
        key.toString());
  }

  @Test
  public void testOrdered() {
    HttpCacheKey key = new HttpCacheKey(new HttpRequest(target));
    key.set("c", "c");
    key.set("b", "b");
    key.set("g", "g");
    key.set("d", "d");
    key.set("e", "e");
    key.set("f", "f");
    key.set("g", "g");
    key.set("a", "a");
    assertEquals(
        "[{\"a\":\"a\"},{\"b\":\"b\"},{\"c\":\"c\"},{\"d\":\"d\"}," +
    	"{\"e\":\"e\"},{\"f\":\"f\"},{\"g\":\"g\"},{\"method\":\"GET\"}" +
    	",{\"url\":\"http://www.example.com/\"}]",
    	key.toString());
  }

  @Test
  public void testWeirdChars() throws Exception {
    final int CHARS_TO_TEST = 2000;
    HttpCacheKey key = new HttpCacheKey(new HttpRequest(target));
    for (char c = 0; c <= CHARS_TO_TEST; ++c) {
      key.set(Character.toString(c), Character.toString(c));
    }
    key.remove("url");
    key.remove("method");

    String out = key.toString();
    JSONArray array = new JSONArray(out);
    for (char c = 0; c <= CHARS_TO_TEST; ++c) {
      JSONObject o = array.getJSONObject(c);
      String s = Character.toString(c);
      assertEquals(s, o.get(s));
    }
  }
}
