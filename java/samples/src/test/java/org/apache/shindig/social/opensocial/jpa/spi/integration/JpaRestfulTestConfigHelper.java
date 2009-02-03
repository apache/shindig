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

import org.apache.shindig.social.core.util.BeanJsonConverter;
import org.apache.shindig.social.core.util.BeanXStreamAtomConverter;
import org.apache.shindig.social.core.util.BeanXStreamConverter;
import org.apache.shindig.social.core.util.xstream.XStream081Configuration;
import org.apache.shindig.social.opensocial.jpa.spi.SpiEntityManagerFactory;
import org.apache.shindig.social.opensocial.service.DataServiceServlet;
import org.apache.shindig.social.opensocial.service.HandlerRegistry;

import javax.persistence.EntityManager;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * JPARestfulTestConfigHelper helps configure a JPA Restful test.
 */
public class JpaRestfulTestConfigHelper {

  /**
   * Init configuration.
   * 
   * @return the injector
   */
  protected static Injector init() {
    EntityManager entityManager = SpiEntityManagerFactory.getEntityManager();
    Injector injector = Guice.createInjector(new JpaTestGuiceModule(entityManager));
    return injector;
  }
  
  /**
   * Gets the data service servlet.
   * 
   * @param injector the injector
   * 
   * @return the data service servlet
   */
  protected static DataServiceServlet getDataServiceServlet(Injector injector) {
 // Set data service servlet again to use JPA guice dependencies
    DataServiceServlet servlet = new DataServiceServlet();
    servlet.setHandlerRegistry(injector.getInstance(HandlerRegistry.class));
    servlet.setBeanConverters(new BeanJsonConverter(injector),
        new BeanXStreamConverter(new XStream081Configuration(injector)),
        new BeanXStreamAtomConverter(new XStream081Configuration(injector)));
    return servlet;
  }
  
}
