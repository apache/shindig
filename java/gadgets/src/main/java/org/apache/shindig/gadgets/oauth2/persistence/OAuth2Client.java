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

import com.google.inject.Inject;

import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Data class for client data stored in persistence.
 *
 * Uses the injected {@link OAuth2Encrypter} protect the client_secret in the persistence store.
 *
 */
public class OAuth2Client implements Serializable {
  private static final long serialVersionUID = -7374658882342619184L;
  private boolean allowModuleOverride;
  private boolean authorizationHeader;
  private String authorizationUrl;
  private String clientAuthenticationType;
  private String clientId;
  private byte[] clientSecret;
  private byte[] encryptedSecret;
  private final transient OAuth2Encrypter encrypter;
  private String gadgetUri;
  private String grantType = OAuth2Message.NO_GRANT_TYPE;
  private String redirectUri;
  private String serviceName;
  private String tokenUrl;
  private OAuth2Accessor.Type type = OAuth2Accessor.Type.UNKNOWN;
  private boolean urlParameter;
  private boolean sharedToken = false;
  private String[] allowedDomains = new String[] {};

  public OAuth2Client() {
    this(null);
  }

  @Inject
  public OAuth2Client(final OAuth2Encrypter encrypter) {
    this.encrypter = encrypter;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof OAuth2Client)) {
      return false;
    }
    final OAuth2Client other = (OAuth2Client) obj;
    if (this.gadgetUri == null) {
      if (other.gadgetUri != null) {
        return false;
      }
    } else if (!this.gadgetUri.equals(other.gadgetUri)) {
      return false;
    }
    if (this.serviceName == null) {
      if (other.serviceName != null) {
        return false;
      }
    } else if (!this.serviceName.equals(other.serviceName)) {
      return false;
    }
    if (this.sharedToken != other.sharedToken) {
      return false;
    }
    return true;
  }

  /**
   * Returns authorization endpoint
   *
   * @return authorization endpoint
   */
  public String getAuthorizationUrl() {
    return this.authorizationUrl;
  }

  /**
   * Returns client authentication type
   *
   * @return client authentication type
   */
  public String getClientAuthenticationType() {
    return this.clientAuthenticationType;
  }

  /**
   * Returns client id.
   *
   * @return client id
   */
  public String getClientId() {
    return this.clientId;
  }

  /**
   * Returns client secret
   *
   * @return client secret
   */
  public byte[] getClientSecret() {
    return this.clientSecret;
  }

  /**
   * Returns encrypted secret
   *
   * @return encrypted secret
   */
  public byte[] getEncryptedSecret() {
    return this.encryptedSecret;
  }

  public OAuth2Encrypter getEncrypter() {
    return this.encrypter;
  }

  public String getGadgetUri() {
    return this.gadgetUri;
  }

  public String getGrantType() {
    return this.grantType;
  }

  public String getRedirectUri() {
    return this.redirectUri;
  }

  public String getServiceName() {
    return this.serviceName;
  }

  public String getTokenUrl() {
    return this.tokenUrl;
  }

  public OAuth2Accessor.Type getType() {
    return this.type;
  }

  @Override
  public int hashCode() {
    if (this.serviceName != null && this.gadgetUri != null) {
      return (this.serviceName + ':' + this.gadgetUri).hashCode();
    }

    return 0;
  }

  public boolean isAllowModuleOverride() {
    return this.allowModuleOverride;
  }

  public boolean isAuthorizationHeader() {
    return this.authorizationHeader;
  }

  public boolean isSharedToken() {
    return this.sharedToken;
  }

  public boolean isUrlParameter() {
    return this.urlParameter;
  }

  public void setAllowModuleOverride(final boolean alllowModuleOverride) {
    this.allowModuleOverride = alllowModuleOverride;
  }

  public void setAuthorizationHeader(final boolean authorizationHeader) {
    this.authorizationHeader = authorizationHeader;
  }

  public void setAuthorizationUrl(final String authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
  }

  public void setClientAuthenticationType(final String clientAuthenticationType) {
    this.clientAuthenticationType = clientAuthenticationType;
  }

  public void setClientId(final String clientId) {
    this.clientId = clientId;
  }

  public void setClientSecret(final byte[] secret) throws OAuth2EncryptionException {
    this.clientSecret = secret;
    if (this.encrypter != null) {
      this.encryptedSecret = this.encrypter.encrypt(secret);
    }
  }

  public void setEncryptedSecret(final byte[] encryptedSecret) throws OAuth2EncryptionException {
    this.encryptedSecret = encryptedSecret;
    if (this.encrypter != null) {
      this.clientSecret = this.encrypter.decrypt(encryptedSecret);
    }
  }

  public void setGadgetUri(final String gadgetUri) {
    this.gadgetUri = gadgetUri;
  }

  public void setGrantType(final String grantType) {
    this.grantType = grantType;
  }

  public void setRedirectUri(final String redirectUri) {
    this.redirectUri = redirectUri;
  }

  public void setServiceName(final String serviceName) {
    this.serviceName = serviceName;
  }

  public void setSharedToken(final boolean sharedToken) {
    this.sharedToken = sharedToken;
  }

  public void setTokenUrl(final String tokenUrl) {
    this.tokenUrl = tokenUrl;
  }

  public void setType(final OAuth2Accessor.Type type) {
    this.type = type;
  }

  public void setUrlParameter(final boolean urlParameter) {
    this.urlParameter = urlParameter;
  }

  /**
   * sets the domains of allowed resource servers
   *
   * @param allowedDomains
   */
  public void setAllowedDomains(final String[] allowedDomains) {
    this.allowedDomains = allowedDomains;
  }

  /**
   * gets the domains of allowed resource servers
   *
   * @return allowed domains
   */
  public String[] getAllowedDomains() {
    return this.allowedDomains;
  }

  @Override
  public String toString() {
    return "org.apache.shindig.gadgets.oauth2.persistence.sample.OAuth2ClientImpl: serviceName = "
            + this.serviceName + " , redirectUri = " + this.redirectUri + " , gadgetUri = "
            + this.gadgetUri + " , clientId = " + this.clientId + " , grantType = "
            + this.grantType + " , type = " + this.type.name() + " , grantType = " + this.grantType
            + " , tokenUrl = " + this.tokenUrl + " , authorizationUrl = " + this.authorizationUrl
            + " , this.clientAuthenticationType = " + this.clientAuthenticationType
            + " , this.sharedToken = " + this.sharedToken + ", this.allowedDomains = "
            + Arrays.asList(this.allowedDomains);
  }

  @Override
  public OAuth2Client clone() {
    final OAuth2Client ret = new OAuth2Client(this.encrypter);
    ret.setAllowModuleOverride(this.allowModuleOverride);
    ret.setAuthorizationHeader(this.authorizationHeader);
    ret.setAuthorizationUrl(this.authorizationUrl);
    ret.setClientAuthenticationType(this.clientAuthenticationType);
    ret.setClientId(this.clientId);
    try {
      ret.setClientSecret(this.clientSecret);
    } catch (final OAuth2EncryptionException e) {
      // no op
    }
    ret.setGadgetUri(this.gadgetUri);
    ret.setGrantType(this.grantType);
    ret.setRedirectUri(this.redirectUri);
    ret.setServiceName(this.serviceName);
    ret.setSharedToken(this.sharedToken);
    ret.setTokenUrl(this.tokenUrl);
    ret.setType(this.type);
    ret.setUrlParameter(this.urlParameter);
    ret.setAllowedDomains(this.getAllowedDomains());

    return ret;
  }
}
