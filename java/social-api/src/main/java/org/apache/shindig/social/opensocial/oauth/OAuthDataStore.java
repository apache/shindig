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
package org.apache.shindig.social.opensocial.oauth;

import com.google.inject.ImplementedBy;
import com.google.common.base.Preconditions;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.sample.oauth.SampleOAuthDataStore;

import java.util.UUID;
import java.util.Date;

@ImplementedBy(SampleOAuthDataStore.class)

/**
 * A class that manages the OAuth data for Shindig, including
 * storing the map of consumer key/secrets, storing request and
 * access tokens, and providing a way to upgrade tokens to
 * authorized values.
 */

public interface OAuthDataStore {
  /**
   * Get the OAuthEntry that corresponds to the oauthToken.
   *
   * @param oauthToken a non-null oauthToken
   * @return a valid OAuthEntry or null if no match
   */
  OAuthEntry getEntry(String oauthToken);


  /**
   * Return the proper security token for a 2 legged oauth request that has been validated
   * for the given consumerKey. App specific checks like making sure the requested user has the
   * app installed should take place in this method.
   *
   * @param consumerKey
   * @param userId
   * @return A valid Security Token
   */
  SecurityToken getSecurityTokenForConsumerRequest(String consumerKey, String userId);

  /**
   * If the passed in consumerKey is valid, pass back the consumerSecret.
   *
   * @param consumerKey A consumer key to test.
   * @return the consumer secret for the specific consumer key.
   */
  String getConsumerSecret(String consumerKey);

  /**
   * Generate a valid requestToken for the given consumerKey.
   *
   * @param consumerKey A valid consumer key
   * @return An OAuthEntry containing a valid request token.
   */
  OAuthEntry generateRequestToken(String consumerKey);


  /**
   * Called when converting a request token to an access token.  This is called
   * in the final phase of 3-legged OAuth after the user authorizes the app.
   *
   * @param entry
   */
  OAuthEntry convertToAccessToken(OAuthEntry entry);


  /**
   * Authorize the request token for the given user id.
   *
   * @param entry A valid OAuthEntry
   * @param userId A user id
   */
  void authorizeToken(OAuthEntry entry, String userId);
}
