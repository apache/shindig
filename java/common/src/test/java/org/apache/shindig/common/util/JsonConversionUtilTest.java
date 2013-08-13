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
package org.apache.shindig.common.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.shindig.common.testing.FakeHttpServletRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Test for conversion of a structured key-value set to a JSON object
 */
public class JsonConversionUtilTest extends Assert {

  @Test
  public void testSimplePathToJsonParsing()
      throws Exception {
    JSONObject root = new JSONObject();
    JsonConversionUtil.buildHolder(root, "a.a.a".split("\\."), 0);
    assertJsonEquals(root, new JSONObject("{a:{a:{}}}"));
  }

  @Test
  public void testArrayPathToJsonParsing()
      throws Exception {
    JSONObject root = new JSONObject();
    JsonConversionUtil.buildHolder(root, "a.a(0).a".split("\\."), 0);
    JsonConversionUtil.buildHolder(root, "a.a(1).a".split("\\."), 0);
    JsonConversionUtil.buildHolder(root, "a.a(2).a".split("\\."), 0);
    assertJsonEquals(root, new JSONObject("{a:{a:[{},{},{}]}}"));
  }

  @Test
  public void testValueToJsonParsing()
      throws Exception {
    String longNumber = "108502345354398668456";
    assertJsonEquals(JsonConversionUtil.convertToJsonValue(longNumber), longNumber);
    String longDoubleOverflow = "108502345354398668456.1234";
    assertJsonEquals(JsonConversionUtil.convertToJsonValue(longDoubleOverflow),
        longDoubleOverflow);
    String longDoubleFractionPart = "1.108502345354398668456108502345354398668456";
    assertJsonEquals(JsonConversionUtil.convertToJsonValue(longDoubleFractionPart),
        longDoubleFractionPart);
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("12345"), 12345);
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("12.345"), 12.345);
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("abc"), "abc");
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("\"a,b,c\""), "a,b,c");
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("true"), true);
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("false"), false);
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("null"), JSONObject.NULL);
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("'abc'"), "abc");
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("a,b,c"),
        new JSONArray(Lists.newArrayList("a", "b", "c")));
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("1,2,3,true,false,null"),
        new JSONArray(Lists.<Object>newArrayList(1, 2, 3, true,
            false, null)));
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("(1)"),
        new JSONArray(Lists.newArrayList(1)));
    assertJsonEquals(JsonConversionUtil.convertToJsonValue("(true)"),
        new JSONArray(Lists.newArrayList(true)));
  }

  @Test
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

  @Test
  public void testJSONToParameterMapParsing()
      throws Exception {
    Map<String, String> resultMap = JsonConversionUtil
        .fromJson(new JSONObject("{a:{b:[{c:\"hello\"},{c:\"hello\"}]}}"));
    assertEquals(2, resultMap.size());
    assertEquals("hello", resultMap.get(".a.b(0).c"));
    assertEquals("hello", resultMap.get(".a.b(1).c"));
  }

  @Test
  public void testJsonFromRequest() throws Exception {
    HttpServletRequest fakeRequest;
    for (String badParms : ImmutableList.of("x=1", "x=1&callback=")) {
      fakeRequest = new FakeHttpServletRequest("http://foo.com/gadgets/rpc?" + badParms);
      assertNull(JsonConversionUtil.fromRequest(fakeRequest));
    }
   }

  public static void assertJsonEquals(Object expected, Object actual)
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

      for (String key : JSONObject.getNames(expectedObject)) {
        assertTrue("missing key " + key, actualObject.has(key));
        assertJsonEquals(expectedObject.get(key), actualObject.get(key));
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
