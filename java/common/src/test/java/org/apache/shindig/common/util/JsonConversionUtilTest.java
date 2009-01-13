/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.common.util;

import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import junit.framework.TestCase;

import java.util.Iterator;

/**
 * Test for conversion of a structured key-value set to a JSON object
 */
public class JsonConversionUtilTest extends TestCase {

  public JsonConversionUtilTest() {
  }

  public void testSimplePathToJsonParsing()
      throws Exception {
    JSONObject root = new JSONObject();
    JsonConversionUtil.buildHolder(root, "a.a.a".split("\\."), 0);
    assertJsonEquals(root, new JSONObject("{a:{a:{}}}"));
  }

  public void testArrayPathToJsonParsing()
      throws Exception {
    JSONObject root = new JSONObject();
    JsonConversionUtil.buildHolder(root, "a.a(0).a".split("\\."), 0);
    JsonConversionUtil.buildHolder(root, "a.a(1).a".split("\\."), 0);
    JsonConversionUtil.buildHolder(root, "a.a(2).a".split("\\."), 0);
    assertJsonEquals(root, new JSONObject("{a:{a:[{},{},{}]}}"));
  }

  public void testValueToJsonParsing()
      throws Exception {
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("abc"), "abc");
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("\"a,b,c\""), "a,b,c");
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("true"), true);
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("false"), false);
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("null"), JSONObject.NULL);
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("'abc'"), "abc");
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("a,b,c"),
        new JSONArray(Lists.newArrayList("a", "b", "c")));
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("1,2,3,true,false,null"),
        new JSONArray(Lists.newArrayList(1, 2, 3, true,
            false, null)));
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("(1)"),
        new JSONArray(Lists.newArrayList(1)));
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("(true)"),
        new JSONArray(Lists.newArrayList(true)));
  }

  public void testParameterMapToJsonParsing()
      throws Exception {
    assertJsonEquals(JsonConversionUtil.parametersToJsonObject(ImmutableMap.of("a.b.c", "1")),
        new JSONObject("{a:{b:{c:1}}}"));
    assertJsonEquals(
        JsonConversionUtil.parametersToJsonObject(ImmutableMap.of("a.b.c", "\"1\"")),
        new JSONObject("{a:{b:{c:\"1\"}}}"));
    assertJsonEquals(JsonConversionUtil.parametersToJsonObject(ImmutableMap.of("a.b.c", "true")),
        new JSONObject("{a:{b:{c:true}}}"));
    assertJsonEquals(
        JsonConversionUtil.parametersToJsonObject(ImmutableMap.of("a.b.c", "false")),
        new JSONObject("{a:{b:{c:false}}}"));
    assertJsonEquals(JsonConversionUtil.parametersToJsonObject(ImmutableMap.of("a.b.c", "null")),
        new JSONObject("{a:{b:{c:null}}}"));
    assertJsonEquals(JsonConversionUtil.parametersToJsonObject(
        ImmutableMap.of("a.b(0).c", "hello", "a.b(1).c", "hello")),
        new JSONObject("{a:{b:[{c:\"hello\"},{c:\"hello\"}]}}"));
    assertJsonEquals(JsonConversionUtil.parametersToJsonObject(
        ImmutableMap.of("a.b.c", "hello, true, false, null, 1,2, \"null\", \"()\"")),
        new JSONObject("{a:{b:{c:[\"hello\",true,false,null,1,2,\"null\",\"()\"]}}}"));
    assertJsonEquals(JsonConversionUtil.parametersToJsonObject(
        ImmutableMap.of("a.b.c", "\"hello, true, false, null, 1,2\"")),
        new JSONObject("{a:{b:{c:\"hello, true, false, null, 1,2\"}}}"));
  }

  public void testJSONToParameterMapParsing()
      throws Exception {
    java.util.Map resultMap = JsonConversionUtil
        .fromJson(new JSONObject("{a:{b:[{c:\"hello\"},{c:\"hello\"}]}}"));
  }

  private void assertJsonEquals(Object expected, Object actual)
      throws JSONException {
    if (expected == null) {
      assertNull(actual);
      return;
    }
    assertNotNull(actual);
    if (expected instanceof JSONObject) {
      JSONObject expectedObject = (JSONObject) expected;
      JSONObject actualObject = (JSONObject) actual;
      if (expectedObject.length() == 0) {
        assertEquals(expectedObject.length(), actualObject.length());
        return;
      }
      assertEquals(expectedObject.names().length(), actualObject.names().length());
      String key;
      for (Iterator keys = expectedObject.keys(); keys.hasNext();
          assertJsonEquals(expectedObject.get(key), actualObject.get(key))) {
        key = (String) keys.next();
        assertTrue(actualObject.has(key));
      }
    } else if (expected instanceof JSONArray) {
      JSONArray expectedArray = (JSONArray) expected;
      JSONArray actualArray = (JSONArray) actual;
      assertEquals(expectedArray.length(), actualArray.length());
      for (int i = 0; i < expectedArray.length(); i++) {
        if (expectedArray.isNull(i)) {
          assertTrue(actualArray.isNull(i));
        } else {
          assertJsonEquals(expectedArray.get(i), actualArray.get(i));
        }
      }

    } else {
      assertEquals(expected, actual);
    }
  }
}
