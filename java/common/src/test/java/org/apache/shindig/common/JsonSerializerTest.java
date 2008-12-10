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
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Tests for JsonSerializer.
 *
 * This class may be executed to perform micro benchmarks comparing the performance of the
 * serializer with that of json.org and net.sf.json.
 */
public class JsonSerializerTest {

  @Test
  public void serializeSimpleJsonObject() throws Exception {
    JSONObject json = new JSONObject("{\"foo\":\"bar\"}");
    assertTrue("Did not produce results matching reference implementation.",
               jsonEquals(json.toString(), JsonSerializer.serialize(json)));
  }

  @Test
  public void serializeSimpleMap() throws Exception {
    Map<String, String> map = ImmutableMap.of("hello", "world", "foo", "bar");
    assertTrue("Did not produce results matching reference implementation.",
        jsonEquals(new JSONObject(map).toString(), JsonSerializer.serialize(map)));
  }

  @Test
  public void serializeSimpleCollection() throws Exception {
    Collection<String> collection = Arrays.asList("foo", "bar", "baz");
    assertEquals("[\"foo\",\"bar\",\"baz\"]", JsonSerializer.serialize(collection));
  }

  @Test
  public void serializeArray() throws Exception {
    String[] array = new String[] {"foo", "bar", "baz"};
    assertEquals("[\"foo\",\"bar\",\"baz\"]", JsonSerializer.serialize(array));
  }

  @Test
  public void serializeJsonArray() throws Exception {
    JSONArray array = new JSONArray(new String[] {"foo", "bar", "baz"});
    assertEquals("[\"foo\",\"bar\",\"baz\"]", JsonSerializer.serialize(array));
  }

  @Test
  public void serializeMixedObjects() throws Exception {
    Map<String, ? extends Object> map = ImmutableMap.of(
        "integer", Integer.valueOf(100),
        "double", Double.valueOf(233333333333.7d),
        "boolean", Boolean.TRUE,
        "map", ImmutableMap.of("hello", "world", "foo", "bar"),
        "string", "hello!");
    assertTrue("Did not produce results matching reference implementation.",
        jsonEquals(new JSONObject(map).toString(), JsonSerializer.serialize(map)));
  }

  @Test
  public void serializeMixedArray() throws Exception {
    Collection<Object> data = Arrays.asList(
        "integer", Integer.valueOf(100),
        "double", Double.valueOf(233333333333.7d),
        "boolean", Boolean.TRUE,
        Arrays.asList("one", "two", "three"),
        new JSONArray(new String[] {"foo", "bar"}),
        "string", "hello!");
    assertEquals(new JSONArray(data).toString(), JsonSerializer.serialize(data));
  }

  @Test
  public void emptyString() throws Exception {
    StringBuilder builder = new StringBuilder();
    JsonSerializer.appendString(builder, "");

    assertEquals("\"\"", builder.toString());
  }

  @Test
  public void escapeSequences() throws Exception {
    StringBuilder builder = new StringBuilder();
    JsonSerializer.appendString(builder, "\t\r value \\\foo\b\uFFFF\uBCAD\n\u0083");

    assertEquals("\"\\t\\r value \\\\\\foo\\b\uFFFF\uBCAD\\n\\u0083\"", builder.toString());
  }

  private static String avg(long start, long end, long runs) {
    double delta = end - start;
    return String.format("%f5", delta / runs);
  }

  private static String runJsonOrgTest(Map<String, Object> data, int iterations) {
    org.json.JSONObject object = new org.json.JSONObject(data);
    long start = System.currentTimeMillis();
    String result = null;
    for (int i = 0; i < iterations; ++i) {
      result = object.toString();
    }
    System.out.println("json.org: " + avg(start, System.currentTimeMillis(), iterations) + "ms");
    return result;
  }

  private static String runSerializerTest(Map<String, Object> data, int iterations) {
    long start = System.currentTimeMillis();
    String result = null;
    for (int i = 0; i < iterations; ++i) {
      result = JsonSerializer.serialize(data);
    }
    System.out.println("serializer: " + avg(start, System.currentTimeMillis(), iterations) + "ms");
    return result;
  }


