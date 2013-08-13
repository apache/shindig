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

class ShindigOAuth {
  public static $VERSION_1_0 = "1.0";
  public static $ENCODING = "UTF-8";
  public static $FORM_ENCODED = "application/x-www-form-urlencoded";
  public static $OAUTH_CONSUMER_KEY = "oauth_consumer_key";
  public static $OAUTH_TOKEN = "oauth_token";
  public static $OAUTH_TOKEN_SECRET = "oauth_token_secret";
  public static $OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
  public static $OAUTH_SIGNATURE = "oauth_signature";
  public static $OAUTH_TIMESTAMP = "oauth_timestamp";
  public static $OAUTH_NONCE = "oauth_nonce";
  public static $OAUTH_VERIFIER = "oauth_verifier";
  public static $OAUTH_VERSION = "oauth_version";
  public static $HMAC_SHA1 = "HMAC_SHA1";
  public static $RSA_SHA1 = "RSA_SHA1";
  public static $BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
  public static $END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
  public static $OAUTH_PROBLEM = "oauth_problem";
}
