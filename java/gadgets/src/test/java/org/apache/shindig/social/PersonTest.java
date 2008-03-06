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
package org.apache.shindig.social;

import junit.framework.TestCase;

public class PersonTest extends TestCase {
  private Person johnDoe;

  @Override
  public void setUp() throws Exception {
    johnDoe = new Person("johnDoeId", new Name("John Doe"));
    Phone[] phones = {
        new Phone("+33H000000000", "home"),
        new Phone("+33M000000000", "mobile"),
        new Phone("+33W000000000", "work")};
    johnDoe.setPhoneNumbers(phones);

    Address[] addresses = {
      new Address("My home address")
    };
    johnDoe.setAddresses(addresses);
  }

  public void testPersonToJson() throws Exception {
    assertEquals(johnDoe.toJson().toString(),
        "{\"id\":\"johnDoeId\"," +
         "\"name\":{\"unstructured\":\"John Doe\"}," +
         "\"phoneNumbers\":[" +
            "{\"number\":\"+33H000000000\",\"type\":\"home\"}," +
            "{\"number\":\"+33M000000000\",\"type\":\"mobile\"}," +
            "{\"number\":\"+33W000000000\",\"type\":\"work\"}]," +
         "\"addresses\":[{\"unstructuredAddress\":\"My home address\"}]}");
  }
}
