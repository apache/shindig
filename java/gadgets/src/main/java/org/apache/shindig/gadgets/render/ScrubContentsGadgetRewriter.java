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
package org.apache.shindig.gadgets.render;

import com.google.inject.Inject;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.parse.caja.CajaCssSanitizer;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.gadgets.uri.ProxyUriManager;

import java.util.Set;

/**
 * When sanitize=1, performs sanitization on the raw content text repeatedly
 * until it doesn't change any more.
 */
public class ScrubContentsGadgetRewriter extends SanitizingGadgetRewriter {

  @Inject
  public ScrubContentsGadgetRewriter(@AllowedTags Set<String> allowedTags,
      @AllowedAttributes Set<String> allowedAttributes,
      ContentRewriterFeature.Factory rewriterFeatureFactory,
      CajaCssSanitizer cssSanitizer,
      ProxyUriManager proxyUriManager) {
    super(allowedTags, allowedAttributes, rewriterFeatureFactory, cssSanitizer, proxyUriManager);
  }

  @Override
  public void rewrite(Gadget gadget, MutableContent content) throws RewritingException {
    if (gadget.sanitizeOutput()) {
      String currentContent = content.getContent();
      String previousContent = null;
      while (!currentContent.equals(previousContent)) {
        previousContent = currentContent;
        MutableContent rewritten = new MutableContent(content.getContentParser(), previousContent);
        super.rewrite(gadget, rewritten);
        currentContent = rewritten.getContent();
      }
      content.setContent(currentContent);
    }
  }
}
