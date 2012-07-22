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
package org.apache.shindig.gadgets.parse;

import com.google.common.collect.ImmutableList;

import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.apache.shindig.gadgets.parse.nekohtml.NekoSimplifiedHtmlParser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.assertEquals;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;

public class HtmlSerializationTest {
  Document doc;
  List<GadgetHtmlParser> parsers;

  @Before
  public void setUp() throws Exception {
    doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    GadgetHtmlParser neko = new NekoSimplifiedHtmlParser(
            new ParseModule.DOMImplementationProvider().get());

    GadgetHtmlParser caja = new CajaHtmlParser(
            new ParseModule.DOMImplementationProvider().get());

    parsers = ImmutableList.of(neko, caja);
  }

  @Test
  @Ignore("Caja parses OS script tags but does not serialize them to their original form")
  public void testSerialize() throws Exception {
    String markup = "<!DOCTYPE html>\n"
        + "<html><head><title>Apache Shindig!</title></head>"
        + "<body>"
        + "<script type=\"text/os-data\" xmlns:os=\"http://ns.opensocial.org/2008/markup\">"
        + "  <os:PeopleRequest groupId=\"@friends\" key=\"friends\" userId=\"@viewer\"></os:PeopleRequest>\n"
        + "</script>"
        + "<script require=\"friends\" type=\"text/os-template\">\n"
        + "  <ul><li repeat=\"${friends}\">\n"
        + "    <span id=\"id${Context.Index}\">${Cur.name.givenName}</span>\n"
        + "  </li></ul>"
        + "</script>"
        + "</body></html>";

    for(GadgetHtmlParser parser : parsers) {
      Document doc = parser.parseDom(markup);
      String result = HtmlSerialization.serialize(doc);
      assertEquals(markup, result);
    }
  }

  @Test
  public void testSerializeHtml() throws Exception {
    String markup = "<!DOCTYPE html>\n"
        + "<html><head><title>Apache Shindig!</title></head>"
        + "<body>"
        + "<div xmlns:osx=\"http://ns.opensocial.org/2008/extensions\">"
        + "<osx:NavigateToApp>\n"
        + "<img border=\"0\" src=\"foo.gif\">\n"
        + "</osx:NavigateToApp>\n"
        + "</div>"
        + "</body></html>";

    for(GadgetHtmlParser parser : parsers) {
      Document doc = parser.parseDom(markup);
      String result = HtmlSerialization.serialize(doc);
      assertEquals(markup, result);
    }
  }
}
