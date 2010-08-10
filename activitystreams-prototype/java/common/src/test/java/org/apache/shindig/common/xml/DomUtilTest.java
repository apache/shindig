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
package org.apache.shindig.common.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.common.xml.XmlUtil;

import com.google.common.collect.ImmutableSet;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

public class DomUtilTest {
  private static final String XML =
      "<root>" +
      "  <other>whatever</other>" +
      "  <element>zero</element>" +
      "  <ElEmEnT>one</ElEmEnT>" +
      "  <element>two</element>" +
      "  <other>not real</other>" +
      "</root>";

  private static Element root;

  @BeforeClass
  public static void createRoot() throws XmlException {
    root = XmlUtil.parse(XML);
  }

  @Test
  public void getFirstNamedChildNode() {
    assertEquals("zero", DomUtil.getFirstNamedChildNode(root, "element").getTextContent());
    assertEquals("whatever", DomUtil.getFirstNamedChildNode(root, "other").getTextContent());
    assertNull("Did not return null for missing element.",
        DomUtil.getFirstNamedChildNode(root, "fake"));
  }

  @Test
  public void getLastNamedChildNode() {
    assertEquals("two", DomUtil.getLastNamedChildNode(root, "element").getTextContent());
    assertEquals("not real", DomUtil.getLastNamedChildNode(root, "other").getTextContent());
    assertNull("Did not return null for missing element.",
        DomUtil.getLastNamedChildNode(root, "fake"));
  }

  @Test
  public void getElementsByTagNameCaseInsensitive() {
    Document doc = root.getOwnerDocument();
    List<Element> elements
        = DomUtil.getElementsByTagNameCaseInsensitive(doc, ImmutableSet.of("element"));
    assertEquals("zero", elements.get(0).getTextContent());
    assertEquals("one", elements.get(1).getTextContent());
    assertEquals("two", elements.get(2).getTextContent());
  }
}
