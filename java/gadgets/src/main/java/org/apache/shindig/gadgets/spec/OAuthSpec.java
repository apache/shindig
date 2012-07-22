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

import com.google.common.collect.Maps;

import org.apache.shindig.common.uri.Uri;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collections;
import java.util.Map;

/**
 * Wraps an &lt;OAuth&gt; element from the gadget spec.
 *
 * Instances are immutable.
 */
public class OAuthSpec {

  /** Keys are service names, values are service descriptors */
  private final Map<String, OAuthService> serviceMap;

  public OAuthSpec(Element element, Uri base) throws SpecParserException {
    serviceMap = Maps.newHashMap();
    NodeList services = element.getElementsByTagName("Service");
    for (int i=0; i < services.getLength(); ++i) {
      Node node = services.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        parseService((Element)node, base);
      }
    }
  }

  private void parseService(Element serviceElement, Uri base) throws SpecParserException {
    OAuthService service = new OAuthService(serviceElement, base);
    serviceMap.put(service.getName(), service);
  }

  public Map<String, OAuthService> getServices() {
    return Collections.unmodifiableMap(serviceMap);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("<OAuth>");
    for (Map.Entry<String, OAuthService> entry : serviceMap.entrySet()) {
      sb.append("<Service name='");
      sb.append(entry.getKey());
      sb.append("'>");
      OAuthService service = entry.getValue();
      sb.append(service.getRequestUrl().toString("Request"));
      sb.append(service.getAccessUrl().toString("Access"));
      sb.append("<Authorization url='").append(service.getAuthorizationUrl().toString()).append("'/>");
      sb.append("</Service>");
    }
    sb.append("</OAuth>");
    return sb.toString();
  }

}
