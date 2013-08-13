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

import static org.apache.shindig.common.JsonAssert.assertJsonEquals;
import static org.junit.Assert.assertEquals;

import com.google.common.base.Strings;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.LinkedHashMultimap;

/**
 * Tests for JsonSerializer.
 *
 * This class may be executed to perform micro benchmarks comparing the performance of the
 * serializer with that of json.org and net.sf.json.
 */
public class JsonSerializerTest {

  private static final String JSON_POJO_AS_JSON = "{string:'string-value',integer:100,'simple!':3}";

  @Test
  public void serializeSimpleJsonObject() throws Exception {
    String json = "{foo:'bar'}";
    assertJsonEquals(json, JsonSerializer.serialize(new JSONObject(json)));
  }

  @Test
  public void serializeSimpleMap() throws Exception {
    Map<String, String> map = new HashMap<String, String>(3, 1);
    map.put("hello", "world");
    map.put("foo", "bar");
    map.put("remove", null);
    assertJsonEquals("{hello:'world',foo:'bar'}", JsonSerializer.serialize(map));
  }

  @Test
  public void serializeSimpleMultimap() throws Exception {
    Multimap<String, String> map = LinkedHashMultimap.create();
    Set<String> methods = ImmutableSet.of("system.listMethods", "people.get");
    map.putAll("hostEndpoint", methods);
    assertJsonEquals("{hostEndpoint : ['system.listMethods', 'people.get']}",
        JsonSerializer.serialize(map));
  }

  @Test
  public void serializeSimpleCollection() throws Exception {
    Collection<String> collection = Arrays.asList("foo", null, "bar", "baz", null);
    assertJsonEquals("['foo','bar','baz']", JsonSerializer.serialize(collection));
  }

  @Test
  public void serializeArray() throws Exception {
    String[] array = {"foo", null, "bar", "baz"};
    assertJsonEquals("['foo','bar','baz']", JsonSerializer.serialize(array));
  }

  @Test
  public void serializeJsonArray() throws Exception {
    JSONArray array = new JSONArray(new String[] {"foo", null, "bar", "baz"});
    assertJsonEquals("['foo','bar','baz']", JsonSerializer.serialize(array));
  }

  @Test
  public void serializeJsonObjectWithComplexArray() throws Exception {
    JSONArray array = new JSONArray();
    array.put(new JsonPojo());
    JSONObject object = new JSONObject();
    object.put("array", array);
    assertJsonEquals("{'array': [" + JSON_POJO_AS_JSON + "]}", JsonSerializer.serialize(object));
  }

  @Test
  public void serializeJsonObjectWithNullPropertyValue() throws Exception {
    String json = "{foo:null}";
    assertJsonEquals(json, JsonSerializer.serialize(new JSONObject(json)));
  }

  @Test
  public void serializePrimitives() throws Exception {
    assertEquals("null", JsonSerializer.serialize((Object) null));
    assertEquals("\"hello\"", JsonSerializer.serialize("hello"));
    assertEquals("100", JsonSerializer.serialize(100));
    assertEquals("125.0", JsonSerializer.serialize(125.0f));
    assertEquals("126.0", JsonSerializer.serialize(126.0));
    assertEquals("1", JsonSerializer.serialize(1L));
    assertEquals("\"RUNTIME\"", JsonSerializer.serialize(RetentionPolicy.RUNTIME));
    assertEquals("\"string buf\"",
        JsonSerializer.serialize(new StringBuilder().append("string").append(' ').append("buf")));
  }

  public static class JsonPojo {
    public String getString() {
      return "string-value";
    }

    @SuppressWarnings("unused")
    private String getPrivateString() {
      throw new UnsupportedOperationException();
    }

    public int getInteger() {
      return 100;
    }

    @JsonProperty("simple!")
    public int getSimpleName() {
      return 3;
    }

    public Object getNullValue() {
      return null;
    }
    @JsonProperty("simple!")
    public void setSimpleName(int foo) {

    }
    @JsonProperty("invalid-setter-two-args")
    public void setInvalidSetterTwoArgs(String foo, String bar) {
    }

    @JsonProperty("invalid-setter-no-args")
    public void setInvalidSetterNoArgs() {
    }

    @JsonProperty("invalid-getter-args")
    public String getInvalidGetterWithArgs(String foo) {
       return "invalid";
    }
  }

  @Test
  public void serializePojo() throws Exception {
    JsonPojo pojo = new JsonPojo();

    assertJsonEquals(JSON_POJO_AS_JSON,
        JsonSerializer.serialize(pojo));
  }

  @Test
  public void serializeMixedObjects() throws Exception {
    Map<String, ?> map = ImmutableMap.of(
        "int", Integer.valueOf(3),
        "double", Double.valueOf(2.7d),
        "bool", Boolean.TRUE,
        "map", ImmutableMap.of("hello", "world", "foo", "bar"),
        "string", "hello!");
    assertJsonEquals(
        "{int:3,double:2.7,bool:true,map:{hello:'world',foo:'bar'},string:'hello!'}",
        JsonSerializer.serialize(map));
  }

  @Test
  public void serializeMixedArray() throws Exception {
    Collection<Object> data = Arrays.asList(
        Integer.valueOf(3),
        Double.valueOf(2.7d),
        Boolean.TRUE,
        Arrays.asList("one", "two", "three"),
        new JSONArray(new String[] {"foo", "bar"}),
        "hello!");
    assertJsonEquals(
        "[3,2.7,true,['one','two','three'],['foo','bar'],'hello!']",
        JsonSerializer.serialize(data));
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

  @Test
  public void escapeBrackets() throws Exception {
    StringBuilder builder = new StringBuilder();
    JsonSerializer.appendString(builder, "Hello<world>foo < bar");

    assertEquals("\"Hello\\u003cworld\\u003efoo \\u003c bar\"", builder.toString());

    // Quick sanity check to make sure that this converts back cleanly.
    JSONObject obj = new JSONObject("{foo:" + builder + '}');
    assertEquals("Hello<world>foo < bar", obj.get("foo"));
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
      data.put("key-" + i, Strings.repeat("small value", 100));
    }
    return data;
  }

  public static Map<String, Object> perfComparison10LargeValuesAndEscapes() {
    Map<String, Object> data = Maps.newHashMap();
    for (int i = 0; i < 10; ++i) {
      data.put("key-" + i, Strings.repeat("\tsmall\r value \\foo\b\uFFFF\uBCAD\n\u0083", 100));
    }
    return data;
  }

  public static Map<String, Object> perfComparison100Arrays() {
    Map<String, Object> data = Maps.newHashMap();
    String[] array = {
      "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"
    };

    for (int i = 0; i < 100; ++i) {
      data.put("key-" + i, array);
    }

    return data;
  }

  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception {
    int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
    System.out.println("Running tests with " + iterations + " iterations.");

    for (Method method : JsonSerializerTest.class.getMethods()) {
      if (method.getName().startsWith("perfComparison")) {
        Map<String, Object> data = (Map<String, Object>)method.invoke(null);
        System.out.println("Running: " + method.getName());

        runJsonOrgTest(data, iterations);
        runSerializerTest(data, iterations);

        // if (!jsonEquals(jsonOrg, netSfJson)) {
        //   System.out.println("net.sf.json did not produce results matching the reference impl.");
        // }
        System.out.println("-----------------------");
      }
    }
    System.out.println("Done");
  }
}
