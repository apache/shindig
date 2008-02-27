/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets;

import junit.framework.TestCase;

import java.net.URI;
import java.util.List;
import java.util.Map;


public class GadgetSpecParserTest extends TestCase {

  private static final GadgetSpecParser parser = new GadgetSpecParser();
  private BasicGadgetId basicId;

  @Override
  public void setUp() throws Exception {
    basicId = new BasicGadgetId(1, "http://example.org/text.xml");
  }

  private static class BasicGadgetId implements GadgetView.ID {
    BasicGadgetId(int moduleId, String uri) throws Exception {
      this.moduleId = moduleId;
      this.uri = new URI(uri);
    }
    public int getModuleId() {
      return moduleId;
    }
    public URI getURI() {
      return uri;
    }
    public String getKey() {
      return uri.toString();
    }

    private URI uri;
    private int moduleId;
  }

  // METADATA
  public void testParsedMetadata() throws Exception {
    String nonCanonicalMetadataXml =
        "<?xml version=\"1.0\"?>" +
        "<Module>" +
        "  <ModulePrefs" +
        "    title=\"TITLE\"" +
        "    title_url=\"TITLE_URL\"" +
        "    description=\"DESCRIPTION\"" +
        "    author=\"AUTHOR_NAME\"" +
        "    author_email=\"author@example.org\"" +
        "    screenshot=\"http://www.example.org/screenshot.png\"" +
        "    thumbnail=\"http://www.example.org/thumbnail.png\"" +
        "  />" +
        "  <Content type=\"html\"></Content>" +
        "</Module>";
    GadgetSpec spec = parser.parse(basicId, nonCanonicalMetadataXml);
    assertEquals("TITLE", spec.getTitle());
    assertEquals("TITLE_URL", spec.getTitleURI().toString());
    assertEquals("DESCRIPTION", spec.getDescription());
    assertEquals("AUTHOR_NAME", spec.getAuthor());
    assertEquals("author@example.org", spec.getAuthorEmail());
    assertEquals("http://www.example.org/screenshot.png",
                 spec.getScreenshot().toString());
    assertEquals("http://www.example.org/thumbnail.png",
                 spec.getThumbnail().toString());
  }
  public void testExtendedMetadata() throws Exception {
    String nonCanonicalMetadataXml =
        "<?xml version=\"1.0\"?>" +
        "<Module>" +
        "  <ModulePrefs" +
        "    title=\"TITLE\"" + // parsing fails without this
        "    author_affiliation=\"AUTHOR_AFFILIATION\"" +
        "    author_location=\"AUTHOR_LOCATION\"" +
        "    author_photo=\"www.example.org/author_photo.png\"" +
        "    author_aboutme=\"AUTHOR_ABOUTME\"" +
        "    author_quote=\"AUTHOR_QUOTE\"" +
        "    author_link=\"www.example.org/author_aboutme.html\"" +
        "    show_stats=\"true\"" +
        "    show_in_directory=\"true\"" +
        "    width=\"200\"" +
        "    height=\"300\"" +
        "    category=\"CATEGORY\"" +
        "    category2=\"CATEGORY2\"" +
        "    singleton=\"SINGLETON\"" +
        "    render_inline=\"RENDER_INLINE\"" +
        "    scaling=\"SCALING\"" +
        "    scrolling=\"SCROLLING\"" +
        "  />" +
        "  <Content type=\"html\"></Content>" +
        "</Module>";
    // Just check that it doesn't cause a parse error
    // As these values are supported in shindig, add them to testParsedMetadata
    parser.parse(basicId, nonCanonicalMetadataXml);
  }

  public void testUnknownAttributesOK() throws Exception {
    String unknownAttributesXml =
        "<?xml version=\"1.0\"?>" +
        "<Module>" +
        "  <ModulePrefs unknownmoduleprefsattr=\"UNKNOWN\" title=\"TITLE\"/>" +
        "  <Content type=\"html\" unknowncontentattr=\"UNKNOWN\"/>" +
        "</Module>";
    parser.parse(basicId, unknownAttributesXml);
  }

  // USERPREFS
  public void testEnumParsing() throws Exception {
    String xml =
        "<?xml version=\"1.0\"?>" +
        "<Module>" +
        "  <ModulePrefs title=\"Test Enum\">" +
        "    <UserPref name=\"test\" datatype=\"enum\">" +
        "      <EnumValue value=\"0\" display_value=\"Zero\"/>" +
        "      <EnumValue value=\"1\" display_value=\"One\"/>" +
        "    </UserPref>" +
        "  </ModulePrefs>" +
        "  <Content type=\"html\">Hello!</Content>" +
        "</Module>";
    GadgetSpec spec = parser.parse(basicId, xml);

    List<GadgetSpec.UserPref> prefs = spec.getUserPrefs();

    assertEquals(1, prefs.size());
    GadgetSpec.UserPref pref = prefs.get(0);
    assertEquals("test", pref.getName());

    Map<String, String> enumValues = pref.getEnumValues();

    assertEquals(2, enumValues.size());

    assertEquals("Zero", enumValues.get("0"));
    assertEquals("One", enumValues.get("1"));
  }