  // private static String runNetSfJsonTest(Map<String, Object> data, int iterations) {
  //   net.sf.json.JSONObject object = net.sf.json.JSONObject.fromObject(data);
  //   long start = System.currentTimeMillis();
  //   String result = null;
  //   for (int i = 0; i < iterations; ++i) {
  //     result = object.toString();
  //   }
  //   System.out.println("net.sf.json: " + avg(start, System.currentTimeMillis(), iterations) + "ms");
  //   return result;
  // }

  public static Map<String, Object> perfComparison100SmallValues() {
    Map<String, Object> data = Maps.newHashMap();
    for (int i = 0; i < 100; ++i) {
      data.put("key-" + i, "small value");
    }

    return data;
  }

  public static Map<String, Object> perfComparison1000SmallValues() {
    Map<String, Object> data = Maps.newHashMap();
    for (int i = 0; i < 1000; ++i) {
      data.put("key-" + i, "small value");
    }

    return data;
  }

  public static Map<String, Object> perfComparison100LargeValues() {
    Map<String, Object> data = Maps.newHashMap();
    for (int i = 0; i < 100; ++i) {
      data.put("key-" + i, StringUtils.repeat("small value", 100));
    }
    return data;
  }

  public static Map<String, Object> perfComparison10LargeValuesAndEscapes() {
    Map<String, Object> data = Maps.newHashMap();
    for (int i = 0; i < 10; ++i) {
      data.put("key-" + i, StringUtils.repeat("\tsmall\r value \\foo\b\uFFFF\uBCAD\n\u0083", 100));
    }
    return data;
  }

  public static Map<String, Object> perfComparison100Arrays() {
    Map<String, Object> data = Maps.newHashMap();
    String[] array = new String[] {
      "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"
    };

    for (int i = 0; i < 100; ++i) {
      data.put("key-" + i, array);
    }

    return data;
  }

  private static boolean jsonEquals(JSONObject left, JSONObject right) {
    if (left.length() != right.length()) {
      return false;
    }
    for (String name : JSONObject.getNames(left)) {
      Object leftValue = left.opt(name);
      Object rightValue = right.opt(name);
      if (leftValue instanceof JSONObject) {
        if (!jsonEquals((JSONObject)leftValue, (JSONObject)rightValue)) {
          return false;
        }
      } else if (leftValue instanceof JSONArray) {
        JSONArray leftArray = (JSONArray)leftValue;
        JSONArray rightArray = (JSONArray)rightValue;
        for (int i = 0; i < leftArray.length(); ++i) {
          if (!(leftArray.opt(i).equals(rightArray.opt(i)))) {
            return false;
          }
        }
      } else if (!leftValue.equals(rightValue)) {
        System.out.println("Not a match: " + leftValue + " != " + rightValue);
        return false;
      }
    }
    return true;
  }

  private static boolean jsonEquals(String reference, String comparison) throws Exception {
    return jsonEquals(new JSONObject(reference), new JSONObject(comparison));
  }

  public static void main(String[] args) throws Exception {
    int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
    System.out.println("Running tests with " + iterations + " iterations.");

    for (Method method : JsonSerializerTest.class.getMethods()) {
      if (method.getName().startsWith("perfComparison")) {
        Map<String, Object> data = (Map<String, Object>)method.invoke(null);
        System.out.println("Running: " + method.getName());

        String jsonOrg = runJsonOrgTest(data, iterations);
        String serializer = runSerializerTest(data, iterations);
        // String netSfJson = runNetSfJsonTest(data, iterations);

        if (!jsonEquals(jsonOrg, serializer)) {
          System.out.println("Serializer did not produce results matching the reference impl.");
        }

        // if (!jsonEquals(jsonOrg, netSfJson)) {
        //   System.out.println("net.sf.json did not produce results matching the reference impl.");
        // }
        System.out.println("-----------------------");
      }
    }
    System.out.println("Done");
  }
}
