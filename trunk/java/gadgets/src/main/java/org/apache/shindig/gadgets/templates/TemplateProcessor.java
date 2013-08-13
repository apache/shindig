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
package org.apache.shindig.gadgets.templates;

import org.apache.shindig.common.Nullable;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.el.ELResolver;

/**
 * A Template Processor can process templates and evaluate expressions.
 */
public interface TemplateProcessor {

  /**
   * Process an entire template.
   *
   * @param template the DOM template, typically a script element
   * @param templateContext a template context providing top-level
   *     variables
   * @param globals ELResolver providing global variables other
   *     than those in the templateContext
   * @return a document fragment with the resolved content
   */
  DocumentFragment processTemplate(Element template,
      TemplateContext templateContext, ELResolver globals, TagRegistry registry);


  /**
   * @return the current template context.
   */
  TemplateContext getTemplateContext();

  /**
   * Process the children of an element or document.
   * @param result the node to which results should be appended
   * @param source the node whose children should be processed
   */
  void processChildNodes(Node result, Node source);

  void processRepeat(Node result, Element element, Iterable<?> dataList,
      Runnable onEachLoop);

    /**
   *  Evaluates an expression within the scope of this processor's context.
   *  @param expression The String expression
   *  @param type Expected result type
   *  @param defaultValue Default value to return
   */
  <T> T evaluate(String expression, Class<T> type, @Nullable T defaultValue);
}
