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

class TokenKey {
  private $userId;
  private $gadgetUri;
  private $moduleId;
  private $tokenName;
  private $serviceName;
  private $appId;

  public function getAppId() {
    return $this->appId;
  }

  public function setAppId($appId) {
    $this->appId = $appId;
  }

  public function getUserId() {
    return $this->userId;
  }

  public function setUserId($userId) {
    $this->userId = $userId;
  }

  public function getGadgetUri() {
    return $this->gadgetUri;
  }

  public function setGadgetUri($gadgetUri) {
    $this->gadgetUri = $gadgetUri;
  }

  public function getModuleId() {
    return $this->moduleId;
  }

  public function setModuleId($moduleId) {
    $this->moduleId = $moduleId;
  }

  public function getTokenName() {
    return $this->tokenName;
  }

  public function setTokenName($tokenName) {
    $this->tokenName = $tokenName;
  }

  public function getServiceName() {
    return $this->serviceName;
  }

  public function setServiceName($serviceName) {
    $this->serviceName = $serviceName;
  }
}
