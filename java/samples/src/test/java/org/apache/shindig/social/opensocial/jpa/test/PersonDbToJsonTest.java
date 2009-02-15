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
package org.apache.shindig.social.opensocial.jpa.test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.social.opensocial.jpa.hibernate.Bootstrap;
import org.apache.shindig.social.opensocial.jpa.spi.JPASocialModule;
import org.apache.shindig.social.opensocial.model.Person;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import javax.persistence.EntityManager;

public class PersonDbToJsonTest {

  private BeanJsonConverter jsonConverter;
  private EntityManager entityManager;

  @Before
  public void setup() {
    Injector injector = Guice.createInjector(new JPASocialModule());
    this.jsonConverter = injector.getInstance(BeanJsonConverter.class);

    Bootstrap b = new Bootstrap();
    this.entityManager = b.getEntityManager("hibernate");
  }

  @Test
  public void convertPersonToJson() {
    Person person = new PersonPopulate(entityManager).createPerson(1, System
        .currentTimeMillis(), new Random());
    jsonConverter.convertToString(person);
  }
}
