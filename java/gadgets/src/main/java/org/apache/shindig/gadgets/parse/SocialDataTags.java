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
package org.apache.shindig.gadgets.parse;

import java.util.List;

import org.apache.shindig.common.xml.DomUtil;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.w3c.dom.Document;

/**
 * Helper class containing all defs regarding social data tags,
 * and one helper method to retrieve all such tags from the document.
 * Neko's parser implementation disallows script tags with children,
 * so as a workaround they convert such tags' name to OSData or OSTemplate.
 * This class provides a helper to select all OSData or OSTemplate tags,
 * irrespective whether this conversion occurred, ensuring that
 * data pipelining and templating can work with any parser.
 *
 * @since 2.0.0
 */
public final class SocialDataTags {
  private SocialDataTags() { }

  /**
   * Allowed tag names for OpenSocial Data and template blocks.
   * Make the tag names lower case since they're normalized by
   * the caja http://code.google.com/p/google-caja/issues/detail?id=1272
   * Another approach is to namespace them but that causes other issues.
   */
  public static final String OSML_DATA_TAG = "osdata";
  public static final String OSML_TEMPLATE_TAG = "ostemplate";

  /**
   * Bi-map of OpenSocial tags to their script type attribute values.
   */
  public static final BiMap<String, String> SCRIPT_TYPE_TO_OSML_TAG = ImmutableBiMap.of(
      "text/os-data", OSML_DATA_TAG, "text/os-template", OSML_TEMPLATE_TAG);

  public static List<Element> getTags(Document doc, String tagName) {
    NodeList list = doc.getElementsByTagName(tagName);
    List<Element> elements = Lists.newArrayListWithExpectedSize(list.getLength());
    for (int i = 0; i < list.getLength(); i++) {
      elements.add((Element) list.item(i));
    }

    // Add equivalent <script> elements
    String scriptType = SCRIPT_TYPE_TO_OSML_TAG.inverse().get(tagName);
    if (scriptType != null) {
      List<Element> scripts =
          DomUtil.getElementsByTagNameCaseInsensitive(doc, ImmutableSet.of("script"));
      for (Element script : scripts) {
        Attr typeAttr = (Attr)script.getAttributes().getNamedItem("type");
        if (typeAttr != null && scriptType.equalsIgnoreCase(typeAttr.getValue())) {
          elements.add(script);
        }
      }
    }
    return elements;
  }

  public static boolean isOpenSocialScript(Element script) {
    Attr typeAttr = (Attr)script.getAttributes().getNamedItem("type");
    return (typeAttr != null && typeAttr.getValue() != null &&
            SCRIPT_TYPE_TO_OSML_TAG.containsKey(typeAttr.getValue()));
  }
}
