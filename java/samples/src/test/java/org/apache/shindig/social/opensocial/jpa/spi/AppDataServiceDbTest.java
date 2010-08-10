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

import org.apache.shindig.protocol.DataCollection;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.apache.shindig.social.opensocial.spi.UserId.Type;

import java.util.Map;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.MapMaker;

/**
 * 
 * Test the AppDataServiceDb implementation.
 *
 */
public class AppDataServiceDbTest {
  
  private static final String DEFAULT_APPLICATION_ID = "app";

  private AppDataServiceDb appDataServiceDb;
  
  /** The bootstrap. */
  private SpiDatabaseBootstrap bootstrap;
  
  @Before
  public void setup() throws Exception {
    EntityManager entityManager = SpiEntityManagerFactory.getEntityManager();
    this.appDataServiceDb = new AppDataServiceDb(entityManager);
    
    // Bootstrap hibernate and associated test db, and setup db with test data
    this.bootstrap = new SpiDatabaseBootstrap(entityManager);
    this.bootstrap.init();
  }
  
  @After
  public void tearDown() throws Exception {
    bootstrap.tearDown();
  }
  
  @Test
  public void getJohnDoeApplicationData() throws Exception {
    Future<DataCollection> results = this.appDataServiceDb.getPersonData(SpiTestUtil.buildUserIds("john.doe"), new GroupId(GroupId.Type.self, "@self"), DEFAULT_APPLICATION_ID, null, SpiTestUtil.DEFAULT_TEST_SECURITY_TOKEN);
    DataCollection data = results.get();
    assertEquals(1, data.getEntry().size());
    assertEquals("0", data.getEntry().get("john.doe").get("count"));
  }
  
  @Test
  public void getJohnDoeApplicationDataWithCountField() throws Exception {
    Future<DataCollection> results = this.appDataServiceDb.getPersonData(SpiTestUtil.buildUserIds("john.doe"), new GroupId(GroupId.Type.self, "@self"), DEFAULT_APPLICATION_ID, SpiTestUtil.asSet("count"), SpiTestUtil.DEFAULT_TEST_SECURITY_TOKEN);
    DataCollection data = results.get();
    assertEquals(1, data.getEntry().size());
    assertEquals("0", data.getEntry().get("john.doe").get("count"));
  }
  
  @Test
  public void getJohnDoeApplicationDataWithInvalidField() throws Exception {
    Future<DataCollection> results = this.appDataServiceDb.getPersonData(SpiTestUtil.buildUserIds("john.doe"), new GroupId(GroupId.Type.self, "@self"), DEFAULT_APPLICATION_ID, SpiTestUtil.asSet("peabody"), SpiTestUtil.DEFAULT_TEST_SECURITY_TOKEN);
    DataCollection data = results.get();
    assertEquals(1, data.getEntry().size());
    assertEquals(null, data.getEntry().get("john.doe").get("count"));
  }
  
  @Test
  public void getJohnDoeFriendsApplicationDataWithCountField() throws Exception {
    Future<DataCollection> results = this.appDataServiceDb.getPersonData(SpiTestUtil.buildUserIds("john.doe"), new GroupId(GroupId.Type.friends, "@friends"), DEFAULT_APPLICATION_ID, SpiTestUtil.asSet("count"), SpiTestUtil.DEFAULT_TEST_SECURITY_TOKEN);
    DataCollection data = results.get();
    assertEquals(3, data.getEntry().size());
    assertEquals("7", data.getEntry().get("jane.doe").get("count"));
    assertEquals("2", data.getEntry().get("george.doe").get("count"));    
  }
  
  @Test
  public void updateJohnDoeApplicationDataSettingCountTo5() throws Exception {
    // Do update
    Map<String, String> values = new MapMaker().makeMap();
    values.put("count", "5");
    this.appDataServiceDb.updatePersonData(new UserId(Type.userId, "john.doe"), new GroupId(GroupId.Type.self, "@self"), DEFAULT_APPLICATION_ID, SpiTestUtil.asSet("count"), values, SpiTestUtil.DEFAULT_TEST_SECURITY_TOKEN);
    
    // Verify that update succeeded
    Future<DataCollection> results = this.appDataServiceDb.getPersonData(SpiTestUtil.buildUserIds("john.doe"), new GroupId(GroupId.Type.self, "@self"), DEFAULT_APPLICATION_ID, null, SpiTestUtil.DEFAULT_TEST_SECURITY_TOKEN);
    DataCollection data = results.get();
    assertEquals(1, data.getEntry().size());
    assertEquals("5", data.getEntry().get("john.doe").get("count"));
  }
  
  @Test
  public void deleteJohnDoeApplicationDataWithCountField() throws Exception {
    // Do delete
    this.appDataServiceDb.deletePersonData(new UserId(Type.userId, "john.doe"), new GroupId(GroupId.Type.self, "@self"), DEFAULT_APPLICATION_ID, SpiTestUtil.asSet("count"), SpiTestUtil.DEFAULT_TEST_SECURITY_TOKEN);
    // Verify that delete succeeded
    Future<DataCollection> results = this.appDataServiceDb.getPersonData(SpiTestUtil.buildUserIds("john.doe"), new GroupId(GroupId.Type.self, "@self"), DEFAULT_APPLICATION_ID, SpiTestUtil.asSet("count"), SpiTestUtil.DEFAULT_TEST_SECURITY_TOKEN);
    DataCollection data = results.get();
    assertEquals(1, data.getEntry().size());
    assertEquals(null, data.getEntry().get("john.doe").get("count"));
  }
  
  @Test
  public void deleteJohnDoeApplicationDataWithInvalidField() throws Exception {
    // Do delete with invalid field
    this.appDataServiceDb.deletePersonData(new UserId(Type.userId, "john.doe"), new GroupId(GroupId.Type.self, "@self"), DEFAULT_APPLICATION_ID, SpiTestUtil.asSet("peabody"), SpiTestUtil.DEFAULT_TEST_SECURITY_TOKEN);
    // Verify that delete did not occur
    Future<DataCollection> results = this.appDataServiceDb.getPersonData(SpiTestUtil.buildUserIds("john.doe"), new GroupId(GroupId.Type.self, "@self"), DEFAULT_APPLICATION_ID, SpiTestUtil.asSet("count"), SpiTestUtil.DEFAULT_TEST_SECURITY_TOKEN);
    DataCollection data = results.get();
    assertEquals(1, data.getEntry().size());
    assertEquals("0", data.getEntry().get("john.doe").get("count"));
  }
    
}
