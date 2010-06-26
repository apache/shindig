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

package org.apache.shindig.social.opensocial.jpa.spi;

import static org.junit.Assert.assertEquals;

import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.protocol.model.SortOrder;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.apache.shindig.social.opensocial.spi.UserId.Type;

import java.util.List;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * Test the PersonServiceDb implementation.
 *
 */
public class PersonServiceDbTest {

  private final Person canonical = SpiTestUtil.buildCanonicalPerson();
  
  private PersonServiceDb personServiceDb;
  
  /** The bootstrap. */
  private SpiDatabaseBootstrap bootstrap;
  
  @Before
  public void setup() throws Exception {
    EntityManager entityManager = SpiEntityManagerFactory.getEntityManager();
    this.personServiceDb = new PersonServiceDb(entityManager);
    
    // Bootstrap hibernate and associated test db, and setup db with test data
    this.bootstrap = new SpiDatabaseBootstrap(entityManager);
    this.bootstrap.init();
  }
  
  @After
  public void tearDown() throws Exception {
    bootstrap.tearDown();
  }
  
  @Test
  public void getCanonicalPerson() throws Exception {
     Future<Person> person = this.personServiceDb.getPerson(new UserId(Type.userId, "canonical"), Person.Field.ALL_FIELDS, SpiTestUtil.DEFAULT_TEST_SECURITY_TOKEN);
     SpiTestUtil.assertPersonEquals(person.get(), canonical);
  }
  
  @Test
  public void getJohnDoeFriendsOrderedByName() throws Exception {
    // Set collection options
    CollectionOptions collectionOptions = new CollectionOptions();
    collectionOptions.setSortBy("name");
    collectionOptions.setSortOrder(SortOrder.ascending);
    collectionOptions.setMax(20);
    
    // Get all friends of john.doe
    Future<RestfulCollection<Person>> result = this.personServiceDb.getPeople(SpiTestUtil.buildUserIds("john.doe"), new GroupId(GroupId.Type.friends, "@friends"), collectionOptions, Person.Field.ALL_FIELDS, SpiTestUtil.DEFAULT_TEST_SECURITY_TOKEN);
    
    RestfulCollection<Person> peopleCollection = result.get();
    assertEquals(3, peopleCollection.getTotalResults());
    assertEquals(0, peopleCollection.getStartIndex());    
    List<Person> people = peopleCollection.getEntry();    
    // The users should be in alphabetical order
    SpiTestUtil.assertPersonEquals(people.get(0), "george.doe", "George Doe");
    SpiTestUtil.assertPersonEquals(people.get(1), "jane.doe", "Jane Doe");     
  }
  
  
  @Test
  public void getJohnDoeFriendsOrderedByNameWithPagination() throws Exception {    
    // Set collection options
    CollectionOptions collectionOptions = new CollectionOptions();
    collectionOptions.setSortBy("name");
    collectionOptions.setSortOrder(SortOrder.ascending);
    collectionOptions.setFirst(0);
    collectionOptions.setMax(1);
    
    // Get first friend of john.doe
    Future<RestfulCollection<Person>> result = this.personServiceDb.getPeople(SpiTestUtil.buildUserIds("john.doe"), new GroupId(GroupId.Type.friends, "@friends"), collectionOptions, Person.Field.ALL_FIELDS, SpiTestUtil.DEFAULT_TEST_SECURITY_TOKEN);    
    RestfulCollection<Person> peopleCollection = result.get();
    assertEquals(3, peopleCollection.getTotalResults());
    assertEquals(0, peopleCollection.getStartIndex());    
    List<Person> people = peopleCollection.getEntry();    
    SpiTestUtil.assertPersonEquals(people.get(0), "george.doe", "George Doe");
    
    // Get second friend of john.doe
    collectionOptions.setFirst(1);
    result = this.personServiceDb.getPeople(SpiTestUtil.buildUserIds("john.doe"), new GroupId(GroupId.Type.friends, "@friends"), collectionOptions, Person.Field.ALL_FIELDS, SpiTestUtil.DEFAULT_TEST_SECURITY_TOKEN);
    peopleCollection = result.get();
    assertEquals(3, peopleCollection.getTotalResults());
    assertEquals(1, peopleCollection.getStartIndex());    
    people = peopleCollection.getEntry();    
    SpiTestUtil.assertPersonEquals(people.get(0), "jane.doe", "Jane Doe");    
  }
  
  
}
