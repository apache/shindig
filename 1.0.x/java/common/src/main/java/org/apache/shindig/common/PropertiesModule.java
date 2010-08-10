/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.shindig.common;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.name.Names;
import com.google.inject.spi.Message;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.util.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * Injects everything from the a property file as a Named value
 * Uses the default shindig.properties file if no other is provided
 */
public class PropertiesModule extends AbstractModule {

  private final static String DEFAULT_PROPERTIES = "shindig.properties";

  private final Properties properties;

  public PropertiesModule() {
    this.properties = readPropertyFile(DEFAULT_PROPERTIES);
  }

  public PropertiesModule(String propertyFile) {
    this.properties = readPropertyFile(propertyFile);
  }

  public PropertiesModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    Names.bindProperties(this.binder(), properties);
  }
  
  private Properties readPropertyFile(String propertyFile) {
    Properties properties = new Properties();
    InputStream is = null;
    try {
      is = ResourceLoader.openResource(propertyFile);
      properties.load(is);
    } catch (IOException e) {
      throw new CreationException(Arrays.asList(
          new Message("Unable to load properties: " + propertyFile)));
    } finally {
      IOUtils.closeQuietly( is );
    }

    return properties;
  }

}
