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
 * Information about an OAuth service that a gadget wants to use.
 *
 * Instances are immutable.
 */
public class OAuthService extends BaseOAuthService {
  private EndPoint requestUrl;
  private EndPoint accessUrl;
  private Uri authorizationUrl;
  private String name;

  /**
   * Constructor for testing only.
   */
  OAuthService() {}

  public OAuthService(Element serviceElement, Uri base) throws SpecParserException {
    name = serviceElement.getAttribute("name");
    NodeList children = serviceElement.getChildNodes();
    for (int i = 0; i < children.getLength(); ++i) {
      Node child = children.item(i);
      if (child.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      String childName = child.getNodeName();
      if ("Request".equals(childName)) {
        if (requestUrl != null) {
          throw new SpecParserException("Multiple OAuth/Service/Request elements");
        }
        requestUrl = parseEndPoint("OAuth/Service/Request", (Element) child, base);
      } else if ("Authorization".equals(childName)) {
        if (authorizationUrl != null) {
          throw new SpecParserException("Multiple OAuth/Service/Authorization elements");
        }
        authorizationUrl = parseAuthorizationUrl((Element) child, base);
      } else if ("Access".equals(childName)) {
        if (accessUrl != null) {
          throw new SpecParserException("Multiple OAuth/Service/Access elements");
        }
        accessUrl = parseEndPoint("OAuth/Service/Access", (Element) child, base);
      }
    }
    if (requestUrl == null) {
      throw new SpecParserException("/OAuth/Service/Request is required");
    }
    if (accessUrl == null) {
      throw new SpecParserException("/OAuth/Service/Access is required");
    }
    if (authorizationUrl == null) {
      throw new SpecParserException("/OAuth/Service/Authorization is required");
    }
    if (requestUrl.location != accessUrl.location) {
      throw new SpecParserException("Access@location must be identical to Request@location");
    }
    if (requestUrl.method != accessUrl.method) {
      throw new SpecParserException("Access@method must be identical to Request@method");
    }
    if (requestUrl.location == Location.BODY && requestUrl.method == Method.GET) {
      throw new SpecParserException("Incompatible parameter location, cannot"
              + "use post-body with GET requests");
    }
  }

  /**
   * Represents /OAuth/Service/Request elements.
   */
  public EndPoint getRequestUrl() {
    return requestUrl;
  }

  /**
   * Represents /OAuth/Service/Access elements.
   */
  public EndPoint getAccessUrl() {
    return accessUrl;
  }

  /**
   * Represents /OAuth/Service/Authorization elements.
   */
  public Uri getAuthorizationUrl() {
    return authorizationUrl;
  }

  /**
   * Represents /OAuth/Service@name
   */
  public String getName() {
    return name;
  }

}
