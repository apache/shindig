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

import com.google.inject.Injector;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

/**
 * A Filter that can use Guice for injecting. Complements InjectedServlet.
 */
public abstract class InjectedFilter implements Filter {
  protected Injector injector;

  public void init(FilterConfig config) throws ServletException {
    ServletContext context = config.getServletContext();
    injector = (Injector) context.getAttribute(GuiceServletContextListener.INJECTOR_ATTRIBUTE);
    if (injector == null) {
      injector = (Injector)
        context.getAttribute(GuiceServletContextListener.INJECTOR_NAME);
      if (injector == null) {
        throw new UnavailableException(
            "Guice Injector not found! Make sure you registered " +
            GuiceServletContextListener.class.getName() + " as a listener");
      }
    }
    injector.injectMembers(this);
  }
}
