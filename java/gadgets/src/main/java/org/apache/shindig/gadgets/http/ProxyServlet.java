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

import org.apache.shindig.gadgets.BasicGadgetSigner;
import org.apache.shindig.gadgets.GadgetSigner;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ProxyServlet extends HttpServlet {
  private final GadgetSigner signer;
  private final ProxyHandler handler;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String output = request.getParameter("output");
    if (output != null && output.equals("js")) {
      handler.fetchJson(request, response, signer);
    } else {
      handler.fetch(request, response, signer);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // Currently they are identical
    doGet(request, response);
  }

  /**
   * Constructs a ProxyServlet with the default (non-secure) GadgetSigner.
   */
  public ProxyServlet() {
    this(new BasicGadgetSigner(), new ProxyHandler());
  }

  /**
   * Creates a ProxyServlet using the specified GadgetSigner.
   * @param signer Used to sign and verify requests
   * @param handler Used to fetch proxied content
   */
  public ProxyServlet(GadgetSigner signer, ProxyHandler handler) {
    this.signer = signer;
    this.handler = handler;
  }
}
