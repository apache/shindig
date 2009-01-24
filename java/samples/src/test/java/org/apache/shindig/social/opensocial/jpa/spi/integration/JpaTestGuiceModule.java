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

import org.apache.shindig.social.opensocial.jpa.AccountDb;
import org.apache.shindig.social.opensocial.jpa.ActivityDb;
import org.apache.shindig.social.opensocial.jpa.AddressDb;
import org.apache.shindig.social.opensocial.jpa.BodyTypeDb;
import org.apache.shindig.social.opensocial.jpa.ListFieldDb;
import org.apache.shindig.social.opensocial.jpa.MediaItemDb;
import org.apache.shindig.social.opensocial.jpa.MessageDb;
import org.apache.shindig.social.opensocial.jpa.NameDb;
import org.apache.shindig.social.opensocial.jpa.OrganizationDb;
import org.apache.shindig.social.opensocial.jpa.PersonDb;
import org.apache.shindig.social.opensocial.jpa.UrlDb;
import org.apache.shindig.social.opensocial.jpa.spi.ActivityServiceDb;
import org.apache.shindig.social.opensocial.jpa.spi.AppDataServiceDb;
import org.apache.shindig.social.opensocial.jpa.spi.PersonServiceDb;
import org.apache.shindig.social.opensocial.model.Account;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.BodyType;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.Message;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Url;
import org.apache.shindig.social.opensocial.service.PersonHandler;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.PersonService;

import javax.persistence.EntityManager;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;

/**
 * Provides component injection for tests
 * Injects Social API and JPA persistence guice modules
 * 
 * @author bens
 */
public class JpaTestGuiceModule extends AbstractModule {
  

  private EntityManager entityManager;
  
  JpaTestGuiceModule(EntityManager entityManager) {
    this.entityManager = entityManager;
  }
  
  /**
   * Bind entity manager, services and entities used by samples
   */
  @Override
  protected void configure() {
    // Entity manager
    this.bind(EntityManager.class).toInstance(this.entityManager);
    
    // Service implementations
    this.bind(ActivityService.class).to(ActivityServiceDb.class).in(Scopes.SINGLETON);
    this.bind(AppDataService.class).to(AppDataServiceDb.class).in(Scopes.SINGLETON);
    this.bind(PersonService.class).to(PersonServiceDb.class).in(Scopes.SINGLETON);

    // Entities
    this.bind(Activity.class).to(ActivityDb.class);
    this.bind(Account.class).to(AccountDb.class);
    this.bind(Address.class).to(AddressDb.class);
    this.bind(BodyType.class).to(BodyTypeDb.class);
    this.bind(ListField.class).to(ListFieldDb.class);
    this.bind(MediaItem.class).to(MediaItemDb.class);
    this.bind(Message.class).to(MessageDb.class);
    this.bind(Name.class).to(NameDb.class);
    this.bind(Organization.class).to(OrganizationDb.class);
    this.bind(Person.class).to(PersonDb.class);
    this.bind(Url.class).to(UrlDb.class);
  }
}
