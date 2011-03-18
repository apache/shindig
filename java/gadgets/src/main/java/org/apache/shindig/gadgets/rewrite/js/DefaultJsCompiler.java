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
package org.apache.shindig.gadgets.rewrite.js;

import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.js.JsContent;
import org.apache.shindig.gadgets.js.JsResponse;
import org.apache.shindig.gadgets.js.JsResponseBuilder;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Base for a JsCompiler implementation.
 */
public class DefaultJsCompiler implements JsCompiler {

  public Iterable<JsContent> getJsContent(JsUri jsUri, FeatureBundle bundle) {
    List<JsContent> jsContent = Lists.newLinkedList();
    for (FeatureResource resource : bundle.getResources()) {
      String content = getFeatureContent(jsUri, resource);
      content = (content != null) ? content : "";
      if (resource.isExternal()) {
        // Support external/type=url feature serving through document.write()
        jsContent.add(new JsContent("document.write('<script src=\"" + content + "\"></script>')",
            "[external:" + content + "]", bundle.getName()));
      } else {
        jsContent.add(new JsContent(content, resource.getName(), bundle.getName()));
      }
      jsContent.add(new JsContent(";\n", "[separator]", bundle.getName()));
    }
    return jsContent;
  }

  public JsResponse compile(JsUri jsUri, String content, List<String> externs) {
    return new JsResponseBuilder().appendJs(content, "[passthrough]").build();
  }

  protected String getFeatureContent(JsUri jsUri, FeatureResource resource) {
    return jsUri.isDebug() ? resource.getDebugContent() : resource.getContent();
  }
}
