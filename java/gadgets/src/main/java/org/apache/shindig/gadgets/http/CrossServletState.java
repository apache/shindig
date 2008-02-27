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

package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetServer;
import org.apache.shindig.gadgets.GadgetSigner;
import org.apache.shindig.gadgets.ProcessingOptions;

import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * Loads shared configuration and creates appropriate class instances for
 * sharing amongst servlets.
 */
public abstract class CrossServletState {
  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.gadgets");
  /**
   * @param config
   * @return A CrossServletState appropriate for the given ServletConfig
   * @throws ServletException
   */
  public static synchronized CrossServletState get(ServletConfig config)
      throws ServletException {
    ServletContext context = config.getServletContext();
    // Check to see if the context has already been created. If it has,
    // just return.
    CrossServletState state
        = (CrossServletState)context.getAttribute("servlet-data");

    if (state == null) {
      String dataClass = context.getInitParameter("servlet-data-class");
      if (dataClass  == null) {
        throw new ServletException("servlet-data-class is missing.");
      }
      logger.info("Loading CrossServletState: " + dataClass);
      try {
        state  = (CrossServletState)Class.forName(dataClass).newInstance();
        state.init(context);
        context.setAttribute("servlet-data", state);
      } catch (InstantiationException ie) {
        throw new ServletException(ie);
      } catch (IllegalAccessException iae) {
        throw new ServletException(iae);
      } catch (ClassNotFoundException cnfe) {
        throw new ServletException(cnfe);
      }
    }
    return state;
  }

  /**
   * @return A GadgetServer instance that is fully configured and
   * ready to be used to process gadgets.
   */
  public abstract GadgetServer getGadgetServer();

  /**
   * @param req The request that a signing token is needed for.
   * @return A unique GadgetSigner for the request
   */
  public abstract GadgetSigner getGadgetSigner(HttpServletRequest req);

  /**
   * Constructs a url for retrieving javascript for the given
   * set of features.
   *
   * @param features
   * @return The url to retrieve the appropriate JS.
   */
  public abstract String getJsUrl(String[] features, ProcessingOptions opts);

  /**
   * Constructs a url for generating an iframe for the given gadget.
   * This only applies for RPC calls that must generate an iframe.
   *
   * TODO: The second parameter here should be something else (perhaps a
   * context object). A better choice would probably be to add the view params
   * to ProcessingOptions and pass that here.
   */
  public abstract String getIframeUrl(Gadget gadget, ProcessingOptions opts);

  /**
   * Initializes this handler using the provided implementation.
   * @param context
   * @throws ServletException
   */
  protected abstract void init(ServletContext context) throws ServletException;
}
