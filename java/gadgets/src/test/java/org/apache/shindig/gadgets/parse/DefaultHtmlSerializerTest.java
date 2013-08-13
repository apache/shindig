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

import org.apache.shindig.gadgets.parse.nekohtml.NekoSimplifiedHtmlParser;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;

public class DefaultHtmlSerializerTest {

  @Test
  public void testComplicatedSerialize() throws Exception {
    String txt = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\""
            + " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
            + "<html xml:lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\">"
            + "<head><title>Apache Shindig!</title></head>"
            + "<body class=\"composite\">\n"
            + "    <div id=\"bodyColumn\">hello\n"
            + "      <div id=\"contentBox\"></div> \n"
            + "      <div class=\"clear\"><hr></div> \n"
            + "    </div>\n"
            +   "</body></html>";
    NekoSimplifiedHtmlParser parser = new NekoSimplifiedHtmlParser(
        new ParseModule.DOMImplementationProvider().get());

    Document doc = parser.parseDom(txt);
    DefaultHtmlSerializer serializer = new DefaultHtmlSerializer();
    assertEquals("Serialized full document", txt, serializer.serialize(doc));
  }

  @Test
  public void testComments() throws Exception {

    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    doc.appendChild(doc.createElement("ABC"));
    doc.appendChild(doc.createComment("XYZ"));

    DefaultHtmlSerializer serializer = new DefaultHtmlSerializer();
    assertEquals("Comment is preserved",
        "<ABC></ABC><!--XYZ-->", serializer.serialize(doc));
  }

  @Test
  public void testEntities() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    Element element = doc.createElement("abc");
    element.setAttribute("a", "\\x3e\">");
    doc.appendChild(element);

    DefaultHtmlSerializer serializer = new DefaultHtmlSerializer();
    assertEquals("Entities escaped",
        "<abc a=\"\\x3e&#34;&gt;\"></abc>", serializer.serialize(doc));
  }

  @Test
  public void testHrefEntities() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    Element element = doc.createElement("a");
    element.setAttribute("href", "http://apache.org/?a=0&query=2+3");
    doc.appendChild(element);

    DefaultHtmlSerializer serializer = new DefaultHtmlSerializer();
    assertEquals("href entities escaped",
        "<a href=\"http://apache.org/?a=0&amp;query=2+3\"></a>",
        serializer.serialize(doc));
  }

  @Test
  public void testDataTemplateTags() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    Element element = doc.createElement("osdata");
    element.setAttribute("xmlns:foo", "#foo");
    doc.appendChild(element);

    DefaultHtmlSerializer serializer = new DefaultHtmlSerializer();
    assertEquals("OSData normalized",
        "<script type=\"text/os-data\" xmlns:foo=\"#foo\"></script>",
        serializer.serialize(doc));
  }
}

