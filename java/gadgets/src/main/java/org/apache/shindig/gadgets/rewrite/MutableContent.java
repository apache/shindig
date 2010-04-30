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
package org.apache.shindig.gadgets.rewrite;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.HtmlSerialization;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Object that maintains a String representation of arbitrary contents
 * and a consistent view of those contents as an HTML parse tree.
 */
public class MutableContent {
  private String content;
  private byte[] contentBytes;
  private HttpResponse contentSource;
  private Document document;
  private int numChanges;
  private final GadgetHtmlParser contentParser;
  private final Map<String, Object> pipelinedData;

  private static final String MUTABLE_CONTENT_LISTENER = "MutableContentListener";

  public static void notifyEdit(Document doc) {
    MutableContent mc = (MutableContent) doc.getUserData(MUTABLE_CONTENT_LISTENER);
    if (mc != null) {
      mc.documentChanged();
    }
  }

  /**
   * Construct with decoded string content
   */
  public MutableContent(GadgetHtmlParser contentParser, String content) {
    this.contentParser = contentParser;
    this.content = content;
    this.numChanges = 0;
    this.pipelinedData = Maps.newHashMap();
  }

  /**
   * Construct with HttpResponse so we can defer string decoding until we actually need
   * the content. Given that we dont rewrite many mime types this is a performance advantage
   */
  public MutableContent(GadgetHtmlParser contentParser, HttpResponse contentSource) {
    this.contentParser = contentParser;
    this.contentSource = contentSource;
    this.pipelinedData = Maps.newHashMap();
  }

  /**
   * Retrieves the current content for this object in String form.
   * If content has been retrieved in parse tree form and has
   * been edited, the String form is computed from the parse tree by
   * rendering it. It is <b>strongly</b> encouraged to avoid switching
   * between retrieval of parse tree (through {@code getParseTree}),
   * with subsequent edits and retrieval of String contents to avoid
   * repeated serialization and deserialization.
   * As a final fallback, if content has been set as bytes, interprets
   * them as a UTF8 String.
   * @return Renderable/active content.
   */
  public String getContent() {
    if (content == null) {
      if (contentSource != null) {
        content = contentSource.getResponseAsString();
        // Clear on first use
        contentSource = null;
      } else if (document != null) {
        content = HtmlSerialization.serialize(document);
      } else if (contentBytes != null) {
        try {
          content = new String(contentBytes, "UTF8");
        } catch (UnsupportedEncodingException e) {
          // Never happens.
        }
      }
    }
    return content;
  }
  
  /**
   * Sets the object's content as a raw String. Note, this operation
   * may clear the document if the content has changed
   * @param newContent New content.
   */
  public void setContent(String newContent) {
    // TODO - Equality check may be unnecessary overhead
    if (content == null || !content.equals(newContent)) {
      content = newContent;
      document = null;
      contentSource = null;
      contentBytes = null;
      numChanges++;
    }
  }

  /**
   * Retrieves the current content for this object as bytes.
   * The returned byte array should be treated as immutable. If it
   * is modified, resultant behavior is
   * @return Active content as bytes.
   */
  public InputStream getContentBytes() {
    if (contentBytes == null) {
      if (contentSource != null) {
        try {
          contentBytes = IOUtils.toByteArray(contentSource.getResponse());
          contentSource = null;
        } catch (IOException e) {
          // Doesn't occur; responseBytes wrapped as a ByteArrayInputStream.
        }
      } else if (content != null) {
        try {
          contentBytes = content.getBytes("UTF8");
        } catch (UnsupportedEncodingException e) {
          // Doesn't happen.
        }
      } else if (document != null) {
        try {
          contentBytes = HtmlSerialization.serialize(document).getBytes("UTF8");
        } catch (UnsupportedEncodingException e) {
          // Doesn't happen.
        }
      }
    }
    return new ByteArrayInputStream(contentBytes);
  }
  
  /**
   * Sets the object's contentBytes as the given raw input.
   * Note, this operation may clear the document if the content has changed.
   * @param newBytes New content.
   */
  public void setContentBytes(byte[] newBytes) {
    if (contentBytes == null || Arrays.equals(contentBytes, newBytes)) {
      contentBytes = newBytes;
      document = null;
      contentSource = null;
      content = null;
      numChanges++;
    }
  }

  /**
   * Notification that the content of the document has changed. Causes the content
   * string and bytes to be cleared.
   */
  public void documentChanged() {
    if (document != null) {
      content = null;
      contentSource = null;
      contentBytes = null;
      numChanges++;
    }
  }
  
  /**
   * Retrieves the object contents in parsed form, if a
   * {@code GadgetHtmlParser} is configured and is able to parse the string
   * contents appropriately. To modify the object's
   * contents by parse tree after setting new String contents,
   * this method must be called again. However, this practice is highly
   * discouraged, as parsing a tree from String is a costly operation and should
   * be done at most once per rewrite.
   */
  public Document getDocument() {
    // TODO - Consider actually imposing one parse limit on rewriter pipeline
    if (document != null) {
      return document;
    }
    try {
      document = contentParser.parseDom(getContent());
      document.setUserData(MUTABLE_CONTENT_LISTENER, this, null);
    } catch (GadgetException e) {
      // TODO: emit info message
      return null;
    }
    return document;
  }
  
  public int getNumChanges() {
    return numChanges;
  }

  /**
   * True if current state has a parsed document. Allows rewriters to switch mode based on
   * which content is most readily available
   */
  public boolean hasDocument() {
    return (document != null);
  }
  
  public void addPipelinedData(String key, Object value) {
    pipelinedData.put(key, value);
  }
  
  public Map<String, Object> getPipelinedData() {
    return pipelinedData;
  }
}
