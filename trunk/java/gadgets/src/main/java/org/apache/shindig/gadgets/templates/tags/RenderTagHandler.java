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

import org.apache.shindig.gadgets.templates.TemplateProcessor;
import org.apache.shindig.gadgets.templates.ElementELResolver.ElementWrapper;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

/**
 * Tag Handler for <os:Render/> tag.
 */
public class RenderTagHandler extends AbstractTagHandler {

  public static final String DEFAULT_NAME = "Render";

  public static final String ATTR_CONTENT = "content";

  @Inject
  public RenderTagHandler() {
    super(TagHandler.OPENSOCIAL_NAMESPACE, DEFAULT_NAME);
  }

  public void process(Node result, Element tag, TemplateProcessor processor) {
    Map<String, Object> myMap = processor.getTemplateContext().getMy();
    if (myMap == null) {
      return;
    }

    String content = tag.getAttribute(ATTR_CONTENT);
    // No @content specified - move it all.
    if ("".equals(content)) {
      Node root = processor.getTemplateContext().getTemplateRoot();
      if (root != null) {
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
          result.appendChild(child.cloneNode(true));
        }
      }
    } else {
      Object value = myMap.get(content);
      // TODO: for non-Elements, output errors
      if (value instanceof ElementWrapper) {
        copyChildren((ElementWrapper) value, result);
      } else if (value instanceof List<?>) {
        List<?> children = (List<?>) value;
        for (Object probablyAnElement : children) {
          if (probablyAnElement instanceof ElementWrapper) {
            copyChildren((ElementWrapper) probablyAnElement, result);
          }
        }
      }
    }
  }

  private void copyChildren(ElementWrapper fromWrapper, Node to) {
    Element from = fromWrapper.element;
    for (Node child = from.getFirstChild(); child != null; child = child.getNextSibling()) {
      to.appendChild(child.cloneNode(true));
    }
  }
}
