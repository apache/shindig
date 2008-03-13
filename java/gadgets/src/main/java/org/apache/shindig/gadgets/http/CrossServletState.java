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
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetServer;
import org.apache.shindig.gadgets.GadgetSigner;
import org.apache.shindig.gadgets.GadgetToken;
import org.apache.shindig.gadgets.RequestSigner;

import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

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
   * @return A gadget signer implementation
   */
  public abstract GadgetSigner getGadgetSigner();

  /**
   * Constructs a url for retrieving javascript for the given
   * set of features.
   *
   * @param features
   * @return The url to retrieve the appropriate JS.
   */
  public abstract String getJsUrl(Set<String> features, GadgetContext context);

  /**
   * Constructs a url for generating an iframe for the given gadget.
   * This only applies for RPC calls that must generate an iframe.
   *
   * @param gadget
   * @return The url for the iframe; may have both query string and fragment
   *     parameters, so caution should be taken when adding your own data.
   */
  public abstract String getIframeUrl(Gadget gadget);

  /**
   * Constructs a RequestSigner object that can be used to sign requests from
   * the given gadget token to implement signed fetch.
   * 
   * @param token the decrypted, verified security token
   * @return a request signer implementing signed fetch.
   * 
   * @see org.apache.shindig.gadgets.SignedFetchRequestSigner
   */
  public abstract RequestSigner makeSignedFetchRequestSigner(GadgetToken token);

  /**
   * Constructs a RequestSigner object that can be used to sign requests from
   * the given gadget token to implement full OAuth.
   * 
   * @param token the decrypted, verified security token
   * @return a request signer implementing signed fetch.
   */
  public abstract RequestSigner makeOAuthRequestSigner(GadgetToken token);

  /**
   * Initializes this handler using the provided implementation.
   * @param context
   * @throws ServletException
   */
  protected abstract void init(ServletContext context) throws ServletException;
}
