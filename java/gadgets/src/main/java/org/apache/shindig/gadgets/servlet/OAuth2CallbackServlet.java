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
import com.google.inject.Provider;
import com.google.inject.name.Named;

import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2CallbackState;
import org.apache.shindig.gadgets.oauth2.OAuth2Error;
import org.apache.shindig.gadgets.oauth2.OAuth2FetcherConfig;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;
import org.apache.shindig.gadgets.oauth2.OAuth2Module;
import org.apache.shindig.gadgets.oauth2.OAuth2Store;
import org.apache.shindig.gadgets.oauth2.handler.AuthorizationEndpointResponseHandler;
import org.apache.shindig.gadgets.oauth2.handler.OAuth2HandlerError;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class OAuth2CallbackServlet extends InjectedServlet {
  private static final long serialVersionUID = -8829844832872635091L;

  private static final String LOG_CLASS = OAuth2CallbackServlet.class.getName();
  private static final Logger LOGGER = Logger.getLogger(OAuth2CallbackServlet.LOG_CLASS);

  private transient List<AuthorizationEndpointResponseHandler> authorizationEndpointResponseHandlers;
  private transient OAuth2Store store;
  private transient Provider<OAuth2Message> oauth2MessageProvider;
  private transient BlobCrypter stateCrypter;
  private transient boolean sendTraceToClient = false;

  // This bit of magic passes the entire callback URL into the opening gadget
  // for later use.
  // gadgets.io.makeRequest (or osapi.oauth) will then pick up the callback URL
  // to complete the
  // oauth dance.
  private static final String RESP_BODY = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" "
          + "\"http://www.w3.org/TR/html4/loose.dtd\">\n"
          + "<html>\n"
          + "<head>\n"
          + "<title>Close this window</title>\n"
          + "</head>\n"
          + "<body>\n"
          + "<script type='text/javascript'>\n"
          + "try {\n"
          + "  window.opener.gadgets.io.oauthReceivedCallbackUrl_ = document.location.href;\n"
          + "} catch (e) {\n"
          + "}\n"
          + "window.close();\n"
          + "</script>\n"
          + "Close this window.\n" + "</body>\n" + "</html>\n";

  private static final String RESP_ERROR_BODY = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" "
          + "\"http://www.w3.org/TR/html4/loose.dtd\">\n"
          + "<html>\n"
          + "<head>\n"
          + "<title>OAuth2 Error</title>\n"
          + "</head>\n"
          + "<body>\n"
          + "<p>error = %s</p>"
          + "<p>error description = %s</p>"
          + "<p>error uri = %s</p>"
          + "Close this window.\n"
          + "</body>\n" + "</html>\n";

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse resp)
          throws IOException {

    OAuth2Accessor accessor = null;
    try {
      final OAuth2Message msg = this.oauth2MessageProvider.get();
      msg.parseRequest(request);
      final OAuth2Error error = msg.getError();
      final String encRequestStateKey = msg.getState();
      if (encRequestStateKey == null) {
        if (error != null) {
          OAuth2CallbackServlet.sendError(error, "encRequestStateKey is null", msg.getErrorDescription(),
                  msg.getErrorUri(), null, resp, null, this.sendTraceToClient);
        } else {
          OAuth2CallbackServlet.sendError(OAuth2Error.CALLBACK_PROBLEM,
                  "OAuth2CallbackServlet requestStateKey is null.", "", "", null, resp, null,
                  this.sendTraceToClient);
        }
        return;
      }

      final OAuth2CallbackState state = new OAuth2CallbackState(this.stateCrypter,
              encRequestStateKey);

      accessor = this.store.getOAuth2Accessor(state);

      if (error != null) {
        OAuth2CallbackServlet.sendError(error, "error parsing request", msg.getErrorDescription(),
                msg.getErrorUri(), accessor, resp, null, this.sendTraceToClient);
        return;
      }

      if (accessor == null || !accessor.isValid() || accessor.isErrorResponse()) {
        String message;
        if (accessor != null) {
          message = accessor.isValid() ? "OAuth2CallbackServlet accessor isErrorResponse "
                  : "OAuth2CallbackServlet accessor is invalid ";
          message = message + accessor;
        } else {
          message = "OAuth2CallbackServlet accessor is null";
        }

        OAuth2CallbackServlet.sendError(OAuth2Error.CALLBACK_PROBLEM, message,
                accessor.getErrorContextMessage(), accessor.getErrorUri(), accessor, resp,
                accessor.getErrorException(), this.sendTraceToClient);

        return;
      }

      if (!accessor.isRedirecting()) {
        // Somehow our accessor got lost. We should not proceed.
        OAuth2CallbackServlet.sendError(OAuth2Error.CALLBACK_PROBLEM,
                "OAuth2CallbackServlet accessor is not valid, isn't redirecting.", "", "",
                accessor, resp, null, this.sendTraceToClient);
        return;
      }

      boolean foundHandler = false;
      for (final AuthorizationEndpointResponseHandler authorizationEndpointResponseHandler : this.authorizationEndpointResponseHandlers) {
        if (authorizationEndpointResponseHandler.handlesRequest(accessor, request)) {
          final OAuth2HandlerError handlerError = authorizationEndpointResponseHandler
                  .handleRequest(accessor, request);
          if (handlerError != null) {
            OAuth2CallbackServlet.sendError(handlerError.getError(),
                    handlerError.getContextMessage(), handlerError.getDescription(),
                    handlerError.getUri(), accessor, resp, handlerError.getCause(),
                    this.sendTraceToClient);
            return;
          }
          foundHandler = true;
          break;
        }
      }

      if (!foundHandler) {
        OAuth2CallbackServlet.sendError(OAuth2Error.NO_RESPONSE_HANDLER,
                "OAuth2Callback servlet couldn't find a AuthorizationEndpointResponseHandler", "",
                "", accessor, resp, null, this.sendTraceToClient);
        return;
      }

      HttpUtil.setNoCache(resp);
      resp.setContentType("text/html; charset=UTF-8");
      resp.getWriter().write(OAuth2CallbackServlet.RESP_BODY);
    } catch (final Exception e) {
      OAuth2CallbackServlet.sendError(OAuth2Error.CALLBACK_PROBLEM,
              "Exception occurred processing redirect.", "", "", accessor, resp, e,
              this.sendTraceToClient);
      if (IOException.class.isInstance(e)) {
        throw (IOException) e;
      }
    } finally {
      if (accessor != null) {
        if (!accessor.isErrorResponse()) {
          accessor.invalidate();
          this.store.removeOAuth2Accessor(accessor);
        } else {
          this.store.storeOAuth2Accessor(accessor);
        }
      }
    }
  }

  private static void sendError(final OAuth2Error error, final String contextMessage,
          final String description, final String uri, final OAuth2Accessor accessor,
          final HttpServletResponse resp, final Throwable t, final boolean sendTraceToClient)
          throws IOException {

    OAuth2CallbackServlet.LOGGER.warning(OAuth2CallbackServlet.LOG_CLASS + " , callback error "
            + error + " -  " + contextMessage + " , " + description + " - " + uri);
    if (t != null) {
      if (OAuth2CallbackServlet.LOGGER.isLoggable(Level.FINE)) {
        OAuth2CallbackServlet.LOGGER.log(Level.FINE, " callback exception ", t);
      }
    }

    HttpUtil.setNoCache(resp);
    resp.setContentType("text/html; charset=UTF-8");

    if (accessor != null) {
      accessor.setErrorResponse(t, error, contextMessage + " , " + description, uri);
    } else {
      // We don't have an accessor to report the error back to the client in the
      // normal manner.
      // Anything is better than nothing, hack something together....
      final String errorResponse;
      if (sendTraceToClient) {
        errorResponse = String.format(OAuth2CallbackServlet.RESP_ERROR_BODY, error.getErrorCode(),
                error.getErrorDescription(description), uri);
      } else {
        errorResponse = String.format(OAuth2CallbackServlet.RESP_ERROR_BODY, error.getErrorCode(),
                "", "");
      }
      resp.getWriter().write(errorResponse);
      return;
    }

    resp.getWriter().write(OAuth2CallbackServlet.RESP_BODY);
  }

  @Inject
  public void setAuthorizationResponseHandlers(
          final List<AuthorizationEndpointResponseHandler> authorizationEndpointResponseHandlers) {
    this.authorizationEndpointResponseHandlers = authorizationEndpointResponseHandlers;
  }

  @Inject
  public void setOAuth2Store(@Named(OAuth2Module.SEND_TRACE_TO_CLIENT)
  final boolean sendTraceToClient) {
    this.sendTraceToClient = sendTraceToClient;
  }

  @Inject
  public void setOAuth2Store(final OAuth2Store store) {
    this.store = store;
  }

  @Inject
  public void setOauth2MessageProvider(final Provider<OAuth2Message> oauth2MessageProvider) {
    this.oauth2MessageProvider = oauth2MessageProvider;
  }

  @Inject
  public void setStateCrypter(@Named(OAuth2FetcherConfig.OAUTH2_STATE_CRYPTER)
  final BlobCrypter stateCrypter) {
    this.stateCrypter = stateCrypter;
  }
}
