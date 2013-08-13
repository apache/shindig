<?php
namespace apache\shindig\gadgets\oauth;
use apache\shindig\common\sample\BasicBlobCrypter;
use apache\shindig\gadgets\sample\BasicGadgetSpecFactory;
use apache\shindig\common\Config;
use apache\shindig\common\RemoteContentRequest;
use apache\shindig\common\RemoteContentFetcher;
use apache\shindig\common\SecurityToken;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Produces OAuth content fetchers for input tokens.
 */
class OAuthFetcherFactory {

  /**
   * used to encrypt state stored on the client
   * @var BlobCrypter
   */
  protected $oauthCrypter;

  /**
   * persistent storage for OAuth tokens
   * @var OAuthStore
   */
  protected $tokenStore;

  /**
   *
   * @param SigningFetcher $fetcher
   * @param BlobCrypter $oauthCrypter
   * @param OAuthStore $tokenStore
   */
  public function __construct($fetcher = null, $oauthCrypter = null, $tokenStore = null) {
    if (isset($oauthCrypter) && isset($tokenStore)) {
      $this->OAuthFetcherFactoryCreate($oauthCrypter, $tokenStore);
    } elseif (isset($fetcher)) {
      $this->OAuthFetcherFactoryInit($fetcher);
    } else {
      throw new \Exception('Wrong number of parameters in the OAuthFetcherFactory constuct');
    }
  }

  /**
   * Initialize the OAuth factory with a default implementation of
   * BlobCrypter and consumer keys/secrets read from oauth.js
   *
   * @param SigningFetcher $fetcher
   */
  public function OAuthFetcherFactoryInit($fetcher) {
    try {
      $BBC = new BasicBlobCrypter();
      $this->oauthCrypter = new BasicBlobCrypter(srand($BBC->MASTER_KEY_MIN_LEN));
      $specFactory = new BasicGadgetSpecFactory();
      $OAuthStore = Config::get('oauth_store');
      $gadgetOAuthTokenStore = Config::get('gadget_oauth_token_store');
      $basicStore = new $gadgetOAuthTokenStore(new $OAuthStore, $specFactory);
      $basicStore->initFromConfigFile($fetcher);
      $this->tokenStore = $basicStore;
    } catch (\Exeption $e) {}
  }

  /**
   * Creates an OAuthFetcherFactory based on prepared crypter and token store.
   *
   * @param $oauthCrypter used to wrap client side state
   * @param OAuthStore $tokenStore used as interface to persistent token store.
   */
  protected function OAuthFetcherFactoryCreate($oauthCrypter, $tokenStore) {
    $this->oauthCrypter = $oauthCrypter;
    $this->tokenStore = $tokenStore;
  }

  /**
   * Produces an OAuthFetcher that will sign requests and delegate actual
   * network retrieval to the {@code fetcher}
   *
   * @param RemoteContentFetcher $fetcher The fetcher that will fetch real content
   * @param SecurityToken $token The gadget token used to identity the user and gadget
   * @param OAuthRequestParams $params The parsed parameters the gadget requested
   * @param string $authType the oauth auth type to use, either "oauth" or "oauth2"
   * @return OAuthFetcher
   * @throws GadgetException
   */
  public function getOAuthFetcher(RemoteContentFetcher $fetcher, SecurityToken $token, OAuthRequestParams $params, $authType) {
    switch ($authType) {
      case RemoteContentRequest::$AUTH_OAUTH:
    return new OAuthFetcher($this->tokenStore, $this->oauthCrypter, $fetcher, $token, $params);
        break;
      case RemoteContentRequest::$AUTH_OAUTH2:
        return new OAuth2Fetcher($this->tokenStore, $this->oauthCrypter, $fetcher, $token, $params);
        break;
    }
    throw new \Exception('invalid oauth authType ' . $authType);
  }
}
