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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Servlet which concatenates the content of several proxied HTTP responses
 *
 * @see org.apache.shindig.gadgets.rewrite.JavascriptTagMerger
 */
public class ConcatProxyServlet extends InjectedServlet {

  private static final Logger logger
      = Logger.getLogger(ConcatProxyServlet.class.getName());

  private transient ProxyHandler proxyHandler;

  @Inject
  public void setProxyHandler(ProxyHandler proxyHandler) {
    this.proxyHandler = proxyHandler;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (request.getHeader("If-Modified-Since") != null) {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }
    if (request.getParameter(ProxyBase.REWRITE_MIME_TYPE_PARAM) != null) {
      response.setHeader("Content-Type",
          request.getParameter(ProxyBase.REWRITE_MIME_TYPE_PARAM));
    }
    if (request.getParameter(ProxyBase.REFRESH_PARAM) != null) {
      HttpUtil.setCachingHeaders(response,
          Integer.valueOf(request.getParameter(ProxyBase.REFRESH_PARAM)));
    }
    response.setHeader("Content-Disposition", "attachment;filename=p.txt");
    ResponseWrapper wrapper = new ResponseWrapper(response);
    for (int i = 1; i < Integer.MAX_VALUE; i++) {
      String url = request.getParameter(Integer.toString(i));
      if (url == null) {
        break;
      }
      try {
        wrapper.getOutputStream().println("/* ---- Start " + url + " ---- */");
        proxyHandler.fetch(new RequestWrapper(request, url), wrapper);
        wrapper.getOutputStream().println("/* ---- End " + url + " ---- */");
      } catch (GadgetException ge) {
        if (ge.getCode() != GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT) {
          outputError(ge, url, response);
          return;
        } else {
          wrapper.getOutputStream().println("/* ---- End " + url + " 404 ---- */");
        }
      }
    }
    response.setStatus(200);
  }

  private void outputError(GadgetException excep, String url, HttpServletResponse resp)
      throws IOException {
    StringBuilder err = new StringBuilder();
    err.append(excep.getCode().toString());
    err.append(" concat(");
    err.append(url);
    err.append(") ");
    err.append(excep.getMessage());

    // Log the errors here for now. We might want different severity levels
    // for different error codes.
    logger.log(Level.INFO, "Concat proxy request failed", err);
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, err.toString());
  }

  /**
   * Simple request wrapper to make repeated calls to ProxyHandler
   */
  private static class RequestWrapper extends HttpServletRequestWrapper {

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

  /**
   * Wrap the response to prevent writing through of the status code and to hold a reference to the
   * stream across multiple proxied parts
   */
  private static class ResponseWrapper extends HttpServletResponseWrapper {

    private ServletOutputStream outputStream;

    private ResponseWrapper(HttpServletResponse httpServletResponse) {
      super(httpServletResponse);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
      if (outputStream == null) {
        outputStream = super.getOutputStream();
      }
      return outputStream;
    }

    public void addCookie(Cookie cookie) {
    }

    // Suppress headers
    public void setDateHeader(String s, long l) {
    }

    public void addDateHeader(String s, long l) {
    }

    public void setHeader(String s, String s1) {
    }

    public void addHeader(String s, String s1) {
    }

    public void setIntHeader(String s, int i) {
    }

    public void addIntHeader(String s, int i) {
    }

    public void setStatus(int i) {
    }
  }
}

