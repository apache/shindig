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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet to act as our OAuth callback URL.  When gadget authors register a consumer key with an
 * OAuth service provider, they can provide a URL pointing to this servlet as their callback URL.
 * 
 * Protocol flow:
 * - gadget discovers it needs approval to access data at OAuth SP.
 * - gadget opens popup window to approval URL, passing URL to this servlet as the oauth_callback
 *   parameter on the approval URL.
 * - user grants approval at service provider
 * - service provider redirects to this servlet
 * - this servlet closes the window
 * - gadget discovers the window has closed and automatically fetches the user's data.
 */
public class OAuthCallbackServlet extends HttpServlet {

  private static final int ONE_HOUR_IN_SECONDS = 3600;
  
  private static final String RESP_BODY =
    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" " +
    "\"http://www.w3.org/TR/html4/loose.dtd\">" +
    "<html>" +
    "<head>" +
    "<title>Close this window</title>" +
    "</head>" +
    "<body>" +
    "<script type=\"text/javascript\">" +
    "window.close();" +
    "</script>" +
    "Close this window." +
    "</body>" +
    "</html>";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    HttpUtil.setCachingHeaders(resp, ONE_HOUR_IN_SECONDS);
    resp.setContentType("text/html; charset=UTF-8");
    resp.getWriter().write(RESP_BODY);
  }
}
