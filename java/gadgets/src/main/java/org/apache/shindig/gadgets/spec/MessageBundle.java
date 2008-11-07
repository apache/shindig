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
package org.apache.shindig.gadgets.spec;

import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.common.xml.XmlUtil;

import com.google.common.collect.Maps;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a messagebundle structure.
 */
public class MessageBundle {

  public static final MessageBundle EMPTY = new MessageBundle();

  private final Map<String, String> messages;
  private final String languageDirection;

   /**
   * Constructs a message bundle from input xml (fetched from an external file).
   *
   * @param locale The LocaleSpec element that this bundle was constructed from.
   * @param xml The content of the remote file.
   * @throws SpecParserException if parsing fails.
   */
  public MessageBundle(LocaleSpec locale, String xml) throws SpecParserException {
    Element doc;
    try {
      doc = XmlUtil.parse(xml);
    } catch (XmlException e) {
      throw new SpecParserException("Malformed XML in file " + locale.getMessages()
          + ": " + e.getMessage());
    }
    messages = parseMessages(doc);
    languageDirection = locale.getLanguageDirection();
  }

  /**
   * Constructs a message bundle from a /ModulePrefs/Locale with nested messages.
   */
  public MessageBundle(Element element) throws SpecParserException {
    messages = parseMessages(element);
    languageDirection = XmlUtil.getAttribute(element, "language_direction", "ltr");
  }

  /**
   * Create a MessageBundle by merging child messages into the parent.
   *
   * @param parent The base bundle.
   * @param child The bundle containing overriding messages.
   */
  public MessageBundle(MessageBundle parent, MessageBundle child) {
    Map<String, String> merged = Maps.newHashMap();
    String dir = null;
    if (parent != null) {
      merged.putAll(parent.messages);
      dir = parent.languageDirection;
    }
    if (child != null) {
      merged.putAll(child.messages);
      dir = child.languageDirection;
    }
    messages = Collections.unmodifiableMap(merged);
    languageDirection = dir;
  }

  private MessageBundle() {
    this.messages = Collections.emptyMap();
    languageDirection = "ltr";
  }

  /**
   * @return The language direction associated with this message bundle, derived from the LocaleSpec
   * element that the bundle was constructed from.
   */
  public String getLanguageDirection() {
    return languageDirection;
  }

  /**
   * @return A read-only view of the message bundle.
   */
  public Map<String, String> getMessages() {
    return messages;
  }

  /**
   * Extracts messages from an element.
   */
  private Map<String, String> parseMessages(Element element)
      throws SpecParserException {
    NodeList nodes = element.getElementsByTagName("msg");
    Map<String, String> messages
        = new HashMap<String, String>(nodes.getLength(), 1);

    for (int i = 0, j = nodes.getLength(); i < j; ++i) {
      Element msg = (Element)nodes.item(i);
      String name = XmlUtil.getAttribute(msg, "name");
      if (name == null) {
        throw new SpecParserException(
            "All message bundle entries must have a name attribute.");
      }
      messages.put(name, msg.getTextContent().trim());
    }
    return Collections.unmodifiableMap(messages);
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("<messagebundle>\n");
    for (Map.Entry<String, String> entry : messages.entrySet()) {
      buf.append("<msg name=\"").append(entry.getKey()).append("\">")
         .append(entry.getValue())
         .append("</msg>\n");
    }
    buf.append("</messagebundle>");
    return buf.toString();
  }
}
