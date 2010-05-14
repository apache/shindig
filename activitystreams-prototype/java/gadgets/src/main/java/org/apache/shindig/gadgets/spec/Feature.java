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

import org.apache.shindig.common.xml.XmlUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 * Represents a Require or Optional tag.
 * No substitutions on any fields.
 */
public class Feature {
  public static final Feature CORE_FEATURE = new Feature("core");
  public static final Feature SECURITY_TOKEN_FEATURE = new Feature("security-token");
  
  // Instantiable only by CORE_FEATURE.
  private Feature(String name) {
    this.params = ImmutableMultimap.of();
    this.required = true;
    this.name = name;
  }
  
  /**
   * Require@feature
   * Optional@feature
   */
  private final String name;
  public String getName() {
    return name;
  }

  /**
   * Require.Param
   * Optional.Param
   *
   * Flattened into a map where Param@name is the key and Param content is
   * the value.
   */
  private final Multimap<String, String> params;
  public Multimap<String, String> getParams() {
    return params;
  }
  
  /**
   * Returns the first value for any feature parameter, or null
   * if the parameter does not exist.
   */
  public String getParam(String key) {
    Collection<String> values = params.get(key);
    if (values == null || values.isEmpty()) {
      return null;
    }
    
    return values.iterator().next();
  }

  /**
   * Returns the values for the key, or an empty collection.
   */
  public Collection<String> getParamCollection(String key) {
    return params.get(key);
  }

  /**
   * Whether this is a Require or an Optional feature.
   */
  private final boolean required;
  public boolean getRequired() {
    return required;
  }

  /**
   * Produces an xml representation of the feature.
   */
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(required ? "<Require" : "<Optional")
       .append(" feature=\"")
       .append(name)
       .append("\">");
    for (Map.Entry<String, Collection<String>> entry : params.asMap().entrySet()) {
      buf.append("\n<Param name=\"")
         .append(entry.getKey())
         .append("\">")
         .append(entry.getValue())
         .append("</Param>");
    }
    buf.append(required ? "</Require>" : "</Optional>");
    return buf.toString();
  }

  /**
   * Creates a new Feature from an xml node.
   *
   * @param feature The feature to create
   * @throws SpecParserException When the Require or Optional tag is not valid
   */
  public Feature(Element feature) throws SpecParserException {
    this.required = feature.getNodeName().equals("Require");
    String name = XmlUtil.getAttribute(feature, "feature");
    if (name == null) {
      throw new SpecParserException(
          (required ? "Require" : "Optional") +"@feature is required.");
    }
    this.name = name;
    NodeList children = feature.getElementsByTagName("Param");
    if (children.getLength() > 0) {
      ImmutableMultimap.Builder<String, String> params = ImmutableMultimap.builder();

      for (int i = 0, j = children.getLength(); i < j; ++i) {
        Element param = (Element)children.item(i);
        String paramName = XmlUtil.getAttribute(param, "name");
        if (paramName == null) {
          throw new SpecParserException("Param@name is required");
        }
        params.put(paramName, param.getTextContent());
      }
      this.params = params.build();
    } else {
      this.params = ImmutableMultimap.of();
    }
  }
}
