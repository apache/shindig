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

import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.oauth2.logger.FilteredLogger;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Maps;

/**
 * See {@link OAuth2Message}
 *
 */
public class BasicOAuth2Message implements OAuth2Message {
  private final static String LOG_CLASS = BasicOAuth2Message.class.getName();
  private final static FilteredLogger LOG = FilteredLogger
      .getFilteredLogger(BasicOAuth2Message.LOG_CLASS);

  private OAuth2Error error;
  private final Map<String, String> params;
  private final Map<String, String> unparsedProperties;

  public BasicOAuth2Message() {
    this.params = Maps.newHashMapWithExpectedSize(5);
    this.unparsedProperties = Maps.newHashMapWithExpectedSize(0);
  }

  public String getAccessToken() {
    return this.params.get(OAuth2Message.ACCESS_TOKEN);
  }

  public String getAuthorization() {
    return this.params.get(OAuth2Message.AUTHORIZATION);
  }

  public OAuth2Error getError() {
    if (this.error == null) {
      final String errorParam = this.params.get(OAuth2Message.ERROR);
      if (errorParam != null) {
        if (OAuth2Message.INVALID_REQUEST.equalsIgnoreCase(errorParam)) {
          this.error = OAuth2Error.SPEC_INVALID_REQUEST;
        } else if (OAuth2Message.UNAUTHORIZED_CLIENT.equalsIgnoreCase(errorParam)) {
          this.error = OAuth2Error.SPEC_UNAUTHORIZED_CLIENT;
        } else if (OAuth2Message.ACCESS_DENIED.equalsIgnoreCase(errorParam)) {
          this.error = OAuth2Error.SPEC_ACCESS_DENIED;
        } else if (OAuth2Message.UNSUPPORTED_RESPONSE_TYPE.equalsIgnoreCase(errorParam)) {
          this.error = OAuth2Error.SPEC_UNSUPPORTED_RESPONSE_TYPE;
        } else if (OAuth2Message.INVALID_SCOPE.equalsIgnoreCase(errorParam)) {
          this.error = OAuth2Error.SPEC_INVALID_SCOPE;
        } else if (OAuth2Message.SERVER_ERROR.equalsIgnoreCase(errorParam)) {
          this.error = OAuth2Error.SPEC_SERVER_ERROR;
        } else if (OAuth2Message.TEMPORARILY_UNAVAILABLE.equalsIgnoreCase(errorParam)) {
          this.error = OAuth2Error.SPEC_TEMPORARILY_UNAVAILABLE;
        } else if (OAuth2Message.INVALID_CLIENT.equalsIgnoreCase(errorParam)) {
          this.error = OAuth2Error.SPEC_INVALID_CLIENT;
        } else if (OAuth2Message.INVALID_GRANT.equalsIgnoreCase(errorParam)) {
          this.error = OAuth2Error.SPEC_INVALID_GRANT;
        } else if (OAuth2Message.UNSUPPORTED_GRANT_TYPE.equalsIgnoreCase(errorParam)) {
          this.error = OAuth2Error.SPEC_UNSUPPORTED_GRANT_TYPE;
        } else {
          this.error = OAuth2Error.UNKNOWN_PROBLEM;
        }
      }
    }

    return this.error;
  }

  public String getErrorDescription() {
    return this.params.get(OAuth2Message.ERROR_DESCRIPTION);
  }

  public String getErrorUri() {
    return this.params.get(OAuth2Message.ERROR_URI);
  }

  public String getExpiresIn() {
    return this.params.get(OAuth2Message.EXPIRES_IN);
  }

  public String getMacAlgorithm() {
    return this.params.get(OAuth2Message.MAC_ALGORITHM);
  }

  public String getMacSecret() {
    return this.params.get(OAuth2Message.MAC_SECRET);
  }

  public Map<String, String> getParameters() {
    return this.params;
  }

