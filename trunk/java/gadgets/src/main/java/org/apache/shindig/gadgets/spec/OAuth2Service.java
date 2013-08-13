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

import org.apache.shindig.common.uri.Uri;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Information about an OAuth2 service that a gadget wants to use.
 *
 * Instances are immutable.
 */
public class OAuth2Service extends BaseOAuthService {
  private EndPoint authorizationUrl;
  private EndPoint tokenUrl;
  private String name;
  private String scope;

  /**
   * Constructor for testing only.
   */
  OAuth2Service() { }

  public OAuth2Service(Element serviceElement, Uri base) throws SpecParserException {
    name = serviceElement.getAttribute("name");
    scope = serviceElement.getAttribute("scope");
    NodeList children = serviceElement.getChildNodes();
    for (int i=0; i < children.getLength(); ++i) {
      Node child = children.item(i);
      if (child.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      String childName = child.getNodeName();
      if ("Authorization".equals(childName)) {
        if (authorizationUrl != null) {
          throw new SpecParserException("Multiple OAuth2/Service/Authorization elements");
        }
        authorizationUrl = parseEndPoint("OAuth2/Service/Authorization", (Element)child, base);
      } else if ("Token".equals(childName)) {
        if (tokenUrl != null) {
          throw new SpecParserException("Multiple OAuth2/Service/Token elements");
        }
        tokenUrl = parseEndPoint("OAuth2/Service/Token", (Element)child, base);
      }
    }
  }

  /**
   * Represents /OAuth2/Service/Authorization elements.
   */
  public EndPoint getAuthorizationUrl() {
    return authorizationUrl;
  }

  /**
   * Represents /OAuth2/Service/Token elements.
   */
  public EndPoint getTokenUrl() {
    return tokenUrl;
  }


  /**
   * Represents /OAuth2/Service@scope
   */
  public String getScope() {
    return scope;
  }

  /**
   * Represents /OAuth/Service@name
   */
  public String getName() {
    return name;
  }



}
