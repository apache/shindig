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
package org.apache.shindig.social.dataservice.integration;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.shindig.social.opensocial.model.ActivityEntry;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class RestfulAtomActivityEntryTest extends AbstractLargeRestfulTests {
  
  private static XPath xpath = XPathFactory.newInstance().newXPath();
  
  private ActivityEntry johnsEntry1;
  private ActivityEntry johnsEntry2;


  @Before
  public void restfulXmlActivityEntryTestBefore() throws Exception {
    johnsEntry1 = RestfulXmlActivityEntryTest.generateEntry1();
    johnsEntry2 = RestfulXmlActivityEntryTest.generateEntry2();
  }

  /**
   * Tests GET for a single ActivityEntry.
   * 
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetActivityEntryAtom() throws Exception {
    String resp = getResponse("/activitystreams/john.doe/@self/@app/object1", "GET", "atom", "application/atom+xml");
    
    // TODO: Uncomment this if ActivityEntry added to opensocial.xsd
    //XSDValidator.validateOpenSocial(resp);
    
    // Build XML Document
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(new InputSource(new StringReader(resp)));
    
    // Test ActivityEntry
    Node entryNode = (Node)xpath.evaluate("/feed/entry", doc, XPathConstants.NODE);
    assertNotNull("ActivityEntry should not be null", entryNode);
    assertActivityEntriesAtomEqual(johnsEntry1, entryNode);
  }

  /**
   * Tests GET for a list of ActivityEntries.
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetActivityEntriesAtom() throws Exception {
    String resp = getResponse("/activitystreams/john.doe/@self", "GET", "atom", "application/atom+xml");
    
    // TODO: Uncomment this if ActivityEntry added to opensocial.xsd
    //XSDValidator.validateOpenSocial(resp);
    
    // Build XML Document
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(new InputSource(new StringReader(resp)));
    
    // Test ActivityEntries
    NodeList entries = (NodeList)xpath.evaluate("/feed/entry", doc, XPathConstants.NODESET);
    assertEquals(2, entries.getLength());
    if (johnsEntry1.getObject().getId().equals(xpath.evaluate("content/activityEntry/object/id", entries.item(0)))) {
      assertActivityEntriesAtomEqual(johnsEntry1, entries.item(0));
      assertActivityEntriesAtomEqual(johnsEntry2, entries.item(1));
    } else {
      assertActivityEntriesAtomEqual(johnsEntry2, entries.item(0));
      assertActivityEntriesAtomEqual(johnsEntry1, entries.item(1));
    }
  }
  
  @Test
  public void testCreateActivityAtom() throws Exception {
    // TODO: REST POST with format = XML or ATOM doens't work; mapping with List or Map doesn't work
  }
  
  /**
   * Asserts that an ActivityEntry is equivalent to its ATOM counterpart.
   * 
   * @param entry is the ActivityEntry to compare to an ATOM node
   * @param atomNode is the ATOM node to compare to the ActivityEntry
   * @throws XPathExpressionException 
   */
  private static void assertActivityEntriesAtomEqual(ActivityEntry entry, Node atomNode) throws XPathExpressionException {
    // Test single level fields
    RestfulXmlActivityEntryTest.assertEqualsOrNull(entry.getObject().getId(), xpath.evaluate("id", atomNode));
    RestfulXmlActivityEntryTest.assertEqualsOrNull(entry.getTitle(), xpath.evaluate("title", atomNode));
    RestfulXmlActivityEntryTest.assertEqualsOrNull(entry.getBody(), xpath.evaluate("summary", atomNode));
    RestfulXmlActivityEntryTest.assertEqualsOrNull(entry.getActor().getId(), xpath.evaluate("author/uri", atomNode));
    RestfulXmlActivityEntryTest.assertEqualsOrNull(entry.getActor().getDisplayName(), xpath.evaluate("author/name", atomNode));

    // Test the ATOM's ActivityEntry within content
    Node entryNode = (Node)xpath.evaluate("content/activityEntry", atomNode, XPathConstants.NODE);
    RestfulXmlActivityEntryTest.assertActivityEntriesEqual(entry, entryNode);
  }
}
