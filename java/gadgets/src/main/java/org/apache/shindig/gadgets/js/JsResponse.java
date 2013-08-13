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
package org.apache.shindig.gadgets.js;

import java.util.Collections;
import java.util.List;

/**
 * An immutable object that contains the response for a JavaScript request.
 * This object is used by JsHandler, JsProcessors, and JsCompiler alike.
 */
public class JsResponse {
  private final List<JsContent> jsCode;
  private final List<String> errors;
  private final String externs;
  private final int cacheTtlSecs;
  private final int statusCode;
  private final boolean proxyCacheable;
  private String codeString;
  private String errorString;

  JsResponse(List<JsContent> jsCode, int statusCode, int cacheTtlSecs,
      boolean proxyCacheable, List<String> errors, String externs) {
    this.jsCode = Collections.unmodifiableList(jsCode);
    this.errors = Collections.unmodifiableList(errors);
    this.statusCode = statusCode;
    this.cacheTtlSecs = cacheTtlSecs;
    this.proxyCacheable = proxyCacheable;
    this.externs = externs;
  }

  /**
   * Returns the JavaScript code to serve.
   */
  public String toJsString() {
    if (codeString == null) {
      StringBuilder sb = new StringBuilder();
      for (JsContent js : getAllJsContent()) {
        sb.append(js.get());
      }
      codeString = sb.toString();
    }
    return codeString;
  }

  /**
   * Returns an iterator starting at the beginning of all JS code in the response.
   */
  public Iterable<JsContent> getAllJsContent() {
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

  /**
   * Returns a list of any error messages associated with this response.
   */
  public List<String> getErrors() {
    return errors;
  }

  /**
   * Returns a string of all error messages associated with this response.
   */
  public String toErrorString() {
    if (errorString == null) {
      StringBuilder sb = new StringBuilder();
      for (String error : getErrors()) {
        sb.append(error);
      }
      errorString = sb.toString();
    }
    return errorString;
  }

  /**
   * Returns a string of generated externs.
   */
  public String getExterns() {
    return externs;
  }
}
