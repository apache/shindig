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

// Could probably gain something by making this more granular.
/**
 * Thrown by OAuth2 request routines.
 *
 */
public class OAuth2RequestException extends Exception {
  private static final long serialVersionUID = 7670892831898874835L;

  /**
   * Error code for the client.
   */
  private final OAuth2Error error;
  private final String errorUri;
  private final String errorDescription;

  /**
   * Error text for the client.
   */
  private final String errorText;

  /**
   * Create an exception and record information about the exception to be returned to the gadget.
   *
   * @param error
   *          {@link OAuth2Error} for this error
   * @param errorText
   *          String to help elaborate on the cause of this error
   * @param cause
   *          {@link Throwable} optional root cause of the error
   */
  public OAuth2RequestException(final OAuth2Error error, final String errorText,
          final Throwable cause) {
    this(error, errorText, cause, "", "");
  }

  /**
   * Create an exception and record information about the exception to be returned to the gadget.
   *
   * @param error
   *          {@link OAuth2Error} for this error
   * @param errorText
   *          String to help elaborate on the cause of this error
   * @param cause
   *          {@link Throwable} optional root cause of the error
   * @param errorUri
   *          optional errorUri from the OAuth2 spec
   * @param errorDescription
   *          optionally provide more details about the error
   */
  public OAuth2RequestException(final OAuth2Error error, final String errorText,
          final Throwable cause, final String errorUri, final String errorDescription) {
    super('[' + error.name() + ',' + String.format(error.toString(), errorText) + ']', cause);
    this.error = error;
    this.errorText = error.getErrorDescription(errorText);
    this.errorUri = errorUri;
    this.errorDescription = errorDescription;
  }

  /**
   * Get the error code
   *
   * @return the {@link OAuth2Error}, never <code>null</code>
   */
  public OAuth2Error getError() {
    return this.error;
  }

  /**
   * Get a description of the exception
   *
   * @return, the error text never <code>null</code>
   */
  public String getErrorText() {
    return this.errorText;
  }

  @Override
  public String getMessage() {
    return this.errorText;
  }

  /**
   * Returns the errorUri, if it was provided by the OAuth2 service provider
   *
   * @return the errorUri, or "" or <code>null</code>
   */
  public String getErrorUri() {
    return this.errorUri;
  }

  /**
   * Returns the more meaningful description of the error
   *
   * @return the errorDescription, or "" or <code>null</code>
   */
  public String getErrorDescription() {
    return this.errorDescription;
  }

  @Override
  public String toString() {
    return '[' + this.error.toString() + ',' + this.errorText + ']';
  }
}
