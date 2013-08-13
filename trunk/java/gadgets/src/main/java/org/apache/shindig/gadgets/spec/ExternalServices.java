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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.apache.shindig.common.xml.XmlUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Map;

/**
 * Represents the ExternalServices tag in the gadget spec.
 *
 * It includes the child ServiceTag and its text element.
 *
 * @since 2.5.0
 */
public class ExternalServices {
  // The name to be used in the "alias" request parameters.
  private static final String ATTR_ALIAS = "alias";

  private Map<String, ServiceTag> serviceTags;

  public ExternalServices(Element element) {
    Map<String, ServiceTag> serviceTagsBuilder = Maps.newLinkedHashMap();
    parseServiceTags(element, serviceTagsBuilder);

    serviceTags = ImmutableMap.copyOf(serviceTagsBuilder);
  }

  public Map<String, ServiceTag> getServiceTags() {
    return serviceTags;
  }

  private void parseServiceTags(Element element, Map<String, ServiceTag> serviceTagsBuilder) {
    NodeList children = element.getChildNodes();
    for (int i = 0, j = children.getLength(); i < j; ++i) {
      Node child = children.item(i);
      String tagName = child.getNodeName();
      if (!(child instanceof Element)) continue;

      // only process ServiceTag child tags
      if(ServiceTag.SERVICE_TAG.equals(tagName)) {
        String alias = XmlUtil.getAttribute(child, ATTR_ALIAS, "");
        String tag = child.getTextContent();
        tag = (tag != null) ? tag.trim() : "";
        ServiceTag serviceTag = new ServiceTag(alias, tag);
        serviceTagsBuilder.put(alias, serviceTag);
      }
    }
  }

  /**
   * Represent the ServiceTag tag in the gadget spec.
   *
   * @since 2.5.0
   */
  public static class ServiceTag {
    public static final String SERVICE_TAG = "ServiceTag";

    private final String alias;
    private final String tag;

    public ServiceTag(String alias, String tag) {
      this.alias = alias;
      this.tag = tag;
    }

    public String getAlias() {
      return alias;
    }

    public String getTag() {
      return tag;
    }
  }
}
