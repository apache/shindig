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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.variables.Substitutions;
import org.apache.shindig.gadgets.variables.Substitutions.Type;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;

public class GadgetSpecTest extends Assert {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/g.xml");

  @Test
  public void testBasic() throws Exception {
    String xml = "<Module>" +
                 "<ModulePrefs title=\"title\"/>" +
                 "<UserPref name=\"foo\" datatype=\"string\"/>" +
                 "<Content type=\"html\">Hello!</Content>" +
                 "</Module>";
    GadgetSpec spec = new GadgetSpec(SPEC_URL, xml);
    assertEquals("title", spec.getModulePrefs().getTitle());
    assertEquals(UserPref.DataType.STRING,
        spec.getUserPrefs().get("foo").getDataType());
    assertEquals("Hello!", spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
  }

  @Test
  public void testUserPrefsOrder() throws Exception {
    String xml = "<Module>" +
                 "<ModulePrefs title=\"title\"/>" +
                 "<UserPref name=\"a\" datatype=\"string\"/>" +
                 "<UserPref name=\"z\" datatype=\"string\"/>" +
                 "<UserPref name=\"b\" datatype=\"string\"/>" +
                 "<UserPref name=\"y\" datatype=\"string\"/>" +
                 "<Content type=\"html\">Hello!</Content>" +
                 "</Module>";
    GadgetSpec spec = new GadgetSpec(SPEC_URL, xml);
    assertEquals("title", spec.getModulePrefs().getTitle());
    Collection<UserPref> prefs = spec.getUserPrefs().values();
    Iterator<UserPref> iter = prefs.iterator();
    assertEquals("a", iter.next().getName());
    assertEquals("z", iter.next().getName());
    assertEquals("b", iter.next().getName());
    assertEquals("y", iter.next().getName());
    assertEquals("Hello!", spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
  }

  @Test
  public void testAlternativeConstructor() throws Exception {
    String xml = "<Module>" +
                 "<ModulePrefs title=\"title\"/>" +
                 "<UserPref name=\"foo\" datatype=\"string\"/>" +
                 "<Content type=\"html\">Hello!</Content>" +
                 "</Module>";
    GadgetSpec spec = new GadgetSpec(SPEC_URL, XmlUtil.parse(xml), xml);
    assertEquals("title", spec.getModulePrefs().getTitle());
    assertEquals(UserPref.DataType.STRING,
        spec.getUserPrefs().get("foo").getDataType());
    assertEquals("Hello!", spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());

    assertEquals(HashUtil.checksum(xml.getBytes()), spec.getChecksum());
  }

  @Test
  public void testMultipleContentSections() throws Exception {
    String xml = "<Module>" +
                 "<ModulePrefs title=\"title\"/>" +
                 "<Content type=\"html\" view=\"hello\">hello </Content>" +
                 "<Content type=\"html\" view=\"world\">world</Content>" +
                 "<Content type=\"html\" view=\"hello, test\">test</Content>" +
                 "</Module>";
    GadgetSpec spec = new GadgetSpec(SPEC_URL, xml);
    assertEquals("hello test", spec.getView("hello").getContent());
    assertEquals("world", spec.getView("world").getContent());
    assertEquals("test", spec.getView("test").getContent());
  }

  @Test(expected=SpecParserException.class)
  public void testMissingModulePrefs() throws Exception {
    String xml = "<Module>" +
                 "<Content type=\"html\"/>" +
                 "</Module>";
    new GadgetSpec(SPEC_URL, xml);
  }

  @Test(expected=SpecParserException.class)
  public void testEnforceOneModulePrefs() throws Exception {
    String xml = "<Module>" +
                 "<ModulePrefs title=\"hello\"/>" +
                 "<ModulePrefs title=\"world\"/>" +
                 "<Content type=\"html\"/>" +
                 "</Module>";
    new GadgetSpec(SPEC_URL, xml);
  }

  @Test(expected=SpecParserException.class)
  public void testEnforceUserPrefNoDuplicate() throws Exception {
    String xml = "<Module>" +
                 "<ModulePrefs title=\"hello\"/>" +
                 "<UserPref name=\"foo\" datatype=\"string\"/>" +
                 "<UserPref name=\"foo\" datatype=\"int\"/>" +
                 "<Content type=\"html\"/>" +
                 "</Module>";
    new GadgetSpec(SPEC_URL, xml);
  }

  @Test
  public void testSubstitutions() throws Exception {
    Substitutions substituter = new Substitutions();
    String title = "Hello, World!";
    String content = "Goodbye, world :(";
    String xml = "<Module>" +
                 "<ModulePrefs title=\"__UP_title__\"/>" +
                 "<Content type=\"html\">__MSG_content__</Content>" +
                 "</Module>";
    substituter.addSubstitution(Type.USER_PREF, "title", title);
    substituter.addSubstitution(Type.MESSAGE, "content", content);

    GadgetSpec baseSpec = new GadgetSpec(SPEC_URL, xml);
    baseSpec.setAttribute("foo", 100);
    baseSpec.setAttribute("bar", "baz");

    GadgetSpec spec = baseSpec.substitute(substituter);
    assertEquals(title, spec.getModulePrefs().getTitle());
    assertEquals(content, spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
    assertEquals(100, spec.getAttribute("foo"));
    assertEquals("baz", spec.getAttribute("bar"));
  }
}
