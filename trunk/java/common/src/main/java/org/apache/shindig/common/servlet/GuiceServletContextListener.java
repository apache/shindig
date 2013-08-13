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

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import org.apache.commons.lang3.StringUtils;

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

  // HNN- constant name matched system.properties <contextparam> specified in the web.xml
  private static final String SYSTEM_PROPERTIES = "system.properties";

  public void contextInitialized(ServletContextEvent event) {
    ServletContext context = event.getServletContext();
    setSystemProperties(context);
    String moduleNames = context.getInitParameter(MODULES_ATTRIBUTE);
    List<Module> modules = Lists.newLinkedList();

    if (moduleNames != null) {
      for (String moduleName : Splitter.on(':').split(moduleNames)) {
        try {
          moduleName = moduleName.trim();
          if (moduleName.length() > 0) {
            try {
              modules.add((Module)Class.forName(moduleName).newInstance());
            } catch (Throwable t) {
              // If we cannot find the class using forName try the current
              // threads class loader
              modules.add((Module)Thread.currentThread().getContextClassLoader()
                  .loadClass(moduleName).newInstance());
            }
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
  }

  public void contextDestroyed(ServletContextEvent event) {
    ServletContext context = event.getServletContext();
    Injector injector = (Injector) context.getAttribute(INJECTOR_ATTRIBUTE);
    if (injector != null) {
        CleanupHandler cleanups = injector.getInstance(CleanupHandler.class);
        cleanups.cleanup();
    }

    context.removeAttribute(INJECTOR_ATTRIBUTE);
  }


  /**
   * This method sets all the (key,value) properties specified in the web.xml <contextparam> system.properties element
   * if they are not empty.
   * @param context the ServletContext
   */
  private void setSystemProperties(ServletContext context){
    String contextRoot = context.getContextPath();
    System.setProperty("shindig.contextroot", contextRoot);
    String systemProperties = context.getInitParameter(SYSTEM_PROPERTIES);

    if (systemProperties!=null && systemProperties.trim().length() > 0){
      for (String prop : Splitter.on('\n').trimResults().split(systemProperties)){
        String[] keyAndvalue = StringUtils.split(prop, "=", 2);
        if (keyAndvalue.length == 2) {
          String key = keyAndvalue[0];
          String value = keyAndvalue[1];
          //set the system property if they are not empty
          if (key!=null && key.trim().length() > 0 && value!=null && value.trim().length() > 0){
            System.setProperty(key,value);
          }
        }
      }
    }
  }


  /**
   * Interface for classes that need to run cleanup code without
   * using Runtime ShutdownHooks (which leaks memory on redeploys)
   */
  public interface CleanupCapable {
    /** Execute the cleanup code. */
    void cleanup();
  }

  /**
   * Injectable handler that allows Guice classes to make themselves cleanup capable.
   */
  @Singleton
  public static class CleanupHandler {
    private List<CleanupCapable> cleanupHandlers = Lists.newArrayList();

    public CleanupHandler() { }
    /**
     * Add a new class instance for running cleanup code.
     *
     * Best way
     *
     * @param cleanupCapable class instance implementing CleanupCapable.
     */
    public void register(CleanupCapable cleanupCapable) {
      cleanupHandlers.add(cleanupCapable);
    }

    public void cleanup() {
      for (CleanupCapable cleanupHandler : cleanupHandlers) {
        cleanupHandler.cleanup();
      }
    }
  }
}

