<?php
namespace apache\shindig\gadgets\oauth;
use apache\shindig\common\BlobCrypterException;

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
 * Handles state passed on the OAuth callback URL.
 */
class OAuthCallbackState {
  public  static $CALLBACK_STATE_MAX_AGE_SECS = 600;
  private static $REAL_CALLBACK_URL_KEY = "u";

  /**
   *
   * @var BlobCrypter
   */
  private $crypter;

  /**
   *
   * @var array
   */
  private $state = array();

  /**
   *
   * @param BlobCrypter $crypter
   * @param string $stateBlob
   */
  public function __construct($crypter, $stateBlob = null) {
    $this->crypter = $crypter;
    if ($stateBlob != null) {
      try {
        $state = $crypter->unwrap($stateBlob, self::$CALLBACK_STATE_MAX_AGE_SECS);
      } catch (BlobCrypterException $e) {
        // Probably too old, pretend we never saw it at all.
      }
      if ($state != null) {
        $this->state = $state;
      }
    }
    return;
  }

  /**
   *
   * @return string
   */
  public function getEncryptedState() {
    return $this->crypter->wrap($this->state);
  }

  /**
   *
   * @return string
   */
  public function getRealCallbackUrl() {
    if (isset($this->state[self::$REAL_CALLBACK_URL_KEY])) {
      return $this->state[self::$REAL_CALLBACK_URL_KEY];
    } else {
      return false;
    }
  }

  /**
   *
   * @param string $callbackUrl
   */
  public function setRealCallbackUrl($callbackUrl) {
    $this->state[self::$REAL_CALLBACK_URL_KEY] = $callbackUrl;
  }
}
