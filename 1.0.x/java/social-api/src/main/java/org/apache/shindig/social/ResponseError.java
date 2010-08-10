/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social;

import javax.servlet.http.HttpServletResponse;
/**
 * An Enumeration for holding all the responses emitted by the social API.
 */
public enum ResponseError {
  /** value representing NOT IMPLEMENTED. */
  NOT_IMPLEMENTED("notImplemented", HttpServletResponse.SC_NOT_IMPLEMENTED),
  /** value representing UNAUTHORIZED. */
  UNAUTHORIZED("unauthorized", HttpServletResponse.SC_UNAUTHORIZED),
  /** value representing FORBIDDEN. */
  FORBIDDEN("forbidden", HttpServletResponse.SC_FORBIDDEN),
  /** value representing BAD REQUEST. */
  BAD_REQUEST("badRequest", HttpServletResponse.SC_BAD_REQUEST),
  /** value representing INTERNAL SERVER ERROR. */
  INTERNAL_ERROR("internalError", HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
  /** value representing EXPECTATION FAILED. */
  LIMIT_EXCEEDED("limitExceeded", HttpServletResponse.SC_EXPECTATION_FAILED);

  /**
   * The json value of the error.
   */
  private final String jsonValue;
  /**
   * The http error code associated with the error.
   */
  private int httpErrorCode;

  /**
   * Construct a Response Error from the jsonValue as a string and the Http Error Code.
   * @param jsonValue the json String representation of the error code.
   * @param httpErrorCode the numeric HTTP error code.
   */
  ResponseError(String jsonValue, int httpErrorCode) {
    this.jsonValue = jsonValue;
    this.httpErrorCode = httpErrorCode;
  }

  /**
   *
   * Converts the ResponseError to a String representation
   */
  @Override
  public String toString() {
    return jsonValue;
  }

  /**
   * Get the HTTP error code.
   * @return the Http Error code.
   */
  public int getHttpErrorCode() {
    return httpErrorCode;
  }
}
