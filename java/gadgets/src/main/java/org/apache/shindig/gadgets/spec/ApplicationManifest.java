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

import com.google.common.collect.ImmutableMap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents an opensocial application manifest.
 */
public class ApplicationManifest {
  public static final String NAMESPACE = "http://ns.opensocial.org/2008/application";

  private final Map<String, String> versions;
  private final Map<String, Uri> gadgets;
  private final Uri uri;

  public ApplicationManifest(Uri uri, Element xml) throws SpecParserException {
    ImmutableMap.Builder<String, String> versions = ImmutableMap.builder();
    ImmutableMap.Builder<String, Uri> gadgets = ImmutableMap.builder();

    NodeList nodes = xml.getElementsByTagName("gadget");
    for (int i = 0, j = nodes.getLength(); i < j; ++i) {
      Element gadget = (Element) nodes.item(i);
      String version = getVersionString(gadget);
      Uri spec = getSpecUri(uri, gadget);
      gadgets.put(version, spec);
      for (String label : getLabels(gadget)) {
        versions.put(label, version);
      }
    }

    this.uri = uri;
    this.versions = versions.build();
    this.gadgets = gadgets.build();
  }

  private static Uri getSpecUri(Uri baseUri, Element gadget) throws SpecParserException {
    NodeList specs = gadget.getElementsByTagName("spec");

    if (specs.getLength() > 1) {
      throw new SpecParserException("Only one spec per gadget block may be specified.");
    } else if (specs.getLength() == 0) {
      throw new SpecParserException("No spec specified.");
    }

    try {
      String relative = specs.item(0).getTextContent();
      Uri specUri = baseUri.resolve(Uri.parse(relative));
      if (specUri.equals(baseUri)) {
        throw new SpecParserException("Manifest is self-referencing.");
      }
      return specUri;
    } catch (IllegalArgumentException e) {
      throw new SpecParserException("Invalid spec URI.");
    }
  }

  private static String getVersionString(Element gadget) throws SpecParserException {
    NodeList versions = gadget.getElementsByTagName("version");

    if (versions.getLength() > 1) {
      throw new SpecParserException("Only one version per gadget block may be specified.");
    } else if (versions.getLength() == 0) {
      throw new SpecParserException("No version specified.");
    }

    return versions.item(0).getTextContent();
  }

  private static List<String> getLabels(Element gadget) {
    NodeList labels = gadget.getElementsByTagName("label");
    List<String> list = new ArrayList<String>(labels.getLength());

    for (int i = 0, j = labels.getLength(); i < j; ++i) {
      list.add(labels.item(i).getTextContent());
    }

    return list;
  }

  /**
   * @return The URI of this manifest.
   */
  public Uri getUri() {
    return uri;
  }

  /**
   * @return The gadget specified for the version string, or null if the version doesn't exist.
   */
  public Uri getGadget(String version) {
    return gadgets.get(version);
  }

  /**
   * @return The version of the gadget for the given label, or null if the label is unsupported.
   */
  public String getVersion(String label) {
    return versions.get(label);
  }
}
