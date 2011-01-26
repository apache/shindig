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

import com.google.common.annotations.VisibleForTesting;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

/**
 * Adds code to call a function after the Javascript code has been interpreted.
 */
public class AddOnloadFunctionProcessor implements JsProcessor {

  @VisibleForTesting
  public static final String ONLOAD_FUNCTION_NAME_ERROR = "Invalid onload callback specified";

  @VisibleForTesting
  public static final String ONLOAD_JS_TPL = "(function() {" +
      "var nm='%s';" +
      "if (typeof window[nm]==='function') {" +
      "window[nm]();" +
      '}' +
      "})();";

  private static final Pattern ONLOAD_FN_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

  public boolean process(JsRequest request, JsResponseBuilder builder)
      throws JsException {
    // Add onload handler to add callback function.
    String onloadStr = request.getJsUri().getOnload();
    if (onloadStr != null) {
      if (!ONLOAD_FN_PATTERN.matcher(onloadStr).matches()) {
        throw new JsException(HttpServletResponse.SC_BAD_REQUEST, ONLOAD_FUNCTION_NAME_ERROR);
      }
      builder.addJsCode(createOnloadScript(onloadStr));
    }
    return true;
  }
  
  
  @VisibleForTesting
  String createOnloadScript(String function) {
    return String.format(ONLOAD_JS_TPL, StringEscapeUtils.escapeJavaScript(function));
  }

}
