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

import java.util.HashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * A factory for creating TestEntityManager objects.
 */
public class SpiEntityManagerFactory {

  /** The entity manager. */
  private EntityManager entityManager;
  
  /** The Constant INSTANCE. */
  public final static SpiEntityManagerFactory INSTANCE = new SpiEntityManagerFactory();
  
  /** The Constant DEFAULT_UNIT_NAME. */
  private static final String DEFAULT_UNIT_NAME = "hibernate_spi_testing";
  
  /**
   * Instantiates a new test entity manager factory.
   */
  private SpiEntityManagerFactory() {
    EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory(DEFAULT_UNIT_NAME, new HashMap<String, String>());
    this.entityManager = entityManagerFactory.createEntityManager();
  }
  
  /**
   * Gets the entity manager.
   * 
   * @return the entity manager
   */
  public static EntityManager getEntityManager() {
    return INSTANCE.entityManager;
  }
}
