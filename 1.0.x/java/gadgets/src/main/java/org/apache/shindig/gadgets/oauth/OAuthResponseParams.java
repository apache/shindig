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

package org.apache.shindig.gadgets.oauth;

import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

/**
 * Container for OAuth specific data to include in the response to the client.
 */
public class OAuthResponseParams {
  
  // names for the JSON values we return to the client
  public static final String CLIENT_STATE = "oauthState";
  public static final String APPROVAL_URL = "oauthApprovalUrl";
  public static final String ERROR_CODE = "oauthError";
  public static final String ERROR_TEXT = "oauthErrorText";
  
  /**
   * Transient state we want to cache client side.
   */
  private OAuthClientState newClientState;
  
  /**
   * Authorization URL for the client.
   */
  private String aznUrl;
  
  /**
   * Error code for the client.
   */
  private OAuthError error;
  
  /**
   * Error text for the client.
   */
  private String errorText;
  
  public OAuthResponseParams(BlobCrypter stateCrypter) {
    newClientState = new OAuthClientState(stateCrypter);
  }
  
  public void addToResponse(HttpResponseBuilder response) {
    if (!newClientState.isEmpty()) {
      try {
        response.setMetadata(CLIENT_STATE, newClientState.getEncryptedState());
      } catch (BlobCrypterException e) {
        // Configuration error somewhere, this should never happen.
        throw new RuntimeException(e);
      }
    }
    if (aznUrl != null) {
      response.setMetadata(APPROVAL_URL, aznUrl);
    }
    if (error != null) {
      response.setMetadata(ERROR_CODE, error.toString());
    }
    if (errorText != null) {
      response.setMetadata(ERROR_TEXT, errorText);
    }
  }

  public OAuthClientState getNewClientState() {
    return newClientState;
  }

  public String getAznUrl() {
    return aznUrl;
  }

  public void setAznUrl(String aznUrl) {
    this.aznUrl = aznUrl;
  }

  public OAuthError getError() {
    return error;
  }

  public void setError(OAuthError error) {
    this.error = error;
  }

  public String getErrorText() {
    return errorText;
  }

  public void setErrorText(String errorText) {
    this.errorText = errorText;
  }
  
}
