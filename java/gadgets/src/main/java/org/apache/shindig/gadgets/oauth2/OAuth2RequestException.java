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

  /**
   * Error text for the client.
   */
  private final String errorText;

  /**
   * Create an exception and record information about the exception to be
   * returned to the gadget.
   *
   * @param error
   * @param errorText
   * @param cause
   */
  public OAuth2RequestException(final OAuth2Error error, final String errorText,
      final Throwable cause) {
    super('[' + error.name() + ',' + String.format(error.toString(), errorText) + ']', cause);
    this.error = error;
    this.errorText = error.getErrorDescription(errorText);
  }

  /**
   * Get the error code
   *
   * @return
   */
  public OAuth2Error getError() {
    return this.error;
  }

  /**
   * Get a meaningful description of the exception
   *
   * @return
   */
  public String getErrorText() {
    return this.errorText;
  }

  @Override
  public String getMessage() {
    return this.errorText;
  }

  @Override
  public String toString() {
    return '[' + this.error.toString() + ',' + this.errorText + ']';
  }
}
