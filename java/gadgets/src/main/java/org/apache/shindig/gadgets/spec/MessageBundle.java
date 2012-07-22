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
package org.apache.shindig.gadgets.spec;

import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.parse.DefaultHtmlSerializer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * Represents a messagebundle structure.
 */
public class MessageBundle {
  public static final MessageBundle EMPTY = new MessageBundle();

  private static final DefaultHtmlSerializer HTML_SERIALIZER = new DefaultHtmlSerializer();
  private final ImmutableMap<String, String> messages;
  private final String languageDirection;

  /* lazily created cache of the json-encoded form of the bundle */
  private String jsonString;

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
   * Constructs a message bundle from a prebuilt map.
   *
   * @param locale The LocaleSpec element that this bundle was constructed from.
   * @param map The content of the message map.
   */
  public MessageBundle(LocaleSpec locale, Map<String, String> map) {
     messages = ImmutableMap.copyOf(map);
     languageDirection = locale.getLanguageDirection();
   }

  /**
   * Constructs a message bundle from a /ModulePrefs/Locale with nested messages.
   * @param element XML Dom element to parse
   * @throws SpecParserException when badly formed xml is provided
   */
  public MessageBundle(Element element) throws SpecParserException {
    messages = parseMessages(element);
    languageDirection = XmlUtil.getAttribute(element, "language_direction", "ltr");
  }

  /**
   * Create a MessageBundle by merging multiple bundles together.
   *
   * @param bundles the bundles to merge, in order
   */
  public MessageBundle(MessageBundle... bundles) {
    Map<String, String> merged = Maps.newHashMap();
    String dir = null;
    for (MessageBundle bundle : bundles) {
      merged.putAll(bundle.messages);
      dir = bundle == EMPTY ? dir : bundle.languageDirection;
    }
    messages = ImmutableMap.copyOf(merged);
    languageDirection = dir != null ? dir : "ltr";
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
    if (jsonString == null) {
      jsonString = JsonSerializer.serialize(messages);
    }
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
      StringWriter sw = new StringWriter();
      NodeList msgChildren = msg.getChildNodes();
      for (int child = 0; child < msgChildren.getLength(); ++child) {
        try {
          if (msgChildren.item(child).getNodeType() == Node.CDATA_SECTION_NODE) {
            // Workaround to treat CDATA as text.
            sw.append(msgChildren.item(child).getTextContent());
          } else {
            HTML_SERIALIZER.serialize(msgChildren.item(child), sw);
          }
        } catch (IOException e) {
          throw new SpecParserException("Unexpected error getting value of msg node",
                                        new XmlException(e));
        }
      }
      messages.put(name, sw.toString());
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
