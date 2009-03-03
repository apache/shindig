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

import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class JsonAssert {
  private JsonAssert() {}

  public static void assertJsonArrayEquals(JSONArray left, JSONArray right) throws Exception {
    if (left.length() != right.length()) {
      assertEquals("Arrays are not of equal length", left.toString(), right.toString());
    }

    for (int i = 0; i < left.length(); ++i) {
      Object leftValue = left.opt(i);
      Object rightValue = right.opt(i);

      assertEquals(left.toString() + " != " + right.toString(),
                   leftValue.getClass(), rightValue.getClass());

      if (leftValue instanceof JSONObject) {
        assertJsonObjectEquals((JSONObject) leftValue, (JSONObject) rightValue);
      } else if (leftValue instanceof JSONArray) {
        assertJsonArrayEquals((JSONArray) leftValue, (JSONArray) rightValue);
      } else {
        assertEquals(leftValue, rightValue);
      }
    }
  }

  public static void assertJsonObjectEquals(JSONObject left, JSONObject right) throws Exception {
    if (left.length() != right.length()) {
      assertEquals("Objects are not of equal size", left.toString(2), right.toString(2));
    }

    // Both are emtpy so skip
    if (JSONObject.getNames(left) == null && JSONObject.getNames(right) == null) {
      return;
    }
    for (String name : JSONObject.getNames(left)) {
      Object leftValue = left.opt(name);
      Object rightValue = right.opt(name);

      if (leftValue != null) {
        assertNotNull(left.toString() + " != " + right.toString(), rightValue);
      }
      assertEquals(left.toString() + " != " + right.toString(),
                   leftValue.getClass(), rightValue.getClass());

      if (leftValue instanceof JSONObject) {
        assertJsonObjectEquals((JSONObject) leftValue, (JSONObject) rightValue);
      } else if (leftValue instanceof JSONArray) {
        assertJsonArrayEquals((JSONArray) leftValue, (JSONArray) rightValue);
      } else {
        assertEquals(leftValue, rightValue);
      }
    }
  }

  public static void assertJsonEquals(String left, String right) throws Exception {
    switch (left.charAt(0)) {
      case '{':
        assertJsonObjectEquals(new JSONObject(left), new JSONObject(right));
        break;
      case '[':
        assertJsonArrayEquals(new JSONArray(left), new JSONArray(right));
        break;
      default:
        assertEquals(left, right);
        break;
    }
  }
}
