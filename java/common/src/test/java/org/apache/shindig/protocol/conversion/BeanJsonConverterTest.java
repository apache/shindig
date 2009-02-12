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

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

public class BeanJsonConverterTest extends TestCase {
  private BeanJsonConverter beanJsonConverter;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    beanJsonConverter = new BeanJsonConverter(Guice.createInjector());
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
