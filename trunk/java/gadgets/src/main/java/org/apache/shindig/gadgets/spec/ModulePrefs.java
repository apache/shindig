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

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.variables.Substitutions;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;




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
  private static final String ATTR_DOCTYPE = "doctype";
  private static final String ATTR_WIDTH = "width";
  private static final String ATTR_HEIGHT = "height";
  private static final String ATTR_CATEGORY = "category";
  private static final String ATTR_CATEGORY2 = "category2";
  private static final Uri EMPTY_URI = Uri.parse("");
  private static final String UP_SUBST_PREFIX = "__UP_";

  // Used to identify Locales that are globally scoped
  private static final String GLOBAL_LOCALE = "";

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

    categories = ImmutableList.of(getAttribute(ATTR_CATEGORY, ""), getAttribute(ATTR_CATEGORY2, ""));

    // Eventually use a list of classes
    MutableBoolean oauthMarker = new MutableBoolean(false);

    Set<ElementVisitor> visitors = ImmutableSet.of(
        new FeatureVisitor(oauthMarker),
        new PreloadVisitor(),
        new OAuthVisitor(oauthMarker),
        new OAuth2Visitor(oauthMarker),
        new IconVisitor(),
        new LocaleVisitor(),
        new LinkVisitor(),
        new ExtraElementsVisitor() // keep this last since it accepts any tag
    );

    walk(element, visitors);

    // Tell the visitors to apply their knowledge
    for (ElementVisitor ev : visitors) {
      ev.apply(this);
    }

    needsUserPrefSubstitution = prefsNeedsUserPrefSubstitution(this);
  }

  /**
   * Produces a new, substituted ModulePrefs
   * @param prefs An existing ModulePrefs instance
   * @param substituter The substituter to apply
   */
  private ModulePrefs(ModulePrefs prefs, Substitutions substituter) {
    base = prefs.base;
    categories = prefs.getCategories();
    features = prefs.getFeatures();
    globalFeatures = prefs.globalFeatures;
    allFeatures = prefs.getAllFeatures();
    allLocales = prefs.allLocales;
    locales = prefs.locales;
    oauth = prefs.oauth;
    oauth2 = prefs.oauth2;

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

    ImmutableMap.Builder<String, Feature> featureBuilder= ImmutableMap.builder();
    for (Map.Entry<String, Feature> feature : features.entrySet()) {
      ImmutableMultimap.Builder<String, String> params = ImmutableMultimap.builder();
      for (Map.Entry<String, String> param: feature.getValue().getParams().entries()){
        String substituted=substituter.substituteString(param.getValue());
        params.put(param.getKey(), substituted);
      }
      Feature oldFeature=feature.getValue();
      Feature newFeature=new Feature(oldFeature.getName(), params.build(), oldFeature.getRequired(), oldFeature.getViews());
      featureBuilder.put(feature.getKey(), newFeature);
    }
    this.features=featureBuilder.build();


    this.extraElements = ImmutableMultimap.copyOf(prefs.extraElements);
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
    String title = getAttribute(ATTR_TITLE);
    return title == null ? "" : title;
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
   * Returns this Gadget's doctype mode.  If null, we will use default mode.
   *
   * @return Value of doctype attribute
   */
  public String getDoctype(){
    return getAttribute(ATTR_DOCTYPE);
  }

  /**
   * @param name the attribute name
   * @return the value of an ModulePrefs attribute by name, or null if the
   *     attribute doesn't exist
   */
  public String getAttribute(String name) {
    return attributes.get(name);
  }

  /**
   * @param name the attribute name
   * @param defaultValue the default Value
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
   * @param name the attribute name
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
   * @param name the attribute name
   * @return the attribute by name converted to a boolean (false if the
   *     attribute doesn't exist)
   */
  public boolean getBoolAttribute(String name) {
    String value = getAttribute(name);
    return Boolean.parseBoolean(value);
  }

  /**
   * @param name the attribute name
   * @return the attribute by name converted to an integer, or 0 if the
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


  private final List<String> categories;
  private List<Feature> allFeatures;
  private Map<String, Feature> features;
  private Map<String, Feature> globalFeatures;
  private List<Preload> preloads;
  private List<Icon> icons;
  private Map<String, Map<Locale, LocaleSpec>>  locales;
  private Map<Locale, LocaleSpec> allLocales;
  private Map<String, LinkSpec> links;
  private OAuthSpec oauth;
  private Multimap<String,Node> extraElements;
  private OAuth2Spec oauth2;


  /**
   * @return Returns a list of flattened attributes for:
   * ModuleSpec@category
   * ModuleSpec@category2
   */
  public List<String> getCategories() {
    return categories;
  }

  /**
   * All features are included in ModulePrefs.
   * View level features have view qualifiers appended.
   * @return a map of ModulePrefs/Require and ModulePrefs/Optional elements to Feature
   */
  public Map<String, Feature> getFeatures() {
    return features;
  }


  /**
   * All features elements defined in ModulePrefs
   * @return a list of all Features included in ModulePrefs
   */
  public List<Feature> getAllFeatures() {
    return allFeatures;
  }

  /**
   * Returns Map of features to load for the given View
   * @return a map of ModuleSpec/Require and ModuleSpec/Optional elements to Feature
   */
  public Map<String, Feature> getViewFeatures(String view) {
    Map<String, Feature> map = Maps.newHashMap();
    // Global features are in all views..
    map.putAll(globalFeatures);
    // By adding view level features last so they can override global feature configurations
    for (Feature feature : features.values()) {
      if (feature.getViews().contains(view)) {
        map.put(feature.getName(), feature);
      }
    }
    return map;
  }

  /**
   * @return a list of Preloads from the ModuleSpec/Preload element
   */
  public List<Preload> getPreloads() {
    return preloads;
  }

  /**
   * @return a list of Icons from the ModuleSpec/Icon element
   */
  public List<Icon> getIcons() {
    return icons;
  }

  /**
   * @return a Map of Locales to LocalSpec from the ModuleSpec/Locale element
   */
  public Map<Locale, LocaleSpec> getLocales() {
    return allLocales;
  }

  /**
   * @return a map of Link names to LinkSpec from the ModuleSpec/Link element
   */
  public Map<String, LinkSpec> getLinks() {
    return links;
  }

  /**
   * @return an OAuthSpec built from the ModuleSpec/OAuthSpec element
   */
  public OAuthSpec getOAuthSpec() {
    return oauth;
  }

  /**
   * @return an OAuth2Spec built from the ModuleSpec/OAuthSpec element
   */
  public OAuth2Spec getOAuth2Spec() {
    return oauth2;
  }

  /**
   * @return a Multimap of tagnames to child elements of the ModuleSpec element
   */
  public Multimap<String,Node> getExtraElements() {
    return extraElements;
  }

  /**
   * Note: not part of the spec.
   *
   * @return true when UserPref-substitutable fields in this prefs require __UP_ substitution.
   */
  public boolean needsUserPrefSubstitution() {
    return needsUserPrefSubstitution;
  }

  /**
   * Gets the global locale spec for the given locale, if any exists.
   *
   * @return The locale spec, if there is a matching one, or null.
   */
  public LocaleSpec getGlobalLocale(Locale locale) {
    return getLocale(locale, GLOBAL_LOCALE);
  }

  /**
   * Gets the locale spec for the given locale and view, if any exists.
   *
   * @return The locale spec, if there is a matching one, or null.
   */
  public LocaleSpec getLocale(Locale locale, String view) {
    if (view == null) {
      view = GLOBAL_LOCALE;
    }
    Map<Locale, LocaleSpec> viewLocales = locales.get(view);
    LocaleSpec locSpec = null;
    if (viewLocales != null) {
      locSpec = viewLocales.get(locale); // Check view specific locale...
    }
    if (locSpec == null && !view.equals(GLOBAL_LOCALE)) { // If not there, check Global map
      locSpec = getGlobalLocale(locale);
    }
    return locSpec;
  }

  /**
   * Produces a new ModulePrefs by substituting hangman variables from
   * substituter. See comments on individual fields to see what actually
   * has substitutions performed.
   *
   * @param substituter the substituter to execute
   * @return a substituted ModulePrefs
   */
  public ModulePrefs substitute(Substitutions substituter) {
    return new ModulePrefs(this, substituter);
  }


  /**
   * Walks child nodes of the given node.
   * @param element root node to be applied
   * @param visitors Set of visitors to apply to children of element.
   * @throws SpecParserException when encountering bad input
   */
  private static void walk(Element element, Set<ElementVisitor> visitors)
      throws SpecParserException {
    NodeList children = element.getChildNodes();
    for (int i = 0, j = children.getLength(); i < j; ++i) {
      Node child = children.item(i);
      String tagName = child.getNodeName();

      if (!(child instanceof Element)) continue;

      // Try our visitors in order until we find a match
      for (ElementVisitor ev : visitors) {
        if (ev.visit(tagName, (Element)child))
          break;
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

    if (extraElements != null) {
      for (Node node : extraElements.values()) {
        Source source = new DOMSource(node);
        StringWriter sw = new StringWriter();
        Result result = new StreamResult(sw);
        try {
          Transformer xformer = TransformerFactory.newInstance().newTransformer();
          xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
          xformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
          // ignore
        } catch (TransformerException e) {
          // ignore
        }
        buf.append(sw.toString());
      }
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

  /**
   * Interface used for parsing specific chunks of the gadget spec
   */
  interface ElementVisitor {
    /**
     * Called on each node that matches
     *
     * @param tag the name of the tag being parsed
     * @param element the element to parse
     * @return true if we handled the tag, false if not
     * @throws SpecParserException when parsing issues are present
     */
    boolean visit(String tag, Element element) throws SpecParserException;

    /**
     * Called when all elements have been processed.  Any data that is set on the ModulePrefs instance should be
     * Immutable
     *
     * @param moduleprefs The moduleprefs object to mutate
     */
    void apply(ModulePrefs moduleprefs);
  }

  /**
   * Processes ModulePrefs/Preload into a list.
   */
  private class PreloadVisitor implements ElementVisitor {
    private final List<Preload> preloaded = Lists.newLinkedList();

    public boolean visit(String tag,Element element) throws SpecParserException {
      if (!"Preload".equals(tag)) return false;

      Preload preload = new Preload(element, base);
      preloaded.add(preload);
      return true;
    }

    public void apply(ModulePrefs moduleprefs) {
      moduleprefs.preloads = ImmutableList.copyOf(preloaded);
    }
  }

  /**
   * Process ModulePrefs/OAuth
   */
  private final class OAuthVisitor implements ElementVisitor {
    private OAuthSpec oauthSpec = null;
    private final MutableBoolean oauthMarker;

    private OAuthVisitor(MutableBoolean oauthMarker) {
      this.oauthMarker = oauthMarker;
    }

    public boolean visit(String tag, Element element) throws SpecParserException {
      if (!"OAuth".equals(tag)) return false;

      if (oauthSpec != null) {
        throw new SpecParserException("ModulePrefs/OAuth may only occur once.");
      }
      oauthSpec = new OAuthSpec(element, base);
      oauthMarker.setValue(true);
      return true;
    }

    public void apply(ModulePrefs moduleprefs) {
      moduleprefs.oauth = oauthSpec;
    }

  }

  /**
   * Process ModulePrefs/OAuth2
   */
  private final class OAuth2Visitor implements ElementVisitor {
    private OAuth2Spec oauth2Spec = null;
    private final MutableBoolean oauth2Marker;

    private OAuth2Visitor(MutableBoolean oauth2Marker) {
      this.oauth2Marker = oauth2Marker;
    }

    public boolean visit(String tag, Element element) throws SpecParserException {
      if (!"OAuth2".equals(tag)) return false;

      if (oauth2Spec != null) {
        throw new SpecParserException("ModulePrefs/OAuth2 may only occur once.");
      }
      oauth2Spec = new OAuth2Spec(element, base);
      oauth2Marker.setValue(true);
      return true;
    }

    public void apply(ModulePrefs moduleprefs) {
      moduleprefs.oauth2 = oauth2Spec;
    }

  }

  /**
   * Processes ModulePrefs/Require and ModulePrefs/Optional
   */
  private static final class FeatureVisitor implements ElementVisitor {
    private final Map<String, Feature> features = Maps.newHashMap();
    private final Map<String, Feature> globalFeatures = Maps.newHashMap();
    private final MutableBoolean oauthMarker;
    private boolean coreIncluded = false;

    private static final Set<String> TAGS = ImmutableSet.of("Require", "Optional");

    private FeatureVisitor(MutableBoolean oauthMarker) {
      this.oauthMarker = oauthMarker;
    }

    public boolean visit(String tag, Element element)
        throws SpecParserException {
      if (!TAGS.contains(tag))
        return false;

      Feature feature = new Feature(element);
      if (feature.getViews().isEmpty()) {
        coreIncluded = coreIncluded || feature.getName().startsWith("core");
        features.put(feature.getName(), feature);
        globalFeatures.put(feature.getName(), feature);
      } else {
        // We are going to include Core feature globally, so skip it if it was
        // included for any Views
        if (!feature.getName().startsWith("core")) {
          // Key view level features by qualifying with the view ID
          for (String view : feature.getViews()) {
            StringBuilder buff = new StringBuilder(feature.getName());
            buff.append('.');
            buff.append(view);
            features.put(buff.toString(), feature);
          }
        }
      }
      return true;
    }

    public void apply(ModulePrefs moduleprefs) {
      if (!coreIncluded) {
        // No library was explicitly included from core - add it as an implicit dependency.
        features.put(Feature.CORE_FEATURE.getName(), Feature.CORE_FEATURE);
        globalFeatures.put(Feature.CORE_FEATURE.getName(), Feature.CORE_FEATURE);
      }
      if (oauthMarker.booleanValue()) {
        // <OAuth>/<OAuth2> tag found: security token needed.
        features.put(Feature.SECURITY_TOKEN_FEATURE.getName(), Feature.SECURITY_TOKEN_FEATURE);
        globalFeatures.put(Feature.SECURITY_TOKEN_FEATURE.getName(), Feature.SECURITY_TOKEN_FEATURE);
      }
      moduleprefs.features = ImmutableMap.copyOf(features);
      moduleprefs.globalFeatures = ImmutableMap.copyOf(globalFeatures);
      moduleprefs.allFeatures = ImmutableList.copyOf(features.values());
    }
  }

  /**
   * Processes ModulePrefs/Icon
   */
  private static class IconVisitor implements ElementVisitor {
    private final List<Icon> icons = Lists.newLinkedList();

    public boolean visit(String tag, Element element) throws SpecParserException {
      if (!"Icon".equals(tag)) return false;

      icons.add(new Icon(element));
      return true;
    }
    public void apply(ModulePrefs moduleprefs) {
      moduleprefs.icons = ImmutableList.copyOf(icons);
    }
  }

  /**
   * Process ModulePrefs/Locale
   */
  private class LocaleVisitor implements ElementVisitor {

    private Map<String, Map<Locale, LocaleSpec>> locales = Maps.newHashMap();

    public boolean visit(String tag, Element element)
        throws SpecParserException {
      if (!"Locale".equals(tag))
        return false;
      LocaleSpec locale = new LocaleSpec(element, base);
      if (locale.getViews().isEmpty()) {
        storeLocaleSpec(GLOBAL_LOCALE, locale);
      } else {
        // We've got a view level Locale, need to store the mapping of Views to
        // the appropriate LocaleSpecs
        for (String view : locale.getViews()) {
          storeLocaleSpec(view,locale);
        }
      }
      return true;
    }

    public void apply(ModulePrefs moduleprefs) {
      Map<Locale, LocaleSpec> allLocales = Maps.newHashMap();
      moduleprefs.locales = locales;
      for(Map<Locale, LocaleSpec> map : locales.values()){
        allLocales.putAll(map);
      }
      moduleprefs.allLocales = ImmutableMap.copyOf(allLocales);
    }

    private void storeLocaleSpec(String view, LocaleSpec locale){
      Map<Locale, LocaleSpec> viewLocaleSpecs;
      if (locales.get(view) == null) {
        viewLocaleSpecs = Maps.newHashMap();
        locales.put(view, viewLocaleSpecs);
      } else {
        viewLocaleSpecs = locales.get(view);
      }
      viewLocaleSpecs.put(new Locale(locale.getLanguage(), locale.getCountry()), locale);
    }

  }

  /**
   * Process ModulePrefs/Link
   */
  private class LinkVisitor implements ElementVisitor {
    private final Map<String, LinkSpec> linkMap = Maps.newHashMap();

    public boolean visit(String tag, Element element) throws SpecParserException {
      if (!"Link".equals(tag)) return false;
      LinkSpec link = new LinkSpec(element, base);
      linkMap.put(link.getRel(), link);
      return true;
    }

    public void apply(ModulePrefs moduleprefs) {
      moduleprefs.links = ImmutableMap.copyOf(linkMap);
    }
  }

  private static class ExtraElementsVisitor implements ElementVisitor {
    private Multimap<String,Node> elements = ArrayListMultimap.create();

    public boolean visit(String tag, Element element) throws SpecParserException {
      elements.put(tag, element.cloneNode(true));
      return true;
    }
    public void apply(ModulePrefs moduleprefs) {
      moduleprefs.extraElements = ImmutableMultimap.copyOf(elements);
    }
  }
}
