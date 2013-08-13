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

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

/**
 * A class with methods to create {@link JsResponse} objects.
 */
public class JsResponseBuilder {
  private static final String EXTERN_DELIM = ";\n";

  private List<JsContent> jsCode;
  private final List<String> errors;
  private int statusCode;
  private int cacheTtlSecs;
  private boolean proxyCacheable;
  private final StringBuilder rawExterns;
  private final List<String> externs;

  public JsResponseBuilder() {
    jsCode = Lists.newLinkedList();
    statusCode = HttpServletResponse.SC_OK;
    cacheTtlSecs = 0;
    proxyCacheable = false;
    errors = Lists.newLinkedList();
    rawExterns = new StringBuilder();
    externs = Lists.newLinkedList();
  }

  public JsResponseBuilder(JsResponse response) {
    this();
    if (response.getAllJsContent() != null) {
      jsCode.addAll(Lists.newArrayList(response.getAllJsContent()));
    }
    if (response.getErrors() != null) {
      errors.addAll(Lists.newArrayList(response.getErrors()));
    }
    if (response.getExterns() != null) {
      rawExterns.append(response.getExterns());
    }
    statusCode = response.getStatusCode();
    cacheTtlSecs = response.getCacheTtlSecs();
    proxyCacheable = response.isProxyCacheable();
  }

  /**
   * Prepend a JS to the response.
   */
  public JsResponseBuilder prependJs(JsContent jsContent) {
    if (canAddContent(jsContent)) {
      jsCode.add(0, jsContent);
    }
    return this;
  }

  /**
   * Prepends JS to the output.
   */
  public JsResponseBuilder prependJs(String content, String name) {
    return prependJs(JsContent.fromText(content, name));
  }

  /**
   * Prepends JS to the output.
   */
  public JsResponseBuilder prependJs(String content, String name, boolean noCompile) {
    return prependJs(JsContent.fromText(content, name, noCompile));
  }

  /**
   * Insert a JS at a specific index.
   */
  public JsResponseBuilder insertJsAt(int index, JsContent jsContent) {
    if (canAddContent(jsContent)) {
      jsCode.add(index, jsContent);
    }
    return this;
  }

  /**
   * Appends more JS to the response.
   */
  public JsResponseBuilder appendJs(JsContent jsContent) {
    if (canAddContent(jsContent)) {
      jsCode.add(jsContent);
    }
    return this;
  }

  /**
   * Helper to append JS to the response w/ a name.
   */
  public JsResponseBuilder appendJs(String content, String name) {
    return appendJs(JsContent.fromText(content, name));
  }

  /**
   * Helper to append JS to the response w/ a name.
   */
  public JsResponseBuilder appendJs(String content, String name, boolean noCompile) {
    return appendJs(JsContent.fromText(content, name, noCompile));
  }

  /**
   * Helper to append a bunch of JS.
   */
  public JsResponseBuilder appendAllJs(Iterable<JsContent> jsBundle) {
    for (JsContent content : jsBundle) {
      appendJs(content);
    }
    return this;
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
   * Appends a blob of raw extern.
   */
  public JsResponseBuilder appendRawExtern(String rawExtern) {
    this.rawExterns.append(rawExtern).append(EXTERN_DELIM);
    return this;
  }

  /**
   * Appends a line of extern.
   */
  public JsResponseBuilder appendExtern(String extern) {
    this.externs.add(extern);
    return this;
  }

  /**
   * Appends externs as from list of strings.
   */
  public JsResponseBuilder appendExterns(List<String> externs) {
    for (String extern : externs) {
      appendExtern(extern);
    }
    return this;
  }

  /**
   * Deletes all externs in the builder.
   */
  public JsResponseBuilder clearExterns() {
    int last = rawExterns.length();
    this.rawExterns.delete(0, last);
    this.externs.clear();
    return this;
  }

  /**
   * Builds a {@link JsResponse} object with the provided data.
   */
  public JsResponse build() {
    return new JsResponse(jsCode, statusCode, cacheTtlSecs, proxyCacheable,
        errors, rawExterns + buildExternString());
  }

  private String buildExternString() {
    StringBuilder builder = new StringBuilder();
    Set<String> set = Sets.newHashSet();
    for (String ext : externs) {
      List<String> expandedList = expand(ext);
      for (String exp : expandedList) {
        if (set.contains(exp)) continue;
        if (exp.endsWith(".prototype")) continue;
        if (!exp.contains(".")) builder.append("var ");
        builder.append(exp).append(" = {}").append(EXTERN_DELIM);
        set.add(exp);
      }
    }
    return builder.toString();
  }

  private List<String> expand(String value) {
    List<String> result = Lists.newArrayList();
    StringBuilder cur = new StringBuilder();
    for (String part : Splitter.on('.').split(value)) {
      cur.append(cur.length() > 0 ? "." : "").append(part);
      result.add(cur.toString());
    }
    return result;
  }

  private boolean canAddContent(JsContent jsContent) {
    return jsContent.get().length() > 0;
  }
}
