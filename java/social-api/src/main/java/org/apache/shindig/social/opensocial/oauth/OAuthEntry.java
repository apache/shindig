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
package org.apache.shindig.social.opensocial.oauth;

import java.io.Serializable;
import java.util.Date;

/**
 * The OAuthEntry class contains state information about OAuth Tokens and
 * Authorization.
 */
public class OAuthEntry implements Serializable {
  public static final long ONE_YEAR = 365 * 24 * 60 * 60 * 1000L;
  public static final long FIVE_MINUTES = 5 * 60 * 1000L;

  // Change this when incompatible changes occur..
  static final long serialVersionUID = 2;

  public static enum Type {
    REQUEST, ACCESS, DISABLED
  }

  public String appId;
  public String callbackUrl;
  public boolean callbackUrlSigned; // true if consumer supports OAuth 1.0a
  public String userId;
  public String token;
  public String tokenSecret;

  public boolean authorized;

  public String consumerKey;

  public Type type;
  public Date issueTime;

  public String domain;
  public String container;
  public String oauthVersion;
  
  public String callbackToken;
  public int callbackTokenAttempts;

  public OAuthEntry() {}

  /**
   * A copy constructor
   * @param old the OAuthEntry to duplicate
   */
  public OAuthEntry(OAuthEntry old) {
    this.appId = old.appId;
    this.callbackUrl = old.callbackUrl;
    this.callbackUrlSigned = old.callbackUrlSigned;
    this.userId = old.userId;
    this.token = old.token;
    this.tokenSecret= old.tokenSecret;
    this.authorized = old.authorized;
    this.consumerKey = old.consumerKey;
    this.type = old.type;
    this.issueTime = old.issueTime;
    this.domain = old.domain;
    this.container = old.container;
    this.oauthVersion = old.oauthVersion;
    this.callbackToken = old.callbackToken;
    this.callbackTokenAttempts = old.callbackTokenAttempts;
  }

  public boolean isExpired() {
    Date currentDate = new Date();
    return currentDate.compareTo(this.expiresAt()) > 0;
  }

  public Date expiresAt() {
    long expirationTime = issueTime.getTime();
    switch (type) {
      case REQUEST:
        expirationTime += FIVE_MINUTES;
        break;
      case ACCESS:
        expirationTime += ONE_YEAR;
        break;
    }

    return new Date(expirationTime);
  }
}
