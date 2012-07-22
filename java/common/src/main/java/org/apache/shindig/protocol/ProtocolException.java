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
package org.apache.shindig.protocol;

import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Preconditions;

/**
 * Unchecked exception class for errors thrown by request handlers
 */
public class ProtocolException extends RuntimeException {
  private final int errorCode;

  /**
   * The application specific response value associated with this exception.
   */
  private final Object response;

  public ProtocolException(int errorCode, String errorMessage, Throwable cause) {
    super(errorMessage, cause);
    checkErrorCode(errorCode);
    this.errorCode = errorCode;
    this.response = null;
  }

  public ProtocolException(int errorCode, String errorMessage) {
    this(errorCode, errorMessage, null);
  }

  public ProtocolException(int errorCode, String errorMessage, Object response) {
    super(errorMessage);
    checkErrorCode(errorCode);
    this.errorCode = errorCode;
    this.response = response;
  }

  public int getCode() {
    return errorCode;
  }

  public Object getResponse() {
    return response;
  }

  private void checkErrorCode(int code) {
    // 200 is not a legit use of ProtocolExceptions.
    Preconditions.checkArgument(code != HttpServletResponse.SC_OK,
        "May not use OK error code with ProtocolException");
  }
}
