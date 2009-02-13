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

import com.google.common.collect.Maps;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.HtmlSerializer;
import org.json.JSONObject;

import org.w3c.dom.Document;

import java.util.Map;

/**
 * Object that maintains a String representation of arbitrary contents
 * and a consistent view of those contents as an HTML parse tree.
 */
public class MutableContent {
  private String content;
  private HttpResponse contentSource;
  private Document document;
  private final GadgetHtmlParser contentParser;
  private final Map<String, JSONObject> pipelinedData;

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
    pipelinedData = Maps.newHashMap();
  }

  /**
   * Construct with HttpResponse so we can defer string decoding until we actually need
   * the content. Given that we dont rewrite many mime types this is a performance advantage
   */
  public MutableContent(GadgetHtmlParser contentParser, HttpResponse contentSource) {
    this.contentParser = contentParser;
    this.contentSource = contentSource;
    pipelinedData = Maps.newHashMap();
  }


  /**
   * Retrieves the current content for this object in String form.
   * If content has been retrieved in parse tree form and has
   * been edited, the String form is computed from the parse tree by
   * rendering it. It is <b>strongly</b> encouraged to avoid switching
   * between retrieval of parse tree (through {@code getParseTree}),
   * with subsequent edits and retrieval of String contents to avoid
   * repeated serialization and deserialization.
   * @return Renderable/active content.
   */
  public String getContent() {
    if (content == null) {
      if (contentSource != null) {
        content = contentSource.getResponseAsString();
        // Clear on first use
        contentSource = null;
      } else if (document != null) {
        content = HtmlSerializer.serialize(document);
      }
    }
    return content;
  }
  
  /**
   * Sets the object's content as a raw String. Note, this operation
   * may clears the document if the content has changed
   * @param newContent New content.
   */
  public void setContent(String newContent) {
    // TODO - Equality check may be unnecessary overhead
    if (content == null || !content.equals(newContent)) {
      content = newContent;
      document = null;
      contentSource = null;
    }
  }


  /**
   * Notification that the content of the document has changed. Causes the content
   * string to be cleared
   */
  public void documentChanged() {
    if (document != null) {
      content = null;
      contentSource = null;
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

  /**
   * True if current state has a parsed document. Allows rewriters to switch mode based on
   * which content is most readily available
   */
  public boolean hasDocument() {
    return (document != null);
  }
  
  public void addPipelinedData(String key, JSONObject value) {
    pipelinedData.put(key, value);
  }
  
  public Map<String, JSONObject> getPipelinedData() {
    return pipelinedData;
  }
}
