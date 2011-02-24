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

import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

public class AddJsLoadCallbackProcessor implements JsProcessor {
  
  @VisibleForTesting
  static final String JSL_CALLBACK_JS = "(function(){" +
      "var j=window['___jsl'];" +
      "if(j['c']&&--j['o']<=0){"+
      "j['c']();" +
      "delete j['c'];" +
      "delete j['o'];" +      
      "}" +
      "})();";

  public boolean process(JsRequest jsRequest, JsResponseBuilder builder) {
    JsUri jsUri = jsRequest.getJsUri();
    if (!jsUri.isNohint()) {
      builder.addJsCode(JSL_CALLBACK_JS);
    }
    return true;
  }

}
