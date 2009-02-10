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
package org.apache.shindig.protocol.conversion;


import org.apache.shindig.protocol.conversion.jsonlib.ApiValidator;
import org.apache.shindig.protocol.conversion.jsonlib.JsonLibTestsGuiceModule;
import org.apache.shindig.protocol.model.TestModel;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import junit.framework.TestCase;
import net.sf.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class BeanJsonLibConverterTest extends TestCase {
  private TestModel.Car car;
  private BeanJsonLibConverter beanJsonConverter;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    car = new TestModel.Car();
    Injector injector = Guice.createInjector(new JsonLibTestsGuiceModule());
    beanJsonConverter = injector.getInstance(BeanJsonLibConverter.class);
  }

  public void testToJsonOnInheritedClass() throws Exception {
    TestModel.ExpensiveCar roller = new TestModel.ExpensiveCar();
    JSONObject result = beanJsonConverter.convertToJson(roller);
    assertEquals(roller.getCost(), result.getInt("cost"));
    assertEquals(roller.getParkingTickets().size(), result.getJSONObject("parkingTickets").size());
  }

  public void testCarToJson() throws Exception {
    JSONObject object = beanJsonConverter.convertToJson(car);
    assertEquals(object, JSONObject.fromObject(TestModel.Car.DEFAULT_JSON));
  }

  public void testMapsToJson() throws Exception {
    Map<String, Map<String, String>> map = Maps.newHashMap();

    Map<String, String> item1Map = Maps.newHashMap();
    item1Map.put("value", "1");

    // Null values shouldn't cause exceptions
    item1Map.put("value2", null);
    map.put("item1", item1Map);

    Map<String, String> item2Map = Maps.newHashMap();
    item2Map.put("value", "2");
    map.put("item2", item2Map);

    String result = beanJsonConverter.convertToString(map);
    // there is introspection that can tell jsonobject -> bean converter what a
    // map should contain, so we have to tell it
    beanJsonConverter.addMapping("item1", Map.class);
    beanJsonConverter.addMapping("item2", Map.class);
    Map<?, ?> parsedMap = beanJsonConverter.convertToObject(result, Map.class);

    ApiValidator.dump(parsedMap);

    assertEquals("1", ((Map<?, ?>) parsedMap.get("item1")).get("value"));
    assertEquals("2", ((Map<?, ?>) parsedMap.get("item2")).get("value"));
  }

  public void testListsToJson() throws Exception {
    Map<String, String> item1Map = Maps.newHashMap();
    item1Map.put("value", "1");

    Map<String, String> item2Map = Maps.newHashMap();
    item2Map.put("value", "2");

    // put the list into a container before serializing, top level lists dont
    // appear
    // to be allowed in json
    // just check that the list is in the holder correctly
    List<Map<String, String>> list = Lists.newArrayList();
    list.add(item1Map);
    list.add(item2Map);
    String result = beanJsonConverter.convertToString(list);
    Map<?, ?>[] parsedList = beanJsonConverter.convertToObject(result, Map[].class);

    assertEquals("1", parsedList[0].get("value"));
    assertEquals("2", parsedList[1].get("value"));
  }

  public void testArrayToJson() throws Exception {
    String[] colors = { "blue", "green", "aquamarine" };
    String result = beanJsonConverter.convertToString(colors);
    String[] parsedColors = beanJsonConverter.convertToObject(result, String[].class);
    assertEquals(colors.length, parsedColors.length);
    assertEquals(colors[0], parsedColors[0]);
    assertEquals(colors[1], parsedColors[1]);
    assertEquals(colors[2], parsedColors[2]);
  }

  @SuppressWarnings("unchecked")
  public void testJsonToMap() throws Exception {
    String jsonActivity = "{count : 0, favoriteColor : 'yellow'}";
    Map<String, Object> data = Maps.newHashMap();
    data = beanJsonConverter.convertToObject(jsonActivity, (Class<Map<String, Object>>) data
        .getClass());

    assertEquals(2, data.size());

    for (Entry<String, Object> entry : data.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (key.equals("count")) {
        assertEquals(0, value);
      } else if (key.equals("favoriteColor")) {
        assertEquals("yellow", value);
      }
    }
  }

  public void testEmptyObject() {
    assertEquals("{}", beanJsonConverter.convertToString(""));
    assertEquals(0, beanJsonConverter.convertToObject("", Map.class).size());
    assertEquals(0, beanJsonConverter.convertToObject("[]", String[].class).length);
    assertEquals(2, beanJsonConverter.convertToObject("[\"a\",\"b\"]", String[].class).length);
  }
}
