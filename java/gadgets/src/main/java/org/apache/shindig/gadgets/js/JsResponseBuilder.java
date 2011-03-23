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

import com.google.common.collect.Lists;

import java.util.LinkedList;

import java.util.List;

/**
 * A class with methods to create {@link JsResponse} objects.
 */
public class JsResponseBuilder {
  private LinkedList<JsContent> jsCode;
  private final List<String> errors;
  private int statusCode;
  private int cacheTtlSecs;
  private boolean proxyCacheable;
  private String externs;

  public JsResponseBuilder() {
    jsCode = Lists.newLinkedList();
    statusCode = HttpServletResponse.SC_OK;
    cacheTtlSecs = 0;
    proxyCacheable = false;
    errors = Lists.newLinkedList();
    externs = null;
  }

  public JsResponseBuilder(JsResponse response) {
    jsCode = Lists.newLinkedList(response.getAllJsContent());
    errors = Lists.newLinkedList(response.getErrors());
    statusCode = response.getStatusCode();
    cacheTtlSecs = response.getCacheTtlSecs();
    proxyCacheable = response.isProxyCacheable();
    externs = response.getExterns();
  }

  /**
   * Appends more JS to the response.
   */
  public JsResponseBuilder appendJs(JsContent jsContent) {
    jsCode.add(jsContent);
    return this;
  }

  /**
   * Helper to append JS to the response w/ a name.
   */
  public JsResponseBuilder appendJs(String content, String name) {
    return appendJs(new JsContent(content, name));
  }

  /**
   * Helper to append a bunch of JS.
   */
  public JsResponseBuilder appendJs(Iterable<JsContent> jsBundle) {
    for (JsContent content : jsBundle) {
      appendJs(content);
    }
    return this;
  }

  /**
   * Prepends JS to the output.
   */
  public JsResponseBuilder prependJs(String content, String name) {
    jsCode.addFirst(new JsContent(content, name));
    return this;
  }

  /**
   * Replaces the current JavaScript code with some new code.
   */
  public JsResponseBuilder setJs(String newContent, String name) {
    return clearJs().appendJs(newContent, name);
  }

  /**
   * Deletes all JavaScript code in the builder.
   */
  public JsResponseBuilder clearJs() {
    this.jsCode = Lists.newLinkedList();
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
   * Adds an error to the response
   */
  public JsResponseBuilder addError(String error) {
    this.errors.add(error);
    return this;
  }

  /**
   * Adds multiple errors to the response
   */
  public JsResponseBuilder addErrors(List<String> errs) {
    this.errors.addAll(errs);
    return this;
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
   * Sets whether the compiled externs.
   */
  public JsResponseBuilder setExterns(String externs) {
    this.externs = externs;
    return this;
  }

  /**
   * Builds a {@link JsResponse} object with the provided data.
   */
  public JsResponse build() {
    return new JsResponse(jsCode, statusCode, cacheTtlSecs, proxyCacheable,
        errors, externs);
  }
}