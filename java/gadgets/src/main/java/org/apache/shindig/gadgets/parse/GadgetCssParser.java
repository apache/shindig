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

import com.google.inject.ImplementedBy;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.caja.CajaCssParser;

import java.util.List;

/**
 * Parser for CSS content. Parsing may be done on a fully-formed
 * CSS block, such as the contents of a CSS file or &lt;style&gt; block.
 * 
 * {@see ParsedCssRule} and {@see ParsedCssDeclaration} for additional
 * parsing requirements and semantics.
 */
@ImplementedBy(CajaCssParser.class)
public interface GadgetCssParser {
  public List<ParsedCssRule> parse(String css) throws GadgetException;
  public List<ParsedCssDeclaration> parseInline(String style) throws GadgetException;
}
