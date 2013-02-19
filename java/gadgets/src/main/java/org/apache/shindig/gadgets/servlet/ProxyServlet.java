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

import org.apache.shindig.auth.AuthInfoUtil;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.oauth2.OAuth2Arguments;
import org.apache.shindig.gadgets.uri.ProxyUriManager;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles open proxy requests (used in rewriting and for URLs returned by gadgets.io.getProxyUrl).
 */
public class ProxyServlet extends InjectedServlet {
  private static final long serialVersionUID = 9085050443492307723L;

  // class name for logging purpose
  private static final String classname = ProxyServlet.class.getName();
  private static final Logger LOG = Logger.getLogger(classname, MessageKeys.MESSAGES);

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
    processRequest(request, servletResponse);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse servletResponse)
      throws IOException {
    processRequest(request, servletResponse);
  }

  private void processRequest(HttpServletRequest request, HttpServletResponse servletResponse)
      throws IOException {
    if (request.getHeader("If-Modified-Since") != null) {
      servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    Uri reqUri = new UriBuilder(request).toUri();

    HttpResponse response;
    try {
      // Parse request uri:
      ProxyUriManager.ProxyUri proxyUri = proxyUriManager.process(reqUri);
      SecurityToken st = AuthInfoUtil.getSecurityTokenFromRequest(request);
      proxyUri.setSecurityToken(st);
      proxyUri.setUserAgent(request.getHeader("User-Agent"));
      // get gadget from security token
      if(proxyUri.getGadget() == null) {
        if(st != null && !st.isAnonymous()) {
          proxyUri.setGadget(st.getAppUrl());
        }
      }
      AuthType authType = proxyUri.getAuthType();
      if(AuthType.OAUTH.equals(authType)) {
        proxyUri.setOAuthArguments(new OAuthArguments(AuthType.OAUTH, request));
      } else if(AuthType.OAUTH2.equals(authType)) {
        proxyUri.setOAuth2Arguments(new OAuth2Arguments(request));
      }

      // TODO: Consider removing due to redundant logic.
      String host = request.getHeader("Host");
      if (!lockedDomainService.isSafeForOpenProxy(host)) {
        // Force embedded images and the like to their own domain to avoid XSS
        // in gadget domains.
        Uri resourceUri = proxyUri.getResource();
        String msg = "Embed request for url " + (resourceUri != null ? resourceUri.toString() : "n/a")
            + " made to wrong domain " + host;
        if (LOG.isLoggable(Level.INFO)) {
          LOG.logp(Level.INFO, classname, "processRequest", MessageKeys.EMBEDED_IMG_WRONG_DOMAIN,
            new Object[] { resourceUri != null ? resourceUri.toString() : "n/a", host });
        }
        throw new GadgetException(GadgetException.Code.INVALID_PARAMETER, msg,
          HttpResponse.SC_BAD_REQUEST);
      }
      if ("POST".equalsIgnoreCase(request.getMethod())) {
        StringBuffer buffer = getPOSTContent(request);
        response = proxyHandler.fetch(proxyUri, buffer.toString());
      } else {
        response = proxyHandler.fetch(proxyUri);
      }
    } catch (GadgetException e) {
      response = ServletUtil.errorResponse(new GadgetException(e.getCode(), e.getMessage(),
          HttpServletResponse.SC_BAD_REQUEST));
    }

    ServletUtil.copyToServletResponseAndOverrideCacheHeaders(response, servletResponse);
  }

  private StringBuffer getPOSTContent(HttpServletRequest request) throws IOException {
    // Convert POST content from request to a string
    StringBuffer buffer = new StringBuffer();
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
      int letter = 0;
      while ((letter = reader.read()) != -1) {
        buffer.append((char) letter);
      }
      reader.close();
    } catch (IOException e) {
      LOG.logp(Level.WARNING, classname, "getPOSTContent", "Caught exception while reading POST body:"
          + e.getMessage());
    } finally {
      IOUtils.closeQuietly(reader);
    }
    return buffer;
  }
}
