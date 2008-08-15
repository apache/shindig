/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.parse;

import java.util.List;

/**
 * Simplified interface wrapping a unit of parsed HTML.
 * Each {@code ParsedHtmlNode} is either text-type or
 * tag-type. The following snippet of HTML provides an example of both types:
 * 
 * &lt;div id="foo"&gt;content&lt;div&gt;
 * 
 * This corresponds to a single top-level {@code ParsedHtmlNode}
 * where {@code getTagName()} returns "div" and has one
 * {@code ParsedHtmlAttribute} with N/V "id"/"foo", {@code getText()}
 * is {@code null}, and has one {@code ParsedHtmlNode} child. That
 * child in turn has {@code getText()} equal to "content", with
 * all other methods returning {@code null}.
 */
public interface ParsedHtmlNode {
  /**
   * @return Tag name for an HTML element, or null if text-type.
   */
  public String getTagName();
  
  /**
   * @return List of HTML attributes on an element, or null if text-type
   */
  public List<ParsedHtmlAttribute> getAttributes();
  
  /**
   * @return List of child nodes of the HTML element, or null if text-type
   */
  public List<ParsedHtmlNode> getChildren();
  
  /**
   * @return Unescaped text as contained in an HTML string; null if tag-type
   */
  public String getText();
}
