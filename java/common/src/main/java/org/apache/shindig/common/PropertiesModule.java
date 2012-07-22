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
    super();
    this.properties = readPropertyFile(getDefaultPropertiesPath());
  }

  public PropertiesModule(String propertyFile) {
    this.properties = readPropertyFile(propertyFile);
  }

  public PropertiesModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    this.binder().bindConstant().annotatedWith(Names.named("shindig.contextroot")).to(getContextRoot());
    Names.bindProperties(this.binder(), getProperties());
    // This could be generalized to inject any system property...
    this.binder().bindConstant().annotatedWith(Names.named("shindig.port")).to(getServerPort());
    this.binder().bindConstant().annotatedWith(Names.named("shindig.host")).to(getServerHostname());
  }

  /**
   * Should return the context root where the current web module is deployed with.  Useful for testing and working out of the box configs.
   * If not set uses fixed value of "".
   * @return an context path as a string.
   */
  protected String getContextRoot() {
    return System.getProperty("shindig.contextroot") != null ? System.getProperty("shindig.contextroot") : "";
  }

  /**
   * Should return the default port set as system property. Return empty string if it's not set.
   * @return an integer port number as a string.
   */
  protected String getServerPort() {
    return System.getProperty("shindig.port") != null ? System.getProperty("shindig.port") : "";
  }

  /*
   * Should return the hostname set as system property. Return empty string  if its' not set.
   * @return a hostname
   */
  protected String getServerHostname() {
    return System.getProperty("shindig.host") != null ? System.getProperty("shindig.host") : "";
  }

  protected static String getDefaultPropertiesPath() {
    return DEFAULT_PROPERTIES;
  }

  protected Properties getProperties() {
    return properties;
  }

  protected Properties readPropertyFile(String propertyFile) {
    Properties properties = new Properties();
    InputStream is = null;
    String contextRoot = getContextRoot();
    try {
      is = ResourceLoader.openResource(propertyFile);
      properties.load(is);

      for (Object key : properties.keySet()) {
        String value = (String)properties.get((String)key);
        if (value != null && value.contains("%contextRoot%")){
          properties.put(key, value.replace(("%contextRoot%"),contextRoot));
        }
      }
    } catch (IOException e) {
      throw new CreationException(Arrays.asList(
          new Message("Unable to load properties: " + propertyFile)));
    } finally {
      IOUtils.closeQuietly(is);
    }
    return properties;
  }
}
