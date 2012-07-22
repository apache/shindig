/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets.oauth2.persistence;


/**
 * Injected into the system to encrypt/decrypt client and token secrets in the
 * persistence layer.
 *
 * This does not apply to any broader concept of token signing or other signing
 * from the OAuth 1.0 implementation.
 *
 */
public interface OAuth2Encrypter {
  /**
   * Decrypts client and token secret
   *
   * @param encryptedSecret
   * @return decryptedSecret
   */
  public byte[] decrypt(byte[] encryptedSecret) throws OAuth2EncryptionException;

  /**
   * Encrypts client and token secret
   *
   * @param plainSecret
   * @return encryptedSecret
   */
  public byte[] encrypt(byte[] plainSecret) throws OAuth2EncryptionException;
}
