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

import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;

/**
 * Interface describing a service that can be used to look up 
 * {@link DelegatedPrincipal}s associated with OAuth tokens.
 */
public interface OAuthTokenPrincipalMapper {

  /**
   * Returns the user for which the access token <code>accessToken</code> had
   * been issued.
   *
   * @param message the OAuth message (including, among other things, the oauth
   *        token and consumer key needed to map to the principal represented
   *        by the token). Other information in the OAuth message may influence
   *        the privileges granted to the principal.
   * @return the Principal that represents the (possibly partial) delegation of
   *         authority.
   * @throws OAuthProblemException when the accessToken was not issued for the
   *         consumer identified in the <code>message</code>.
   * @throws PrincipalMapperException when there was a problem retrieving the
   *         user associated with the principal.
   */
  OAuthPrincipal getPrincipalForToken(OAuthMessage message)
      throws OAuthProblemException, PrincipalMapperException;
}
