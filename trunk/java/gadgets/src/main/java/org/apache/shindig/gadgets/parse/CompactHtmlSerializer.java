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
package org.apache.shindig.gadgets.parse;

import com.google.common.collect.ImmutableSortedSet;

import org.w3c.dom.Node;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * Performs simple content compaction while writing HTML documents. The compaction includes:
 * <ul>
 * <li>Collapsing consecutive whitespaces while preserving those within style, pre and script tags
 * <li>Removing HTML comments while preserving IE conditional comments
 * </ul>
 *
 * TODO - Consider adding attribute quoting elimination, empty attribute elimination where safe
 * end-tag elmination where safe.
 */
public class CompactHtmlSerializer extends DefaultHtmlSerializer {

  private static final ImmutableSortedSet<String> SPECIAL_TAGS = ImmutableSortedSet
      .orderedBy(String.CASE_INSENSITIVE_ORDER)
      .add("style", "pre", "script", "textarea")
      .build();
  private static final String HTML_WHITESPACE = " \t\r\n";

  @Override
  protected void writeText(Node n, Appendable output) throws IOException {
    if (isSpecialTag(n.getParentNode().getNodeName())) {
      super.writeText(n, output);
    } else {
      collapseWhitespace(n.getTextContent(), output);
    }
  }

  @Override
  protected void writeComment(Node n, Appendable output) throws IOException {
    if (isSpecialTag(n.getParentNode().getNodeName())) {
      super.writeComment(n, output);
    } else if (isIeConditionalComment(n)) {
      super.writeComment(n, output);
    }
  }

  /**
   * See <a href="http://msdn.microsoft.com/en-us/library/ms537512(printer).aspx">MSDN</a>
   * and <a href="http://www.quirksmode.org/css/condcom.html">PPK</a>
   */
  private boolean isIeConditionalComment(Node n) {
    String comment = n.getTextContent();
    return comment.contains("[if ") && comment.contains("[endif]");
  }

  /**
   * Returns true if a tag with a given tagName should preserve any whitespaces
   * in its children nodes.
   */
  static boolean isSpecialTag(String tagName) {
    return SPECIAL_TAGS.contains(tagName);
  }

  /**
   * Collapse any consecutive HTML whitespace characters inside a string into
   * one space character (0x20). This method will not output any characters when
   * the given string is entirely composed of whitespaces.
   *
   * References:
   * <ul>
   * <li>http://www.w3.org/TR/html401/struct/text.html#h-9.1</li>
   * <li>http://java.sun.com/javase/6/docs/api/java/lang/Character.html#isWhitespace(char)</li>
   * </ul>
   */
  static void collapseWhitespace(String str, Appendable output) throws IOException {
    str = StringUtils.stripStart(str, HTML_WHITESPACE);

    // Whitespaces between a sequence of non-whitespace characters
    boolean seenWhitespace = false;
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);

      if (HTML_WHITESPACE.indexOf(c) != -1) {
        seenWhitespace = true;
      } else {
        if (seenWhitespace) {
          output.append(' ');
        }
        output.append(c);

        seenWhitespace = false;
      }
    }
  }
}
