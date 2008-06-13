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
import org.apache.shindig.gadgets.Substitutions;

import org.w3c.dom.Element;

import java.net.URI;
import java.util.List;

/**
 * Represents a Content section, but normalized into an individual
 * view value after views are split on commas.
 */
public class View {

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
  public void setHref(URI href) {
    this.href = href;
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
  public void setContent(String content) {
    this.content = content;
  }

  /**
   * Whether or not the content section has any __UP_ hangman variables.
   */
  private final boolean needsUserPrefSubstitution;
  public boolean needsUserPrefSubstitution() {
    return needsUserPrefSubstitution;
  }

  /**
   * Creates a new view by performing hangman substitution. See field comments
   * for details on what gets substituted.
   *
   * @param substituter
   * @return The substituted view.
   */
  public View substitute(Substitutions substituter, boolean rewrite) {
    View view = new View(this);
    if (rewrite && rewrittenContent != null) {
      view.content = substituter.substituteString(null, rewrittenContent);
    } else {
      view.content = substituter.substituteString(null, content);
    }
    view.href = substituter.substituteUri(null, href);
    return view;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("<Content type=\"")
       .append(rawType)
       .append("\" href=\"")
       .append(href)
       .append("\" view=\"")
       .append(name)
       .append("\" quirks=\"")
       .append(quirks)
       .append("\" preferredHeight=\"")
       .append(preferredHeight)
       .append("\" preferredWidth=\"")
       .append(preferredWidth)
       .append("\">")
       .append(content)
       .append("</Content>");
    return buf.toString();
  }

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
    StringBuilder content = new StringBuilder();
    for (Element element : elements) {
      contentType = XmlUtil.getAttribute(element, "type");
      if (contentType != null) {
        ContentType newType = ContentType.parse(contentType);
        if (type != null && newType != type) {
          throw new SpecParserException(
              "You may not mix content types in the same view.");
        } else {
          type = newType;
        }
      }
      href = XmlUtil.getUriAttribute(element, "href", href);
      quirks = XmlUtil.getBoolAttribute(element, "quirks", quirks);
      preferredHeight = XmlUtil.getIntAttribute(element, "preferred_height");
      preferredWidth = XmlUtil.getIntAttribute(element, "preferred_width");
      content.append(element.getTextContent());
    }
    this.content = content.toString();
    this.needsUserPrefSubstitution = this.content.contains("__UP_");
    this.quirks = quirks;
    this.href = href;
    this.rawType = contentType;
    this.type = type;
    this.preferredHeight = preferredHeight;
    this.preferredWidth = preferredWidth;
    if (type == ContentType.URL && this.href == null) {
      throw new SpecParserException(
          "Content@href must be set when Content@type is \"url\".");
    }
  }

  /**
   * Allows the creation of a view from an existing view so that localization
   * can be performed.
   *
   * @param view
   */
  private View(View view) {
    needsUserPrefSubstitution = view.needsUserPrefSubstitution;
    name = view.name;
    rawType = view.rawType;
    type = view.type;
    quirks = view.quirks;
    preferredHeight = view.preferredHeight;
    preferredWidth = view.preferredWidth;
  }

  /**
   * Possible values for Content@type
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

  //
  // Decorations
  //
  private String rewrittenContent;

  public String getRewrittenContent() {
    return rewrittenContent;
  }

  public void setRewrittenContent(String rewrittenContent) {
    this.rewrittenContent = rewrittenContent;
  }
}