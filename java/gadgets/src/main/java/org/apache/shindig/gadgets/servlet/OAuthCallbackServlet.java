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
import com.google.inject.name.Named;

import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.oauth.OAuthCallbackState;
import org.apache.shindig.gadgets.oauth.OAuthFetcherConfig;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
public class OAuthCallbackServlet extends InjectedServlet {

  private static final long serialVersionUID = 7126255229334669172L;

  public static final String CALLBACK_STATE_PARAM = "cs";
  public static final String REAL_DOMAIN_PARAM = "d";
  private static final int ONE_HOUR_IN_SECONDS = 3600;

  // This bit of magic passes the entire callback URL into the opening gadget for later use.
  // gadgets.io.makeRequest (or osapi.oauth) will then pick up the callback URL to complete the
  // oauth dance.
  private static final String RESP_BODY =
    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" " +
    "\"http://www.w3.org/TR/html4/loose.dtd\">\n" +
    "<html>\n" +
    "<head>\n" +
    "<title>Close this window</title>\n" +
    "</head>\n" +
    "<body>\n" +
    "<script type='text/javascript'>\n" +
    "try {\n" +
    "  window.opener.gadgets.io.oauthReceivedCallbackUrl_ = document.location.href;\n" +
    "} catch (e) {\n" +
    "}\n" +
    "window.close();\n" +
    "</script>\n" +
    "Close this window.\n" +
    "</body>\n" +
    "</html>\n";

  private transient BlobCrypter stateCrypter;

  @Inject
  public void setStateCrypter(@Named(OAuthFetcherConfig.OAUTH_STATE_CRYPTER) BlobCrypter stateCrypter) {
    checkInitialized();
    this.stateCrypter = stateCrypter;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    OAuthCallbackState callbackState = new OAuthCallbackState(stateCrypter,
        req.getParameter(CALLBACK_STATE_PARAM));
    if (callbackState.getRealCallbackUrl() != null) {
      // Copy the query parameters from this URL over to the real URL.
      UriBuilder realUri = UriBuilder.parse(callbackState.getRealCallbackUrl());
      Map<String, List<String>> params = UriBuilder.splitParameters(req.getQueryString());
      for (Map.Entry<String, List<String>> entry : params.entrySet()) {
        realUri.putQueryParameter(entry.getKey(), entry.getValue());
      }
      realUri.removeQueryParameter(CALLBACK_STATE_PARAM);
      HttpUtil.setCachingHeaders(resp, ONE_HOUR_IN_SECONDS, true);
      resp.sendRedirect(realUri.toString());
      return;
    }
    HttpUtil.setCachingHeaders(resp, ONE_HOUR_IN_SECONDS, true);
    resp.setContentType("text/html; charset=UTF-8");
    resp.getWriter().write(RESP_BODY);
  }
}
