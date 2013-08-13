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
package org.apache.shindig.gadgets.oauth2;

import java.util.List;

import org.apache.shindig.common.Pair;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.oauth.OAuthResponseParams;
import org.apache.shindig.gadgets.oauth2.logger.FilteredLogger;

import com.google.common.collect.Lists;

/**
 * Container for OAuth2 specific data to include in the response to the client.
 */
public class OAuth2ResponseParams {
  public static final String APPROVAL_URL = OAuthResponseParams.APPROVAL_URL;
  public static final String ERROR_CODE = OAuthResponseParams.ERROR_CODE;
  public static final String ERROR_TEXT = OAuthResponseParams.ERROR_TEXT;
  public static final String ERROR_TRACE = "oauthErrorTrace";
  public static final String ERROR_URI = "oauthErrorUri";
  public static final String ERROR_EXPLANATION = "oauthErrorExplanation";

  private String authorizationUrl;
  private String requestTraceString;
  private String message;

  public OAuth2ResponseParams() {
  }

  /**
   * Add a request/response pair to our trace of actions associated with this
   * request.
   */
  public void addRequestTrace(final HttpRequest request, final HttpResponse response) {
    final List<Pair<HttpRequest, HttpResponse>> requestTrace = Lists.newArrayList();
    requestTrace.add(Pair.of(request, response));
    this.requestTraceString = OAuth2ResponseParams.getRequestTrace(requestTrace);
  }

  public void addDebug(final String debugMessage) {
    this.message = debugMessage;
  }

  public void addToResponse(final HttpResponseBuilder responseBuilder, final String errorCode,
      final String errorDescription, final String errorUri, final String errorExplanation) {

    if (errorCode != null) {
      responseBuilder.setMetadata(OAuth2ResponseParams.ERROR_CODE, errorCode);
    } else {
      responseBuilder.setMetadata(OAuth2ResponseParams.ERROR_CODE, "");
    }

    if (errorUri != null) {
      responseBuilder.setMetadata(OAuth2ResponseParams.ERROR_URI, errorUri);
    } else {
      responseBuilder.setMetadata(OAuth2ResponseParams.ERROR_URI, "");
    }

    if (errorDescription != null) {
      responseBuilder.setMetadata(OAuth2ResponseParams.ERROR_TEXT, errorDescription);
    } else {
      responseBuilder.setMetadata(OAuth2ResponseParams.ERROR_TEXT, "");
    }

    if (errorExplanation != null) {
      responseBuilder.setMetadata(OAuth2ResponseParams.ERROR_EXPLANATION, errorExplanation);
    } else {
      responseBuilder.setMetadata(OAuth2ResponseParams.ERROR_EXPLANATION, "");
    }

    String _message = "\n";
    if (this.message != null) {
      _message = _message + this.message + '\n';
    }
    if (this.requestTraceString != null) {
      _message = _message + this.requestTraceString;
    }
    responseBuilder.setMetadata(OAuth2ResponseParams.ERROR_TRACE, _message);
  }

  public String getAuthorizationUrl() {
    return this.authorizationUrl;
  }

  public void setAuthorizationUrl(final String authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
  }

  private static String getRequestTrace(final List<Pair<HttpRequest, HttpResponse>> requestTrace) {
    final StringBuilder trace = new StringBuilder();
    int i = 1;
    for (final Pair<HttpRequest, HttpResponse> event : requestTrace) {
      trace.append("\n==== Sent request ").append(i).append(":\n");
      if (event.one != null) {
        trace.append(FilteredLogger.filterSecrets(event.one.toString()));
      }
      trace.append("\n==== Received response ").append(i).append(":\n");
      if (event.two != null) {
        trace.append(FilteredLogger.filterSecrets(event.two.toString()));
      }
      trace.append("\n====");
      ++i;
    }
    return trace.toString();
  }
}
