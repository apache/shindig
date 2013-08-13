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

import com.google.inject.Inject;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.uri.AccelUriManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles requests for accel servlet.
 * The objective is to accelerate web pages.
 *
 * @since 2.0.0
 */
public class HtmlAccelServlet extends InjectedServlet {
  private static final long serialVersionUID = -424353123863813052L;

  private static final Logger logger = Logger.getLogger(
      HtmlAccelServlet.class.getName());
  private transient AccelHandler accelHandler;

  @Inject
  public void setHandler(AccelHandler accelHandler) {
    checkInitialized();
    this.accelHandler = accelHandler;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse servletResponse)
      throws IOException {
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("Accel request = " + request.toString());
    }

    HttpRequest req = ServletUtil.fromHttpServletRequest(request);
    req.setContainer(AccelUriManager.CONTAINER);
    HttpResponse response;
    try {
      response = accelHandler.fetch(req);
    } catch (GadgetException e) {
      response = ServletUtil.errorResponse(e);
    }

    ServletUtil.copyToServletResponse(response, servletResponse);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    doGet(request, response);
  }
}
