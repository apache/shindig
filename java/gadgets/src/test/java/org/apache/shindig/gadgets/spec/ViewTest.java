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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.expressions.RootELResolver;
import org.apache.shindig.gadgets.variables.Substitutions;
import org.apache.shindig.gadgets.variables.Substitutions.Type;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ViewTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/g.xml");

  @Test
  public void testSimpleView() throws Exception {
    String viewName = "VIEW NAME";
    String content = "This is the content";

    String xml = "<Content" +
                 " type=\"html\"" +
                 " view=\"" + viewName + '\"' +
                 " quirks=\"false\"><![CDATA[" +
                    content +
                 "]]></Content>";

    View view = new View(viewName, Arrays.asList(XmlUtil.parse(xml)), SPEC_URL);

    assertEquals(viewName, view.getName());
    Assert.assertFalse(view.getQuirks());
    assertEquals(View.ContentType.HTML, view.getType());
    assertEquals("html", view.getRawType());
    assertEquals(content, view.getContent());
    assertTrue("Default value for sign_owner should be true.", view.isSignOwner());
    assertTrue("Default value for sign_viewer should be true.", view.isSignViewer());
  }

  @Test
  public void testConcatenation() throws Exception {
    String body1 = "Hello, ";
    String body2 = "World!";
    String content1 = "<Content type=\"html\">" + body1 + "</Content>";
    String content2 = "<Content type=\"html\">" + body2 + "</Content>";
    View view = new View("test", Arrays.asList(XmlUtil.parse(content1),
                                               XmlUtil.parse(content2)), SPEC_URL);
    assertEquals(body1 + body2, view.getContent());
  }

  @Test
  public void testNonStandardContentType() throws Exception {
    String contentType = "html-inline";
    String xml = "<Content" +
                 " type=\"" + contentType + '\"' +
                 " quirks=\"false\"><![CDATA[blah]]></Content>";
    View view = new View("default", Arrays.asList(XmlUtil.parse(xml)), SPEC_URL);

    assertEquals(View.ContentType.HTML, view.getType());
    assertEquals(contentType, view.getRawType());
  }

  @Test
  public void testHtmlSanitizedContentType() throws Exception {
    String contentType = "x-html-sanitized";
    String xml = "<Content" +
                 " type=\"" + contentType + '\"' +
                 " quirks=\"false\"><![CDATA[blah]]></Content>";
    View view = new View("default", Arrays.asList(XmlUtil.parse(xml)), SPEC_URL);

    assertEquals(View.ContentType.HTML_SANITIZED, view.getType());
    assertEquals(contentType, view.getRawType());
  }

  @Test(expected = SpecParserException.class)
  public void testContentTypeConflict() throws Exception {
    String content1 = "<Content type=\"html\"/>";
    String content2 = "<Content type=\"url\" href=\"http://example.org/\"/>";
    new View("test", Arrays.asList(XmlUtil.parse(content1), XmlUtil.parse(content2)), SPEC_URL);
  }

  @Test(expected = SpecParserException.class)
  public void testHrefOnTypeUrl() throws Exception {
    String xml = "<Content type=\"url\"/>";
    new View("dummy", Arrays.asList(XmlUtil.parse(xml)), SPEC_URL);
  }

  @Test(expected = SpecParserException.class)
  public void testHrefMalformed() throws Exception {
    // Unfortunately, this actually does URI validation rather than URL, so
    // most anything will pass. urn:isbn:0321146530 is valid here.
    String xml = "<Content type=\"url\" href=\"fobad@$%!fdf\"/>";
    new View("dummy", Arrays.asList(XmlUtil.parse(xml)), SPEC_URL);
  }

  @Test
  public void testQuirksCascade() throws Exception {
    String content1 = "<Content type=\"html\" quirks=\"true\"/>";
    String content2 = "<Content type=\"html\" quirks=\"false\"/>";
    View view = new View("test", Arrays.asList(XmlUtil.parse(content1),
                                               XmlUtil.parse(content2)), SPEC_URL);
    Assert.assertFalse(view.getQuirks());
  }

  @Test
  public void testQuirksCascadeReverse() throws Exception {
    String content1 = "<Content type=\"html\" quirks=\"false\"/>";
    String content2 = "<Content type=\"html\" quirks=\"true\"/>";
    View view = new View("test", Arrays.asList(XmlUtil.parse(content1),
                                               XmlUtil.parse(content2)), SPEC_URL);
    Assert.assertTrue(view.getQuirks());
  }

  @Test
  public void testPreferredHeight() throws Exception {
    String content1 = "<Content type=\"html\" preferred_height=\"100\"/>";
    String content2 = "<Content type=\"html\" preferred_height=\"300\"/>";
    View view = new View("test", Arrays.asList(XmlUtil.parse(content1),
                                               XmlUtil.parse(content2)), SPEC_URL);
    assertEquals(300, view.getPreferredHeight());
  }

  @Test
  public void testPreferredWidth() throws Exception {
    String content1 = "<Content type=\"html\" preferred_width=\"300\"/>";
    String content2 = "<Content type=\"html\" preferred_width=\"172\"/>";
    View view = new View("test", Arrays.asList(XmlUtil.parse(content1),
                                               XmlUtil.parse(content2)), SPEC_URL);
    assertEquals(172, view.getPreferredWidth());
  }

  @Test
  public void testContentSubstitution() throws Exception {
    String xml
        = "<Content type=\"html\">Hello, __MSG_world__ __MODULE_ID__</Content>";

    Substitutions substituter = new Substitutions();
    substituter.addSubstitution(Type.MESSAGE, "world", "foo __UP_planet____BIDI_START_EDGE__");
    substituter.addSubstitution(Type.USER_PREF, "planet", "Earth");
    substituter.addSubstitution(Type.BIDI, "START_EDGE", "right");
    substituter.addSubstitution(Type.MODULE, "ID", "3");

    View view = new View("test",
        Arrays.asList(XmlUtil.parse(xml)), SPEC_URL).substitute(substituter);
    assertEquals("Hello, foo Earthright 3", view.getContent());
  }

  @Test
  public void testHrefSubstitution() throws Exception {
    String href = "http://__MSG_domain__/__MODULE_ID__?dir=__BIDI_DIR__";
    String xml = "<Content type=\"url\" href=\"" + href + "\"/>";

    Substitutions substituter = new Substitutions();
    substituter.addSubstitution(Type.MESSAGE, "domain", "__UP_subDomain__.example.org");
    substituter.addSubstitution(Type.USER_PREF, "subDomain", "up");
    substituter.addSubstitution(Type.BIDI, "DIR", "rtl");
    substituter.addSubstitution(Type.MODULE, "ID", "123");

    View view = new View("test",
        Arrays.asList(XmlUtil.parse(xml)), SPEC_URL).substitute(substituter);
    assertEquals("http://up.example.org/123?dir=rtl",
                 view.getHref().toString());
  }

  @Test
  public void testHrefRelativeSubstitution() throws Exception {
    String href = "__MSG_foo__";
    String xml = "<Content type=\"url\" href=\"" + href + "\"/>";

    Substitutions substituter = new Substitutions();
    substituter.addSubstitution(Type.MESSAGE, "foo", "/bar");

    View view = new View("test", Arrays.asList(XmlUtil.parse(xml)), SPEC_URL);
    view = view.substitute(substituter);
    assertEquals(Uri.parse("//example.org/bar"), view.getHref());
  }

  @Test
  public void testHrefWithoutSchemaResolution() throws Exception {
    String href = "//xyz.com/gadget.xml";
    String xml = "<Content type=\"url\" href=\"" + href + "\"/>";

    View view = new View("test", Arrays.asList(XmlUtil.parse(xml)), SPEC_URL);
    assertEquals(Uri.parse(href), view.getHref());
  }

  @Test
  public void authAttributes() throws Exception {
    String xml = "<Content type='html' sign_owner='false' sign_viewer='false' foo='bar' " +
                 "yo='momma' sub='__MSG_view__'/>";

    View view = new View("test", Arrays.asList(XmlUtil.parse(xml)), SPEC_URL);
    Substitutions substituter = new Substitutions();
    substituter.addSubstitution(Substitutions.Type.MESSAGE, "view", "stuff");
    View substituted = view.substitute(substituter);
    assertEquals("bar", substituted.getAttributes().get("foo"));
    assertEquals("momma", substituted.getAttributes().get("yo"));
    assertEquals("stuff", substituted.getAttributes().get("sub"));
    assertFalse("sign_owner parsed incorrectly.", view.isSignOwner());
    assertFalse("sign_viewer parsed incorrectly.", view.isSignViewer());
  }

  @Test
  public void testSocialPreload() throws Exception {
    String xml = "<Content href=\"http://example.org/proxied.php\" "
        + "authz=\"SIGNED\">"
        + "<OwnerRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " fields=\"name,id\""
        + "/></Content>";
    View view = new View("test", Arrays.asList(XmlUtil.parse(xml)), SPEC_URL);
    PipelinedData.Batch batch = view.getPipelinedData().getBatch(
        Expressions.forTesting(), new RootELResolver());

    assertEquals(1, batch.getPreloads().size());
    assertTrue(batch.getPreloads().containsKey("key"));
  }

  @Test(expected = SpecParserException.class)
  public void testSocialPreloadWithoutAuth() throws Exception {
    // Not signed, so a parse exception will result
    String xml = "<Content href=\"http://example.org/proxied.php\" "
        + "sign_owner=\"true\">"
        + "<OwnerRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " fields=\"name,id\""
        + "/></Content>";
    new View("test", Arrays.asList(XmlUtil.parse(xml)), SPEC_URL);
  }

  @Test(expected = SpecParserException.class)
  public void testSocialPreloadWithoutSignOwner() throws Exception {
    // Signed, but not by owner when owner data is fetched
    String xml = "<Content href=\"http://example.org/proxied.php\" "
        + "authz=\"SIGNED\" sign_owner=\"false\">"
        + "<OwnerRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " fields=\"name,id\""
        + "/></Content>";
    new View("test", Arrays.asList(XmlUtil.parse(xml)), SPEC_URL);
  }

  @Test(expected = SpecParserException.class)
  public void testSocialPreloadWithoutSignViewer() throws Exception {
    // Signed, but not by viewer when viewer data is fetched
    String xml = "<Content href=\"http://example.org/proxied.php\" "
        + "authz=\"SIGNED\" sign_viewer=\"false\">"
        + "<ViewerRequest xmlns=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" "
        + " key=\"key\""
        + " fields=\"name,id\""
        + "/></Content>";
    new View("test", Arrays.asList(XmlUtil.parse(xml)), SPEC_URL);
  }
}
