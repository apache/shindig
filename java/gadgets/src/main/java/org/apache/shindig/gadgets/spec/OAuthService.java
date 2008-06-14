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

import java.net.URI;

import org.apache.shindig.common.xml.XmlUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Information about an OAuth service that a gadget wants to use.
 *
 * Instances are immutable.
 */
public class OAuthService {
  private EndPoint requestUrl;
  private EndPoint accessUrl;
  private URI authorizationUrl;
  private String name;

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
  public URI getAuthorizationUrl() {
    return authorizationUrl;
  }
  
  /**
   * Represents /OAuth/Service@name
   */
  public String getName() {
    return name;
  }  
  
  /**
   * Method to use for requests to an OAuth request token or access token URL.
   */
  public enum Method {
    GET, POST;
  }
  
  /**
   * Location for OAuth parameters in requests to an OAuth request token,
   * access token, or resource URL.  (Lowercase to match gadget spec schema)
   */
  public enum Location {
    header, url, body;
  }
  
  private static final String URL_ATTR = "url";
  private static final String PARAM_LOCATION_ATTR = "param_location";
  private static final String METHOD_ATTR = "method";
  
  /**
   * Description of an OAuth request token or access token URL.
   */
  public static class EndPoint {
    public final URI url;
    public final Method method;
    public final Location location;
    
    public EndPoint(URI url, Method method, Location location) {
      this.url = url;
      this.method = method;
      this.location = location;
    }

    public String toString(String element) {
      return "<" + element + " url='" + url.toString() + "' " +
      		"method='" + method + "' param_location='" + location + "'/>";
    }
  }
  
  public OAuthService(Element serviceElement) throws SpecParserException {
    name = serviceElement.getAttribute("name");
    NodeList children = serviceElement.getChildNodes();
    for (int i=0; i < children.getLength(); ++i) {
      Node child = children.item(i);
      if (child.getNodeType() != Element.ELEMENT_NODE) {
        continue;
      }
      String childName = child.getNodeName();
      if (childName.equals("Request")) {
        if (requestUrl != null) {
          throw new SpecParserException("Multiple OAuth/Service/Request elements");
        }
        requestUrl = parseEndPoint("OAuth/Service/Request", (Element)child);
      } else if (childName.equals("Authorization")) {
        if (authorizationUrl != null) {
          throw new SpecParserException("Multiple OAuth/Service/Authorization elements");
        }
        authorizationUrl = parseAuthorizationUrl((Element)child);
      } else if (childName.equals("Access")) {
        if (accessUrl != null) {
          throw new SpecParserException("Multiple OAuth/Service/Access elements");          
        }
        accessUrl = parseEndPoint("OAuth/Service/Access", (Element)child);
      }
    }
  }
  
  /**
   * Constructor for testing only.
   */
  OAuthService() {    
  }

  URI parseAuthorizationUrl(Element child) throws SpecParserException {
    URI url = XmlUtil.getHttpUriAttribute(child, URL_ATTR);
    if (url == null) {
      throw new SpecParserException("OAuth/Service/Authorization @url is not valid: " +
          child.getAttribute(URL_ATTR));
    }
    return url;
  }


  EndPoint parseEndPoint(String where, Element child) throws SpecParserException {
    URI url = XmlUtil.getHttpUriAttribute(child, URL_ATTR);
    if (url == null) {
      throw new SpecParserException("Not an HTTP url: " + child.getAttribute(URL_ATTR));
    }
    
    Location location = Location.header;
    String locationString = child.getAttribute(PARAM_LOCATION_ATTR);
    if (!"".equals(locationString)) {
      location = Location.valueOf(locationString);
    }
    
    Method method = Method.POST;
    String methodString = child.getAttribute(METHOD_ATTR);
    if (!"".equals(methodString)) {
      method = Method.valueOf(methodString);
    }
    return new EndPoint(url, method, location);
  }
}
