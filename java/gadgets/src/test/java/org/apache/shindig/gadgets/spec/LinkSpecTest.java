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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.variables.Substitutions;

import org.junit.Test;

/**
 * Tests for Link.
 */
public class LinkSpecTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/g.xml");
  private static final String REL_VALUE = "foo";
  private static final Uri HREF_VALUE = Uri.parse("http://example.org/foo");
  private static final String DEFAULT_METHOD_VALUE = "GET";
  private static final String METHOD_VALUE = "POST";

  @Test
  public void parseBasicLink() throws Exception {
    String xml = "<Link rel='" + REL_VALUE + "' href='" + HREF_VALUE + "'/>";

    LinkSpec link = new LinkSpec(XmlUtil.parse(xml), SPEC_URL);

    assertEquals(REL_VALUE, link.getRel());
    assertEquals(HREF_VALUE, link.getHref());
  }

  @Test
  public void parseRelativeLink() throws Exception {
    String xml = "<Link rel='" + REL_VALUE + "' href='/foo'/>";

    LinkSpec link = new LinkSpec(XmlUtil.parse(xml), SPEC_URL);

    link = link.substitute(new Substitutions());

    assertEquals(REL_VALUE, link.getRel());
    assertEquals(HREF_VALUE.resolve(Uri.parse("/foo")), link.getHref());
  }

  @Test
  public void parseMethodAttribute() throws Exception {
    String xml = "<Link rel='" + REL_VALUE + "' href='" + HREF_VALUE + "'/>";
    LinkSpec link = new LinkSpec(XmlUtil.parse(xml), SPEC_URL);
    assertEquals(DEFAULT_METHOD_VALUE, link.getMethod());
  }

  @Test
  public void parseAltMethodAttribute() throws Exception {
    String xml = "<Link rel='" + REL_VALUE + "' href='" + HREF_VALUE + "' method='POST'/>";
    LinkSpec link = new LinkSpec(XmlUtil.parse(xml), SPEC_URL);
    assertEquals(METHOD_VALUE, link.getMethod());
  }

  @Test
  public void substitutionsPerformed() throws Exception {
    String rel = "foo.bar";
    String href = "jp-DE.xml";
    Uri expectedHref = Uri.parse("http://example.org/jp-DE.xml");
    String xml = "<Link rel='__MSG_rel__' href='http://example.org/__MSG_href__'/>";

    LinkSpec link = new LinkSpec(XmlUtil.parse(xml), SPEC_URL);
    Substitutions substitutions = new Substitutions();
    substitutions.addSubstitution(Substitutions.Type.MESSAGE, "rel", rel);
    substitutions.addSubstitution(Substitutions.Type.MESSAGE, "href", href);
    LinkSpec substituted = link.substitute(substitutions);

    assertEquals(rel, substituted.getRel());
    assertEquals(expectedHref, substituted.getHref());
  }

  @Test(expected = SpecParserException.class)
  public void parseNoRel() throws Exception {
    String xml = "<Link href='foo'/>";
    new LinkSpec(XmlUtil.parse(xml), SPEC_URL);
  }

  @Test(expected = SpecParserException.class)
  public void parseNoHref() throws Exception {
    String xml = "<Link rel='bar'/>";
    new LinkSpec(XmlUtil.parse(xml), SPEC_URL);
  }

  @Test(expected = SpecParserException.class)
  public void parseBogusHref() throws Exception {
    String xml = "<Link rel='foo' href='$%^$#%$#$%'/>";
    new LinkSpec(XmlUtil.parse(xml), SPEC_URL);
  }

  @Test
  public void toStringIsSane() throws Exception {
    String xml = "<Link rel='" + REL_VALUE + "' href='" + HREF_VALUE + "'/>";

    LinkSpec link = new LinkSpec(XmlUtil.parse(xml), SPEC_URL);
    LinkSpec link2 = new LinkSpec(XmlUtil.parse(link.toString()), SPEC_URL);

    assertEquals(link.getRel(), link2.getRel());
    assertEquals(link.getHref(), link2.getHref());
    assertEquals(REL_VALUE, link2.getRel());
    assertEquals(HREF_VALUE, link2.getHref());
  }
}
