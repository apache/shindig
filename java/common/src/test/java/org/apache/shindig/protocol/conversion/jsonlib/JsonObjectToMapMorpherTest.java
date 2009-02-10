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
package org.apache.shindig.protocol.conversion.jsonlib;

import net.sf.ezmorph.Morpher;
import net.sf.ezmorph.ObjectMorpher;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * Tests the JsonObjectMapMorpher
 */
public class JsonObjectToMapMorpherTest {

  /**
   * Test method for
   * {@link org.apache.shindig.protocol.conversion.jsonlib.JsonObjectToMapMorpher#morphsTo()}.
   */
  @Test
  public void testMorphsTo() {
    Morpher m = new JsonObjectToMapMorpher();
    assertSame(Map.class, m.morphsTo());
  }

  /**
   * Test method for
   * {@link org.apache.shindig.protocol.conversion.jsonlib.JsonObjectToMapMorpher#supports(java.lang.Class)}.
   */
  @Test
  public void testSupports() {
    Morpher m = new JsonObjectToMapMorpher();
    assertTrue(m.supports(JSONObject.class));
    assertFalse(m.supports(JSON.class));
    assertFalse(m.supports(List.class));
  }

  /**
   * Test method for
   * {@link org.apache.shindig.protocol.conversion.jsonlib.JsonObjectToMapMorpher#morph(java.lang.Object)}.
   */
  @Test
  public void testMorph() {
    ObjectMorpher om = new JsonObjectToMapMorpher();
    JSONObject testObj = new JSONObject();
    testObj.put("x", "y");
    testObj.put("1", "z");
    Object o = om.morph(testObj);
    assertNotSame(testObj, o);
    if (o instanceof Map) {
      Map<?, ?> fm = (Map<?, ?>) o;
      assertEquals("y", fm.get("x"));
      assertEquals("z", fm.get("1"));
      assertNull(fm.get("xyz"));
    }
    try {
      om.morph(o);
      fail();
    } catch (ClassCastException cce) {
      assertTrue(true);
    }
  }

}
