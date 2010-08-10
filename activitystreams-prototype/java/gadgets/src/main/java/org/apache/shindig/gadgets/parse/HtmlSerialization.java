/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.parse;

import com.google.common.collect.ImmutableSet;

import org.apache.xerces.xni.QName;
import org.cyberneko.html.HTMLEntities;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;

/**
 * Static class with helpers to manage serialization of a Document.
 * Binds an HtmlSerializer to a Document as user data, and pulls it out
 * to achieve actual serialization.
 */
public class HtmlSerialization {

  /**
   * Used to key an instance of HtmlSerializer in
   * document.getUserData
   */
  public static final String KEY = "serializer";

  /**
   * Used by a parser to record the original length of the content it parsed
   * Can be used to optimize output buffers
   */
  private static final String ORIGINAL_LENGTH = "original-length";

  public static final Set<String> URL_ATTRIBUTES = ImmutableSet.of("href", "src");

  /**
   * Attach a serializer instance to the document
   * @param doc
   * @param serializer
   * @param originalContent may be null
   */
  public static void attach(Document doc, HtmlSerializer serializer, String originalContent) {
    doc.setUserData(KEY, serializer, null);
    if (originalContent != null) {
      doc.setUserData(ORIGINAL_LENGTH, originalContent.length(), null);
    }
  }

  /**
   * Copy serializer from one document to another. Note this requires that
   * serializers are thread safe
   */
  static void copySerializer(Document from, Document to) {
    Integer length = (Integer)from.getUserData(ORIGINAL_LENGTH);
    if (length != null) to.setUserData(ORIGINAL_LENGTH, length, null);
    to.setUserData(KEY, from.getUserData(KEY), null);
  }

  /**
   * Get the length of the original version of the document
   * @param doc
   * @return
   */
  private static int getOriginalLength(Document doc) {
    Integer length = (Integer)doc.getUserData(ORIGINAL_LENGTH);
    if (length == null) return -1;
    return length;
  }

  /**
   * Create a writer sized to the original length of the document
   * @param doc
   * @return
   */
  public static StringWriter createWriter(Document doc) {
    int originalLength = getOriginalLength(doc);
    if (originalLength == -1) {
      return new StringWriter(8192);
    } else {
      // Typically rewriting makes a document larger
      return new StringWriter((originalLength * 11) / 10);
    }
  }

  /**
   * Call the attached serializer and output the document
   * @param doc
   * @return
   */
  public static String serialize(Document doc) {
    return ((HtmlSerializer) doc.getUserData(KEY)).serialize(doc);
  }

  public static void printEscapedText(CharSequence text, Appendable output) throws IOException {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      String entity = HTMLEntities.get(c);
      if (entity != null) {
        output.append('&').append(entity).append(";");
      } else {
        output.append(c);
      }
    }
  }

  /**
   * Returns true if the listed attribute is an URL attribute.
   */
  public static boolean isUrlAttribute(QName name, String attributeName) {
    return name.uri == null && URL_ATTRIBUTES.contains(attributeName);
  }

}
