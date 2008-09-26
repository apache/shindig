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

import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.variables.Substitutions;

import org.junit.Test;

import java.net.URI;

/**
 * Tests for Link.
 */
public class LinkSpecTest {
  private static final String REL_VALUE = "foo";
  private static final URI HREF_VALUE = URI.create("http://example.org/foo");

  @Test
  public void parseBasicLink() throws Exception {
    String xml = "<Link rel='" + REL_VALUE + "' href='" + HREF_VALUE + "'/>";

    LinkSpec link = new LinkSpec(XmlUtil.parse(xml));

    assertEquals(REL_VALUE, link.getRel());
    assertEquals(HREF_VALUE, link.getHref());
  }

  @Test
  public void substitutionsPerformed() throws Exception {
    String rel = "foo.bar";
    String href = "jp-DE.xml";
    URI expectedHref = URI.create("http://example.org/jp-DE.xml");
    String xml = "<Link rel='__MSG_rel__' href='http://example.org/__MSG_href__'/>";

    LinkSpec link = new LinkSpec(XmlUtil.parse(xml));
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
    new LinkSpec(XmlUtil.parse(xml));
  }

  @Test(expected = SpecParserException.class)
  public void parseNoHref() throws Exception {
    String xml = "<Link rel='bar'/>";
    new LinkSpec(XmlUtil.parse(xml));
  }

  @Test(expected = SpecParserException.class)
  public void parseBogusHref() throws Exception {
    String xml = "<Link rel='foo' href='$%^$#%$#$%'/>";
    new LinkSpec(XmlUtil.parse(xml));
  }

  @Test
  public void toStringIsSane() throws Exception {
    String xml = "<Link rel='" + REL_VALUE + "' href='" + HREF_VALUE + "'/>";

    LinkSpec link = new LinkSpec(XmlUtil.parse(xml));
    LinkSpec link2 = new LinkSpec(XmlUtil.parse(link.toString()));

    assertEquals(link.getRel(), link2.getRel());
    assertEquals(link.getHref(), link2.getHref());
    assertEquals(REL_VALUE, link2.getRel());
    assertEquals(HREF_VALUE, link2.getHref());

  }
}
