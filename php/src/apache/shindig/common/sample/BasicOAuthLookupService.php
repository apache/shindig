<?php
namespace apache\shindig\common\sample;
use apache\shindig\social\oauth\OAuthSecurityToken;
use apache\shindig\common\OAuthLookupService;

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
 * Basic implementation of OAuthLookupService
 */
class BasicOAuthLookupService extends OAuthLookupService {

  /**
   * {@inheritDoc}
   */
  public function getSecurityToken($oauthRequest, $appUrl, $userId, $contentType) {
    return new OAuthSecurityToken($userId, $appUrl, $this->getAppId($appUrl), "samplecontainer");
  }

  /**
   *
   * @param string $appUrl
   * @return int
   */
  private function getAppId($appUrl) {
    return 0; // a real implementation would look this up
  }
}
