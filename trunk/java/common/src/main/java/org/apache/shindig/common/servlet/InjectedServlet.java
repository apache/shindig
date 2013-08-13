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

import com.google.common.base.Preconditions;
import com.google.inject.Injector;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;

/**
 * Supports DI for servlets. Can't handle ctor injection since
 * the servlet spec requires configuration being done in init().
 */
public abstract class InjectedServlet extends HttpServlet {
  protected Injector injector;
  protected transient boolean initialized = false;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    ServletContext context = config.getServletContext();
    injector = (Injector) context.getAttribute(GuiceServletContextListener.INJECTOR_ATTRIBUTE);
    if (injector == null) {
      injector = (Injector) context.getAttribute(GuiceServletContextListener.INJECTOR_NAME);
      if (injector == null) {
        throw new UnavailableException(
            "Guice Injector not found! Make sure you registered " +
                GuiceServletContextListener.class.getName() + " as a listener");
      }
    }
    injector.injectMembers(this);
    initialized = true;
  }

  /**
   * Called in each guice injected method to insure we are not initialized twice
   */
  protected void checkInitialized() {
    Preconditions.checkState(!initialized, "Servlet already initialized");
  }

  /**
   * Writes the state of this InjectedServlet during serialization.
   *
   * @param out The stream to which to save the state
   *
   * @throws IOException if an error occurs
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
  }

  /**
   * Restores the state of this InjectedServlet during deserialization.
   *
   * <p>Upon deserialization, this InjectedServlet will not be functional
   * until after its init method was called, which will cause all necessary
   * injection to happen.
   *
   * @param in The stream from which to restore the state
   *
   * @throws IOException if an error occurs
   * @throws ClassNotFoundException if a class cannot be found
   */
  private void readObject(ObjectInputStream in)
          throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initialized = false;
  }
}
