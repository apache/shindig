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

import org.apache.shindig.common.util.JsonConversionUtilTest;
import org.apache.shindig.protocol.model.TestModel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import junit.framework.TestCase;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

public class BeanJsonConverterTest extends TestCase {
  private TestModel.Car car;
  private BeanJsonConverter beanJsonConverter;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    car = new TestModel.Car();
    beanJsonConverter = new BeanJsonConverter(Guice.createInjector());
  }

  public void testToJsonOnInheritedClass() throws Exception {
    TestModel.ExpensiveCar roller = new TestModel.ExpensiveCar();
    JSONObject result = (JSONObject) beanJsonConverter.convertToJson(roller);
    assertEquals(roller.getCost(), result.getInt("cost"));
    assertEquals(roller.getParkingTickets().size(), result.getJSONObject("parkingTickets").length());
  }

  public void testCarToJson() throws Exception {
    JSONObject result = (JSONObject) beanJsonConverter.convertToJson(car);
    JsonConversionUtilTest.assertJsonEquals(new JSONObject(TestModel.Car.DEFAULT_JSON), result);
  }


  public void testJsonToCar() throws Exception {
    String carJson = "{engine:[{value:DIESEL},{value:TURBO}],parkingTickets:{SF:$137,NY:'$301'}," +
            "passengers:[{gender:female,name:'Mum'}, {gender:male,name:'Dad'}]}";

    TestModel.Car car = beanJsonConverter.convertToObject(carJson, TestModel.Car.class);
    ArrayList<TestModel.Engine> engineInfo = Lists.newArrayList(TestModel.Engine.DIESEL,
        TestModel.Engine.TURBO);
    for (int i = 0; i < car.getEngine().size(); i++) {
      assertEquals(car.getEngine().get(i).getValue(), engineInfo.get(i));
    }

    assertEquals(car.getParkingTickets(), ImmutableMap.of("SF", "$137", "NY", "$301"));
    TestModel.Passenger mum = car.getPassengers().get(0);
    assertEquals(mum.getGender(), TestModel.Gender.female);
    assertEquals(mum.getName(), "Mum");
    TestModel.Passenger dad = car.getPassengers().get(1);
    assertEquals(dad.getGender(), TestModel.Gender.male);
    assertEquals(dad.getName(), "Dad");
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

    JSONObject jsonMap = (JSONObject) beanJsonConverter.convertToJson(map);

    assertEquals("1", jsonMap.getJSONObject("item1").getString("value"));
    assertEquals("2", jsonMap.getJSONObject("item2").getString("value"));
  }

  @SuppressWarnings("unchecked")
  public void testListsToJson() throws Exception {
    Map<String, String> item1Map = Maps.newHashMap();
    item1Map.put("value", "1");

    Map<String, String> item2Map = Maps.newHashMap();
    item2Map.put("value", "2");

    JSONArray jsonArray = (JSONArray) beanJsonConverter.convertToJson(
        Lists.newArrayList(item1Map, item2Map));

    assertEquals("1", ((JSONObject) jsonArray.get(0)).getString("value"));
    assertEquals("2", ((JSONObject) jsonArray.get(1)).getString("value"));
  }

  public void testArrayToJson() throws Exception {
    String[] colors = {"blue", "green", "aquamarine"};
    JSONArray jsonArray = (JSONArray) beanJsonConverter.convertToJson(colors);

    assertEquals(colors.length, jsonArray.length());
    assertEquals(colors[0], jsonArray.get(0));
  }

  @SuppressWarnings("unchecked")
  public void testJsonToMap() throws Exception {
    String jsonActivity = "{count : 0, favoriteColor : 'yellow'}";
    Map<String, String> data = Maps.newHashMap();
    data = beanJsonConverter.convertToObject(jsonActivity,
        (Class<Map<String, String>>) data.getClass());

    assertEquals(2, data.size());

    for (Entry<String, String> entry : data.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (key.equals("count")) {
        assertEquals("0", value);
      } else if (key.equals("favoriteColor")) {
        assertEquals("yellow", value);
      }
    }
  }
}
