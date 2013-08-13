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

import com.google.common.annotations.VisibleForTesting;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

/**
 * Adds code to call an onload function after the Javascript code has been interpreted.
 * The onload function can be injected in one of two ways:
 * 1. Via the &onload query parameter. This is used for "first-stage" JS when the method
 *    is directly injected.
 * 2. Via ___jsl.c variable. This is set by "loader" JS which loads highly-cached
 *    2nd-stage code that does not have onload injected.
 */
public class AddOnloadFunctionProcessor implements JsProcessor {
  private static final String ONLOAD_CODE_ID = "[onload-processor]";
  private static final String JSL_CODE_ID = "[jsload-callback]";

  @VisibleForTesting
  public static final String ONLOAD_FUNCTION_NAME_ERROR = "Invalid onload callback specified";

  @VisibleForTesting
  public static final String ONLOAD_JS_TPL = "(function() {" +
      "var nm='%s';" +
      "if (typeof window[nm]==='function') {" +
      "window[nm]();" +
      '}' +
      "})();";

  @VisibleForTesting
  static final String JSL_CALLBACK_JS = "(function(){" +
      "var j=window['___jsl'];" +
      "if(j['c']&&--j['o']<=0){"+
      "j['c']();" +
      "delete j['c'];" +
      "delete j['o'];" +
      '}' +
      "})();";

  private static final Pattern ONLOAD_FN_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

  public boolean process(JsRequest request, JsResponseBuilder builder)
      throws JsException {
    JsUri jsUri = request.getJsUri();

    // Add onload handler to add callback function.
    String onloadStr = jsUri.getOnload();
    if (onloadStr != null) {
      if (!ONLOAD_FN_PATTERN.matcher(onloadStr).matches()) {
        throw new JsException(HttpServletResponse.SC_BAD_REQUEST, ONLOAD_FUNCTION_NAME_ERROR);
      }
      builder.appendJs(createOnloadScript(onloadStr), ONLOAD_CODE_ID);
    } else if (jsUri.isNohint()) {
      // "Second-stage" JS, which may have had a callback set by loader.
      // This type of JS doesn't create a hint, but does attempt to use one.
      builder.appendJs(JSL_CALLBACK_JS, JSL_CODE_ID, true);
    }
    return true;
  }

  @VisibleForTesting
  protected String createOnloadScript(String function) {
    return String.format(ONLOAD_JS_TPL, StringEscapeUtils.escapeEcmaScript(function));
  }

}
