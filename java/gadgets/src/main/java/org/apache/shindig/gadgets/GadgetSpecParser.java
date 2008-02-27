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
package org.apache.shindig.gadgets;

import org.apache.shindig.util.Check;
import org.apache.shindig.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Parses Gadget spec XML into a basic data structure.
 */
public class GadgetSpecParser {

  private static final String[] CATEGORY_ATTRS = {"category", "category2"};

  /**
   * Parses the raw input XML and returns a new GadgetSpec with the processed
   * content.
   *
   * @param id Gadget.ID object that resulted in the provided data
   * @param xml The raw input xml.
   * @return A new GadgetSpec
   * @throws SpecParserException If {@code data} does not represent a valid
   * {@code GadgetSpec}
   */
  public GadgetSpec parse(GadgetView.ID id, String xml)
      throws SpecParserException {
    if (xml.length() == 0) {
      throw new SpecParserException(GadgetException.Code.EMPTY_XML_DOCUMENT);
    }

    Document doc;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      InputSource is = new InputSource(new StringReader(xml));
      doc = factory.newDocumentBuilder().parse(is);
    } catch (SAXException e) {
      throw new SpecParserException(e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new SpecParserException(e.getMessage());
    } catch (IOException e) {
      throw new SpecParserException(e.getMessage());
    }

    ParsedGadgetSpec spec = new ParsedGadgetSpec();
    Element root = doc.getDocumentElement();

    NodeList modulePrefs = root.getElementsByTagName("ModulePrefs");
    if (modulePrefs.getLength() != 1) {
      throw new SpecParserException("Missing or duplicated <ModulePrefs>");
    }
    processModulePrefs(id, modulePrefs.item(0), spec);

    NodeList userPrefs = root.getElementsByTagName("UserPref");
    for (int i = 0, j = userPrefs.getLength(); i < j; ++i) {
      spec.getUserPrefs().add(processUserPref(userPrefs.item(i)));
    }

    NodeList content = root.getElementsByTagName("Content");
    for (int i = 0, j = content.getLength(); i < j; ++i) {
      processContent(spec, content.item(i));
    }

    NodeList requires = root.getElementsByTagName("Require");
    for (int i = 0, j = requires.getLength(); i < j; ++i) {
      processFeature(spec, requires.item(i), true);
    }

    NodeList optionals = root.getElementsByTagName("Optional");
    for (int i = 0, j = optionals.getLength(); i < j; ++i) {
      processFeature(spec, optionals.item(i), false);
    }

    return spec;
  }

  /**
   * Processes the &lt;ModulePrefs&gt; section of the spec.
   * @param id Gadget's identifier
   * @param prefs Root node of the ModulePrefs section
   * @param spec Output object populated with module prefs data
   * @throws SpecParserException If malformed data is found
   */
  private void processModulePrefs(Gadget.ID id,
                                  Node prefs,
                                  ParsedGadgetSpec spec)
      throws SpecParserException {
    String title = XmlUtil.getAttribute(prefs, "title");
    if (null == title) {
      throw new SpecParserException("Missing \"title\" attribute.");
    }
    spec.title = title;
    spec.titleUrl = XmlUtil.getUriAttribute(prefs, "title_url");
    spec.description = XmlUtil.getAttribute(prefs, "description");
    spec.directoryTitle = XmlUtil.getAttribute(prefs, "directory_title");
    spec.author = XmlUtil.getAttribute(prefs, "author");
    spec.authorEmail = XmlUtil.getAttribute(prefs, "author_email");
    spec.thumbnail = XmlUtil.getUriAttribute(prefs, "thumbnail");
    spec.screenshot = XmlUtil.getUriAttribute(prefs, "screenshot");

    for (String attrName : CATEGORY_ATTRS) {
      String attr = XmlUtil.getAttribute(prefs, attrName);
      if (attr != null) {
        spec.categories.add(attr);
      }
    }

    NodeList children = prefs.getChildNodes();
    for (int i = 0, j = children.getLength(); i < j; ++i) {
      Node child = children.item(i);
      if ("Locale".equals(child.getNodeName())) {
        spec.localeSpecs.add(processLocale(children.item(i), id.getURI()));
      }
    }

    // TODO: Icon parsing
  }

