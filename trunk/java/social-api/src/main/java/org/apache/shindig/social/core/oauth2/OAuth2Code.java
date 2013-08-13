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
package org.apache.shindig.social.core.oauth2;

import java.util.List;

import org.apache.shindig.social.core.oauth2.OAuth2Types.CodeType;

/**
 * Represents a "code" string in an OAuth 2.0 handshake, including authorization
 * code, access token, or refresh token. These signatures may all expire. They
 * may also be associated with a redirect_url and/or another code.
 */
public class OAuth2Code implements Comparable<OAuth2Code> {

  private String value;
  private String redirectURI;
  private long expiration;
  private List<String> scope; // TODO (Eric): should be a string, interpret as a list during validation
  private OAuth2Client client;
  private OAuth2Code relatedAuthCode;
  private OAuth2Code relatedRefreshToken;
  private OAuth2Code relatedAccessToken;
  private CodeType type;

  public OAuth2Code() {

  }

  /**
   * Constructs an OAuth2Code.
   *
   * @param value is the String key that makes up the code
   * @param redirectURI is redirect URI associated with this code
   * @param expiration indicates when this code expires
   * @param scope indicates the scope of this code
   */
  public OAuth2Code(String value, String redirectURI, long expiration,
      List<String> scope) {
    this.value = value;
    this.redirectURI = redirectURI;
    this.expiration = expiration;
    this.scope = scope;
  }

  /**
   * Constructs an OAuth2Code with a value.
   *
   * @param value is the String key that makes up the code
   */
  public OAuth2Code(String value) {
    this.value = value;
  }

  /**
   * Returns the value of this code.
   *
   * @return String is the key of this code
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the value of this code.
   *
   * @param value is the value to set this code to
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Returns the redirect URI associated with this code.
   *
   * @return String represents this code's redirect URI
   */
  public String getRedirectURI() {
    return redirectURI;
  }

  /**
   * Sets the redirect URI associated with this code.
   *
   * @param redirectURI represents the redirect URI of this code
   */
  public void setRedirectURI(String redirectURI) {
    this.redirectURI = redirectURI;
  }

  /**
   * Returns when this code expires.
   *
   * @return long represents when this code will expire
   */
  public long getExpiration() {
    return expiration;
  }

  /**
   * Sets the expiration of this code.
   *
   * @param expiration is when this code will expire
   */
  public void setExpiration(long expiration) {
    this.expiration = expiration;
  }

  /**
   * Compares this code to another code.
   *
   * @return int indicates how the value of this code compares to another
   */
  public int compareTo(OAuth2Code target) {
    return value.compareTo(target.getValue());
  }

  /**
   * Returns the scope of this code.
   *
   * @return List<String> represents the scope of this code
   */
  public List<String> getScope() {
    return scope;
  }

  /**
   * Sets the scope of this code.
   *
   * @param scope is this code's authorized scope
   */
  public void setScope(List<String> scope) {
    this.scope = scope;
  }

  /**
   * Sets the client associated with this code.
   *
   * @param client is the client to associate with this code
   */
  public void setClient(OAuth2Client client) {
    this.client = client;
  }

  /**
   * Returns the client associated with this code.
   *
   * @return OAuth2Client represents the client associated with this code
   */
  public OAuth2Client getClient() {
    return client;
  }

  /**
   * Sets the type of this code; one of
   *  AUTHORIZATION_CODE,
   *  ACCESS_TOKEN,
   *  REFRESH_TOKEN
   *
   * @param type is this code's type
   */
  public void setType(CodeType type) {
    this.type = type;
  }

  /**
   * Returns the type of this code.
   *
   * @return CodeType represents the type of this code
   */
  public CodeType getType() {
    return type;
  }

  /**
   * Sets the authorization code that this code is related to, if applicable.
   *
   * @param code is the authorization code to associate with this code
   */
  public void setRelatedAuthCode(OAuth2Code code) {
    this.relatedAuthCode = code;
  }

  /**
   * Returns the authorization code related to this code.
   *
   * @return OAuth2Code is the authorization code related to this code
   */
  public OAuth2Code getRelatedAuthCode() {
    return relatedAuthCode;
  }

  /**
   * Sets the related refresh token.
   *
   * @param relatedRefreshToken is the refresh token related to this code
   */
  public void setRelatedRefreshToken(OAuth2Code relatedRefreshToken) {
    this.relatedRefreshToken = relatedRefreshToken;
  }

  /**
   * Gets the related refresh token.
   *
   * @return OAuth2Code is the refresh token related to this code
   */
  public OAuth2Code getRelatedRefreshToken() {
    return relatedRefreshToken;
  }

  /**
   * Sets the related access token.
   *
   * @param relatedAccessToken is the access token related to this code
   */
  public void setRelatedAccessToken(OAuth2Code relatedAccessToken) {
    this.relatedAccessToken = relatedAccessToken;
  }

  /**
   * Gets the related access token.
   *
   * @return OAuth2Code is the access token related to this code
   */
  public OAuth2Code getRelatedAccessToken() {
    return relatedAccessToken;
  }
}
