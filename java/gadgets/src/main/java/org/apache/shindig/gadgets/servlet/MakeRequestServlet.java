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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

/**
 * Handles calls to gadgets.io.makeRequest.
 *
 * GET and POST are supported so as to facilitate improved browser caching.
 *
 * Currently this just delegates to ProxyHandler, which deals with both
 * makeRequest and open proxy calls.
 */
public class MakeRequestServlet extends InjectedServlet {
  private MakeRequestHandler makeRequestHandler;

  @Inject
  public void setMakeRequestHandler(MakeRequestHandler makeRequestHandler) {
    this.makeRequestHandler = makeRequestHandler;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    makeRequestHandler.fetch(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request,  HttpServletResponse response)
      throws IOException {
    doGet(request, response);
  }
}
