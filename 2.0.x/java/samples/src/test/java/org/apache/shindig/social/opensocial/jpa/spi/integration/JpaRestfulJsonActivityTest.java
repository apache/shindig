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

import org.apache.shindig.social.dataservice.integration.RestfulJsonActivityTest;
import org.apache.shindig.social.opensocial.jpa.spi.SpiDatabaseBootstrap;

import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;

/**
 * JPA restful Json activity test, which wraps around shindig's <code>RestfulJsonActivityTest</code>
 * but uses the JPA ActivityService implementation <code>ActivityServiceDb</code>.
 */
public class JpaRestfulJsonActivityTest extends RestfulJsonActivityTest {
    
  /** The bootstrap. */
  private SpiDatabaseBootstrap bootstrap;
  
  /**
   * Calls super.setup so to initialise servlet and easy mock objects.
   * Note that super.setup (i.e. AbstractLargeRestfulTests) also injects SocialApiTestsGuiceModule,
   * which will be overriden here to use the JPA guice bindings
   * 
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {

    // Init config
    Injector injector = JpaRestfulTestConfigHelper.init();
    this.setServlet(JpaRestfulTestConfigHelper.getDataServiceServlet(injector));
    
    // Bootstrap hibernate and associated test db, and setup db with test data
    this.bootstrap = injector.getInstance(SpiDatabaseBootstrap.class);
    this.bootstrap.init();
  }
  
  @After
  public void tearDown() throws Exception {
    this.bootstrap.tearDown();
  }

}
