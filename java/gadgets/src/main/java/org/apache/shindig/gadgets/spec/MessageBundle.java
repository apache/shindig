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
import com.google.common.collect.ImmutableMap;

import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Map;

/**
 * Represents a messagebundle structure.
 */
public class MessageBundle {

  public static final MessageBundle EMPTY = new MessageBundle();

  private final ImmutableMap<String, String> messages;
  private final String languageDirection;
  private final String jsonString;

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
    jsonString = new JSONObject(messages).toString();
    languageDirection = locale.getLanguageDirection();
  }

   /**
   * Constructs a message bundle from a prebuilt map.
   *
   * @param locale The LocaleSpec element that this bundle was constructed from.
   * @param map The content of the message map.
   */
  public MessageBundle(LocaleSpec locale, Map<String, String> map) {
     messages = ImmutableMap.copyOf(map);
     languageDirection = locale.getLanguageDirection();
     jsonString = new JSONObject(messages).toString();
   }

  /**
   * Constructs a message bundle from a /ModulePrefs/Locale with nested messages.
   * @param element XML Dom element to parse
   * @throws SpecParserException when badly formed xml is provided
   */
  public MessageBundle(Element element) throws SpecParserException {
    messages = parseMessages(element);
    jsonString = new JSONObject(messages).toString();
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
    messages = ImmutableMap.copyOf(merged);
    jsonString = new JSONObject(messages).toString();
    languageDirection = dir;
  }

  private MessageBundle() {
    this.messages = ImmutableMap.of();
    jsonString = "{}";
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
   * Return the message bundle contents as a JSON encoded string.
   *
   * @return json representation of the message bundler
   */
  public String toJSONString() {
    return jsonString;
  }

  /**
   * Extracts messages from an element.
   * @param element Xml dom containing mesage bundle nodes
   * @return Immutable map of message keys to values
   * @throws SpecParserException when invalid xml is parsed
   */
  private ImmutableMap<String, String> parseMessages(Element element)
      throws SpecParserException {
    NodeList nodes = element.getElementsByTagName("msg");

    Map<String, String> messages = Maps.newHashMapWithExpectedSize(nodes.getLength());

    for (int i = 0, j = nodes.getLength(); i < j; ++i) {
      Element msg = (Element)nodes.item(i);
      String name = XmlUtil.getAttribute(msg, "name");
      if (name == null) {
        throw new SpecParserException(
            "All message bundle entries must have a name attribute.");
      }
      messages.put(name, msg.getTextContent().trim());
    }

    return ImmutableMap.copyOf(messages);
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
