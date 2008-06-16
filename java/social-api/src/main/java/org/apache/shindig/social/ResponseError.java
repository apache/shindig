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

public enum ResponseError {
  NOT_IMPLEMENTED("notImplemented", HttpServletResponse.SC_NOT_IMPLEMENTED),
  UNAUTHORIZED("unauthorized", HttpServletResponse.SC_UNAUTHORIZED),
  FORBIDDEN("forbidden", HttpServletResponse.SC_FORBIDDEN),
  BAD_REQUEST("badRequest", HttpServletResponse.SC_BAD_REQUEST),
  INTERNAL_ERROR("internalError", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

  private final String jsonValue;
  private int httpErrorCode;

  ResponseError(String jsonValue, int httpErrorCode) {
    this.jsonValue = jsonValue;
    this.httpErrorCode = httpErrorCode;
  }

  @Override
  public String toString() {
    return jsonValue;
  }

  public int getHttpErrorCode() {
    return httpErrorCode;
  }
}
