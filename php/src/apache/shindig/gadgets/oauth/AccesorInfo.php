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

class AccesorInfo {
  /**
   * @var OAuthAccessor
   */
  public $accessor;
  public $httpMethod;
  public $signatureType;
  public $paramLocation;

  public function getParamLocation() {
    return $this->paramLocation;
  }

  public function setParamLocation($paramLocation) {
    $this->paramLocation = $paramLocation;
  }

  /**
   * @return OAuthAccessor
   */
  public function getAccessor() {
    return $this->accessor;
  }

  public function setAccessor($accessor) {
    $this->accessor = $accessor;
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
