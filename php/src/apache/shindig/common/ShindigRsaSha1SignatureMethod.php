<?php
namespace apache\shindig\common;

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

class ShindigRsaSha1SignatureMethod extends \OAuthSignatureMethod_RSA_SHA1 {
  private $privateKey;
  private $publicKey;

  public function __construct($privateKey, $publicKey) {
    $this->privateKey = $privateKey;
    $this->publicKey = $publicKey;
  }

  protected function fetch_public_cert(&$request) {
    return $this->publicKey;
  }

  protected function fetch_private_cert(&$request) {
    return $this->privateKey;
  }
}
