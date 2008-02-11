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
package org.apache.shindig.gadgets;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Processor for message bundle XML.
 */
public class MessageBundleParser {
  /**
   * Processes a single &lt;msg&gt; tag.
   *
   * @param messages The message list
   * @param msg The msg DOM node
   */
  private void processMessage(Map<String, String> messages, Node msg) {
    NamedNodeMap attrs = msg.getAttributes();
    Node name = attrs.getNamedItem("name");
    if (name != null) {
      messages.put(name.getNodeValue(), msg.getTextContent().trim());
    }
  }

  /**
   * Parses the message bundle into a map of translations.
   *
   * @param xml Raw XML to parse
   * @return Message bundle resulting from the parse
   * @throws GadgetException If the bundle is empty or malformed
   */
  public MessageBundle parse(String xml) throws GadgetException {
    if (xml.length() == 0) {
      throw new GadgetException(GadgetException.Code.EMPTY_XML_DOCUMENT);
    }

    Document doc;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      InputSource is = new InputSource(new StringReader(xml));
      doc = factory.newDocumentBuilder().parse(is);
    } catch (SAXException e) {
      throw new GadgetException(GadgetException.Code.MALFORMED_XML_DOCUMENT);
    } catch (ParserConfigurationException e) {
      throw new GadgetException(GadgetException.Code.MALFORMED_XML_DOCUMENT);
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.MALFORMED_XML_DOCUMENT);
    }

    Map<String, String> messages = new HashMap<String, String>();

    NodeList nodes = doc.getElementsByTagName("msg");
    for (int i = 0, j = nodes.getLength(); i < j; ++i) {
      processMessage(messages, nodes.item(i));
    }
    return new MessageBundle(messages);
  }
}
