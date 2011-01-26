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

import javax.servlet.http.HttpServletResponse;

/**
 * A class with methods to create {@link JsResponse} objects.
 */
public class JsResponseBuilder {

  private StringBuilder jsCode;
  private int statusCode;
  private int cacheTtlSecs;
  private boolean proxyCacheable;
  
  public JsResponseBuilder() {
    jsCode = new StringBuilder();
    statusCode = HttpServletResponse.SC_OK;
    cacheTtlSecs = 0;
    proxyCacheable = false;
  }

  public JsResponseBuilder(JsResponse response) {
    jsCode = new StringBuilder(response.jsCode);
    statusCode = response.statusCode;
    cacheTtlSecs = response.cacheTtlSecs;
    proxyCacheable = response.proxyCacheable;
  }
  
  /**
   * Returns a StringBuilder to modify the current JavaScript code.
   */
  public StringBuilder getJsCode() {
    return jsCode;
  }
  
  /**
   * Replaces the current JavaScript code with some new code.
   */
  public JsResponseBuilder setJsCode(CharSequence code) {
    this.jsCode = new StringBuilder(code);
    return this;
  }

  /**
   * Deletes all JavaScript code in the builder.
   */
  public JsResponseBuilder clearJsCode() {
    jsCode = new StringBuilder();
    return this;
  }

  /**
   * Appends some JavaScript code to the end of the current code.  
   */
  public JsResponseBuilder addJsCode(CharSequence data) {
    jsCode.append(data);
    return this;
  }

  /**
   * Sets the HTTP status code.
   */
  public JsResponseBuilder setStatusCode(int responseCode) {
    this.statusCode = responseCode;
    return this;
  }
  
  /**
   * Returns the HTTP status code.
   */
  public int getStatusCode() {
    return statusCode;
  }
  
  /**
   * Sets the cache TTL in seconds for the response being built.
   * 
   * 0 seconds means "no cache"; a value below 0 means "cache forever".
   */
  public JsResponseBuilder setCacheTtlSecs(int cacheTtlSecs) {
    this.cacheTtlSecs = cacheTtlSecs;
    return this;
  }
  
  /**
   * Returns the cache TTL in seconds for the response.
   */
  public int getCacheTtlSecs() {
    return cacheTtlSecs;
  }

  /**
   * Sets whether the response can be cached by intermediary proxies.
   */
  public JsResponseBuilder setProxyCacheable(boolean proxyCacheable) {
    this.proxyCacheable = proxyCacheable;
    return this;
  }
  
  /**
   * Returns whether the response can be cached by intermediary proxies.
   */
  public boolean isProxyCacheable() {
    return proxyCacheable;
  }

  /**
   * Builds a {@link JsResponse} object with the provided data.
   */
  public JsResponse build() {
    return new JsResponse(jsCode.toString(), statusCode, cacheTtlSecs, proxyCacheable);
  }
}