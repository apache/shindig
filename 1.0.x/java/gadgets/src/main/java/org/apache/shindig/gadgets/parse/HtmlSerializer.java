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

import org.w3c.dom.Document;

import java.io.StringWriter;

/**
 * Serialize a w3c document. An implementation of this interface should be bound
 * to the document produced by an implementor of HtmlParser and retrieveable via
 * document.getUserData(HtmlSerializer.KEY)
 */
public abstract class HtmlSerializer {

  /**
   * Used to key an instance of HtmlSerializer in
   * document.getUserData
   */
  private static final String KEY = "serializer";

  /**
   * Used by a parser to record the original length of the content it parsed
   * Can be used to optimize output buffers
   */
  private static final String ORIGINAL_LENGTH = "original-length";

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
  public static void copySerializer(Document from, Document to) {
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
  protected static StringWriter createWriter(Document doc) {
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
    return ((HtmlSerializer)doc.getUserData(KEY)).serializeImpl(doc);
  }

  /**
   * Overridden by implementations
   * @param doc
   * @return
   */
  protected abstract String serializeImpl(Document doc);

}
