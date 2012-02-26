/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.shindig.gadgets.oauth2;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import org.apache.shindig.gadgets.oauth2.logger.FilteredLogger;

/**
 * Any time there's an error in the OAuth2 layer it's reported with an
 * OAuth2Error.
 *
 * errorCode - should correspond to an OAuth2Message errorCode when appropriate.
 *
 */
public enum OAuth2Error {
  AUTHORIZATION_CODE_PROBLEM("authorization_code_problem"),
  AUTHORIZE_PROBLEM("authorize_problem"),
  AUTHENTICATION_PROBLEM( "authentication_problem"),
  BEARER_TOKEN_PROBLEM("bearer_token_problem"),
  CALLBACK_PROBLEM("callback_problem"),
  CLIENT_CREDENTIALS_PROBLEM("client_credentials_problem"),
  CODE_GRANT_PROBLEM("code_grant_problem"),
  FETCH_INIT_PROBLEM("fetch_init_problem"),
  FETCH_PROBLEM("fetch_problem"),
  GADGET_SPEC_PROBLEM("gadget_spec_problem"),
  GET_OAUTH2_ACCESSOR_PROBLEM("get_oauth2_accessor_problem"),
  LOOKUP_SPEC_PROBLEM("lookup_spec_problem"),
  MAC_TOKEN_PROBLEM("mac_token_problem"),
  MISSING_FETCH_PARAMS("missing_fetch_params"),
  MISSING_SERVER_RESPONSE("missing_server_response"),
  NO_RESPONSE_HANDLER("no_response_handler"),
  NO_GADGET_SPEC("no_gadget_spec"),
  REFRESH_TOKEN_PROBLEM("refresh_token_problem"),
  SECRET_ENCRYPTION_PROBLEM("secret_encryption_problem"),
  SPEC_ACCESS_DENIED("access_denied"),
  SPEC_INVALID_CLIENT("invalid_client"),
  SPEC_INVALID_GRANT("invalid_grant"),
  SPEC_INVALID_REQUEST("invalid_request"),
  SPEC_INVALID_SCOPE("invalid_scope"),
  SPEC_SERVER_ERROR("server_error"),
  SPEC_TEMPORARILY_UNAVAILABLE("temporarily_unavailable"),
  SPEC_UNAUTHORIZED_CLIENT("unauthorized_client"),
  SPEC_UNSUPPORTED_GRANT_TYPE("unsupported_grant_type"),
  SPEC_UNSUPPORTED_RESPONSE_TYPE("unsupported_response_type"),
  SERVER_REJECTED_REQUEST("server_rejected_request"),
  TOKEN_RESPONSE_PROBLEM("token_response_problem"),
  UNKNOWN_PROBLEM("unknown_problem");

  public static final String MESSAGES = "org.apache.shindig.gadgets.oauth2.resource";

  private static final String MESSAGE_HEADER = "message_header";

  private final String errorCode;
  private final String errorDescription;
  private final String errorExplanation;

  private OAuth2Error(final String errorCode) {
    this.errorCode = errorCode;
    String header = OAuth2Request.class.getName() + " encountered a problem: ";
    String _errorDescription = errorCode;
    String _errorExplanation = errorCode;

    FilteredLogger LOG = null;
    try {
      LOG = FilteredLogger.getFilteredLogger(OAuth2Error.class.getName());
      final ResourceBundle resourceBundle = LOG.getResourceBundle();
      if (resourceBundle != null) {
        final String bundleHeader = resourceBundle.getString(OAuth2Error.MESSAGE_HEADER);
        if (bundleHeader != null) {
          header = MessageFormat.format(bundleHeader, OAuth2Request.class.getName());
        }

        final String bundleErrorDescription = resourceBundle.getString(this.errorCode);
        if ((bundleErrorDescription == null) || (bundleErrorDescription.length() == 0)) {
          _errorDescription = header + this.errorCode;
        } else {
          _errorDescription = header + bundleErrorDescription;
        }

        final String bundleErrorExplanation = resourceBundle.getString(this.errorCode
            + ".explanation");
        if ((bundleErrorExplanation == null) || (bundleErrorExplanation.length() == 0)) {
          _errorExplanation = _errorDescription;
        } else {
          _errorExplanation = bundleErrorExplanation;
        }
      }
    } catch (final Exception e) {
      if (LOG != null) {
        if (LOG.isLoggable()) {
          LOG.log("error loading OAuth2Error messages", e);
        }
      } else {
        e.printStackTrace();
      }
    }

    this.errorDescription = _errorDescription;
    this.errorExplanation = _errorExplanation;
  }

  public String getErrorCode() {
    return this.errorCode;
  }

  public String getErrorDescription(final Object... objects) {
    return MessageFormat.format(this.errorDescription, objects);
  }

  public String getErrorExplanation() {
    return this.errorExplanation;
  }
}