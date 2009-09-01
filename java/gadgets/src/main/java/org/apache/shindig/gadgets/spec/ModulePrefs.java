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
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.variables.Substitutions;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.Collections;
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
  private static final Uri EMPTY_URI = Uri.parse("");
  private static final String UP_SUBST_PREFIX = "__UP_";

  private final Map<String, String> attributes;
  private final Uri base;
  private final boolean needsUserPrefSubstitution;

  public ModulePrefs(Element element, Uri base) throws SpecParserException {
    this.base = base;
    attributes = Maps.newHashMap();
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
    LocaleVisitor localeVisitor = new LocaleVisitor();
    LinkVisitor linkVisitor = new LinkVisitor();

    Map<String, ElementVisitor> visitors = new ImmutableMap.Builder<String,ElementVisitor>()
        .put("Preload", preloadVisitor)
        .put("Optional", featureVisitor)
        .put("Require", featureVisitor)
        .put("OAuth", oauthVisitor)
        .put("Icon", iconVisitor)
        .put("Locale", localeVisitor)
        .put("Link", linkVisitor)
        .build();

    walk(element, visitors);

    preloads = Collections.unmodifiableList(preloadVisitor.preloaded);
    features = Collections.unmodifiableMap(featureVisitor.features);
    icons = Collections.unmodifiableList(iconVisitor.icons);
    locales = Collections.unmodifiableMap(localeVisitor.localeMap);
    links = Collections.unmodifiableMap(linkVisitor.linkMap);
    oauth = oauthVisitor.oauthSpec;
    needsUserPrefSubstitution = prefsNeedsUserPrefSubstitution(this);
  }

  /**
   * Produces a new, substituted ModulePrefs
   */
  private ModulePrefs(ModulePrefs prefs, Substitutions substituter) {
    base = prefs.base;
    categories = prefs.getCategories();
    features = prefs.getFeatures();
    locales = prefs.getLocales();
    oauth = prefs.oauth;

    List<Preload> preloads = Lists.newArrayList();
    for (Preload preload : prefs.preloads) {
      preloads.add(preload.substitute(substituter));
    }
    this.preloads = ImmutableList.copyOf(preloads);

    List<Icon> icons = Lists.newArrayList();
    for (Icon icon : prefs.icons) {
      icons.add(icon.substitute(substituter));
    }
    this.icons = ImmutableList.copyOf(icons);

    ImmutableMap.Builder<String, LinkSpec> links = ImmutableMap.builder();
    for (LinkSpec link : prefs.links.values()) {
      LinkSpec sub = link.substitute(substituter);
      links.put(sub.getRel(), sub);
    }
    this.links = links.build();

    ImmutableMap.Builder<String, String> attributes = ImmutableMap.builder();
    for (Map.Entry<String, String> attr : prefs.attributes.entrySet()) {
      String substituted = substituter.substituteString(attr.getValue());
      attributes.put(attr.getKey(), substituted);
    }
    this.attributes = attributes.build();
    this.needsUserPrefSubstitution = prefs.needsUserPrefSubstitution;
  }

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
  public Uri getTitleUrl() {
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
  public Uri getScreenshot() {
    return getUriAttribute(ATTR_SCREENSHOT);
  }

  /**
   * ModulePrefs@thumbnail
   *
   * Message Bundles
   */
  public Uri getThumbnail() {
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
  public Uri getAuthorPhoto() {
    return getUriAttribute(ATTR_AUTHOR_PHOTO);
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
  public Uri getAuthorLink() {
    return getUriAttribute(ATTR_AUTHOR_LINK);
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
  public Uri getUriAttribute(String name) {
    String uriAttribute = getAttribute(name);
    if (uriAttribute != null) {
      try {
        Uri uri = Uri.parse(uriAttribute);
        return base.resolve(uri);
      } catch (IllegalArgumentException e) {
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
    return Boolean.parseBoolean(value);
  }

  /**
   * @return the attribute by name converted to an interger, or 0 if the
   *     attribute doesn't exist or is not a valid number.
   */
  public int getIntAttribute(String name) {
    String value = getAttribute(name);
    if (value == null) {
      return 0;
    } else {
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException e) {
        return 0;
      }
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
   * Not part of the spec. Indicates whether UserPref-substitutable
   * fields in this prefs require __UP_ substitution.
   */
  public boolean needsUserPrefSubstitution() {
    return needsUserPrefSubstitution;
  }

  /**
   * Gets the locale spec for the given locale, if any exists.
   *
   * @return The locale spec, if there is a matching one, or null.
   */
  public LocaleSpec getLocale(Locale locale) {
    return locales.get(locale);
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

    Joiner j = Joiner.on("\n");

    j.appendTo(buf, preloads);
    j.appendTo(buf, features.values());
    j.appendTo(buf, icons);
    j.appendTo(buf, locales.values());
    j.appendTo(buf, links.values());

    if (oauth != null) {
      buf.append(oauth).append('\n');
    }
    buf.append("</ModulePrefs>");
    return buf.toString();
  }

  /**
   * @param prefs ModulePrefs object
   * @return true if any UserPref-substitutable fields in the given
   * {@code prefs} require such substitution.
   */
  static boolean prefsNeedsUserPrefSubstitution(ModulePrefs prefs) {
    for (Preload preload : prefs.preloads) {
      if (preload.getHref().toString().contains(UP_SUBST_PREFIX)) {
        return true;
      }
    }
    return prefs.getTitle().contains(UP_SUBST_PREFIX) ||
           prefs.getTitleUrl().toString().contains(UP_SUBST_PREFIX);
  }

  interface ElementVisitor {
    void visit(Element element) throws SpecParserException;
  }

  /**
   * Processes ModulePrefs/Preload into a list.
   */
  private class PreloadVisitor implements ElementVisitor {
    private final List<Preload> preloaded = Lists.newLinkedList();

    protected PreloadVisitor() {
    }

    public void visit(Element element) throws SpecParserException {
      Preload preload = new Preload(element, base);
      preloaded.add(preload);
    }
  }

  /**
   * Process ModulePrefs/OAuth
   */
  private class OAuthVisitor implements ElementVisitor {
    private OAuthSpec oauthSpec;

    public OAuthVisitor() {
      this.oauthSpec = null;
    }

    public void visit(Element element) throws SpecParserException {
      if (oauthSpec != null) {
        throw new SpecParserException("ModulePrefs/OAuth may only occur once.");
      }
      oauthSpec = new OAuthSpec(element, base);
    }
  }

  /**
   * Processes ModulePrefs/Require and ModulePrefs/Optional
   */
  private static class FeatureVisitor implements ElementVisitor {
    private final Map<String, Feature> features = Maps.newHashMap();

    public void visit (Element element) throws SpecParserException {
      Feature feature = new Feature(element);
      features.put(feature.getName(), feature);
    }
  }

  /**
   * Processes ModulePrefs/Icon
   */
  private static class IconVisitor implements ElementVisitor {
    private final List<Icon> icons = Lists.newLinkedList();

    public void visit(Element element) throws SpecParserException {
      icons.add(new Icon(element));
    }
  }

  /**
   * Process ModulePrefs/Locale
   */
  private class LocaleVisitor implements ElementVisitor {
    private final Map<Locale, LocaleSpec> localeMap = Maps.newHashMap();

    public void visit(Element element) throws SpecParserException {
      LocaleSpec locale = new LocaleSpec(element, base);
      localeMap.put(new Locale(locale.getLanguage(), locale.getCountry()), locale);
    }

  }

  /**
   * Process ModulePrefs/Link
   */
  private class LinkVisitor implements ElementVisitor {
    private final Map<String, LinkSpec> linkMap = Maps.newHashMap();

    public void visit(Element element) throws SpecParserException {
      LinkSpec link = new LinkSpec(element, base);
      linkMap.put(link.getRel(), link);
    }
  }
}
