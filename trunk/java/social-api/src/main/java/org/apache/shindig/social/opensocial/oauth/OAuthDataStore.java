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
package org.apache.shindig.social.opensocial.oauth;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthProblemException;

import org.apache.shindig.auth.SecurityToken;

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
   * Returning null may allow for other authentication schemes to be used.  Throwing an
   * OAuthProblemException will allows errors to be returned to the caller.
   *
   * @param consumerKey A consumer key
   * @param userId The userId to validate.
   * @return A valid Security Token
   * @throws OAuthProblemException when there are errors
   */
  SecurityToken getSecurityTokenForConsumerRequest(String consumerKey, String userId) throws OAuthProblemException;

  /**
   * Lookup consumers.  Generally this corresponds to an opensocial Application
   * but could be abstracted in other ways.  If you have multiple containers you
   * may want to include the container as part of the identifier.
   *
   * Your consumer object should have the key and secret, a link to your provider
   * plus you should consider setting properties that correspond to the metadata
   * in the opensocial app like icon, description, etc.
   *
   * Returning null will inform the client that the consumer key does not exist.  If you
   * want to control the error response throw an OAuthProblemException
   *
   * @param consumerKey A valid, non-null ConsumerKey
   * @return the consumer object corresponding to the specified key.
   * @throws OAuthProblemException when the implementing class wants to signal errors
   */
  OAuthConsumer getConsumer(String consumerKey) throws OAuthProblemException;

  /**
   * Generate a valid requestToken for the given consumerKey.
   *
   * @param consumerKey A valid consumer key
   * @param signedCallbackUrl Callback URL sent from consumer, may be null.  If callbackUrl is not
   *     null then the returned entry should have signedCallbackUrl set to true.
   * @return An OAuthEntry containing a valid request token.
   * @throws OAuthProblemException when the implementing class wants to control the error response
   */
  OAuthEntry generateRequestToken(String consumerKey, String oauthVersion, String signedCallbackUrl)
      throws OAuthProblemException;


  /**
   * Called when converting a request token to an access token.  This is called
   * in the final phase of 3-legged OAuth after the user authorizes the app.
   *
   * @param entry The Entry to convert
   * @return a new entry with type Type.ACCESS
   * @throws OAuthProblemException when the implementing class wants to control the error response
   */
  OAuthEntry convertToAccessToken(OAuthEntry entry) throws OAuthProblemException;


  /**
   * Authorize the request token for the given user id.
   *
   * @param entry A valid OAuthEntry
   * @param userId A user id
   * @throws OAuthProblemException when the implementing class wants to control the error response
   */
  void authorizeToken(OAuthEntry entry, String userId) throws OAuthProblemException;

  /**
   * Mark a token DISABLED and store it.
   *
   * @param entry A valid OAuthEntry
   */
  void disableToken(OAuthEntry entry);

  /**
   * Remove a token
   *
   * @param entry A valid OAuthEntry
   */
  void removeToken(OAuthEntry entry);

}
