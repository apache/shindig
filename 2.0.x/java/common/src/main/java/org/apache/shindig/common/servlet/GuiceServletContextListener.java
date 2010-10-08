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
package org.apache.shindig.common.servlet;

import com.google.common.collect.Lists;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.tools.jmx.Manager;
import org.apache.commons.lang.StringUtils;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Creates a global guice injector and stores it in a servlet context parameter
 * for injecting servlets.
 */
public class GuiceServletContextListener implements ServletContextListener {
  public static final String INJECTOR_ATTRIBUTE = "guice-injector";
  public static final String MODULES_ATTRIBUTE = "guice-modules";

  // From guice-servlet-2.0
  public static final String INJECTOR_NAME = Injector.class.getName();
  
  //HNN- constant name matched system.properties <contextparam> specified in the web.xml
  private static final String SYSTEM_PROPERTIES = "system.properties";
  private boolean jmxInitialized = false;

  public void contextInitialized(ServletContextEvent event) {
    ServletContext context = event.getServletContext();
    //HNN setting all system.properties specified in the web.xml
    setSystemProperties(context);     
    String moduleNames = context.getInitParameter(MODULES_ATTRIBUTE);
    List<Module> modules = Lists.newLinkedList();
    if (moduleNames != null) {
      for (String moduleName : StringUtils.split(moduleNames, ':')) {
        try {
          moduleName = moduleName.trim();
          if (moduleName.length() > 0) {
            modules.add((Module)Class.forName(moduleName).newInstance());
          }
        } catch (InstantiationException e) {
          throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }
    Injector injector = Guice.createInjector(Stage.PRODUCTION, modules);
    context.setAttribute(INJECTOR_ATTRIBUTE, injector);

    try {
      if (!jmxInitialized) {
        Manager.manage("ShindigGuiceContext", injector);
        jmxInitialized = true;
      }
    } catch (Exception e) {
      // Ignore errors
    }
  }

  public void contextDestroyed(ServletContextEvent event) {
    ServletContext context = event.getServletContext();
    context.removeAttribute(INJECTOR_ATTRIBUTE);
  }
  
  /**
   * This method sets all the (key,value) properties specified in the web.xml <contextparam> system.properties element
   * if they are not empty.
   * @param context
   */
  private void setSystemProperties(ServletContext context){
    String systemProperties = context.getInitParameter(SYSTEM_PROPERTIES);
    String key=null;
    String value=null;
    if(systemProperties!=null && systemProperties.trim().length()>0){
      for (String aProperty : StringUtils.split(systemProperties, '\n')){
      String[] keyAndvalue = StringUtils.split(aProperty.trim(), "=",2);
        if(keyAndvalue.length==2){
        key=keyAndvalue[0];
          value=keyAndvalue[1];
          //set the system property if they are not empty
          if(key!=null && key.trim().length()>0 && value!=null && value.trim().length()>0){
            System.setProperty(key,value);
          }
        }
      }
    }
  }  
}

