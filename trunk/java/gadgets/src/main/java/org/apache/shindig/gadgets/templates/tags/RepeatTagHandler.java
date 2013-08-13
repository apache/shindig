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
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.inject.Inject;

/**
 * Implementation of the <os:Repeat> tag.
 */
public class RepeatTagHandler extends AbstractTagHandler {

  static final String TAG_REPEAT = "Repeat";
  static final String EXPRESSION_ATTR = "expression";
  static final String IF_ATTR = "if";

  @Inject
  public RepeatTagHandler() {
    super(TagHandler.OPENSOCIAL_NAMESPACE, TAG_REPEAT);
  }

  public void process(final Node result, final Element tag, final TemplateProcessor processor) {
    Iterable<?> repeat = getValueFromTag(tag, EXPRESSION_ATTR, processor, Iterable.class);
    if (repeat != null) {
      final Attr ifAttribute = tag.getAttributeNode(IF_ATTR);

      // On each iteration, process child nodes, after checking the value of the "if" attribute
      processor.processRepeat(result, tag, repeat, new Runnable() {
        public void run() {
          if (ifAttribute != null) {
            if (!processor.evaluate(ifAttribute.getValue(), Boolean.class, false)) {
              return;
            }
          }

          processor.processChildNodes(result, tag);
        }
      });
    }
  }
}
