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

  private static class BasicGadgetId implements GadgetView.ID {
    URI uri;
    public URI getURI() {
      return uri;
    }
    int moduleId;
    public int getModuleId() {
      return moduleId;
    }
    public String getKey() {
      return uri.toString();
    }
  }

  private GadgetSpec parse(String specXml) throws Exception {
    BasicGadgetId id = new BasicGadgetId();
    id.uri = new URI("http://example.org/text.xml");
    id.moduleId = 1;
    return parser.parse(id, specXml);
  }

  private void assertParseException(String specXml) throws Exception {
    try {
      GadgetSpec spec = parse(specXml);
      fail();
    } catch (SpecParserException ex) {
      // expected
    }
  }

  public void testBasicGadget() throws Exception {
    String xml = "<?xml version=\"1.0\"?>" +
                 "<Module>" +
                 "<ModulePrefs title=\"Hello, world!\"/>" +
                 "<Content type=\"html\">Hello!</Content>" +
                 "</Module>";
    GadgetSpec spec = parse(xml);

    assertEquals("Hello!", spec.getContentData());
    assertEquals("Hello, world!", spec.getTitle());
  }

  public void testEnumParsing() throws Exception {
    String xml = "<?xml version=\"1.0\"?>" +
                 "<Module>" +
                 "<ModulePrefs title=\"Test Enum\">" +
                 "<UserPref name=\"test\" datatype=\"enum\">" +
                 "<EnumValue value=\"0\" display_value=\"Zero\"/>" +
                 "<EnumValue value=\"1\" display_value=\"One\"/>" +
                 "</UserPref>" +
                 "</ModulePrefs>" +
                 "<Content type=\"html\">Hello!</Content>" +
                 "</Module>";
    GadgetSpec spec = parse(xml);

    List<GadgetSpec.UserPref> prefs = spec.getUserPrefs();

    assertEquals(1, prefs.size());
    GadgetSpec.UserPref pref = prefs.get(0);
    assertEquals("test", pref.getName());

    Map<String, String> enumValues = pref.getEnumValues();

    assertEquals(2, enumValues.size());

    assertEquals("Zero", enumValues.get("0"));
    assertEquals("One", enumValues.get("1"));
  }

  public void testNonCanonicalMetadata() throws Exception {
    String desc = "World greetings";
    String dirTitle = "Dir title";
    String cat = "hello";
    String cat2 = "hello2";
    String thumb = "http://foo.com/bar.xml";
    String xml = "<?xml version=\"1.0\"?>" +
                 "<Module>" +
                 "<ModulePrefs title=\"Hello, world!\"" +
                 " description=\"" + desc + "\"" +
                 " directory_title=\"" + dirTitle + "\"" +
                 " thumbnail=\"" + thumb + "\"" +
                 " category=\"" + cat + "\" category2=\"" + cat2 + "\"/>" +
                 "<Content type=\"html\">Hello!</Content>" +
                 "</Module>";
    GadgetSpec spec = parse(xml);

    assertEquals(desc, spec.getDescription());
    assertEquals(dirTitle, spec.getDirectoryTitle());
    assertEquals(thumb, spec.getThumbnail().toString());
    assertEquals(2, spec.getCategories().size());
    assertEquals(cat, spec.getCategories().get(0));
    assertEquals(cat2, spec.getCategories().get(1));
  }

  public void testIllegalUris() throws Exception {
    String xml = "<?xml version=\"1.0\"?>" +
                 "<Module>" +
                 "<ModulePrefs title=\"Hello, world!\"" +
                 " thumbnail=\"foo.com#bar#png\"/>" +
                 "<Content type=\"html\">Hello!</Content>" +
                 "</Module>";
    assertParseException(xml);
  }
}