  /**
   * Processes the &lt;Locale&gt; section of the spec.
   *
   * @param locale Root node of Locale section
   * @param baseUrl Base url for relative spec paths
   * @return Message bundle object resulting from parsing
   * @throws SpecParserException If a malformed message bundle URI is found
   */
  private ParsedGadgetSpec.ParsedMessageBundle processLocale(
      Node locale, URI baseUrl) throws SpecParserException {
    String messages = XmlUtil.getAttribute(locale, "messages");
    String country = XmlUtil.getAttribute(locale, "country", "all");
    String language = XmlUtil.getAttribute(locale, "lang", "all");

    String direction
        = XmlUtil.getAttribute(locale, "language_direction", "ltr");
    boolean rightToLeft = false;
    if ("rtl".equals(direction)) {
      rightToLeft = true;
    }

    ParsedGadgetSpec.ParsedMessageBundle bundle =
        new ParsedGadgetSpec.ParsedMessageBundle();
    if (messages != null) {
      try {
        bundle.url = new URI(new URL(baseUrl.toURL(), messages).toString());
      } catch (URISyntaxException e) {
        throw new SpecParserException("Bad message bundle url: " + messages);
      } catch (MalformedURLException e) {
        throw new SpecParserException("Bad message bundle url: " + messages);
      }
    }
    bundle.locale = new Locale(language, country);
    bundle.rightToLeft = rightToLeft;

    return bundle;
  }

  /**
   * Processes a &lt;UserPref&gt; tag.
   *
   * @param pref UserPref DOM node
   * @throws SpecParserException If the section is missing a name attribute
   */
  private ParsedGadgetSpec.ParsedUserPref processUserPref(Node pref)
      throws SpecParserException {
    ParsedGadgetSpec.ParsedUserPref up = new ParsedGadgetSpec.ParsedUserPref();

    String name = XmlUtil.getAttribute(pref, "name");
    if (name == null) {
      throw new SpecParserException("All UserPrefs must have name attributes.");
    }
    up.name = name;

    up.displayName = XmlUtil.getAttribute(pref, "display_name");
    up.dataType = ParsedGadgetSpec.ParsedUserPref.parse(
        XmlUtil.getAttribute(pref, "datatype"));
    up.defaultValue = XmlUtil.getAttribute(pref, "default_value");

    // Check for enum types.
    up.enumValues = new HashMap<String, String>();
    NodeList children = pref.getChildNodes();
    for (int i = 0, j = children.getLength(); i < j; ++i) {
      Node child = children.item(i);
      if ("EnumValue".equals(child.getNodeName())) {
        String value = XmlUtil.getAttribute(child, "value");
        if (value != null) {
          up.enumValues.put(value,
              XmlUtil.getAttribute(child, "display_value", value));
        }
      }
    }
    return up;
  }

  /**
   * Processes the &lt;Content&gt; section
   * @param spec Object whose content info is to be filled in
   * @param content Content DOM node
   * @throws SpecParserException If a malformed content section is found
   */
  private void processContent(ParsedGadgetSpec spec, Node content)
      throws SpecParserException {
    String[] viewNames
        = XmlUtil.getAttribute(content, "view", "default").split(",");
    for (String viewName : viewNames) {
      spec.addContent(viewName.trim(), content);
    }
  }


  /**
   * Processes &ltlOptional&gt; and &lt;Require&gt; tags.
   *
   * @param spec Spec whose features to populate
   * @param feature A Required or Optional DOM node
   * @param required True if Required, False if Optional
   * @throws SpecParserException If node is missing a name attribute
   */
  private void processFeature(ParsedGadgetSpec spec,
                              Node feature,
                              boolean required)
      throws SpecParserException {
    String name = XmlUtil.getAttribute(feature, "feature");
    if (name == null) {
      throw new SpecParserException(
          "Feature not specified in <" +
          (required ? "Required" : "Optional") +
          "> tag");
    } else {
      Map<String, String> params = new HashMap<String, String>();
      NodeList children = feature.getChildNodes();
      for (int i = 0, j = children.getLength(); i < j; ++i) {
        Node child = children.item(i);
        if ("Param".equals(child.getNodeName())) {
          String param = XmlUtil.getAttribute(child, "name");
          if (param != null) {
            params.put(param, child.getTextContent());
          } else {
            throw new SpecParserException("Missing name attribute in <Param>.");
          }
        }
      }
      ParsedGadgetSpec.ParsedFeatureSpec featureSpec =
        new ParsedGadgetSpec.ParsedFeatureSpec();
      featureSpec.name = name;
      featureSpec.optional = !required;
      featureSpec.params = params;
      spec.requires.put(featureSpec.name, featureSpec);
    }
  }

