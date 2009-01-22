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
package org.apache.shindig.auth;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Implements a specific authentication mechanism and produces a SecurityToken when authentication
 * is successful.
 */
public interface AuthenticationHandler {

  /**
   * @return The name of the authentication handler, used for debugging.
   */
  String getName();

  /**
   * Produce a security token extracted from the HTTP request.
   *
   * @param request The request to extract a token from.
   * @return A valid security token for the request, or null if it wasn't possible to authenticate.
   */
  SecurityToken getSecurityTokenFromRequest(HttpServletRequest request);

    /**
     * Return a String to be used for a WWW-Authenticate header. This will be called if the
     * call to getSecurityTokenFromRequest returns null.
     *
     * If non-null/non-blank it will be added to the Response.
     * See Section 6.1.3 of the Portable Contacts Specification
     *
     * @param realm the name of the realm to use for the authenticate header
     * @return Header value for a WWW-Authenticate Header
     */
  String getWWWAuthenticateHeader(String realm);

  /**
   * An exception thrown by an AuthenticationHandler in the situation where
   * a malformed credential or token is passed. A handler which throws this exception
   * is required to include the appropriate error state in the servlet response
   */
  public static final class InvalidAuthenticationException extends RuntimeException {

     private Map<String,String> additionalHeaders;
     private String redirect;

     /**
      * @param message Message to output in error response
      * @param cause Underlying exception
      */
     public InvalidAuthenticationException(String message, Throwable cause) {
       this(message, cause, null, null);
     }

     /**
      * @param message Message to output in error response
      * @param cause Underlying exception
      * @param additionalHeaders Headers to add to error response
      * @param redirect URL to redirect to on error
      */
     public InvalidAuthenticationException(String message, Throwable cause,
         Map<String,String> additionalHeaders, String redirect) {
       super(message, cause);
       this.additionalHeaders = additionalHeaders;
       this.redirect = redirect;
     }

     public Map<String, String> getAdditionalHeaders() {
       return additionalHeaders;
     }

     public String getRedirect() {
       return redirect;
     }
   }
}
