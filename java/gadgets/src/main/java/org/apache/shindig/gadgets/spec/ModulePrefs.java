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
import org.apache.shindig.gadgets.Substitutions;
import org.apache.shindig.util.XmlUtil;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents the ModulePrefs element of a gadget spec.
 *
 * This encapsulates most gadget meta data, including everything except for
 * Content and UserPref nodes.
 */
public class ModulePrefs {
  // Canonical spec items first.

  /**
   * ModulePrefs@title
   *
   * User Pref + Message Bundle + Bidi
   */
  private String title;
  public String getTitle() {
    return title;
  }

  /**
   * ModulePrefs@title_url
   *
   * User Pref + Message Bundle + Bidi
   */
  private URI titleUrl;
  public URI getTitleUrl() {
    return titleUrl;
  }

  /**
   * ModulePrefs@description
   *
   * Message Bundles
   */
  private String description;
  public String getDescription() {
    return description;
  }

  /**
   * ModulePrefs@author
   *
   * Message Bundles
   */
  private String author;
  public String getAuthor() {
    return author;
  }

  /**
   * ModulePrefs@author_email
   *
   * Message Bundles
   */
  private String authorEmail;
  public String getAuthorEmail() {
    return authorEmail;
  }

  /**
   * ModulePrefs@screenshot
   *
   * Message Bundles
   */
  private URI screenshot;
  public URI getScreenshot() {
    return screenshot;
  }

  /**
   * ModulePrefs@thumbnail
   *
   * Message Bundles
   */
  private URI thumbnail;
  public URI getThumbnail() {
    return thumbnail;
  }

  // Extended data (typically used by directories)

  /**
   * ModulePrefs@directory_title
   *
   * Message Bundles
   */
  private String directoryTitle;
  public String getDirectoryTitle() {
    return directoryTitle;
  }

  /*
   * The following ModulePrefs attributes are skipped:
   *
   * author_affiliation
   * author_location
   * author_photo
   * author_aboutme
   * author_quote
   * author_link
   * show_stats
   * show_in_directory
   */

  /**
   * ModuleSpec@width
   */
  private final int width;
  public int getWidth() {
    return width;
  }

  /**
   * ModuleSpec@width
   */
  private final int height;
  public int getHeight() {
    return height;
  }

  /**
   * ModuleSpec@category
   * ModuleSpec@category2
   * These fields are flattened into a single list.
   */
  private final List<String> categories;
  public List<String> getCategories() {
    return categories;
  }

  /**
   * Skipped:
   *
   * singleton
   * render_inline
   * scaling
   * scrolling
   */

  /**
   * ModuleSpec.Require
   * ModuleSpec.Optional
   */
  private final Map<String, Feature> features;
  public Map<String, Feature> getFeatures() {
    return features;
  }

  /**
   * ModuleSpec.Preload
   */
  private final List<URI> preloads;
  public List<URI> getPreloads() {
    return preloads;
  }

  /**
   * ModuleSpec.Icon
   */
  private List<Icon> icons;
  public List<Icon> getIcons() {
    return icons;
  }

  /**
   * ModuleSpec.Locale
   */
  private final Map<Locale, LocaleSpec> locales;
  public Map<Locale, LocaleSpec> getLocales() {
    return locales;
  }

  /**
   * Attempts to retrieve a valid LocaleSpec for the given Locale.
   * First tries to find an exact language / country match.
   * Then tries to find a match for language / all.
   * Then tries to find a match for all / all.
   * Finally gives up.
   * @param locale
   * @return The locale spec, if there is a matching one, or null.
   */
  public LocaleSpec getLocale(Locale locale) {
    if (locales.size() == 0) {
      return null;
    }
    LocaleSpec localeSpec = locales.get(locale);
    if (localeSpec == null) {
      locale = new Locale(locale.getLanguage(), "ALL");
      localeSpec = locales.get(locale);
      if (localeSpec == null) {
        localeSpec = locales.get(GadgetSpec.DEFAULT_LOCALE);
      }
    }

    return localeSpec;
  }

  /**
   * Produces a new ModulePrefs by substituting hangman variables from
   * substituter. See comments on individual fields to see what actually
   * has substitutions performed.
   *
   * @param substituter
   */
  public ModulePrefs substitute(Substitutions substituter) {
    ModulePrefs prefs = new ModulePrefs(this);

    // Icons, if any
    if (icons.size() == 0) {
      prefs.icons = Collections.emptyList();
    } else {
      List<Icon> iconList = new ArrayList<Icon>(icons.size());
      for (Icon icon : icons) {
        iconList.add(icon.substitute(substituter));
      }
      prefs.icons = Collections.unmodifiableList(iconList);
    }

    Substitutions.Type type = Substitutions.Type.MESSAGE;
    // Most attributes only get strings.
    prefs.author = substituter.substituteString(type, author);
    prefs.authorEmail = substituter.substituteString(type, authorEmail);
    prefs.description = substituter.substituteString(type, description);
    prefs.directoryTitle = substituter.substituteString(type, directoryTitle);
    prefs.screenshot = substituter.substituteUri(type, screenshot);
    prefs.thumbnail = substituter.substituteUri(type, thumbnail);

    // All types.
    prefs.title = substituter.substituteString(null, title);
    prefs.titleUrl = substituter.substituteUri(null, titleUrl);
    return prefs;
  }