  /**
   * {@code GadgetSpec} implementation populated by the parsing of Gadget XML.
   */
  private static class ParsedGadgetSpec implements GadgetSpec {
    private String author;
    private String authorEmail;
    private String description;
    private String directoryTitle;
    private Map<String, ParsedView> views
        = new HashMap<String, ParsedView>();
    private List<Icon> icons = new ArrayList<Icon>();
    private List<LocaleSpec> localeSpecs = new ArrayList<LocaleSpec>();
    private List<String> preloads = new ArrayList<String>();
    private Map<String, FeatureSpec> requires
        = new HashMap<String, FeatureSpec>();
    private URI screenshot;
    private URI thumbnail;
    private String title;
    private URI titleUrl;
    private List<UserPref> userPrefs = new ArrayList<UserPref>();
    private List<String> categories = new ArrayList<String>();

    public GadgetSpec copy() {
      // TODO: actually clone this thing.
      ParsedGadgetSpec spec = new ParsedGadgetSpec();
      spec.author = author;
      spec.authorEmail = authorEmail;
      spec.description = description;
      spec.directoryTitle = directoryTitle;
      spec.views = new HashMap<String, ParsedView>(views);
      spec.icons = new ArrayList<Icon>(icons);
      spec.localeSpecs = new ArrayList<LocaleSpec>(localeSpecs);
      spec.preloads = new ArrayList<String>(preloads);
      spec.requires = new HashMap<String, FeatureSpec>(requires);
      spec.screenshot = screenshot;
      spec.thumbnail = thumbnail;
      spec.title = title;
      spec.titleUrl = titleUrl;
      spec.userPrefs = new ArrayList<UserPref>(userPrefs);
      spec.categories = new ArrayList<String>(categories);
      return spec;
    }

    public String getAuthor() {
      return author;
    }

    public String getAuthorEmail() {
      return authorEmail;
    }

    public String getDescription() {
      return description;
    }

    public String getDirectoryTitle() {
      return directoryTitle;
    }

    private static class ParsedIcon implements Icon {
      private String mode;
      private URI url;
      private String type;

      public String getMode() {
        return mode;
      }

      public URI getURI() {
        return url;
      }

      public String getType() {
        return type;
      }
    }

    public List<Icon> getIcons() {
      return icons;
    }

    private static class ParsedMessageBundle implements LocaleSpec {
      private URI url;
      private Locale locale;
      private boolean rightToLeft;

      public URI getURI() {
        return url;
      }

      public Locale getLocale() {
        return locale;
      }

      public boolean isRightToLeft() {
        return rightToLeft;
      }
    }

    public List<LocaleSpec> getLocaleSpecs() {
      return localeSpecs;
    }

    public List<String> getPreloads() {
      return preloads;
    }

    private static class ParsedFeatureSpec implements FeatureSpec {
      private String name;
      private Map<String, String> params;
      private boolean optional;

      public String getName() {
        return name;
      }

      public Map<String, String> getParams() {
        return Collections.unmodifiableMap(params);
      }

      public boolean isOptional() {
        return optional;
      }
    }

    public Map<String, FeatureSpec> getRequires() {
      return requires;
    }

    public URI getScreenshot() {
      return screenshot;
    }

    public URI getThumbnail() {
      return thumbnail;
    }

    public String getTitle() {
      return title;
    }

    public URI getTitleURI() {
      return titleUrl;
    }

    private static class ParsedUserPref implements UserPref {
      private String name;
      private String displayName;
      private String defaultValue;
      private boolean required;
      private DataType dataType;
      private Map<String, String> enumValues;

