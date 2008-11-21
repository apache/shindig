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
package org.apache.shindig.social.opensocial.jpa.eclipselink;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import javax.persistence.EntityManager;

/**
 * Creates an Eclipse Entity Manager.
 */
public class EclipseEntityManagerProvider implements Provider<EntityManager> {

  private Bootstrap bootstrap;
  private String unitName;

  /**
   * 
   */
  @Inject
  public EclipseEntityManagerProvider(Bootstrap bootstrap,
      @Named("jpa.socialapi.unitname") String unitName) {
    this.unitName = unitName;
    this.bootstrap = bootstrap;
  }

  /**
   * {@inheritDoc}
   * 
   * @see com.google.inject.Provider#get()
   */
  public EntityManager get() {
    return bootstrap.getEntityManager(unitName);
  }

}
