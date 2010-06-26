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

import org.apache.shindig.social.opensocial.jpa.EmailDb;
import org.apache.shindig.social.opensocial.jpa.openjpa.Bootstrap;
import org.apache.shindig.social.opensocial.model.Person;


import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import java.util.Random;

/**
 * 
 */
public class SchemaOpenJPATestOff {

  private static EntityManager entityManager;

  // @BeforeClass
  public static void config() {
    Bootstrap b = new Bootstrap();
    // Bootstrap b = new
    // Bootstrap("com.mysql.jdbc.Driver",
    // "jdbc:mysql://localhost/sakaikernel?useUnicode=true&amp;characterEncoding=UTF-8",
    // "sakaikernel", "sakaikernel","1","1");
    entityManager = b.getEntityManager("openjpa");
  }

  // @AfterClass
  public static void stop() {
  }

  // @Test
  public void checkSimpleInsert() throws Exception {
    EntityTransaction transaction = entityManager.getTransaction();
    transaction.begin();
    EmailDb email = new EmailDb();
    email.setType("email");
    email.setValue("ieb@tfd.co.uk");
    entityManager.persist(email);
    transaction.commit();
  }

  // @Test
  public void checkPersonCreate() throws Exception {
    EntityTransaction transaction = entityManager.getTransaction();
    transaction.begin();
    PersonPopulate pp = new PersonPopulate(entityManager);
    int i = 1;
    long key = System.currentTimeMillis();
    Random r = new Random();
    Person p = pp.createPerson(i, key, r);
    entityManager.persist(p);
    transaction.commit();
  }

  // @Test
  public void fillDatbase() throws Exception {
    EntityTransaction transaction = entityManager.getTransaction();
    transaction.begin();
    PersonPopulate pp = new PersonPopulate(entityManager);
    long key = System.currentTimeMillis();
    Random r = new Random();
    for (int i = 0; i < 20; i++) {
      Person p = pp.createPerson(i, key, r);
      entityManager.persist(p);
      if (i % 10 == 0) {
        transaction.commit();
        transaction = entityManager.getTransaction();
        transaction.begin();
      }
    }
    transaction.commit();
  }

}