      private static DataType parse(String str) {
        for (DataType dt : UserPref.DataType.values()) {
          if (dt.toString().equalsIgnoreCase(str)) {
            return dt;
          }
        }
        return UserPref.DataType.STRING;
      }

      public String getName() {
        return name;
      }

      public String getDisplayName() {
        return displayName;
      }

      public String getDefaultValue() {
        return defaultValue;
      }

      public boolean isRequired() {
        return required;
      }

      public DataType getDataType() {
        return dataType;
      }

      public Map<String, String> getEnumValues() {
        return enumValues;
      }
    }

    public List<UserPref> getUserPrefs() {
      return userPrefs;
    }


    private static class ParsedView implements View {
      private static final String QUIRKS_ATTR_NAME = "quirks";
      private static final String TYPE_ATTR_NAME = "type";
      private static final String HREF_ATTR_NAME = "href";
      private URI href = null;
      private ContentType type = null;
      private boolean quirks = true;
      private final StringBuilder builder = new StringBuilder();

      /**
       * Appends to an existing view.
       * @param node
       * @throws SpecParserException
       */
      public void append(Node node)
          throws SpecParserException {
        String newType = XmlUtil.getAttribute(node, TYPE_ATTR_NAME);
        if (newType != null) {
          ContentType contentType = ContentType.parse(newType);
          if (type != null && !type.equals(contentType)) {
            throw new SpecParserException(
                GadgetException.Code.INVALID_PARAMETER,
                "Can't mix content types for the same view.");
          }
          type = contentType;
        }
        String quirkAttr = XmlUtil.getAttribute(node, QUIRKS_ATTR_NAME);
        if (quirkAttr != null) {
          if ("false".equals(quirkAttr)) {
            quirks = false;
          } else {
            quirks = true;
          }
        }
        href = XmlUtil.getUriAttribute(node, HREF_ATTR_NAME, href);
        if (ContentType.URL.equals(type) && href == null) {
          throw new SpecParserException(GadgetException.Code.INVALID_PARAMETER,
              "href attribute is required for type=url gadgets. " +
              "It is either missing or malformed");
        } else if (ContentType.HTML.equals(type) && href != null) {
          throw new SpecParserException(GadgetException.Code.INVALID_PARAMETER,
              "href attribute is not allowed for type=html gadgets.");
        }
        builder.append(node.getTextContent());
      }

      public ContentType getType() {
        return type;
      }

      /**
       * Must be a URI type gadget.
       *
       * @return The URI for this gadget spec.
       * @throws IllegalStateException if contentType is not URI.
       */
      public URI getHref() {
        Check.eq(type, ContentType.URL,
            "Attempted to get href of a non-url type gadget!");
        return href;
      }

      /**
       * @return The HTML content for the default view of this gadget spec.
       * @throws IllegalStateException if contentType is not HTML.
       */
      public String getData() {
        return builder.toString();
      }

      public boolean getQuirks() {
        return quirks;
      }

      @Override
      public String toString() {
        return String.format(
            "<Content quirks=\"%s\" href=\"%s\"><![CDATA[%s]]></Content>",
            quirks ? "true" : "false",
            href == null ? "" : href.toString(),
            builder.toString());
      }
    }

    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder();
      buf.append("\nGadget Spec Debug String: ")
         .append(views.size())
         .append(" views");
      for (Map.Entry<String, ParsedView> entry : views.entrySet()) {
        buf.append("\nView = ")
           .append(entry.getKey())
           .append("\n")
           .append(entry.getValue().toString());
      }
      return buf.toString();
    }

    public View getView(String viewName) {
      if (viewName == null || viewName.length() == 0) {
        viewName = DEFAULT_VIEW;
      }
      return views.get(viewName);
    }

    public List<String> getCategories() {
      return categories;
    }

    /**
     * Adds content to a view. Creates the view if it doesn't exist, and
     * appends if it does.
     *
     * @param view
     * @param node
     * @throws SpecParserException
     */
    void addContent(String view, Node node)
        throws SpecParserException {
      if (view == null || view.length() == 0) {
        view = DEFAULT_VIEW;
      }

      if (!views.containsKey(view)) {
        views.put(view, new ParsedView());
      }
      views.get(view).append(node);
    }
  }
}
