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
package org.apache.shindig.gadgets.oauth;

import com.google.common.collect.Maps;

import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypterException;

import java.util.Map;

/**
 * Handles state passed on the OAuth callback URL.
 *
 * TODO: there's probably an abstract superclass that can be reused by OAuthClientState and this
 * class.
 */
public class OAuthCallbackState {

  private final BlobCrypter crypter;
  private OAuthCallbackStateToken state;

  public OAuthCallbackState(BlobCrypter crypter) {
    this.crypter = crypter;
    this.state = new OAuthCallbackStateToken();
  }

  public OAuthCallbackState(BlobCrypter crypter, String stateBlob) {
    this.crypter = crypter;
    Map<String, String> state = null;
    if (stateBlob != null) {
      try {
        state = crypter.unwrap(stateBlob);
        if (state == null) {
          state = Maps.newHashMap();
        }
        this.state = new OAuthCallbackStateToken(state);
        this.state.enforceNotExpired();
      } catch (BlobCrypterException e) {
        // Too old, or corrupt.  Ignore it.
        state = null;
      }
    }
    if (state == null) {
      this.state = new OAuthCallbackStateToken();
    }
  }

  public String getEncryptedState() throws BlobCrypterException {
    return crypter.wrap(state.toMap());
  }

  public String getRealCallbackUrl() {
    return state.getRealCallbackUrl();
  }

  public void setRealCallbackUrl(String realCallbackUrl) {
    state.setRealCallbackUrl(realCallbackUrl);
  }
}
