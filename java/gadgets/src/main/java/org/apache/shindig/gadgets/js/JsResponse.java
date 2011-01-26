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

package org.apache.shindig.gadgets.js;


/**
 * An immutable object that contains the response for a JavaScript request.
 */
public class JsResponse {
 
  String jsCode;
  int cacheTtlSecs;
  int statusCode;
  boolean proxyCacheable;
  
  public JsResponse(String jsCode, int statusCode, int cachingPolicy, boolean proxyCacheable) {
    this.jsCode = jsCode;
    this.statusCode = statusCode;
    this.cacheTtlSecs = cachingPolicy;
    this.proxyCacheable = proxyCacheable;
  }
  
  /**
   * Returns the JavaScript code to serve.
   */
  public String getJsCode() {
    return jsCode;
  }
  
  /**
   * Returns the HTTP status code.
   */
  public int getStatusCode() {
    return statusCode;
  }
  
  /**
   * Returns whether the current response code is an error code.
   */
  public boolean isError() {
    return statusCode >= 400;
  }
  
  /**
   * Returns the cache TTL in seconds for this response.
   * 
   * 0 seconds means "no cache"; a value below 0 means "cache forever".
   */
  public int getCacheTtlSecs() {
    return cacheTtlSecs;
  }
  
  /**
   * Returns whether the response can be cached by intermediary proxies.
   */
  public boolean isProxyCacheable() {
    return proxyCacheable;
  }
}
