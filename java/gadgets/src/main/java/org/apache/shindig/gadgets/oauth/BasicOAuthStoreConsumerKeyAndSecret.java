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

/**
 * @author beaton@google.com (Your Name Here)
 *
 */
public class BasicOAuthStoreConsumerKeyAndSecret {

  public static enum KeyType { HMAC_SYMMETRIC, RSA_PRIVATE }

  /** Value for oauth_consumer_key */
  private final String consumerKey;

  /** HMAC secret, or RSA private key, depending on keyType */
  private final String consumerSecret;

  /** Type of key */
  private final KeyType keyType;

  /** Name of public key to use with xoauth_public_key parameter.  May be null */
  private final String keyName;

  public BasicOAuthStoreConsumerKeyAndSecret(String key, String secret, KeyType type, String name) {
    consumerKey = key;
    consumerSecret = secret;
    keyType = type;
    keyName = name;
  }
  
  public String getConsumerKey() {
    return consumerKey;
  }
  
  public String getConsumerSecret() {
    return consumerSecret;
  }
  
  public KeyType getKeyType() {
    return keyType;
  }
  
  public String getKeyName() {
    return keyName;
  }
}
