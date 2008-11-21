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

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.spi.Message;

import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.social.opensocial.jpa.PersonDb;
import org.apache.shindig.social.opensocial.jpa.eclipselink.EclipseEntityManagerProvider;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.PersonService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import javax.persistence.EntityManager;

/**
 * 
 */
public class JPASocialModule extends AbstractModule {

  private final static String DEFAULT_PROPERTIES = "socialjpa.properties";
  private Properties properties;

  /**
   * 
   */
  public JPASocialModule() {
    InputStream is = null;
    try {
      is = this.getClass().getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES);
      if (is == null) {
        System.err.println("Cant loacate properties");
        throw new IOException("Failed to open " + DEFAULT_PROPERTIES);
      }
      properties = new Properties();
      properties.load(is);
    } catch (IOException e) {
      throw new CreationException(Arrays.asList(new Message(
          "Unable to load properties: " + DEFAULT_PROPERTIES)));
    } finally {
      try {
        if (is != null) {
          is.close();
        }
      } catch (IOException e) {
        // dont care about this.
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    Names.bindProperties(this.binder(), properties);
    bind(EntityManager.class).toProvider(EclipseEntityManagerProvider.class)
        .in(Scopes.SINGLETON);
    bind(ActivityService.class).to(ActivityServiceDb.class)
        .in(Scopes.SINGLETON);
    bind(PersonService.class).to(PersonServiceDb.class).in(Scopes.SINGLETON);
    bind(AppDataService.class).to(AppDataServiceDb.class).in(Scopes.SINGLETON);
  }
}
