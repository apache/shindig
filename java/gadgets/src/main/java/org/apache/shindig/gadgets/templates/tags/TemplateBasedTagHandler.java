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
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * TagHandler implemented by an declarative XML definition.
 */
public class TemplateBasedTagHandler extends AbstractTagHandler {

  private final Element templateDefinition;

  public TemplateBasedTagHandler(Element templateDefinition, String namespaceUri, String tagName) {
    super(namespaceUri, tagName);
    this.templateDefinition = templateDefinition;
  }

  public void process(Node result, Element tagInstance, TemplateProcessor processor) {
    // Process the children of the tag
    DocumentFragment processedContent = processChildren(tagInstance, processor);

    // Save the old values of "My", "Cur", and the template root element,
    // and update each
    Map<String, Object> oldMy = processor.getTemplateContext().setMy(
        computeMy(tagInstance, processedContent, processor));
    Object oldCur = processor.getTemplateContext().setCur(null);
    Node oldTemplateRoot = processor.getTemplateContext().setTemplateRoot(processedContent);

    processTemplate(result, tagInstance, processor);

    // And restore the template context
    processor.getTemplateContext().setMy(oldMy);
    processor.getTemplateContext().setCur(oldCur);
    processor.getTemplateContext().setTemplateRoot(oldTemplateRoot);
  }

  /** Process the template content in the new EL state */
  protected void processTemplate(Node result, Element tagInstance, TemplateProcessor processor) {
    processor.processChildNodes(result, templateDefinition);
  }

  /**
   * Compute the value of ${My} for this tag execution.
   */
  protected Map<String, Object> computeMy(Element tagInstance, Node processedContent,
      TemplateProcessor processor) {
    Map<String, Object> myMap = Maps.newHashMap();

    NodeList children = processedContent.getChildNodes();

    for (int i = 0;  i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child instanceof Element) {
        Element el = (Element) child;
        String name = el.getLocalName();
        // TODO: why???  There should always be a local name.
        if (name == null) {
          name = el.getNodeName();
        }

        ElementWrapper wrapper = new ElementWrapper(el);
        Object previous = myMap.get(name);
        if (previous == null) {
          myMap.put(name, wrapper);
        } else if (previous instanceof ElementWrapper) {
          List<ElementWrapper> bucket = Lists.newArrayListWithCapacity(children.getLength());
          bucket.add((ElementWrapper) previous);
          bucket.add(wrapper);
          myMap.put(name, bucket);
         } else {
           // Must be a List<ElementWrapper>
           @SuppressWarnings("unchecked")
           List<ElementWrapper> bucket = (List<ElementWrapper>) previous;
           bucket.add(wrapper);
        }
      }
    }

    NamedNodeMap atts = tagInstance.getAttributes();
    for (int i = 0; i < atts.getLength(); i++) {
      String name = atts.item(i).getNodeName();
      // Overwrite any pre-existing values, as attributes take
      // precedence over elements.  This is wasteful if there are attributes
      // and elements with the same name, but that should be very rare
      myMap.put(name, getValueFromTag(tagInstance, name, processor, Object.class));
    }

    return myMap;
  }
}