  // TYPE = URL
  public void testTypeUrlParsing() throws Exception {
    GadgetSpec spec = parser.parse(basicId,
        attrs1Xml("type=\"url\" href=\"http://www.example.org\""));
    assertEquals(spec.getView(null).getType(), Gadget.ContentType.URL);
    assertEquals(spec.getView(null).getHref().toString(),
        "http://www.example.org");
  }
  public void testTypeUrlHrefInvalid() throws Exception {
    assertParseException(basicId, attrs1Xml("type=\"url\" href=\":INVALID_URL\""),
        GadgetException.Code.INVALID_PARAMETER);
  }

  // TYPE = HTML
  public void testTypeHtmlParsing() throws Exception {
    final String typeHtmlXml =
        "<Module>" +
        "  <ModulePrefs title=\"Title\"/>" +
        "  <Content type=\"html\">CONTENTS</Content>" +
        "</Module>";
    GadgetSpec spec = parser.parse(basicId, typeHtmlXml);
    assertEquals(Gadget.ContentType.HTML, spec.getView(null).getType());
    assertEquals("CONTENTS", spec.getView(null).getData());
  }
  public void testTypeHtmlWithHrefAttr() throws Exception {
    assertParseException(basicId, attrs1Xml("type=\"html\" href=\"www.example.org\""),
        GadgetException.Code.INVALID_PARAMETER);
  }

  // MULTIPLE VIEWS
  public void testMultipleViews() throws Exception {
    System.out.println(junit.runner.Version.id());

    final String multipleViewsXml =
        "<Module>" +
        "  <ModulePrefs title=\"Title\"/>" +
        "  <Content type=\"html\" view=\"mobile\">mobileCSS</Content>" +
        "  <Content type=\"html\" view=\"profile\">profileCSS</Content>" +
        "  <Content type=\"html\" view=\"mobile,profile\">+sharedHTML</Content>" +
        "  <Content type=\"url\" view=\"maximized\" href=\"http://www.example.org\"></Content>" +
        "</Module>";
    GadgetSpec spec = parser.parse(basicId, multipleViewsXml);
    assertEquals(Gadget.ContentType.HTML, spec.getView("mobile").getType());
    assertEquals("mobileCSS+sharedHTML", spec.getView("mobile").getData());
    assertEquals(Gadget.ContentType.HTML, spec.getView("profile").getType());
    assertEquals("profileCSS+sharedHTML", spec.getView("profile").getData());
    assertEquals(Gadget.ContentType.URL, spec.getView("maximized").getType());
    assertEquals("http://www.example.org",
        spec.getView("maximized").getHref().toString());
  }
  public void testMultipleViewsMixedTypes() throws Exception {
    assertParseException(basicId,
      attrs2Xml("type=\"url\" href=\"www.example.org\"", "type=\"html\""),
      GadgetException.Code.INVALID_PARAMETER);
  }
  public void testMultipleViewsAttributePrecedence() throws Exception {
    // first declaration of attribute takes precedence
    GadgetSpec spec = parser.parse(basicId,
        attrs2Xml("type=\"html\" view=\"A\" quirks=\"false\"",
                  "type=\"html\" view=\"A,B\" quirks=\"true\""));
    assertEquals(true, spec.getView("A").getQuirks());
    assertEquals(true, spec.getView("B").getQuirks());
  }

  // QUIRKS ATTRIBUTE
  public void testQuirksParsing() throws Exception {
    boolean quirksDefault = true;

    GadgetSpec spec = parser.parse(basicId, attrs1Xml("type=\"html\""));
    assertEquals("Quirks default: ", quirksDefault, spec.getView(null).getQuirks());

    spec = parser.parse(basicId, attrs1Xml("type=\"html\" quirks=\"true\""));
    assertEquals("Parsing quirks=\"true\"", true, spec.getView(null).getQuirks());

    spec = parser.parse(basicId, attrs1Xml("type=\"html\" quirks=\"false\""));
    assertEquals("Parsing quirks=\"false\"", false, spec.getView(null).getQuirks());
  }

  private String attrs1Xml(String attr1) {
    String xml =
        "<?xml version=\"1.0\"?>" +
        "<Module>" +
        "  <ModulePrefs title=\"Hello, world!\"/>" +
        "  <Content %s></Content>" +
        "</Module>";
    return String.format(xml, attr1);
  }

  private String attrs2Xml(String attr1, String attr2) {
    String xml =
        "<?xml version=\"1.0\"?>" +
        "<Module>" +
        "  <ModulePrefs title=\"Hello, world!\"/>" +
        "  <Content %s></Content>" +
        "  <Content %s></Content>" +
        "</Module>";
    return String.format(xml, attr1, attr2);
  }

  private void assertParseException(BasicGadgetId id, String xml,
      GadgetException.Code code) {
    GadgetException.Code test = null;
    try {
      parser.parse(id, xml);
    } catch (SpecParserException e) {
      test = e.getCode();
    }
    assertEquals(code, test);
  }
}
