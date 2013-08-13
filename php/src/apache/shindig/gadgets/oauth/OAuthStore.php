<?php
namespace apache\shindig\gadgets\oauth;

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

interface OAuthStore {

  public function setOAuthConsumerKeyAndSecret($providerKey, $keyAndSecret);

  public function setTokenAndSecret($tokenKey, $tokenInfo);

  public function removeTokenAndSecret($tokenKey);

  /**
   * Retrieve an OAuthAccessor that is ready to sign OAuthMessages for
   * resource access.
   * @param TokenKey $tokenKey a structure uniquely identifying the token: a userId,
   *                 a gadgetId, a moduleId (in case there are more than one
   *                 gadget of the same type on a page), a tokenName (which
   *                 distinguishes this token from others that the same gadget
   *                 might hold for the same service provider) and a serviceName
   *                 (which is the same as the service name in the ProviderKey
   *                 structure).
   * @param ProviderInfo $provInfo provider information. The store combines information stored
   *                 in the store (consumer key/secret, token, token secret,
   *                 etc.) with the provider information (access URL, request
   *                 URL etc.) passed in here to create an AccessorInfo object.
   *                 If no information can be found in the
   *                 store, it may use default keys that identify the container,
   *                 as opposed to consumer keys and secrets that are specific
   *                 to this gadget.
   * @return an OAuthAccessor object than can be passed to an OAuthMessage.sign
   *         method.
   */
  public function getOAuthAccessorTokenKey(TokenKey $tokenKey, ProviderInfo $provInfo);

  public function getOAuthAccessorProviderKey(ProviderKey $providerKey, ProviderInfo $provInfo);

}
