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

import org.apache.shindig.gadgets.GadgetServerConfigReader;
import org.apache.shindig.gadgets.GadgetException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ProxyServlet extends HttpServlet {

  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.gadgets");

  private CrossServletState servletState;
  private ProxyHandler proxyHandler;

  @Override
  public void init(ServletConfig config) throws ServletException {
    servletState = CrossServletState.get(config);
    GadgetServerConfigReader serverConfig
        = servletState.getGadgetServer().getConfig();
    proxyHandler = new ProxyHandler(serverConfig.getContentFetcher());
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String output = request.getParameter("output");
    try {
      if ("js".equals(output)) {
        proxyHandler.fetchJson(
            request, response, servletState.getGadgetSigner(request));
      } else {
        proxyHandler.fetch(
            request, response, servletState.getGadgetSigner(request));
      }
    } catch (GadgetException ge) {
      outputError(ge, response);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // Currently they are identical
    doGet(request, response);
  }

  private void outputError(GadgetException excep, HttpServletResponse resp) throws IOException {
    StringBuilder err = new StringBuilder();
    err.append(excep.getCode().toString());
    err.append(' ');
    err.append(excep.getMessage());

    // Log the errors here for now. We might want different severity levels
    // for different error codes.
    logger.log(Level.INFO, "Proxy request failed", err);
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, err.toString());
  }
}
