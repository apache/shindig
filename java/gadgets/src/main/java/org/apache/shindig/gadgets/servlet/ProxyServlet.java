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
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.uri.ProxyUriManager;

import java.io.IOException;
import java.util.logging.Logger;

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
  
  private static final Logger LOG = Logger.getLogger(ProxyServlet.class.getName());
  
  private transient ProxyUriManager proxyUriManager;
  private transient LockedDomainService lockedDomainService;
  private transient ProxyHandler proxyHandler;

  @Inject
  public void setProxyHandler(ProxyHandler proxyHandler) {
    checkInitialized();
    this.proxyHandler = proxyHandler;
  }
  
  @Inject
  public void setProxyUriManager(ProxyUriManager proxyUriManager) {
    checkInitialized();
    this.proxyUriManager = proxyUriManager;
  }
  
  @Inject
  public void setLockedDomainService(LockedDomainService lockedDomainService) {
    checkInitialized();
    this.lockedDomainService = lockedDomainService;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse servletResponse)
      throws IOException {
    if (request.getHeader("If-Modified-Since") != null) {
      servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    Uri reqUri = new UriBuilder(request).toUri();
    HttpResponse response = null;
    try {
      // Parse request uri:
      ProxyUriManager.ProxyUri proxyUri = proxyUriManager.process(reqUri);

      // TODO: Consider removing due to redundant logic.
      String host = request.getHeader("Host");
      if (!lockedDomainService.isSafeForOpenProxy(host)) {
        // Force embedded images and the like to their own domain to avoid XSS
        // in gadget domains.
        Uri resourceUri = proxyUri.getResource();
        String msg = "Embed request for url " +
            (resourceUri != null ? resourceUri.toString() : "n/a") + " made to wrong domain " + host;
        LOG.info(msg);
        throw new GadgetException(GadgetException.Code.INVALID_PARAMETER, msg,
            HttpResponse.SC_BAD_REQUEST);
      }
      
      response = proxyHandler.fetch(proxyUri);
    } catch (GadgetException e) {
      response = ServletUtil.errorResponse(new GadgetException(e.getCode(), e.getMessage(),
          HttpServletResponse.SC_BAD_REQUEST));
    }
    
    ServletUtil.copyResponseToServlet(response, servletResponse);
  }
}
