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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
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
    NamedNodeMap attrs = prefs.getAttributes();

    Node title = attrs.getNamedItem("title");
    if (null == title) {
      throw new SpecParserException("Missing \"title\" attribute.");
    }
    spec.title = title.getNodeValue();

    Node titleUrl = attrs.getNamedItem("title_url");
    if (null != titleUrl) {
      try {
        spec.titleUrl = new URI(titleUrl.getNodeValue());
      } catch (URISyntaxException e) {
        throw new SpecParserException(
            "Malformed \"title_url\": " + titleUrl.getNodeValue());
      }
    }

    NodeList children = prefs.getChildNodes();
    for (int i = 0, j = children.getLength(); i < j; ++i) {
      Node child = children.item(i);
      if (child.getNodeName().equals("Locale")) {
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
      Node locale,
      URI baseUrl) throws SpecParserException {
    NamedNodeMap attrs = locale.getAttributes();
    Node messagesAttr = attrs.getNamedItem("messages");
    Node languageAttr = attrs.getNamedItem("lang");
    Node countryAttr = attrs.getNamedItem("country");
    Node rtlAttr = attrs.getNamedItem("language_direction");

    String messages = null;
    if (null != messagesAttr) {
      messages = messagesAttr.getNodeValue();
    }

    String country;
    if (null == countryAttr) {
      country = "all";
    } else {
      country = countryAttr.getNodeValue();
    }

    String language;
    if (null == languageAttr) {
      language = "all";
    } else {
      language = languageAttr.getNodeValue();
    }

    boolean rightToLeft = false;
    if (rtlAttr != null && "rtl".equals(rtlAttr.getTextContent())) {
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
    NamedNodeMap attrs = pref.getAttributes();
    Node name = attrs.getNamedItem("name");
    if (null == name) {
      throw new SpecParserException("All UserPrefs must have name attributes.");
    }
    up.name = name.getNodeValue();

    Node displayName = attrs.getNamedItem("display_name");
    if (null != displayName) {
      up.displayName = displayName.getNodeValue();
    }

    Node dataType = attrs.getNamedItem("datatype");
    if (null == dataType) {
      up.dataType = GadgetSpec.UserPref.DataType.STRING;
    } else {
      up.dataType =
          ParsedGadgetSpec.ParsedUserPref.parse(dataType.getNodeValue());
    }

    Node defaultValue = attrs.getNamedItem("default_value");
    if (null != defaultValue) {
      up.defaultValue = defaultValue.getNodeValue();
    }

    // Check for enum types.
    up.enumValues = new HashMap<String, String>();
    NodeList children = pref.getChildNodes();
    for (int i = 0, j = children.getLength(); i < j; ++i) {
      Node child = children.item(i);
      if (child.getNodeName().equals("EnumValue")) {
        NamedNodeMap childAttrs = child.getAttributes();

        // Must have both name and value.
        Node value = childAttrs.getNamedItem("value");
        Node displayValue = childAttrs.getNamedItem("display_value");
        if (value != null) {
          String valueText = value.getTextContent();
          String displayText = displayValue == null
              ? valueText
              : displayValue.getTextContent();
          up.enumValues.put(valueText, displayText);
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
    NamedNodeMap attrs = content.getAttributes();
    Node type = attrs.getNamedItem("type");
    if (null == type) {
      throw new SpecParserException("No content type specified!");
    } else if ("url".equals(type.getNodeValue())) {
      spec.contentType = GadgetSpec.ContentType.URL;
      Node href = attrs.getNamedItem("href");
      if (href != null) {
        try {
          spec.contentHref = new URI(href.getNodeValue());
        } catch (URISyntaxException e) {
          throw new SpecParserException("Malformed <Content> href value");
        }
      }
    } else {
      spec.contentType = GadgetSpec.ContentType.HTML;
      Node viewNode = attrs.getNamedItem("view");
      String viewStr = (viewNode == null) ? "" : viewNode.getNodeValue();
      String[] views = viewStr.split(",");
      Node child = content.getFirstChild();
      String contentData = content.getTextContent();
      if (contentData.length() > 0) {
        for (String view : views) {
          spec.addContent(view, contentData);
        }
      } else {
        throw new SpecParserException("Empty or malformed <Content> section!");
      }
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
    NamedNodeMap attrs = feature.getAttributes();
    Node name = attrs.getNamedItem("feature");
    if (name == null || name.getNodeValue().length() == 0) {
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
          NamedNodeMap paramAttrs = child.getAttributes();
          Node paramName = paramAttrs.getNamedItem("name");
          if (paramName != null) {
            params.put(paramName.getNodeValue(), child.getTextContent());
          } else {
            throw new SpecParserException("Missing name attribute in <Param>.");
          }
        }
      }
      ParsedGadgetSpec.ParsedFeatureSpec featureSpec =
        new ParsedGadgetSpec.ParsedFeatureSpec();
      featureSpec.name = name.getNodeValue();
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
    private ContentType contentType;
    private URI contentHref;
    private Map<String, StringBuilder> contentData
        = new HashMap<String, StringBuilder>();
    private List<Icon> icons = new ArrayList<Icon>();
    private List<LocaleSpec> localeSpecs = new ArrayList<LocaleSpec>();
    private List<String> preloads = new ArrayList<String>();
    private Map<String, FeatureSpec> requires
        = new HashMap<String, FeatureSpec>();
    private String screenshot;
    private String thumbnail;
    private String title;
    private URI titleUrl;
    private List<UserPref> userPrefs = new ArrayList<UserPref>();

    public GadgetSpec copy() {
      // TODO: actually clone this thing.
      ParsedGadgetSpec spec = new ParsedGadgetSpec();
      spec.author = author;
      spec.authorEmail = authorEmail;
      spec.description = description;
      spec.directoryTitle = directoryTitle;
      spec.contentType = contentType;
      spec.contentHref = contentHref;
      spec.contentData = new HashMap<String, StringBuilder>(contentData);
      spec.icons = new ArrayList<Icon>(icons);
      spec.localeSpecs = new ArrayList<LocaleSpec>(localeSpecs);
      spec.preloads = new ArrayList<String>(preloads);
      spec.requires = new HashMap<String, FeatureSpec>(requires);
      spec.screenshot = screenshot;
      spec.thumbnail = thumbnail;
      spec.title = title;
      spec.titleUrl = titleUrl;
      spec.userPrefs = new ArrayList<UserPref>(userPrefs);
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

    public String getScreenshot() {
      return screenshot;
    }

    public String getThumbnail() {
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
        return null;
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

    public ContentType getContentType() {
      return contentType;
    }

    public URI getContentHref() {
      Check.is(contentType == ContentType.URL,
               "getContentHref() requires contentType URL");
      return contentHref;
    }

    public String getContentData() {
      return getContentData(DEFAULT_VIEW);
    }

    public String getContentData(String view) {
      Check.is(contentType == ContentType.HTML,
               "getContentData() requires contentType HTML");
      if (view == null || view.length() == 0) {
        view = DEFAULT_VIEW;
      }

      StringBuilder content = contentData.get(view);
      if (content == null) {
        content = contentData.get(DEFAULT_VIEW);
      }

      if (content == null) {
        return "";
      }

      return content.toString();
    }

    // TODO: Synchronizing this seems unnecessary...a parse job should never
    // happen across threads, and addContent *can't* be called anywhere but
    // within a call to GadgetSpecParser.parse()
    public synchronized void addContent(String view, String content) {
      if (view == null || view.length() == 0) {
        view = DEFAULT_VIEW;
      }

      if (!contentData.containsKey(view)) {
        contentData.put(view, new StringBuilder());
      }

      contentData.get(view).append(content);
    }
  }
}
