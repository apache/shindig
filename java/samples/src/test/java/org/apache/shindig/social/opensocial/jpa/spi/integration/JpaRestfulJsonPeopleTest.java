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

package org.apache.shindig.social.opensocial.jpa.spi.integration;

import org.apache.shindig.social.dataservice.integration.RestfulJsonPeopleTest;
import org.apache.shindig.social.opensocial.jpa.spi.SpiDatabaseBootstrap;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.inject.Injector;

/**
 * JPA restful Json people test, which wraps around shindig's <code>RestfulJsonPeopleTest</code>
 * but uses the JPA PersonService implementation <code>PersonServiceDb</code>
 */
public class JpaRestfulJsonPeopleTest extends RestfulJsonPeopleTest {
  
  /** The bootstrap. */
  private SpiDatabaseBootstrap bootstrap;
  
  /**
   * Calls super.setup so to initialise servlet and easy mock objects.
   * Note that super.setup (i.e. AbstractLargeRestfulTests) also injects SocialApiTestsGuiceModule,
   * which will be overriden here to use the JPA guice bindings
   * 
   * @throws Exception the exception
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    // Init config
    Injector injector = JpaRestfulTestConfigHelper.init();
    this.setServlet(JpaRestfulTestConfigHelper.getDataServiceServlet(injector));
    
    // Bootstrap hibernate and associated test db, and setup db with test data
    this.bootstrap = injector.getInstance(SpiDatabaseBootstrap.class);
    this.bootstrap.init();
  }
  
  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    this.bootstrap.tearDown();
  }
	
  /**
   * TODO Overriding this test because of an existing jira issue
   * (https://issues.apache.org/jira/browse/SHINDIG-778)
   * where totalResults should be the same as specified in the count parameter.
   * Remove once issue 778 has been fixed.
   */
	@Test
  public void testGetPeoplePagination() throws Exception {
    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("sortBy", "name");
    extraParams.put("sortOrder", null);
    extraParams.put("filterBy", null);
    extraParams.put("startIndex", "0");
    extraParams.put("count", "1");
    extraParams.put("fields", null);

    String resp = getResponse("/people/john.doe/@friends", "GET", extraParams, null, "application/json");
    JSONObject result = getJson(resp);

    assertEquals(1, result.getInt("totalResults"));
    assertEquals(0, result.getInt("startIndex"));

    JSONArray people = result.getJSONArray("entry");
    assertPerson(people.getJSONObject(0), "george.doe", "George Doe");

    // Get the second page
    extraParams.put("startIndex", "1");
    resp = getResponse("/people/john.doe/@friends", "GET", extraParams, null, "application/json");
    result = getJson(resp);

    assertEquals(1, result.getInt("totalResults"));
    assertEquals(1, result.getInt("startIndex"));

    people = result.getJSONArray("entry");
    assertPerson(people.getJSONObject(0), "jane.doe", "Jane Doe");
  }

	/**
	 * Convenience assert method
	 * 
	 * @param person
	 * @param expectedId
	 * @param expectedName
	 * @throws Exception
	 */
  private void assertPerson(JSONObject person, String expectedId, String expectedName)
      throws Exception {
    assertEquals(expectedId, person.getString("id"));
    assertEquals(expectedName, person.getJSONObject("name").getString("unstructured"));
  }
  
}