  /**
   * Walks child nodes of the given node.
   * @param element
   * @param visitors Map of tag names to visitors for that tag.
   */
  private static void walk(Element element, Map<String, ElementVisitor> visitors)
      throws SpecParserException {
    NodeList children = element.getChildNodes();
    for (int i = 0, j = children.getLength(); i < j; ++i) {
      Node child = children.item(i);
      ElementVisitor visitor = visitors.get(child.getNodeName());
      if (visitor != null) {
        visitor.visit((Element)child);
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("<ModulePrefs")
       .append(" title=\"").append(title).append("\"")
       .append(" author=\"").append(author).append("\"")
       .append(" author_email=\"").append(authorEmail).append("\"")
       .append(" description=\"").append(description).append("\"")
       .append(" directory_title=\"").append(directoryTitle).append("\"")
       .append(" screenshot=\"").append(screenshot).append("\"")
       .append(" thumbnail=\"").append(thumbnail).append("\"")
       .append(" height=\"").append(height).append("\"")
       .append(" width=\"").append(width).append("\"")
       .append(" category1=\"").append(categories.get(0)).append("\"")
       .append(" category2=\"").append(categories.get(1)).append("\"")
       .append(">\n");
    for (URI preload : preloads) {
      buf.append("<Preload href=\"").append(preload).append("\"/>\n");
    }
    for (Feature feature : features.values()) {
      buf.append(feature).append("\n");
    }
    for (Icon icon : icons) {
      buf.append(icon).append("\n");
    }
    for (LocaleSpec locale : locales.values()) {
      buf.append(locale).append("\n");
    }
    buf.append("</ModulePrefs>");
    return buf.toString();
  }

  /**
   * @param element
   * @param specUrl
   */
  public ModulePrefs(Element element, URI specUrl) throws SpecParserException {
    title = XmlUtil.getAttribute(element, "title");
    if (title == null) {
      throw new SpecParserException("ModulePrefs@title is required.");
    }
    URI emptyUri = URI.create("");
    titleUrl = XmlUtil.getUriAttribute(element, "title_url", emptyUri);
    author = XmlUtil.getAttribute(element, "author", "");
    authorEmail = XmlUtil.getAttribute(element, "author_email", "");
    description = XmlUtil.getAttribute(element, "description", "");
    directoryTitle = XmlUtil.getAttribute(element, "directory_title", "");
    screenshot = XmlUtil.getUriAttribute(element, "screenshot", emptyUri);
    thumbnail = XmlUtil.getUriAttribute(element, "thumbnail", emptyUri);

    String height = XmlUtil.getAttribute(element, "height");
    if (height == null) {
      this.height = 0;
    } else {
      this.height = Integer.parseInt(height);
    }
    String width = XmlUtil.getAttribute(element, "width");
    if (width == null) {
      this.width = 0;
    } else {
      this.width = Integer.parseInt(width);
    }
    categories = Arrays.asList(
        XmlUtil.getAttribute(element, "category1", ""),
        XmlUtil.getAttribute(element, "category2", ""));

    // Child elements
    PreloadVisitor preloadVisitor = new PreloadVisitor();
    FeatureVisitor featureVisitor = new FeatureVisitor();
    IconVisitor iconVisitor = new IconVisitor();
    LocaleVisitor localeVisitor = new LocaleVisitor(specUrl);
    Map<String, ElementVisitor> visitors = new HashMap<String, ElementVisitor>();
    visitors.put("Preload", preloadVisitor);
    visitors.put("Optional", featureVisitor);
    visitors.put("Require", featureVisitor);
    visitors.put("Icon", iconVisitor);
    visitors.put("Locale", localeVisitor);
    walk(element, visitors);
    preloads = Collections.unmodifiableList(preloadVisitor.preloads);
    features = Collections.unmodifiableMap(featureVisitor.features);
    icons = Collections.unmodifiableList(iconVisitor.icons);
    locales = Collections.unmodifiableMap(localeVisitor.locales);
  }

  /**
   * Creates an empty module prefs for substitute() to use.
   */
  private ModulePrefs(ModulePrefs prefs) {
    categories = prefs.getCategories();
    preloads = prefs.getPreloads();
    features = prefs.getFeatures();
    locales = prefs.getLocales();
    height = prefs.getHeight();
    width = prefs.getWidth();
  }
}

interface ElementVisitor {
  public void visit(Element element) throws SpecParserException;
}

/**
 * Processes ModulePrefs.Preload into a list.
 */
class PreloadVisitor implements ElementVisitor {
  final List<URI> preloads = new LinkedList<URI>();
  public void visit(Element element) throws SpecParserException {
    URI href = XmlUtil.getUriAttribute(element, "href");
    if (href == null) {
      throw new SpecParserException("Preload@href is required.");
    }
    preloads.add(href);
  }
}

/**
 * Processes ModulePrefs.Require and ModulePrefs.Optional
 */
class FeatureVisitor implements ElementVisitor {
  final Map<String, Feature> features = new HashMap<String, Feature>();
  public void visit (Element element) throws SpecParserException {
    Feature feature = new Feature(element);
    features.put(feature.getName(), feature);
  }
}

/**
 * Processes ModulePrefs.Icon
 */
class IconVisitor implements ElementVisitor {
  final List<Icon> icons = new LinkedList<Icon>();
  public void visit(Element element) throws SpecParserException {
    icons.add(new Icon(element));
  }
}

/**
 * Process ModulePrefs.Locale
 */
class LocaleVisitor implements ElementVisitor {
  final URI base;
  final Map<Locale, LocaleSpec> locales
      = new HashMap<Locale, LocaleSpec>();
  public void visit(Element element) throws SpecParserException {
    LocaleSpec locale = new LocaleSpec(element, base);
    locales.put(new Locale(locale.getLanguage(), locale.getCountry()), locale);
  }
  public LocaleVisitor(URI base) {
    this.base = base;
  }
}
