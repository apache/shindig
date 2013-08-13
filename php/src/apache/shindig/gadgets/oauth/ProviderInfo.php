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

class ProviderInfo {
  private $provider;
  private $httpMethod;
  private $signatureType;
  private $paramLocation;

  // this can be null if we have not negotiated a consumer key and secret
  // yet with the provider, or if we decided that we want to use a global
  // public key
  private $keyAndSecret;

  public function getParamLocation() {
    return $this->paramLocation;
  }

  public function setParamLocation($paramLocation) {
    $this->paramLocation = $paramLocation;
  }

  public function getKeyAndSecret() {
    return $this->keyAndSecret;
  }

  public function setKeyAndSecret($keyAndSecret) {
    $this->keyAndSecret = $keyAndSecret;
  }

  public function getProvider() {
    return $this->provider;
  }

  public function setProvider(OAuthServiceProvider $provider) {
    $this->provider = $provider;
  }

  public function getHttpMethod() {
    return $this->httpMethod;
  }

  public function setHttpMethod($httpMethod) {
    $this->httpMethod = $httpMethod;
  }

  public function getSignatureType() {
    return $this->signatureType;
  }

  public function setSignatureType($signatureType) {
    $this->signatureType = $signatureType;
  }
}