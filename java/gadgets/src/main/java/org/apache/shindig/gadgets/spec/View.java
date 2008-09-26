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
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.variables.Substitutions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import edu.emory.mathcs.backport.java.util.Collections;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a Content section, but normalized into an individual
 * view value after views are split on commas.
 */
public class View implements RequestAuthenticationInfo {
  private static final Set<String> KNOWN_ATTRIBUTES = ImmutableSet.of(
      "type", "view", "href", "preferred_height", "preferred_width", "authz", "quirks",
      "sign_owner", "sign_viwer"
  );

  /**
   * @param elements List of all views, in order, that make up this view.
   *     An ordered list is required per the spec, since values must
   *     overwrite one another.
   * @throws SpecParserException
   */
  public View(String name, List<Element> elements) throws SpecParserException {
    this.name = name;

    boolean quirks = true;
    URI href = null;
    String contentType = null;
    ContentType type = null;
    int preferredHeight = 0;
    int preferredWidth = 0;
    String auth = null;
    boolean signOwner = true;
    boolean signViewer = true;
    Map<String, String> attributes = Maps.newHashMap();
    StringBuilder content = new StringBuilder();
    for (Element element : elements) {
      contentType = XmlUtil.getAttribute(element, "type");
      if (contentType != null) {
        ContentType newType = ContentType.parse(contentType);
        if (type != null && newType != type) {
          throw new SpecParserException("You may not mix content types in the same view.");
        } else {
          type = newType;
        }
      }
      href = XmlUtil.getUriAttribute(element, "href", href);
      quirks = XmlUtil.getBoolAttribute(element, "quirks", quirks);
      preferredHeight = XmlUtil.getIntAttribute(element, "preferred_height");
      preferredWidth = XmlUtil.getIntAttribute(element, "preferred_width");
      auth = XmlUtil.getAttribute(element, "authz", auth);
      signOwner = XmlUtil.getBoolAttribute(element, "sign_owner", signOwner);
      signViewer = XmlUtil.getBoolAttribute(element, "sign_viewer", signViewer);
      content.append(element.getTextContent());
      NamedNodeMap attrs = element.getAttributes();
      for (int i = 0; i < attrs.getLength(); ++i) {
        Node attr = attrs.item(i);
        if (!KNOWN_ATTRIBUTES.contains(attr.getNodeName())) {
          attributes.put(attr.getNodeName(), attr.getNodeValue());
        }
      }
    }
    this.content = content.toString();
    this.needsUserPrefSubstitution = this.content.contains("__UP_");
    this.quirks = quirks;
    this.href = href;
    this.rawType = contentType == null ? "html" : contentType;
    this.type = type == null ? ContentType.HTML : type;
    this.preferredHeight = preferredHeight;
    this.preferredWidth = preferredWidth;
    this.attributes = Collections.unmodifiableMap(attributes);
    this.authType = AuthType.parse(auth);
    this.signOwner = signOwner;
    this.signViewer = signViewer;
    if (type == ContentType.URL && this.href == null) {
      throw new SpecParserException("Content@href must be set when Content@type is \"url\".");
    }
  }

  /**
   * Allows the creation of a view from an existing view so that localization
   * can be performed.
   */
  private View(View view, Substitutions substituter) {
    needsUserPrefSubstitution = view.needsUserPrefSubstitution;
    name = view.name;
    rawType = view.rawType;
    type = view.type;
    quirks = view.quirks;
    preferredHeight = view.preferredHeight;
    preferredWidth = view.preferredWidth;
    authType = view.authType;
    signOwner = view.signOwner;
    signViewer = view.signViewer;

    content = substituter.substituteString(null, view.content);
    href = substituter.substituteUri(null, view.href);
    Map<String, String> attributes = Maps.newHashMap();
    for (Map.Entry<String, String> entry : view.attributes.entrySet()) {
      attributes.put(entry.getKey(), substituter.substituteString(null, entry.getValue()));
    }
    this.attributes = Collections.unmodifiableMap(attributes);
  }

  /**
   * Content@view
   */
  private final String name;
  public String getName() {
    return name;
  }

  /**
   * Content@type
   */
  private final ContentType type;
  public ContentType getType() {
    return type;
  }

  /**
   * Content@type - the raw, possibly non-standard string
   */
  private final String rawType;
  public String getRawType() {
    return rawType;
  }

  /**
   * Content@href
   *
   * All substitutions
   */
  private URI href;
  public URI getHref() {
    return href;
  }

  /**
   * Content@quirks
   */
  private final boolean quirks;
  public boolean getQuirks() {
    return quirks;
  }

  /**
   * Content@preferred_height
   */
  private final int preferredHeight;
  public int getPreferredHeight() {
    return preferredHeight;
  }

  /**
   * Content@preferred_width
   */
  private final int preferredWidth;
  public int getPreferredWidth() {
    return preferredWidth;
  }

  /**
   * Content#CDATA
   *
   * All substitutions
   */
  private String content;
  public String getContent() {
    return content;
  }

  /**
   * Set content for a type=html, href=URL style gadget.
   * This is the last bastion of GadgetSpec mutability,
   * and should only be used for the described case.
   * Call nulls out href in order to indicate content was
   * successfully retrieved.
   * @param content New gadget content retrieved from href.
   */
  public void setHrefContent(String content) {
    this.content = content;
    this.href = null;
  }

  /**
   * Whether or not the content section has any __UP_ hangman variables.
   */
  private final boolean needsUserPrefSubstitution;
  public boolean needsUserPrefSubstitution() {
    return needsUserPrefSubstitution;
  }

  /**
   * Content/@authz
   */
  private final AuthType authType;
  public AuthType getAuthType() {
    return authType;
  }

  /**
   * Content/@sign_owner
   */
  private final boolean signOwner;
  public boolean isSignOwner() {
    return signOwner;
  }

  /**
   * Content/@sign_viewer
   */
  private final boolean signViewer;
  public boolean isSignViewer() {
    return signViewer;
  }

  /**
   * All attributes.
   */
  private final Map<String, String> attributes;
  public Map<String, String> getAttributes() {
    return attributes;
  }

  /**
   * Creates a new view by performing hangman substitution. See field comments
   * for details on what gets substituted.
   *
   * @param substituter
   * @return The substituted view.
   */
  public View substitute(Substitutions substituter) {
    return new View(this, substituter);
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("<Content")
       .append(" type='").append(rawType).append('\'')
       .append(" href='").append(href).append('\'')
       .append(" view='").append(name).append('\'')
       .append(" quirks='").append(quirks).append('\'')
       .append(" preferred_height='").append(preferredHeight).append('\'')
       .append(" preferred_width='").append(preferredWidth).append('\'')
       .append(" authz=").append(authType.toString().toLowerCase()).append('\'');
    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      buf.append(entry.getKey()).append("='").append(entry.getValue()).append('\'');
    }
    buf.append("'>")
       .append(content)
       .append("</Content>");
    return buf.toString();
  }

  /**
   * Possible values for Content/@type
   */
  public enum ContentType {
    HTML, URL;

    /**
     * @param value
     * @return The parsed value (defaults to html)
     */
    public static ContentType parse(String value) {
      return "url".equals(value) ? URL : HTML;
    }
  }
}