  public String getRefreshToken() {
    return this.params.get(OAuth2Message.REFRESH_TOKEN);
  }

  public String getState() {
    return this.params.get(OAuth2Message.STATE);
  }

  public String getTokenType() {
    return this.params.get(OAuth2Message.TOKEN_TYPE);
  }

  public Map<String, String> getUnparsedProperties() {
    // TODO ARC
    return this.unparsedProperties;
  }

  public void parseFragment(final String fragment) {
    final Uri uri = Uri.parse(fragment);
    final Map<String, List<String>> _params = uri.getFragmentParameters();
    for (final Entry<String, List<String>> entry : _params.entrySet()) {
      this.params.put(entry.getKey(), entry.getValue().get(0));
    }
  }

  public void parseJSON(final String response) {
    try {
      final JSONObject jsonObject = new JSONObject(response);
      final String accessToken = jsonObject.optString(OAuth2Message.ACCESS_TOKEN, null);
      if (accessToken != null) {
        this.params.put(OAuth2Message.ACCESS_TOKEN, accessToken);
      }

      final String tokenType = jsonObject.optString(OAuth2Message.TOKEN_TYPE, null);
      if (tokenType != null) {
        this.params.put(OAuth2Message.TOKEN_TYPE, tokenType);
      }

      final String expiresIn = jsonObject.optString(OAuth2Message.EXPIRES_IN, null);
      if (expiresIn != null) {
        this.params.put(OAuth2Message.EXPIRES_IN, expiresIn);
      }

      final String refreshToken = jsonObject.optString(OAuth2Message.REFRESH_TOKEN, null);
      if (refreshToken != null) {
        this.params.put(OAuth2Message.REFRESH_TOKEN, refreshToken);
      }

      final String _error = jsonObject.optString(OAuth2Message.ERROR, null);
      if (_error != null) {
        this.params.put(OAuth2Message.ERROR, _error);
      }

      final String errorDescription = jsonObject.optString(OAuth2Message.ERROR_DESCRIPTION, null);
      if (errorDescription != null) {
        this.params.put(OAuth2Message.ERROR_DESCRIPTION, errorDescription);
      }

      final String errorUri = jsonObject.optString(OAuth2Message.ERROR_URI, null);
      if (errorUri != null) {
        this.params.put(OAuth2Message.ERROR_URI, errorUri);
      }
    } catch (final JSONException e) {
      if (BasicOAuth2Message.LOG.isLoggable()) {
        BasicOAuth2Message.LOG.log("JSONException parsing response", e);
      }
      this.params.put(OAuth2Message.ERROR, "JSONException parsing response");
    }
  }

  public void parseQuery(final String query) {
    final Uri uri = Uri.parse(query);
    final Map<String, List<String>> _params = uri.getQueryParameters();
    for (final Entry<String, List<String>> entry : _params.entrySet()) {
      this.params.put(entry.getKey(), entry.getValue().get(0));
    }
    if ((!this.params.containsKey(OAuth2Message.EXPIRES_IN))
        && (this.params.containsKey("expires"))) {
      this.params.put(OAuth2Message.EXPIRES_IN, this.params.get("expires"));
    }
  }

  public void parseRequest(final HttpServletRequest request) {
    @SuppressWarnings("unchecked")
    final Enumeration<String> paramNames = request.getParameterNames();
    while (paramNames.hasMoreElements()) {
      final String paramName = paramNames.nextElement();
      final String param = request.getParameter(paramName);
      this.params.put(paramName, param);
    }
  }

  public void setError(OAuth2Error error) {
    this.params.put(OAuth2Message.ERROR, error.getErrorCode());
  }

  public void setErrorDescription(String errorDescription) {
    this.params.put(OAuth2Message.ERROR_DESCRIPTION, errorDescription);
  }

  public void setErrorUri(String errorUri) {
    this.params.put(OAuth2Message.ERROR_URI, errorUri);
  }
}
