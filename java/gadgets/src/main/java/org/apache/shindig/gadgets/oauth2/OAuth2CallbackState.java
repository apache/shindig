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
package org.apache.shindig.gadgets.oauth2;

import com.google.common.collect.Maps;

import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.gadgets.oauth2.logger.FilteredLogger;

import java.io.Serializable;
import java.util.Map;

public class OAuth2CallbackState implements Serializable {
  private static final long serialVersionUID = 6591011719613609006L;
  private static final String LOG_CLASS = OAuth2CallbackState.class.getName();
  private static final FilteredLogger LOG = FilteredLogger
          .getFilteredLogger(OAuth2CallbackState.LOG_CLASS);

  private final transient BlobCrypter crypter;
  private OAuth2CallbackStateToken state;

  public OAuth2CallbackState() {
    this(null);
  }

  public OAuth2CallbackState(final BlobCrypter crypter) {
    this.crypter = crypter;
    this.state = new OAuth2CallbackStateToken();
  }

  public OAuth2CallbackState(final BlobCrypter crypter, final String stateBlob) {
    this.crypter = crypter;

    Map<String, String> state = null;
    if (stateBlob != null && crypter != null) {
      try {
        state = crypter.unwrap(stateBlob);

        if (state == null) {
          state = Maps.newHashMap();
        }
        this.state = new OAuth2CallbackStateToken(state);
        this.state.enforceNotExpired();
      } catch (final BlobCrypterException e) {
        // Too old, or corrupt. Ignore it.
        state = null;
        if (OAuth2CallbackState.LOG.isLoggable()) {
          OAuth2CallbackState.LOG.log("OAuth2CallbackState stateBlob decryption failed", e);
        }
      }
    }
    if (state == null) {
      this.state = new OAuth2CallbackStateToken();
    }
  }

  public String getEncryptedState() throws BlobCrypterException {
    String ret = null;
    if (this.crypter != null) {
      ret = this.crypter.wrap(this.state.toMap());
    }

    return ret;
  }

  public String getGadgetUri() {
    return this.state.getGadgetUri();
  }

  public void setGadgetUri(final String gadgetUri) {
    this.state.setGadgetUri(gadgetUri);
  }

  public String getServiceName() {
    return this.state.getServiceName();
  }

  public void setServiceName(final String serviceName) {
    this.state.setServiceName(serviceName);
  }

  public String getUser() {
    return this.state.getUser();
  }

  public void setUser(final String user) {
    this.state.setUser(user);
  }

  public String getScope() {
    return this.state.getScope();
  }

  public void setScope(final String scope) {
    this.state.setScope(scope);
  }
}
