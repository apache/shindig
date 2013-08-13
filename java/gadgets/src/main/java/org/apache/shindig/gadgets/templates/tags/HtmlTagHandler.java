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
package org.apache.shindig.gadgets.templates.tags;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.HtmlSerialization;
import org.apache.shindig.gadgets.templates.TemplateProcessor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;

import com.google.inject.Inject;

/**
 * A TagHandler for the &lt;os:Html code="..."/&gt; tag.
 * The value of the @code attribute will be treated as HTML markup.
 */
public class HtmlTagHandler extends AbstractTagHandler {

  static final String TAG_NAME = "Html";
  static final String ATTR_CODE = "code";

  private final GadgetHtmlParser parser;

  @Inject
  public HtmlTagHandler(GadgetHtmlParser parser) {
    super(TagHandler.OPENSOCIAL_NAMESPACE, TAG_NAME);
    this.parser = parser;
  }

  public void process(Node result, Element tag, TemplateProcessor processor) {
    String code = getValueFromTag(tag, ATTR_CODE, processor, String.class);
    if ((code == null) || "".equals(code)) {
      return;
    }

    try {
      parser.parseFragment(code, result);
    } catch (GadgetException ge) {
      try {
        StringBuilder sb = new StringBuilder("Error: ");
        HtmlSerialization.printEscapedText(ge.getMessage(), sb);
        Node comment = result.getOwnerDocument().createComment(sb.toString());
        result.appendChild(comment);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
