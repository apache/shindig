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

import com.google.common.base.Preconditions;

/**
 * Thrown by OAuth request routines.
 * @since 2.0.0
 */
public class OAuthRequestException extends Exception {

  /**
   * Error code for the client.
   */
  private String error;

  /**
   * Error text for the client.
   */
  private String errorText;


  /**
   * Create an exception and record information about the exception to be returned to the gadget.
   * @param error
   */
  public OAuthRequestException (OAuthError error) {
    this(error.name(), error.toString());
  }


  /**
   * Create an exception and record information about the exception to be returned to the gadget.
   * @param error
   * @param errorText
   */
  public OAuthRequestException (OAuthError error, String errorText) {
    this(error.name(), String.format(error.toString(), errorText));
  }

  /**
   * Create an exception and record information about the exception to be returned to the gadget.
   * @param error
   * @param errorText
   * @param cause
   */
  public OAuthRequestException(OAuthError error, String errorText, Throwable cause) {
    this(error.name(), String.format(error.toString(), errorText), cause);
  }


  /**
   * Create an exception and record information about the exception to be returned to the gadget.
   * @param error
   * @param errorText
   */
  public OAuthRequestException(String error, String errorText) {
    super('[' + error + ',' + errorText + ']');
    this.error = Preconditions.checkNotNull(error);
    this.errorText = Preconditions.checkNotNull(errorText);
  }


  /**
   * Create an exception and record information about the exception to be returned to the gadget.
   * @param error
   * @param errorText
   * @param cause
   */
  public OAuthRequestException(String error, String errorText, Throwable cause) {
    super('[' + error + ',' + errorText + ']', cause);
    this.error = Preconditions.checkNotNull(error);
    this.errorText = Preconditions.checkNotNull(errorText);
  }

  /**
   * Create an exception and record information about the exception to be returned to the gadget.
   * @param message
   */
  public OAuthRequestException(String message) {
    super(message);
  }


  /**
   * Create an exception and record information about the exception to be returned to the gadget.
   * @param message
   * @param cause
   */
  public OAuthRequestException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Get the error code
   * @return
   */
  public String getError() {
    return error;
  }

  /**
   * Get a meaningful description of the exception
   * @return
   */
  public String getErrorText() {
    return errorText;
  }

  @Override
  public String getMessage() {
    return errorText;
  }

  @Override
  public String toString() {
    return '[' + error + ',' + errorText + ']';
  }
}
