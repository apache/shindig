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
package org.apache.shindig.gadgets.rewrite;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.HtmlSerialization;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Object that maintains a String representation of arbitrary contents
 * and a consistent view of those contents as an HTML parse tree.
 */
public class MutableContent {
  private static final Map<String, Object> EMPTY_MAP = ImmutableMap.of();

  // String representation of contentBytes taking into account the correct
  // encoding of the content.
  private String content;
  private byte[] contentBytes;

  // Encoding of the content bytes. UTF-8 by default.
  private Charset contentEncoding;

  private HttpResponse contentSource;

  private Document document;
  private int numChanges = 0;
  private final GadgetHtmlParser contentParser;
  private Map<String, Object> pipelinedData;

  private static final String MUTABLE_CONTENT_LISTENER = "MutableContentListener";
  //class name for logging purpose
  private static final String classname = MutableContent.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

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
    this.contentEncoding = Charsets.UTF_8;
  }

  /**
   * Construct with HttpResponse so we can defer string decoding until we actually need
   * the content. Given that we dont rewrite many mime types this is a performance advantage
   */
  public MutableContent(GadgetHtmlParser contentParser, HttpResponse contentSource) {
    this.contentParser = contentParser;
    this.contentSource = contentSource;
    this.contentEncoding = contentSource != null ? contentSource.getEncodingCharset() : null;
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
        Charset useEncoding = contentEncoding != null ? contentEncoding : Charsets.UTF_8;
        content = useEncoding.decode(ByteBuffer.wrap(contentBytes)).toString();
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
      incrementNumChanges();
    }
  }

  /**
   * Retrieves the current content for this object as an InputStream.
   * @return Active content as InputStream.
   */
  public InputStream getContentBytes() {
    return new ByteArrayInputStream(getRawContentBytes());
  }

  protected byte[] getRawContentBytes() {
    if (contentBytes == null) {
      if (contentSource != null) {
        try {
          setContentBytesState(IOUtils.toByteArray(contentSource.getResponse()),
              contentSource.getEncodingCharset());
          contentSource = null;
        } catch (IOException e) {
          // Doesn't occur; responseBytes wrapped as a ByteArrayInputStream.
        }
      } else if (content != null) {
        // If retrieving a String here, we've already converted to UTF8.
        // Be sure to reflect this when setting bytes.
        // In the case of HttpResponseBuilder, this re-sets charset in Content-Type
        // to UTF-8 rather than whatever it was before. We do this to standardize
        // on UTF-8 for all String handling.
        setContentBytesState(CharsetUtil.getUtf8Bytes(content), Charsets.UTF_8);
      } else if (document != null) {
        setContentBytesState(
            CharsetUtil.getUtf8Bytes(HtmlSerialization.serialize(document)), Charsets.UTF_8);
      }
    }
    return contentBytes;
  }

  /**
   * Sets the object's contentBytes as the given raw input. If ever interpreted
   * as a String, the data will be decoded as the encoding specified.
   * Note, this operation may clear the document if the content has changed.
   * Also note, it's mandated that the new bytes array will NOT be modified
   * by the caller of this API. The array is not copied, for performance reasons.
   * If the caller may modify a byte array, it MUST pass in a new copy.
   * @param newBytes New content.
   */
  public void setContentBytes(byte[] newBytes, Charset newEncoding) {
    if (contentBytes == null || !Arrays.equals(contentBytes, newBytes)) {
      setContentBytesState(newBytes, newEncoding);
      document = null;
      contentSource = null;
      content = null;
      incrementNumChanges();
    }
  }

  /**
   * Sets content to new byte array, with unspecified charset. It is
   * recommended to use the {@code setContentBytes(byte[], Charset)} API instead,
   * where possible.
   * @param newBytes New content.
   */
  public final void setContentBytes(byte[] newBytes) {
    setContentBytes(newBytes, null);
  }

  /**
   * Sets internal state having to do with content bytes, from the provided
   * byte array and charset.
   * This MUST be the only place in which MutableContent's notion of encoding is mutated.
   * @param newBytes New content.
   * @param newEncoding Encoding for the bytes, or null for unspecified.
   */
  protected void setContentBytesState(byte[] newBytes, Charset newEncoding) {
    contentBytes = newBytes;
    contentEncoding = newEncoding;
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
      incrementNumChanges();
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
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.logp(Level.WARNING, classname, "getDocument", MessageKeys.EXCEPTION_PARSING_CONTENT);
        LOG.log(Level.WARNING, e.getMessage(), e);
      }
      return null;
    }
    return document;
  }

  public GadgetHtmlParser getContentParser() {
    return contentParser;
  }

  public int getNumChanges() {
    return numChanges;
  }

  protected void incrementNumChanges() {
    ++numChanges;
  }

  /**
   * True if current state has a parsed document. Allows rewriters to switch mode based on
   * which content is most readily available
   */
  public boolean hasDocument() {
    return (document != null);
  }

  public void addPipelinedData(String key, Object value) {
    if (null == pipelinedData) {
      pipelinedData = Maps.newHashMap();
    }
    pipelinedData.put(key, value);
  }

  public Map<String, Object> getPipelinedData() {
    return (null == pipelinedData) ? EMPTY_MAP : pipelinedData;
  }
}
