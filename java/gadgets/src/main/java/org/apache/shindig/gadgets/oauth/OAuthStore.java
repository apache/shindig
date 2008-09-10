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
package org.apache.shindig.gadgets.oauth;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.gadgets.GadgetException;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;

/**
 * Interface to an OAuth Data Store. A shindig gadget server can act as an
 * OAuth consumer, using OAuth tokens to talk to OAuth service providers on
 * behalf of the gadgets it is proxying requests for. An OAuth consumer needs
 * to permanently store gadgets it has collected, and retrieve the
 * appropriate tokens when proxying a request for a gadget.
 *
 * An OAuth Data Store stores three things:
 *  (1) information about OAuth service providers, including the three
 *      URLs that define OAuth providers (defined in OAuthStore.ProviderInfo)
 *  (2) information about consumer keys and secrets that gadgets might have
 *      negotiated with OAuth service providers, or that containers might have
 *      negotiated on behalf of the gadgets. This information is bound to
 *      the service provider it pertains to and can only be stored if the
 *      corresponding service provider information is already stored in the
 *      OAuth store (defined in OAuthStore.ConsumerKeyAndSecret).
 *  (3) OAuth access tokens and their corresponding token secrets. (defined
 *      in OAuthStore.TokenInfo).
 *
 *  Note that we do not store request tokens in the OAuth store.
 */
public interface OAuthStore {

  /**
   * Information about an OAuth consumer.
   */
  public static class ConsumerInfo {
    private final OAuthConsumer consumer;
    private final String keyName;
    
    /**
     * @param consumer the OAuth consumer
     * @param keyName the name of the key to use for this consumer (passed on query parameters to
     * help with key rotation.)
     */
    public ConsumerInfo(OAuthConsumer consumer, String keyName) {
      this.consumer = consumer;
      this.keyName = keyName;
    }
    
    public OAuthConsumer getConsumer() {
      return consumer;
    }
    
    public String getKeyName() {
      return keyName;
    }
  }
  
  /**
   * Information about an access token.
   */
  public static class TokenInfo {
    private final String accessToken;
    private final String tokenSecret;
    public TokenInfo(String token, String secret) {
      accessToken = token;
      tokenSecret = secret;
    }
    public String getAccessToken() {
      return accessToken;
    }
    public String getTokenSecret() {
      return tokenSecret;
    }
  }

  /**
   * Retrieve OAuth consumer to use for requests.  The returned consumer is ready to use for signed
   * fetch requests.
   * 
   * @param securityToken token for user/gadget making request.
   * @param serviceName gadget's nickname for the service being accessed.
   * @param provider OAuth service provider info to be inserted into the returned consumer.
   * 
   * @throws GadgetException if no OAuth consumer can be found (e.g. no consumer key can be used.)
   */
  public ConsumerInfo getConsumerKeyAndSecret(SecurityToken securityToken, String serviceName,
      OAuthServiceProvider provider) throws GadgetException;

  /**
   * Retrieve OAuth access token to use for the request.
   * @param securityToken token for user/gadget making request.
   * @param consumerInfo OAuth consumer that will be used for the request.
   * @param serviceName gadget's nickname for the service being accessed.
   * @param tokenName gadget's nickname for the token to use.
   * @return the token and secret, or null if none exist
   * @throws GadgetException if an error occurs during lookup
   */
  public TokenInfo getTokenInfo(SecurityToken securityToken, ConsumerInfo consumerInfo,
      String serviceName, String tokenName) throws GadgetException;

  /**
   * Set the access token for the given user/gadget/service/token
   */
  public void setTokenInfo(SecurityToken securityToken, ConsumerInfo consumerInfo, String serviceName,
      String tokenName, TokenInfo tokenInfo) throws GadgetException;

  /**
   * Remove the access token for the given user/gadget/service/token
   */
  public void removeToken(SecurityToken securityToken, ConsumerInfo consumerInfo,
      String serviceName, String tokenName) throws GadgetException;
}
