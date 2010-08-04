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

import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

/**
 * Handles open proxy requests (used in rewriting and for URLs returned by
 * gadgets.io.getProxyUrl).
 */
public class ProxyServlet extends InjectedServlet {
  private static final long serialVersionUID = 9085050443492307723L;
  
  private transient ProxyHandler proxyHandler;
  private transient boolean initialized;

  @Inject
  public void setProxyHandler(ProxyHandler proxyHandler) {
    if (initialized) {
      throw new IllegalStateException("Servlet already initialized");
    }
    this.proxyHandler = proxyHandler;
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    initialized = true;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse servletResponse)
      throws IOException {
    if (request.getHeader("If-Modified-Since") != null) {
      servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }
    
    HttpRequest req = ServletUtil.fromHttpServletRequest(request);
    HttpResponse response = null;
    try {
      response = proxyHandler.fetch(req);
    } catch (GadgetException e) {
      response = ServletUtil.errorResponse(new GadgetException(e.getCode(), e.getMessage(),
          HttpServletResponse.SC_BAD_REQUEST));
    }
    
    ServletUtil.copyResponseToServlet(response, servletResponse);
  }
}
