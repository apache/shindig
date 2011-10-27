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
import com.google.common.collect.Sets;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import java.util.Collection;
import java.util.Set;

/**
 * Injects a global ___jsl.l variable with information about the JS request.
 *
 * Used when loading embedded JS configuration in core.config/config.js.
 */
public class AddJslLoadedVariableProcessor implements JsProcessor {
  private static final String CODE_ID = "[jsload-loaded-info]";

  @VisibleForTesting
  static final String TEMPLATE =
      "window['___jsl']['l'] = (window['___jsl']['l'] || []).concat(%s);";

  public boolean process(JsRequest jsRequest, JsResponseBuilder builder) throws JsException {
    JsUri jsUri = jsRequest.getJsUri();
    if (!jsUri.isNohint()) {
      Set<String> result = getBundleNames(jsUri, false);
      result.removeAll(getBundleNames(jsUri, true));
      String array = toArrayString(result);
      builder.appendJs(String.format(TEMPLATE, array), CODE_ID);
    }
    return true;
  }

  protected Set<String> getBundleNames(JsUri jsUri, boolean loaded) throws JsException {
    GadgetContext ctx = new JsGadgetContext(jsUri);
    Collection<String> libs = loaded ? jsUri.getLoadedLibs() : jsUri.getLibs();
    Set<String> ret = Sets.newLinkedHashSet(); // ordered set for testability.
    ret.addAll(libs);
    return ret;
  }

  private String toArrayString(Set<String> bundles) {
    StringBuilder builder = new StringBuilder();
    for (String bundle : bundles) {
      if (builder.length() > 0) builder.append(',');
      builder.append('\'').append(StringEscapeUtils.escapeJavaScript(bundle)).append('\'');
    }
    return '[' + builder.toString() + ']';
  }
}
