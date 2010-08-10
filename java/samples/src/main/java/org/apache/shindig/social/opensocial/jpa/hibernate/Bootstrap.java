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

package org.apache.shindig.social.opensocial.jpa.hibernate;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 */
public class Bootstrap {

  private static final String DB_DRIVER = "db.driver";
  private static final String DB_URL = "db.url";
  private static final String DB_USER = "db.user";
  private static final String DB_PASSWORD = "db.password";
  private static final String DB_MIN_WRITE = "db.write.min";
  private static final String DB_MIN_NUM_READ = "db.read.min";
  private static final Logger LOG = Logger.getLogger(Boolean.class.getName());
  private EntityManager entityManager;

  @Inject
  public Bootstrap(@Named(DB_DRIVER) String dbDriver,
      @Named(DB_URL) String dbUrl, @Named(DB_USER) String dbUser,
      @Named(DB_PASSWORD) String dbPassword,
      @Named(DB_MIN_NUM_READ) String minRead,
      @Named(DB_MIN_WRITE) String minWrite) {

  }
 
  public Bootstrap() {
   
  }

  public void init(String unitName) {

    Map<String, String> properties = Maps.newHashMap();

    LOG.info("Starting connection manager with properties " + properties);

    EntityManagerFactory emFactory = Persistence.createEntityManagerFactory(
        unitName, properties);
    entityManager = emFactory.createEntityManager();
  }

  /**
   * @param unitName
   * @return
   */
  public EntityManager getEntityManager(String unitName) {
    if (entityManager == null) {
      init(unitName);
    }
    return entityManager;
  }
}
