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
import static org.junit.Assert.assertNull;

import org.apache.shindig.common.JsonSerializerTest.JsonPojo;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class JsonUtilTest {
  @Test
  public void getPropertyOfJsonObject() throws Exception {
    JSONObject json = new JSONObject("{a: 1, b: '2'}");
    assertEquals(1, JsonUtil.getProperty(json, "a"));
    assertEquals("2", JsonUtil.getProperty(json, "b"));
    assertNull(JsonUtil.getProperty(json, "c"));
  }

  @Test
  public void getPropertyOfMap() throws Exception {
    Map<String, Object> map = ImmutableMap.of("a", (Object) 1, "b", "2");
        assertEquals(1, JsonUtil.getProperty(map, "a"));
    assertEquals("2", JsonUtil.getProperty(map, "b"));
    assertNull(JsonUtil.getProperty(map, "c"));
  }

  @Test
  public void getPropertyOfPojo() throws Exception {
    JsonPojo pojo = new JsonPojo();
    assertEquals("string-value", JsonUtil.getProperty(pojo, "string"));
    assertEquals(100, JsonUtil.getProperty(pojo, "integer"));
    assertEquals(3, JsonUtil.getProperty(pojo, "simple!"));
    assertNull(JsonUtil.getProperty(pojo, "not"));
  }

  @Test
  public void excludedPropertiesOfPojo() throws Exception {
    JsonPojo pojo = new JsonPojo();
    // These exist as getters on all objects, but not as properties
    assertNull(JsonUtil.getProperty(pojo, "class"));
    assertNull(JsonUtil.getProperty(pojo, "declaringClass"));
  }

  private class DuplicateBase<type> {
    public type getValue() {
      return null;
    }
  }

  private class Duplicate extends DuplicateBase<String> {
    public String getValue() {
      return "duplicate";
    }
  }

  @Test
  public void duplicateMethodPojo() throws Exception {
    Duplicate pojo = new Duplicate();
    assertEquals("duplicate", JsonUtil.getProperty(pojo, "value"));
  }
}
