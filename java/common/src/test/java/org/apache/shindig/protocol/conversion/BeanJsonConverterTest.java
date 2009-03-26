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

import org.apache.shindig.protocol.model.TestModel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.TypeLiteral;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class BeanJsonConverterTest extends TestCase {
  private BeanJsonConverter beanJsonConverter;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    beanJsonConverter = new BeanJsonConverter(Guice.createInjector());
  }

  public static class TestObject {
    static String staticValue;
    String hello;
    int count;
    List<TestObject> children;
    TestEnum testEnum;

    public static void setSomeStatic(String staticValue) {
      TestObject.staticValue = staticValue;
    }

    public void setHello(String hello) {
      this.hello = hello;
    }

    public void setCount(int count) {
      this.count = count;
    }

    public void setChildren(List<TestObject> children) {
      this.children = children;
    }

    public enum TestEnum {
      foo, bar, baz;
    }

    public void setTestEnum(TestEnum testEnum) {
      this.testEnum = testEnum;
    }
  }

  public void testJsonToObject() throws Exception {
    String json = "{" +
        "hello:'world'," +
        "count:10," +
        "someStatic:'foo'," +
        "testEnum:'bar'," +
        "children:[{hello:'world-2',count:11},{hello:'world-3',count:12}]}";

    TestObject object = beanJsonConverter.convertToObject(json, TestObject.class);

    assertEquals("world", object.hello);
    assertEquals(10, object.count);
    assertEquals("world-2", object.children.get(0).hello);
    assertEquals(11, object.children.get(0).count);
    assertEquals("world-3", object.children.get(1).hello);
    assertEquals(12, object.children.get(1).count);
    assertNull("Should not set static values", TestObject.staticValue);
    assertEquals(TestObject.TestEnum.bar, object.testEnum);
  }

  public void testJsonToPrimitives() throws Exception {
    String simpleJson = "{hello:'world',count:10}";

    Object object = beanJsonConverter.convertToObject(simpleJson, null);

    Map<?, ?> map = (Map<?, ?>) object;

    assertEquals("world", map.get("hello"));
    assertEquals(10, map.get("count"));
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

  public void testJsonToMap() throws Exception {
    String jsonActivity = "{count : 0, favoriteColor : 'yellow'}";
    Map<String, Object> data = Maps.newHashMap();
    data = beanJsonConverter.convertToObject(jsonActivity,
        new TypeLiteral<Map<String, Object>>(){}.getType());

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

  public void testJsonToMapWithConversion() throws Exception {
    String jsonActivity = "{count : 0, favoriteColor : 'yellow'}";
    Map<String, String> data = Maps.newHashMap();
    data = beanJsonConverter.convertToObject(jsonActivity,
        new TypeLiteral<Map<String, String>>(){}.getType());

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

  public void testJsonToNestedGeneric() throws Exception {
    String jsonActivity = "{key0:[0,1,2],key1:[3,4,5]}";
    Map<String, List<Integer>> data = Maps.newHashMap();
    data = beanJsonConverter.convertToObject(jsonActivity,
        new TypeLiteral<Map<String, List<Integer>>>(){}.getType());

    assertEquals(2, data.size());

    assertEquals(Arrays.asList(0, 1, 2), data.get("key0"));
    assertEquals(Arrays.asList(3, 4, 5), data.get("key1"));
  }

  public void testEmptyJsonMap() throws Exception {
    String emptyMap = "{}";
    Map<String, String> data = beanJsonConverter.convertToObject(emptyMap, 
         new TypeLiteral<Map<String,String>>(){}.getType());
    assertTrue(data.size() == 0);
  }
}
