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

import net.oauth.OAuthAccessor;

import org.apache.shindig.gadgets.oauth.OAuthStore.ConsumerInfo;

/**
 * OAuth related data accessor
 */
public class AccessorInfo {

  public static enum HttpMethod {
    GET,
    POST
  }

  public static enum OAuthParamLocation {
    AUTH_HEADER,
    POST_BODY,
    URI_QUERY
  }

  private final OAuthAccessor accessor;
  private final ConsumerInfo consumer;
  private final HttpMethod httpMethod;
  private final OAuthParamLocation paramLocation;
  private String sessionHandle;
  private long tokenExpireMillis;

  public AccessorInfo(OAuthAccessor accessor, ConsumerInfo consumer, HttpMethod httpMethod,
      OAuthParamLocation paramLocation, String sessionHandle, long tokenExpireMillis) {
    this.accessor = accessor;
    this.consumer = consumer;
    this.httpMethod = httpMethod;
    this.paramLocation = paramLocation;
    this.sessionHandle = sessionHandle;
    this.tokenExpireMillis = tokenExpireMillis;
  }

  public OAuthParamLocation getParamLocation() {
    return paramLocation;
  }

  public OAuthAccessor getAccessor() {
    return accessor;
  }

  public ConsumerInfo getConsumer() {
    return consumer;
  }

  public HttpMethod getHttpMethod() {
    return httpMethod;
  }

  public String getSessionHandle() {
    return sessionHandle;
  }

  public void setSessionHandle(String sessionHandle) {
    this.sessionHandle = sessionHandle;
  }

  public long getTokenExpireMillis() {
    return tokenExpireMillis;
  }

  public void setTokenExpireMillis(long tokenExpireMillis) {
    this.tokenExpireMillis = tokenExpireMillis;
  }
}
