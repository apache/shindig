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
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import java.util.Collection;
import java.util.List;

/**
 * Injects a global ___jsl variable with information about the JS request.
 * 
 * Used when loading embedded JS configuration in core.config/config.js.
 */
public class InjectJsInfoVariableProcessor implements JsProcessor {
  private static final String CODE_ID = "[jsload-code-info]";

  @VisibleForTesting
  static final String HINT_TEMPLATE = 
      "window['___jsl'] = window['___jsl'] || {};" +
      "window['___jsl']['u'] = '%s';" +
      "window['___jsl']['f'] = [%s];";

  private final JsUriManager jsUriManager;

  @Inject
  public InjectJsInfoVariableProcessor(JsUriManager jsUriManager) {
    this.jsUriManager = jsUriManager;
  }

  public boolean process(JsRequest jsRequest, JsResponseBuilder builder) {
    JsUri jsUri = jsRequest.getJsUri();
    if (!jsUri.isNohint()) {
      String uri = StringEscapeUtils.escapeJavaScript(getUri(jsUri));
      String features = getFeatures(jsUri);
      builder.prependJs(String.format(HINT_TEMPLATE, uri, features), CODE_ID);
    }
    return true;
  }

  private String getUri(JsUri jsUri) {
    Uri uri = jsUri.getOrigUri();
    if (uri == null || jsUri.isJsload()) {
      JsUri auxUri = new JsUri(jsUri);
      auxUri.setJsload(false);
      if (jsUri.isJsload()) {
        auxUri.setNohint(true);
      }
      uri = jsUriManager.makeExternJsUri(auxUri);
    }
    return uri.toString();
  }

  private String getFeatures(JsUri jsUri) {
    Collection<String> libs = jsUri.getLibs();
    List<Object> features = Lists.newArrayList();
    for (String lib : libs) {
      features.add("'" + StringEscapeUtils.escapeJavaScript(lib) + "'");
    }
    return StringUtils.join(features, ",");
  }
}
