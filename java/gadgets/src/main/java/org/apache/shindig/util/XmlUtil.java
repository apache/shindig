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

package org.apache.shindig.util;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.net.URI;
import java.net.URISyntaxException;

public class XmlUtil {

  /**
   * Extracts an attribute from a node.
   *
   * @param node
   * @param attr
   * @param def
   * @return The value of the attribute, or def
   */
  public static String getAttribute(Node node, String attr, String def) {
    NamedNodeMap attrs = node.getAttributes();
    Node val = attrs.getNamedItem(attr);
    if (val != null) {
      return val.getNodeValue();
    }
    return def;
  }

  /**
   * @param node
   * @param attr
   * @return The value of the given attribute, or null if not present.
   */
  public static String getAttribute(Node node, String attr) {
    return getAttribute(node, attr, null);
  }

  /**
   * Retrieves an attribute as a URI.
   * @param node
   * @param attr
   * @return The parsed uri, or def if the attribute doesn't exist or can not
   *     be parsed as a URI.
   */
  public static URI getUriAttribute(Node node, String attr, URI def) {
    String uri = getAttribute(node, attr);
    if (uri != null) {
      try {
        return new URI(uri);
      } catch (URISyntaxException e) {
        return def;
      }
    }
    return def;
  }

  /**
   * Retrieves an attribute as a URI.
   * @param node
   * @param attr
   * @return The parsed uri, or null.
   */
  public static URI getUriAttribute(Node node, String attr) {
    return getUriAttribute(node, attr, null);
  }
}
