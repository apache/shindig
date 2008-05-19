/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.apache.shindig.gadgets.servlet;

import com.google.inject.Inject;

import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.gadgets.GadgetException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet which concatenates the content of several proxied HTTP responses
 *
 * @see org.apache.shindig.gadgets.rewrite.JavascriptTagMerger
 */
public class ConcatProxyServlet extends InjectedServlet {

  private static final Logger logger
      = Logger.getLogger(ConcatProxyServlet.class.getName());

  private ProxyHandler proxyHandler;

  @Inject
  public void setProxyHandler(ProxyHandler proxyHandler) {
    this.proxyHandler = proxyHandler;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    for (int i = 1; i < Integer.MAX_VALUE; i++) {
      String url = request.getParameter(Integer.toString(i));
      if (url == null) {
        break;
      }
      try {
        proxyHandler.fetch(new RequestWrapper(request, url), response);
      } catch (GadgetException ge) {
        outputError(ge, response);
      }
    }
  }


  private void outputError(GadgetException excep, HttpServletResponse resp)
      throws IOException {
    StringBuilder err = new StringBuilder();
    err.append(excep.getCode().toString());
    err.append(' ');
    err.append(excep.getMessage());

    // Log the errors here for now. We might want different severity levels
    // for different error codes.
    logger.log(Level.INFO, "Concat proxy request failed", err);
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, err.toString());
  }

  /** Simple request wrapper to make repeated calls to ProxyHandler */
  private class RequestWrapper extends HttpServletRequestWrapper {

    private final String url;

    private RequestWrapper(HttpServletRequest httpServletRequest, String url) {
      super(httpServletRequest);
      this.url = url;
    }

    @Override
    public String getParameter(String paramName) {
      if (ProxyHandler.URL_PARAM.equals(paramName)) {
        return url;
      }
      return super.getParameter(paramName);
    }
  }
}

