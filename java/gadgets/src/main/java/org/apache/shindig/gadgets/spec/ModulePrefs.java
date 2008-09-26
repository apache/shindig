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
import org.apache.shindig.gadgets.variables.Substitutions;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URI;
import java.net.URISyntaxException;
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

  private static final String ATTR_TITLE = "title";
  private static final String ATTR_TITLE_URL = "title_url";
  private static final String ATTR_DESCRIPTION = "description";
  private static final String ATTR_AUTHOR = "author";
  private static final String ATTR_AUTHOR_EMAIL = "author_email";
  private static final String ATTR_SCREENSHOT = "screenshot";
  private static final String ATTR_THUMBNAIL = "thumbnail";
  private static final String ATTR_DIRECTORY_TITLE = "directory_title";
  private static final String ATTR_AUTHOR_AFFILIATION = "author_affiliation";
  private static final String ATTR_AUTHOR_LOCATION = "author_location";
  private static final String ATTR_AUTHOR_PHOTO = "author_photo";
  private static final String ATTR_AUTHOR_ABOUTME = "author_aboutme";
  private static final String ATTR_AUTHOR_QUOTE = "author_quote";
  private static final String ATTR_AUTHOR_LINK = "author_link";
  private static final String ATTR_SHOW_STATS = "show_stats";
  private static final String ATTR_SHOW_IN_DIRECTORY = "show_in_directory";
  private static final String ATTR_SINGLETON = "singleton";
  private static final String ATTR_SCALING = "scaling";
  private static final String ATTR_SCROLLING = "scrolling";
  private static final String ATTR_WIDTH = "width";
  private static final String ATTR_HEIGHT = "height";
  private static final String ATTR_CATEGORY = "category";
  private static final String ATTR_CATEGORY2 = "category2";

  private static final URI EMPTY_URI = URI.create("");

  private final Map<String, String> attributes;

  // Canonical spec items first.

  /**
   * ModulePrefs@title
   *
   * User Pref + Message Bundle + Bidi
   */
  public String getTitle() {
    return getAttribute(ATTR_TITLE);
  }

  /**
   * ModulePrefs@title_url
   *
   * User Pref + Message Bundle + Bidi
   */
  public URI getTitleUrl() {
    return getUriAttribute(ATTR_TITLE_URL);
  }

  /**
   * ModulePrefs@description
   *
   * Message Bundles
   */
  public String getDescription() {
    return getAttribute(ATTR_DESCRIPTION);
  }

  /**
   * ModulePrefs@author
   *
   * Message Bundles
   */
  public String getAuthor() {
    return getAttribute(ATTR_AUTHOR);
  }

  /**
   * ModulePrefs@author_email
   *
   * Message Bundles
   */
  public String getAuthorEmail() {
    return getAttribute(ATTR_AUTHOR_EMAIL);
  }

  /**
   * ModulePrefs@screenshot
   *
   * Message Bundles
   */
  public URI getScreenshot() {
    return getUriAttribute(ATTR_SCREENSHOT);
  }

  /**
   * ModulePrefs@thumbnail
   *
   * Message Bundles
   */
  public URI getThumbnail() {
    return getUriAttribute(ATTR_THUMBNAIL);
  }

  // Extended data (typically used by directories)

  /**
   * ModulePrefs@directory_title
   *
   * Message Bundles
   */
  public String getDirectoryTitle() {
    return getAttribute(ATTR_DIRECTORY_TITLE);
  }

  /**
   * ModulePrefs@author_affiliation
   *
   * Message Bundles
   */
  public String getAuthorAffiliation() {
    return getAttribute(ATTR_AUTHOR_AFFILIATION);
  }

  /**
   * ModulePrefs@author_location
   *
   * Message Bundles
   */
  public String getAuthorLocation() {
    return getAttribute(ATTR_AUTHOR_LOCATION);
  }

  /**
   * ModulePrefs@author_photo
   *
   * Message Bundles
   */
  public String getAuthorPhoto() {
    return getAttribute(ATTR_AUTHOR_PHOTO);
  }

  /**
   * ModulePrefs@author_aboutme
   *
   * Message Bundles
   */
  public String getAuthorAboutme() {
    return getAttribute(ATTR_AUTHOR_ABOUTME);
  }

  /**
   * ModulePrefs@author_quote
   *
   * Message Bundles
   */
  public String getAuthorQuote() {
    return getAttribute(ATTR_AUTHOR_QUOTE);
  }

  /**
   * ModulePrefs@author_link
   *
   * Message Bundles
   */
  public String getAuthorLink() {
    return getAttribute(ATTR_AUTHOR_LINK);
  }

  /**
   * ModulePrefs@show_stats
   */
  public boolean getShowStats() {
    return getBoolAttribute(ATTR_SHOW_STATS);
  }

  /**
   * ModulePrefs@show_in_directory
   */
  public boolean getShowInDirectory() {
    return getBoolAttribute(ATTR_SHOW_IN_DIRECTORY);
  }

  /**
   * ModulePrefs@singleton
   */
  public boolean getSingleton() {
    return getBoolAttribute(ATTR_SINGLETON);
  }

  /**
   * ModulePrefs@scaling
   */
  public boolean getScaling() {
    return getBoolAttribute(ATTR_SCALING);
  }

  /**
   * ModulePrefs@scrolling
   */
  public boolean getScrolling() {
    return getBoolAttribute(ATTR_SCROLLING);
  }

  /**
   * ModuleSpec@width
   */
  public int getWidth() {
    return getIntAttribute(ATTR_WIDTH);
  }

  /**
   * ModuleSpec@height
   */
  public int getHeight() {
    return getIntAttribute(ATTR_HEIGHT);
  }

  /**
   * @return the value of an ModulePrefs attribute by name, or null if the
   *     attribute doesn't exist
   */
  public String getAttribute(String name) {
    return attributes.get(name);
  }

  /**
   * @return the value of an ModulePrefs attribute by name, or the default
   *     value if the attribute doesn't exist
   */
  public String getAttribute(String name, String defaultValue) {
    String value = getAttribute(name);
    if (value == null) {
      return defaultValue;
    } else {
      return value;
    }
  }

  /**
   * @return the attribute by name converted to an URI, or the empty URI if the
   *    attribute couldn't be converted
   */
  public URI getUriAttribute(String name) {
    String uri = getAttribute(name);
    if (uri != null) {
      try {
        return new URI(uri);
      } catch (URISyntaxException e) {
        return EMPTY_URI;
      }
    }
    return EMPTY_URI;
  }

  /**
   * @return the attribute by name converted to a boolean (false if the
   *     attribute doesn't exist)
   */
  public boolean getBoolAttribute(String name) {
    String value = getAttribute(name);
    return !(value == null || "false".equals(value));
  }

  /**
   * @return the attribute by name converted to an interger, or 0 if the
   *     attribute doesn't exist
   */
  public int getIntAttribute(String name) {
    String value = getAttribute(name);
    if (value == null) {
      return 0;
    } else {
      // TODO might want to handle parse exception here
      return Integer.parseInt(value);
    }
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
   * ModuleSpec/Require
   * ModuleSpec/Optional
   */
  private final Map<String, Feature> features;
  public Map<String, Feature> getFeatures() {
    return features;
  }

  /**
   * ModuleSpec/Preload
   */
  private final List<Preload> preloads;
  public List<Preload> getPreloads() {
    return preloads;
  }

  /**
   * ModuleSpec/Icon
   */
  private final List<Icon> icons;
  public List<Icon> getIcons() {
    return icons;
  }

  /**
   * ModuleSpec/Locale
   */
  private final Map<Locale, LocaleSpec> locales;
  public Map<Locale, LocaleSpec> getLocales() {
    return locales;
  }

  /**
   * ModuleSpec/Link
   */
  private final Map<String, LinkSpec> links;
  public Map<String, LinkSpec> getLinks() {
    return links;
  }
  
  /**
   * ModuleSpec/OAuthSpec
   */
  private final OAuthSpec oauth;
  public OAuthSpec getOAuthSpec() {
    return oauth;
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
    if (locales.isEmpty()) {
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
    return new ModulePrefs(this, substituter);
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
    buf.append("<ModulePrefs");

    for (Map.Entry<String, String> attr : attributes.entrySet()) {
      buf.append(' ').append(attr.getKey()).append("=\"")
         .append(attr.getValue()).append('\"');
    }
    buf.append(">\n");

    for (Preload preload : preloads) {
      buf.append(preload).append('\n');
    }
    for (Feature feature : features.values()) {
      buf.append(feature).append('\n');
    }
    for (Icon icon : icons) {
      buf.append(icon).append('\n');
    }
    for (LocaleSpec locale : locales.values()) {
      buf.append(locale).append('\n');
    }
    for (LinkSpec link : links.values()) {
      buf.append(link).append('\n');
    }
    if (oauth != null) {
      buf.append(oauth).append('\n');
    }
    buf.append("</ModulePrefs>");
    return buf.toString();
  }

  /**
   * @param element
   * @param specUrl
   */
  public ModulePrefs(Element element, URI specUrl) throws SpecParserException {
    attributes = new HashMap<String, String>();
    NamedNodeMap attributeNodes = element.getAttributes();
    for (int i = 0; i < attributeNodes.getLength(); i++) {
      Node node = attributeNodes.item(i);
      attributes.put(node.getNodeName(), node.getNodeValue());
    }

    if (getTitle() == null) {
      throw new SpecParserException("ModulePrefs@title is required.");
    }

    categories = Arrays.asList(
        getAttribute(ATTR_CATEGORY, ""), getAttribute(ATTR_CATEGORY2, ""));

    // Child elements
    PreloadVisitor preloadVisitor = new PreloadVisitor();
    FeatureVisitor featureVisitor = new FeatureVisitor();
    OAuthVisitor oauthVisitor = new OAuthVisitor();
    IconVisitor iconVisitor = new IconVisitor();
    LocaleVisitor localeVisitor = new LocaleVisitor(specUrl);
    LinkVisitor linkVisitor = new LinkVisitor();

    Map<String, ElementVisitor> visitors
        = new HashMap<String, ElementVisitor>(6, 1);
    visitors.put("Preload", preloadVisitor);
    visitors.put("Optional", featureVisitor);
    visitors.put("Require", featureVisitor);
    visitors.put("OAuth", oauthVisitor);
    visitors.put("Icon", iconVisitor);
    visitors.put("Locale", localeVisitor);
    visitors.put("Link", linkVisitor);

    walk(element, visitors);

    preloads = Collections.unmodifiableList(preloadVisitor.preloads);
    features = Collections.unmodifiableMap(featureVisitor.features);
    icons = Collections.unmodifiableList(iconVisitor.icons);
    locales = Collections.unmodifiableMap(localeVisitor.locales);
    links = Collections.unmodifiableMap(linkVisitor.links);
    oauth = oauthVisitor.oauth;
  }

  /**
   * Produces a new, substituted ModulePrefs
   */
  private ModulePrefs(ModulePrefs prefs, Substitutions substituter) {
    categories = prefs.getCategories();
    features = prefs.getFeatures();
    locales = prefs.getLocales();
    oauth = prefs.oauth;

    List<Preload> preloads = new ArrayList<Preload>(prefs.preloads.size());
    for (Preload preload : prefs.preloads) {
      preloads.add(preload.substitute(substituter));
    }
    this.preloads = Collections.unmodifiableList(preloads);

    List<Icon> icons = new ArrayList<Icon>(prefs.icons.size());
    for (Icon icon : prefs.icons) {
      icons.add(icon.substitute(substituter));
    }
    this.icons = Collections.unmodifiableList(icons);

    Map<String, LinkSpec> links = new HashMap<String, LinkSpec>(prefs.links.size());
    for (LinkSpec link : prefs.links.values()) {
      LinkSpec sub = link.substitute(substituter);
      links.put(sub.getRel(), sub);
    }
    this.links = Collections.unmodifiableMap(links);

    Map<String, String> attributes
        = new HashMap<String, String>(prefs.attributes.size());
    for (Map.Entry<String, String> attr : prefs.attributes.entrySet()) {
      String substituted = substituter.substituteString(null, attr.getValue());
      attributes.put(attr.getKey(), substituted);
    }
    this.attributes = Collections.unmodifiableMap(attributes);
  }
}

interface ElementVisitor {
  public void visit(Element element) throws SpecParserException;
}

/**
 * Processes ModulePrefs/Preload into a list.
 */
class PreloadVisitor implements ElementVisitor {
  final List<Preload> preloads = new LinkedList<Preload>();
  public void visit(Element element) throws SpecParserException {
    Preload preload = new Preload(element);
    preloads.add(preload);
  }
}

/**
 * Process ModulePrefs/OAuth
 */
class OAuthVisitor implements ElementVisitor {
  OAuthSpec oauth;
  public void visit(Element element) throws SpecParserException {
    if (oauth != null) {
      throw new SpecParserException("ModulePrefs/OAuth may only occur once.");
    }
    oauth = new OAuthSpec(element);
  }
  public OAuthVisitor() {
    this.oauth = null;
  }
}

/**
 * Processes ModulePrefs/Require and ModulePrefs/Optional
 */
class FeatureVisitor implements ElementVisitor {
  final Map<String, Feature> features = new HashMap<String, Feature>();
  public void visit (Element element) throws SpecParserException {
    Feature feature = new Feature(element);
    features.put(feature.getName(), feature);
  }
}

/**
 * Processes ModulePrefs/Icon
 */
class IconVisitor implements ElementVisitor {
  final List<Icon> icons = new LinkedList<Icon>();
  public void visit(Element element) throws SpecParserException {
    icons.add(new Icon(element));
  }
}

/**
 * Process ModulePrefs/Locale
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

/**
 * Process ModulePrefs/Link
 */
class LinkVisitor implements ElementVisitor {
  final Map<String, LinkSpec> links = new HashMap<String, LinkSpec>();

  public void visit(Element element) throws SpecParserException {
    LinkSpec link = new LinkSpec(element);
    links.put(link.getRel(), link);
  }
}
