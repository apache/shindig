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
package org.apache.shindig.gadgets;

public enum JsCompileMode {
  // Concats all build-time compile-version JS.
  COMPILE_CONCAT("0"),
  // Performs run-time compilation of concatenated built-time debug-version JS.
  // All symbols exported (and as long as in transitive dependency) are
  // retained/unbofuscated.
  CONCAT_COMPILE_EXPORT_ALL("1"),
  // Like run CONCAT_COMPILE_EXPORT_ALL, except the only retained/unobfuscated
  // symbols are ones exported from the explicitly-requested features, ie: if
  // feature=foo depends on feature=bar, /gadgets/js/foo will expose foo.*
  // exported APIs, not bar.* exported APIs. This can potentially eliminate all
  // un-used code.
  CONCAT_COMPILE_EXPORT_EXPLICIT("2");

  private final String paramValue;

  private JsCompileMode(String paramValue) {
    this.paramValue = paramValue;
  }

  public String getParamValue() {
    return paramValue;
  }

  public static JsCompileMode valueOfParam(String param) {
    for (JsCompileMode mode : JsCompileMode.values()) {
      String modeParam = mode.getParamValue();
      if (modeParam.equals(param)) {
        return mode;
      }
    }
    return getDefault();
  }

  public static JsCompileMode getDefault() {
    return JsCompileMode.COMPILE_CONCAT;
  }

}
