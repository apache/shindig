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
package org.apache.shindig.auth;

import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;

/**
 * Wrapper class for AuthInfoUtil to provide injection with in request scope.
 */
public class AuthInfo {
  private final HttpServletRequest req;

  /**
   * Create AuthInfo from a given HttpServletRequest
   * @param req
   */
  @Inject
  public AuthInfo(HttpServletRequest req) {
    this.req = req;
  }

  /** Export attribute names in current name space */
  public static class Attribute {
    public static final AuthInfoUtil.Attribute SECURITY_TOKEN =
        AuthInfoUtil.Attribute.SECURITY_TOKEN;
    public static final AuthInfoUtil.Attribute AUTH_TYPE =
        AuthInfoUtil.Attribute.AUTH_TYPE;
  }

  /**
   * Get the security token for this request.
   *
   * @return The security token
   */
  public SecurityToken getSecurityToken() {
    return AuthInfoUtil.getSecurityTokenFromRequest(req);
  }

  /**
   * Get the hosted domain for this request.
   *
   * @return The domain, or {@code null} if no domain was found
   */
  public String getAuthType() {
    return AuthInfoUtil.getAuthTypeFromRequest(req);
  }
}
