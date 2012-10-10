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
package org.apache.shindig.gadgets.servlet;

import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.gadgets.GadgetException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

/**
 * Handles calls to gadgets.io.makeRequest.
 *
 * GET and POST are supported so as to facilitate improved browser caching.
 *
 * Currently this just delegates to MakeRequestHandler, which deals with both
 * makeRequest and open proxy calls.
 */
public class MakeRequestServlet extends InjectedServlet {

  private static final long serialVersionUID = -8298705081500283786L;
  private static final String classname = MakeRequestServlet.class.getName();
  private static final Logger LOG = Logger.getLogger(classname, MessageKeys.MESSAGES);

  private transient MakeRequestHandler makeRequestHandler;

  @Inject
  public void setMakeRequestHandler(MakeRequestHandler makeRequestHandler) {
    checkInitialized();
    this.makeRequestHandler = makeRequestHandler;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    try {
      makeRequestHandler.fetch(request, response);
    } catch (GadgetException e) {
      if (LOG.isLoggable(Level.FINEST)) {
        LOG.logp(Level.FINEST, classname, "doGet", MessageKeys.HTTP_ERROR_FETCHING, e);
      }
      int responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
      if (e.getCode() != GadgetException.Code.INTERNAL_SERVER_ERROR) {
        responseCode = HttpServletResponse.SC_BAD_REQUEST;
      }
      response.sendError(responseCode, e.getMessage() != null ? e.getMessage() : "");
    }
  }

  @Override
  protected void doPost(HttpServletRequest request,  HttpServletResponse response)
      throws IOException {
    doGet(request, response);
  }
}
