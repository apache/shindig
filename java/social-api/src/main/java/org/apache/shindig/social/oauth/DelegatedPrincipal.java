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
package org.apache.shindig.social.oauth;

import java.security.Principal;

/**
 * The principal class that is provided by OAuthServletFilter. Servlets can use
 * HttpServletRequest.getUserPrincipal() and downcast the return value to
 * DelegatedPrincipal in order to obtain an object representing the
 * authenticated principal.
 *
 * A DelegatedPrincipal will only have a subset of the privileges compared to
 * the privileges held by the original user.
 *
 * TODO: Implement appropriate methods that deal with the privileges held by
 *       this principal.
 */
public abstract class DelegatedPrincipal implements Principal {

  public String getName() {
    return new StringBuilder()
        .append("DelegatedPrincipal[delegator: ")
        .append(getDelegator())
        .append(", delegatee: ")
        .append(getDelegatee())
        .append("]")
        .toString();
  }

  /**
   * Returns the user id, in a format native to the container, of the user that
   * delegated some authority to this principal
   */
  public abstract String getDelegator();

  /**
   * Returns the name of the entity to which the user delegated some authority.
   */
  public abstract String getDelegatee();
}
