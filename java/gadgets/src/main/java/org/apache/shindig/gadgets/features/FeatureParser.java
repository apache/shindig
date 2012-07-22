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
package org.apache.shindig.gadgets.features;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.GadgetException;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.Map;

/**
 * Parses feature.xml files into an intermediary Java object for further processing.
 * This is largely an implementation detail of FeatureRegistry.
 */
class FeatureParser {
  public ParsedFeature parse(Uri parent, String xml) throws GadgetException {
    Element doc;
    try {
      doc = XmlUtil.parse(xml);
    } catch (XmlException e) {
      throw new GadgetException(GadgetException.Code.MALFORMED_XML_DOCUMENT, e);
    }

    String name = null;
    List<String> deps = Lists.newArrayList();
    List<ParsedFeature.Bundle> bundles = Lists.newArrayList();
    boolean supportDefer = false;

    NodeList children = doc.getChildNodes();
    for (int i = 0, j = children.getLength(); i < j; ++i) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        Element element = (Element)child;
        if (element.getTagName().equals("name")) {
          name = element.getTextContent();
        } else if (element.getTagName().equals("dependency")) {
          deps.add(element.getTextContent());
        } else {
          String type = element.getTagName().toLowerCase();
          List<ParsedFeature.Resource> resources = Lists.newArrayList();
          NodeList resourceKids = element.getElementsByTagName("script");
          for (int x = 0, y = resourceKids.getLength(); x < y; ++x) {
            Element resourceChild = (Element)resourceKids.item(x);
            String src = resourceChild.getAttribute("src");
            String content = resourceChild.getTextContent();
            Map<String, String> attribs = getAttribs(resourceChild);
            Uri source = null;
            if (src != null && src.length() > 0) {
              if (!"false".equals(attribs.get("inline"))) {
                source = parent.resolve(FeatureRegistry.getComponentUri(src));
              } else {
                source = Uri.parse(src);
              }
            }
            resources.add(new ParsedFeature.Resource(
                source,
                src != null && src.length() != 0 ? null : content,
                getAttribs(resourceChild)));
          }
          List<ApiDirective> apiDirectives = Lists.newArrayList();
          NodeList apiKids = element.getElementsByTagName("api");
          for (int x = 0, y = apiKids.getLength(); x < y; ++x) {
            Element apiChild = (Element)apiKids.item(x);
            supportDefer = "true".equalsIgnoreCase(apiChild.getAttribute("supportDefer"));
            NodeList apiElems = apiChild.getChildNodes();
            for (int a = 0, b = apiElems.getLength(); a < b; ++a) {
              Node apiElemNode = apiElems.item(a);
              if (apiElemNode.getNodeType() == Node.ELEMENT_NODE) {
                Element apiElem = (Element)apiElemNode;
                boolean isImport = "uses".equals(apiElem.getNodeName());
                boolean isExport = "exports".equals(apiElem.getNodeName());
                if (isImport || isExport) {
                  apiDirectives.add(new ApiDirective(
                      apiElem.getAttribute("type"), apiElem.getTextContent(), isImport));
                }
              }
            }
          }
          bundles.add(new ParsedFeature.Bundle(name, type, getAttribs(element),
              resources, apiDirectives, supportDefer));
        }
      }
    }

    return new ParsedFeature(name, deps, bundles);
  }

  private Map<String, String> getAttribs(Element element) {
    ImmutableMap.Builder<String, String> attribs = ImmutableMap.builder();
    NamedNodeMap attribNodes = element.getAttributes();
    for (int x = 0, y = attribNodes.getLength(); x < y; ++x) {
      Attr attr = (Attr)attribNodes.item(x);
      if (!attr.getName().equals("src")) {
        attribs.put(attr.getName(), attr.getValue());
      }
    }
    return attribs.build();
  }

  static final class ParsedFeature {
    private final String name;
    private final List<String> deps;
    private final List<Bundle> bundles;

    private ParsedFeature(String name, List<String> deps, List<Bundle> bundles) {
      this.name = name;
      this.deps = ImmutableList.copyOf(deps);
      this.bundles = ImmutableList.copyOf(bundles);
    }

    public String getName() {
      return name;
    }

    public List<String> getDeps() {
      return deps;
    }

    public List<Bundle> getBundles() {
      return bundles;
    }

    public final static class Bundle {
      private final String name;
      private final String type;
      private final Map<String, String> attribs;
      private final List<Resource> resources;
      private final List<ApiDirective> apiDirectives;
      private final boolean supportDefer;

      private Bundle(String name, String type, Map<String, String> attribs,
          List<Resource> resources, List<ApiDirective> apiDirectives, boolean supportDefer) {
        this.name = name;
        this.type = type;
        this.attribs = attribs;
        this.resources = resources;
        this.apiDirectives = apiDirectives;
        this.supportDefer = supportDefer;
      }

      public String getName() {
        return name;
      }

      public String getType() {
        return type;
      }

      public Map<String, String> getAttribs() {
        return attribs;
      }

      public List<Resource> getResources() {
        return resources;
      }

      public List<ApiDirective> getApis() {
        return apiDirectives;
      }

      public boolean isSupportDefer() {
        return supportDefer;
      }
    }

    static final class Resource {
      private final Uri source;
      private final String content;
      private final Map<String, String> attribs;

      private Resource(Uri source, String content, Map<String, String> attribs) {
        this.source = source;
        this.content = content;
        this.attribs = ImmutableMap.copyOf(attribs);
      }

      public Uri getSource() {
        return source;
      }

      public String getContent() {
        return content;
      }

      public Map<String, String> getAttribs() {
        return attribs;
      }
    }
  }
}
