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
package org.apache.shindig.protocol.model;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Limited model to fully exercise data binding
 */
public class Model {

  public static class Car {
    public static final String DEFAULT_JSON =
        "{\"engine\":[{\"value\":\"GAS\"},{\"value\":\"HYBRID\"}]," +
            "\"parkingTickets\":{\"TOKYO\":\"250Y\",\"BERKELEY\":\"$120\"}," +
            "\"passengers\":[{\"gender\":\"male\",\"name\":\"Dick Dastardly\"},{\"gender\":\"female\",\"name\":\"Speed Racer\"}]}";

    public static final String DEFAULT_XML =
        "<response>" +
            "<engine>" +
              "<EnumImpl><value><declaringClass>org.apache.shindig.protocol.model.Model$Engine</declaringClass><displayValue>Gas</displayValue></value></EnumImpl>" +
              "<EnumImpl><value><declaringClass>org.apache.shindig.protocol.model.Model$Engine</declaringClass><displayValue>Hybrid</displayValue></value></EnumImpl>" +
            "</engine>" +
            "<parkingTickets>" +
              "<entry><key>TOKYO</key><value>250Y</value></entry>" +
              "<entry><key>BERKELEY</key><value>$120</value></entry>" +
            "</parkingTickets>" +
            "<passengers>" +
              "<ModelPassenger>" +
                "<gender><declaringClass>org.apache.shindig.protocol.model.Model$Gender</declaringClass></gender>" +
                "<name>Dick Dastardly</name>" +
              "</ModelPassenger>" +
              "<ModelPassenger>" +
                "<gender><declaringClass>org.apache.shindig.protocol.model.Model$Gender</declaringClass></gender>" +
                "<name>Speed Racer</name>" +
              "</ModelPassenger>" +
            "</passengers></response>";

    private List<Enum<Engine>> engine;
    private Map<String, String> parkingTickets;
    private List<Passenger> passengers;

    public Car() {
      List<Enum<Engine>> engines = Lists.newArrayList();
      engines.add(new EnumImpl<Engine>(Engine.GAS, null));
      engines.add(new EnumImpl<Engine>(Engine.HYBRID, null));
      engine = engines;
      parkingTickets = Maps.newHashMap();
      parkingTickets.put("BERKELEY", "$120");
      parkingTickets.put("TOKYO", "250Y");
      passengers = Lists.newArrayList();
      passengers.add(new Passenger("Dick Dastardly", Gender.male));
      passengers.add(new Passenger("Speed Racer", Gender.female));
    }

    public Car(List<Enum<Engine>> engine, Map<String, String> parkingTickets,
               List<Passenger> passengers) {
      this.engine = engine;
      this.parkingTickets = parkingTickets;
      this.passengers = passengers;
    }

    public List<Enum<Engine>> getEngine() {
      return engine;
    }

    public void setEngine(List<Enum<Engine>> engine) {
      this.engine = engine;
    }

    public Map<String, String> getParkingTickets() {
      return parkingTickets;
    }

    public void setParkingTickets(Map<String, String> parkingTickets) {
      this.parkingTickets = parkingTickets;
    }

    public List<Passenger> getPassengers() {
      return passengers;
    }

    public void setPassengers(List<Passenger> passengers) {
      this.passengers = passengers;
    }
  }

  public static class ExpensiveCar extends Car {
    private int cost = 100000;

    public int getCost() {
      return cost;
    }

    public void setCost(int cost) {
      this.cost = cost;
    }
  }

  public static class Passenger {
    private String name;
    private Gender gender;

    public Passenger() {
      name = "Speed Racer";
      gender = Gender.female;
    }

    public Passenger(String name, Gender gender) {
      this.name = name;
      this.gender = gender;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Gender getGender() {
      return gender;
    }

    public void setGender(Gender gender) {
      this.gender = gender;
    }
  }

  public enum Engine implements org.apache.shindig.protocol.model.Enum.EnumKey {
    DIESEL("DIESEL", "Diesel"),
    GAS("GAS", "Gas"),
    HYBRID("HYBRID", "Hybrid"),
    TURBO("TURBO", "Turbo");

    private final String jsonString;
    private final String displayValue;

    private Engine(String jsonString, String displayValue) {
      this.jsonString = jsonString;
      this.displayValue = displayValue;
    }

    public String toString() {
      return this.jsonString;
    }

    public String getDisplayValue() {
      return displayValue;
    }
  }

  public enum Gender {
    male,
    female
  }
}
