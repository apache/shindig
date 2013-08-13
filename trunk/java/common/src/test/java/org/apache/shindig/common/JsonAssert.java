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
package org.apache.shindig.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.json.JSONArray;
import org.json.JSONObject;

public final class JsonAssert {
  private JsonAssert() {}

  public static void assertJsonArrayEquals(JSONArray expected, JSONArray actual) throws Exception {
    assertJsonArrayEquals(null, expected, actual);
  }

  public static void assertJsonArrayEquals(String message, JSONArray expected, JSONArray actual)
          throws Exception {
    if (expected.length() != actual.length()) {
      assertEquals("Arrays are not of equal length", expected.toString(), actual.toString());
    }

    for (int i = 0; i < expected.length(); ++i) {
      Object expectedValue = expected.opt(i);
      Object actualValue = actual.opt(i);

      assertSame(expected.toString() + " != " + actual.toString(), expectedValue.getClass(),
              actualValue.getClass());

      if (expectedValue instanceof JSONObject) {
        assertJsonObjectEquals(message, (JSONObject) expectedValue, (JSONObject) actualValue);
      } else if (expectedValue instanceof JSONArray) {
        assertJsonArrayEquals(message, (JSONArray) expectedValue, (JSONArray) actualValue);
      } else {
        assertEquals(expectedValue, actualValue);
      }
    }
  }

  public static void assertJsonObjectEquals(JSONObject expected, JSONObject actual)
          throws Exception {
    assertJsonObjectEquals(null, expected, actual);
  }

  public static void assertJsonObjectEquals(String message, JSONObject expected, JSONObject actual)
          throws Exception {
    if (expected.length() != actual.length()) {
      assertEquals("Objects are not of equal size", expected.toString(2), actual.toString(2));
    }

    // Both are empty so skip
    if (JSONObject.getNames(expected) == null && JSONObject.getNames(actual) == null) {
      return;
    }
    for (String name : JSONObject.getNames(expected)) {
      Object expectedValue = expected.opt(name);
      Object actualValue = actual.opt(name);

      if (expectedValue != null) {
        assertNotNull(expected.toString() + " != " + actual.toString(), actualValue);
      }
      assertSame(expected.toString() + " != " + actual.toString(), expectedValue.getClass(),
              actualValue.getClass());

      if (expectedValue instanceof JSONObject) {
        assertJsonObjectEquals(message, (JSONObject) expectedValue, (JSONObject) actualValue);
      } else if (expectedValue instanceof JSONArray) {
        assertJsonArrayEquals(message, (JSONArray) expectedValue, (JSONArray) actualValue);
      } else {
        assertEquals(expectedValue, actualValue);
      }
    }
  }

  public static void assertJsonEquals(String expected, String actual) throws Exception {
    assertJsonEquals(null, expected, actual);
  }

  public static void assertJsonEquals(String message, String expected, String actual)
          throws Exception {
    switch (expected.charAt(0)) {
    case '{':
      assertJsonObjectEquals(message, new JSONObject(expected), new JSONObject(actual));
      break;
    case '[':
      assertJsonArrayEquals(message, new JSONArray(expected), new JSONArray(actual));
      break;
    default:
      assertEquals(expected, actual);
      break;
    }
  }

  public static void assertObjectEquals(Object expected, Object actual) throws Exception {
    assertObjectEquals(null, expected, actual);
  }

  public static void assertObjectEquals(String message, Object expected, Object actual)
          throws Exception {
    if (!(expected instanceof String)) {
      expected = JsonSerializer.serialize(expected);
    }

    if (!(actual instanceof String)) {
      actual = JsonSerializer.serialize(actual);
    }

    assertJsonEquals(message, (String) expected, (String) actual);
  }
}
