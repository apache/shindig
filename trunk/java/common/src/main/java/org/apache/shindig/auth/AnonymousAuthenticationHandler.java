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
import com.google.inject.name.Named;

import javax.servlet.http.HttpServletRequest;

/**
 * Handled Anonymous Authentication, including returning an "anonymous" security token.
 */
public class AnonymousAuthenticationHandler implements AuthenticationHandler {
  public static final String ALLOW_UNAUTHENTICATED = "shindig.allowUnauthenticated";
  private final boolean allowUnauthenticated;

  @Inject
  public AnonymousAuthenticationHandler(@Named(ALLOW_UNAUTHENTICATED)
      boolean allowUnauthenticated) {
    this.allowUnauthenticated = allowUnauthenticated;
  }

  public String getName() {
    return AuthenticationMode.UNAUTHENTICATED.name();
  }

  public SecurityToken getSecurityTokenFromRequest(HttpServletRequest request) {
    if (allowUnauthenticated) {
      return new AnonymousSecurityToken();
    }
    return null;
  }

  public String getWWWAuthenticateHeader(String realm) {
    return null;
  }
}
