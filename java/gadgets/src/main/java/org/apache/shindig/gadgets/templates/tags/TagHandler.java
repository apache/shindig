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
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A Handler for custom tags in template markup.
 */
public interface TagHandler {

  /**
   * Namespace used by tags in the default Opensocial namespace.
   */
  public static final String OPENSOCIAL_NAMESPACE = "http://ns.opensocial.org/2008/markup";

  /**
   * @return the local name of the element this tag parses.
   */
  String getTagName();

  /**
   * @return the namespace of the element this tag parses.
   */
  String getNamespaceUri();

  /**
   * Processes the custom tag.
   * @param result A Node to append output to.
   * @param tag The Element reference to the tag, useful for inspecting
   *     attributes and children
   * @param processor A TemplateProcessor, used to evaluate expressions and render
   *     sub-templates if needed.
   */
  void process(Node result, Element tag, TemplateProcessor processor);

}
