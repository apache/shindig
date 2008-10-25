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
import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.w3c.dom.Document;

/**
 * Parser for arbitrary HTML content. The content may simply be a
 * fragment or snippet of HTML rather than a fully-structured Document,
 * so the interface returns a list of {@code ParsedHtmlNode} objects
 * rather than a single top-level item.
 * 
 * {@see ParsedHtmlNode} for parsing details
 */
@ImplementedBy(CajaHtmlParser.class)
public abstract class GadgetHtmlParser {

  /**
   * @param content
   * @return true if we detect a preamble of doctype or html
   */
  protected static boolean attemptFullDocParseFirst(String content) {
    String normalized = content.substring(Math.min(100, content.length())).toUpperCase();
    return normalized.contains("<!DOCTYPE") || normalized.contains("<HTML");
  }

  /**
   * @param source
   * @return a parsed document or document fragment
   * @throws GadgetException
   */
  public abstract Document parseDom(String source) throws GadgetException;
}
