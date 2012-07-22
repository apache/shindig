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
package org.apache.shindig.gadgets.oauth2.persistence.sample;

import java.io.Serializable;

/**
 * Representation of OAuth2 endpoints and other relevant endpoint data.
 *
 */
public class OAuth2Provider implements Serializable {
  private static final long serialVersionUID = -6539761759797255778L;

  private boolean authorizationHeader = true;
  private String authorizationUrl;
  private String clientAuthenticationType;
  private String name;
  private String tokenUrl;
  private boolean urlParameter = false;

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof OAuth2Provider)) {
      return false;
    }
    final OAuth2Provider other = (OAuth2Provider) obj;
    if (this.name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!this.name.equals(other.name)) {
      return false;
    }

    return true;
  }

  public String getAuthorizationUrl() {
    return this.authorizationUrl;
  }

  public String getClientAuthenticationType() {
    return this.clientAuthenticationType;
  }

  public String getName() {
    return this.name;
  }

  public String getTokenUrl() {
    return this.tokenUrl;
  }

  @Override
  public int hashCode() {
    if (this.name != null) {
      return this.name.hashCode();
    }

    return 0;
  }

  public boolean isAuthorizationHeader() {
    return this.authorizationHeader;
  }

  public boolean isUrlParameter() {
    return this.urlParameter;
  }

  public void setAuthorizationHeader(boolean authorizationHeader) {
    this.authorizationHeader = authorizationHeader;
  }

  public void setAuthorizationUrl(final String authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
  }

  public void setClientAuthenticationType(final String clientAuthenticationType) {
    this.clientAuthenticationType = clientAuthenticationType;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public void setTokenUrl(final String tokenUrl) {
    this.tokenUrl = tokenUrl;
  }

  public void setUrlParameter(boolean urlParameter) {
    this.urlParameter = urlParameter;
  }

  @Override
  public String toString() {
    return "org.apache.shindig.gadgets.oauth2.persistence.sample.OAuth2Provider: name = "
        + this.name + " , authorizationUrl = " + this.authorizationUrl + " , tokenUrl = "
        + this.tokenUrl + " , clientAuthenticationType = " + this.clientAuthenticationType;
  }
}
