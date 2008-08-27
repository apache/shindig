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
package org.apache.shindig.social.core.oauth;


import org.apache.shindig.common.SecurityToken;

import javax.servlet.http.HttpServletRequest;

/**
 * Class to get/set authorization information on a servlet request.
 * Used by auth filters.
 */
public class AuthInfo {
  
  /**
   * Constants for request attribute keys
   */
  enum Attribute {
    /** The security token */
    SECURITY_TOKEN,
    /** The named auth type */
    AUTH_TYPE;

    public String getId() {
      return Attribute.class.getName() + "." + this.name();
    }
  }

  /**
   * Get the security token for this request.
   *
   * @param req The request
   * @return The security token
   */
  public static SecurityToken getSecurityToken(HttpServletRequest req) {
    return getRequestAttribute(req, Attribute.SECURITY_TOKEN);
  }

  /**
   * Get the hosted domain for this request.
   *
   * @param req The request
   * @return The domain, or {@code null} if no domain was found
   */
  public static String getAuthType(HttpServletRequest req) {
    return getRequestAttribute(req, Attribute.AUTH_TYPE);
  }

  /**
   * Set the security token for the request.
   *
   * @param req The request
   * @param token The security token
   */
  public static void setSecurityToken(HttpServletRequest req, SecurityToken token) {
    setRequestAttribute(req, Attribute.SECURITY_TOKEN, token);
  }

  /**
   * Set the auth type for the request.
   *
   * @param req The request
   * @param authType The named auth type
   */
  public static void setAuthType(HttpServletRequest req, String authType) {
    setRequestAttribute(req, Attribute.AUTH_TYPE, authType);
  }

  /**
   * Set a standard request attribute.
   *
   * @param req The request
   * @param att The attribute
   * @param value The value
   */
  private static<T> void setRequestAttribute(HttpServletRequest req,
      Attribute att, T value) {
    req.setAttribute(att.getId(), value);
  }

  /**
   * Get a standard attribute
   *
   * @param req The request
   * @param att The attribute
   * @return The value
   */
  @SuppressWarnings("unchecked")
  private static<T> T getRequestAttribute(HttpServletRequest req,
      Attribute att) {
    return (T)req.getAttribute(att.getId());
  }
}
