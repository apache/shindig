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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.shindig.social.core.model.ActionLinkImpl;
import org.apache.shindig.social.core.model.ActivityEntryImpl;
import org.apache.shindig.social.core.model.ActivityObjectImpl;
import org.apache.shindig.social.core.model.MediaLinkImpl;
import org.apache.shindig.social.core.model.StandardLinkImpl;
import org.apache.shindig.social.opensocial.model.ActionLink;
import org.apache.shindig.social.opensocial.model.ActivityEntry;
import org.apache.shindig.social.opensocial.model.ActivityObject;
import org.apache.shindig.social.opensocial.model.MediaLink;
import org.apache.shindig.social.opensocial.model.StandardLink;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class RestfulXmlActivityEntryTest extends AbstractLargeRestfulTests {
  
  private static XPath xpath = XPathFactory.newInstance().newXPath();
  
  private ActivityEntry johnsEntry1;
  private ActivityEntry johnsEntry2;


  @Before
  public void restfulXmlActivityEntryTestBefore() throws Exception {
    johnsEntry1 = generateEntry1();
    johnsEntry2 = generateEntry2();
  }
  
  protected static ActivityEntry generateEntry1() {
    ActivityObject actor = new ActivityObjectImpl();
    actor.setId("john.doe");
    actor.setDisplayName("John Doe");
    
    MediaLink image = new MediaLinkImpl();
    image.setUrl("https://docs.google.com/uc?id=0B-zwcqgujo7CMTRmZTg3Y2UtZGZlYS00MTY4LWFjOTItNDRiNmY3MzRmYzQ5&export=download&hl=en");
    image.setType("http://activitystrea.ms/schema/1.0/picture");
    image.setWidth(400);
    image.setHeight(300);
    image.setDuration(93);
    
    ActionLink actionLink1 = new ActionLinkImpl();
    actionLink1.setCaption("Frozen at the top of the German Alps");
    actionLink1.setTarget("http://johnsalbums/germany123/mediaItem3");
    ActionLink actionLink2 = new ActionLinkImpl();
    actionLink2.setCaption("Sign of Garmisch-Partenkirchen");
    actionLink2.setTarget("http://johnsalbums/germany123/mediaItem6");
    List<ActionLink> actionLinks = new ArrayList<ActionLink>();
    actionLinks.add(actionLink1);
    actionLinks.add(actionLink2);
    
    StandardLink standardLink1 = new StandardLinkImpl();
    standardLink1.setHref("www.mypics.com/1");
    standardLink1.setType("image/jpg");
    StandardLink standardLink2 = new StandardLinkImpl();
    standardLink2.setHref("www.mypics.com/2");
    standardLink2.setType("image/jpg");
    StandardLink standardLink3 = new StandardLinkImpl();
    standardLink3.setHref("www.mypics.com/3");
    standardLink3.setType("image/jpg");
    StandardLink standardLink4 = new StandardLinkImpl();
    standardLink4.setHref("www.mypics.com/4");
    standardLink4.setType("image/jpg");
    List<StandardLink> standardLinksList1 = new ArrayList<StandardLink>();
    standardLinksList1.add(standardLink1);
    standardLinksList1.add(standardLink2);
    List<StandardLink> standardLinksList2 = new ArrayList<StandardLink>();
    standardLinksList2.add(standardLink3);
    standardLinksList2.add(standardLink4);
    Map<String, List<StandardLink>> standardLinks = new HashMap<String, List<StandardLink>>();
    standardLinks.put("myRel1", standardLinksList1);
    standardLinks.put("myRel2", standardLinksList2);
    
    ActivityObject johnsObject = new ActivityObjectImpl();
    johnsObject.setId("object1");
    johnsObject.setDisplayName("Frozen Eric");
    johnsObject.setLink("http://www.johnsalbums/germany123");
    johnsObject.setObjectType("picture");
    johnsObject.setImage(image);
    johnsObject.setActionLinks(actionLinks);
    
    List<String> to = new ArrayList<String>();
    to.add("jane.doe");
    to.add("canonical");
    List<String> cc = new ArrayList<String>();
    cc.add("george.doe");
    
    ActivityEntry entry = new ActivityEntryImpl();
    entry.setTitle("John posted a photo");
    entry.setBody("John Doe posted a photo to the album Germany 2009");
    entry.setPostedTime("2010-04-27T06:02:36+0000");
    entry.setActor(actor);
    entry.setVerb("post");
    entry.setObject(johnsObject);
    entry.setStandardLinks(standardLinks);
    entry.setTo(to);
    entry.setCc(cc);
    return entry;
  }
  
  protected static ActivityEntry generateEntry2() {
    ActivityObject actor = new ActivityObjectImpl();
    actor.setId("john.doe");
    actor.setDisplayName("John Doe");
    
    ActivityObject johnsObject = new ActivityObjectImpl();
    johnsObject.setId("object2");
    johnsObject.setDisplayName("Super simple ActivityObject");
    johnsObject.setObjectType("article");
    
    ActivityEntry entry = new ActivityEntryImpl();
    entry.setTitle("Super simple ActivityEntry");
    entry.setPostedTime("2010-04-27T06:02:36+0000");
    entry.setActor(actor);
    entry.setVerb("post");
    entry.setObject(johnsObject);
    return entry;
  }

  /**
   * Tests GET for a single ActivityEntry.
   * 
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetActivityEntryXml() throws Exception {
    String resp = getResponse("/activitystreams/john.doe/@self/@app/object1", "GET", "xml", "application/xml");
    
    // TODO: Uncomment this if ActivityEntry added to opensocial.xsd
    //XSDValidator.validateOpenSocial(resp);
    
    // Build XML Document
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(new InputSource(new StringReader(resp)));
    
    // Test ActivityEntry
    Node entryNode = (Node)xpath.evaluate("/response/activityEntry", doc, XPathConstants.NODE);
    assertNotNull("ActivityEntry should not be null", entryNode);
    assertActivityEntriesEqual(johnsEntry1, entryNode);
  }

  /**
   * Tests GET for a list of ActivityEntries.
   *
   * @throws Exception if test encounters an error
   */
  @Test
  public void testGetActivityEntriesXml() throws Exception {
    String resp = getResponse("/activitystreams/john.doe/@self", "GET", "xml", "application/xml");
    
    // TODO: Uncomment this if ActivityEntry added to opensocial.xsd
    //XSDValidator.validateOpenSocial(resp);
    
    // Build XML Document
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(new InputSource(new StringReader(resp)));
    
    // Test ActivityEntries
    NodeList entries = (NodeList)xpath.evaluate("/response/entry/activityEntry", doc, XPathConstants.NODESET);
    assertEquals(2, entries.getLength());
    if (johnsEntry1.getObject().getId().equals(xpath.evaluate("object/id", entries.item(0)))) {
      assertActivityEntriesEqual(johnsEntry1, entries.item(0));
      assertActivityEntriesEqual(johnsEntry2, entries.item(1));
    } else {
      assertActivityEntriesEqual(johnsEntry2, entries.item(0));
      assertActivityEntriesEqual(johnsEntry1, entries.item(1));
    }
  }
  
  @Test
  public void testCreateActivityEntryXml() throws Exception {
    // TODO: REST POST with format = XML or ATOM doens't work; mapping with List or Map doesn't work
  }
  
  // ------------------------ ACTIVITYENTRY EQUALITY TESTS --------------------------
  /**
   * Utility to determine if two ActivityEntries are equal.  Tests the
   * structure of the XML.
   * 
   * @param entry is an ActivityEntry to compare
   * @param entryNode is an XML node containing the other ActivityEntry
   * 
   * @throws XPathExpressionException if something goes wrong
   */
  protected static void assertActivityEntriesEqual(ActivityEntry entry, Node entryNode) throws XPathExpressionException {
    if (entry != null) {
      // Test single level items
      assertEqualsOrNull(entry.getTitle(), xpath.evaluate("title", entryNode));
      assertEqualsOrNull(entry.getBody(), xpath.evaluate("body", entryNode));
      assertEqualsOrNull(entry.getPostedTime(), xpath.evaluate("postedTime", entryNode));
      assertEqualsOrNull(entry.getVerb(), xpath.evaluate("verb", entryNode));
      
      // Test ActivityEntry's ActivityObjects
      assertActivityObjectsEqual(entry.getActor(), (Node)xpath.evaluate("actor", entryNode, XPathConstants.NODE));
      assertActivityObjectsEqual(entry.getObject(), (Node)xpath.evaluate("object", entryNode, XPathConstants.NODE));
      assertActivityObjectsEqual(entry.getTarget(), (Node)xpath.evaluate("target", entryNode, XPathConstants.NODE));
      assertActivityObjectsEqual(entry.getGenerator(), (Node)xpath.evaluate("generator", entryNode, XPathConstants.NODE));
      assertActivityObjectsEqual(entry.getProvider(), (Node)xpath.evaluate("provider", entryNode, XPathConstants.NODE));
      
      // Test ActivityEntry's List<String>
      assertListsStringEqual(entry.getTo(), (NodeList)xpath.evaluate("to", entryNode, XPathConstants.NODESET));
      assertListsStringEqual(entry.getCc(), (NodeList)xpath.evaluate("cc", entryNode, XPathConstants.NODESET));
      assertListsStringEqual(entry.getBcc(), (NodeList)xpath.evaluate("bcc", entryNode, XPathConstants.NODESET));
      
      // Test ActivityEntry's Map<String, List<StandardLink>>
      assertStandardLinkMapsEqual(entry.getStandardLinks(), (Node)xpath.evaluate("standardLinks", entryNode, XPathConstants.NODE));
    } else {
      assertNull("EntryNode should be null", entryNode);
    }
  }
  
  /**
   * Asserts that two ActivityObjects are equal.
   * 
   * @param object is an ActivityObject to compare with an XML node
   * @param objectNode is the XML node to compare with the ActivityObject
   * @throws XPathExpressionException 
   */
  private static void assertActivityObjectsEqual(ActivityObject object, Node objectNode) throws XPathExpressionException {
    if (object != null) {
      // Test single level items
      assertEqualsOrNull(object.getId(), xpath.evaluate("id", objectNode));
      assertEqualsOrNull(object.getDisplayName(), xpath.evaluate("displayName", objectNode));
      assertEqualsOrNull(object.getLink(), xpath.evaluate("link", objectNode));
      assertEqualsOrNull(object.getObjectType(), xpath.evaluate("objectType", objectNode));
      
      // Test ActivityObject's image
      assertMediaLinksEqual(object.getImage(), (Node)xpath.evaluate("image", objectNode, XPathConstants.NODE));
      
      // Test ActivityObject's inReplyTo
      assertActivityObjectsEqual(object.getInReplyTo(), (Node)xpath.evaluate("inReplyTo", objectNode, XPathConstants.NODE));
      
      // Test ActivityObject's Map<String, List<StandardLink>> standardLinks
      assertStandardLinkMapsEqual(object.getStandardLinks(), (Node)xpath.evaluate("standardLinks", objectNode, XPathConstants.NODE));
      
      // Test ActivityObject's List<ActivityObject> elements
      assertListsActivityObjectEqual(object.getAttachedObjects(), (NodeList)xpath.evaluate("attachedObjects", objectNode, XPathConstants.NODESET));
      assertListsActivityObjectEqual(object.getReplies(), (NodeList)xpath.evaluate("repies", objectNode, XPathConstants.NODESET));
      assertListsActivityObjectEqual(object.getReactions(), (NodeList)xpath.evaluate("reactions", objectNode, XPathConstants.NODESET));
      
      // Test ActivityObject's actionLinks
      assertListsActionLinkEqual(object.getActionLinks(), (NodeList)xpath.evaluate("actionLinks/actionLink", objectNode, XPathConstants.NODESET));
    } else {
      assertNull("ActivityObject should be null", objectNode);
    }
  }
  
  /**
   * Asserts that a Map<String, List<StandardLink>> is equal its XML NodeList equivalent.
   * 
   * @param map is the map object to compare to the XML node
   * @param mapNode is the XML node to compare to the map object
   * 
   * @throws XPathExpressionException if something goes wrong :)
   */
  private static void assertStandardLinkMapsEqual(Map<String, List<StandardLink>> map, Node mapNode) throws XPathExpressionException {
    if (map != null) {
      NodeList entries = (NodeList)xpath.evaluate("entry", mapNode, XPathConstants.NODESET);
      assertEquals(map.size(), entries.getLength());
      for (String rel : map.keySet()) {
        Node relNode = findNode("key", rel, entries);
        assertNotNull("'" + rel + "' rel not found", relNode);
        NodeList standardLinkNodes = (NodeList)xpath.evaluate("value/standardLink", relNode, XPathConstants.NODESET);
        List<StandardLink> standardLinks = map.get(rel);
        assertListsStandardLinkEqual(standardLinks, standardLinkNodes);
      }
    } else {
      assertNull("mapNode should be null", mapNode);
    }
  }
  
  /**
   * Asserts that a List<String> is equal to its XML node equivalent.
   * 
   * @param list is the list to compare to an XML node
   * @param nodeList is the XML node to compare to the list
   * 
   * @throws XPathExpressionException if something goes wrong :)
   */
  private static void assertListsStringEqual(List<String> list, NodeList nodeList) throws XPathExpressionException {
    if (list != null) {
      assertEquals(list.size(), nodeList.getLength());
      for (String value : list) {
        assertNotNull("'" + value + "' was not found in nodeList", findNode(".", value, nodeList));
      }
    } else {
      assertEquals(0, nodeList.getLength());
    }
  }
  
  /**
   * Asserts that a List<ActivityObject> is equal to its XML node equivalent.
   * 
   * @param list is the list to compare to an XML node
   * @param nodeList is the XML node to compare to the list
   * 
   * @throws XPathExpressionException if something goes wrong :)
   */
  private static void assertListsActivityObjectEqual(List<ActivityObject> list, NodeList nodeList) throws XPathExpressionException {
    if (list != null) {
      assertEquals(list.size(), nodeList.getLength());
      for (ActivityObject object : list) {
        Node objectNode = findNode("id", object.getId(), nodeList);
        assertNotNull("node with id '" + object.getId() + "' not found", objectNode);
        assertActivityObjectsEqual(object, objectNode);
      }
    } else {
      assertEquals(0, nodeList.getLength());
    }
  }
  
  /**
   * Asserts that a List<ActionLink> is equal to its XML node equivalent.
   * 
   * @param list is the list to compare to an XML node
   * @param nodeList is the XML node to compare to the list
   * 
   * @throws XPathExpressionException if something goes wrong :)
   */
  private static void assertListsActionLinkEqual(List<ActionLink> list, NodeList nodeList) throws XPathExpressionException {
    if (list != null) {
      assertEquals(list.size(), nodeList.getLength());
      for (ActionLink actionLink : list) {
        Node actionLinkNode = findNode("target", actionLink.getTarget(), nodeList);
        assertNotNull("node with target '" + actionLink.getTarget() + "' not found", actionLinkNode);
        assertActionLinksEqual(actionLink, actionLinkNode);
      }
    } else {
      assertEquals(0, nodeList.getLength());
    }
  }
  
  /**
   * Asserts that a List<StandardLink> is equal to its XML node equivalent.
   * 
   * @param list is the list to compare to an XML node
   * @param nodeList is the XML node to compare to the list
   * 
   * @throws XPathExpressionException if something goes wrong :)
   */
  private static void assertListsStandardLinkEqual(List<StandardLink> list, NodeList nodeList) throws XPathExpressionException {
    if (list != null) {
      assertEquals(list.size(), nodeList.getLength());
      for (StandardLink standardLink : list) {
        Node standardLinkNode = findNode("href", standardLink.getHref(), nodeList);
        assertNotNull("node with href '" + standardLink.getHref() + "' not found", standardLinkNode);
        assertStandardLinksEqual(standardLink, standardLinkNode);
      }
    } else {
      assertEquals(0, nodeList.getLength());
    }
  }
  
  /**
   * Asserts that two MediaLinks are equal.
   * 
   * @param media is the MediaLink object to compare with the XML node
   * @param mediaNode mediaNode is the XML node to compare with the MediaLink object
   * @throws XPathExpressionException 
   */
  private static void assertMediaLinksEqual(MediaLink media, Node mediaNode) throws XPathExpressionException {
    if (media != null) {
      assertEqualsOrNull(media.getDuration().toString(), xpath.evaluate("duration", mediaNode));
      assertEqualsOrNull(media.getHeight().toString(), xpath.evaluate("height", mediaNode));
      assertEqualsOrNull(media.getWidth().toString(), xpath.evaluate("width", mediaNode));
      assertEqualsOrNull(media.getType(), xpath.evaluate("type", mediaNode));
      assertEqualsOrNull(media.getUrl(), xpath.evaluate("url", mediaNode));
    } else {
      assertNull("Image should be null", mediaNode);
    }
  }
  
  /**
   * Asserts an ActionLink to its XML node equivalent
   * 
   * @param actionLink is the ActionLink to compare to the XML node
   * @param actionLinkNode is the XML node to compare to the ActionLink
   * @throws XPathExpressionException 
   */
  private static void assertActionLinksEqual(ActionLink actionLink, Node actionLinkNode) throws XPathExpressionException {
    assertEqualsOrNull(actionLink.getTarget(), xpath.evaluate("target", actionLinkNode));
    assertEqualsOrNull(actionLink.getCaption(), xpath.evaluate("caption", actionLinkNode));
  }
  
  /**
   * Asserts that two StandardLinks are equal.
   * 
   * @throws XPathExpressionException if something goes wrong :)
   */
  private static void assertStandardLinksEqual(StandardLink standardLink, Node standardLinkNode) throws XPathExpressionException {
    assertEqualsOrNull(standardLink.getHref(), xpath.evaluate("href", standardLinkNode));
    assertEqualsOrNull(standardLink.getType(), xpath.evaluate("type", standardLinkNode));
    assertEqualsOrNull(standardLink.getInline(), xpath.evaluate("inline", standardLinkNode));
  }
  
  /**
   * Finds a Node within a NodeList with the given key at the given path.
   * 
   * @param path is the path to the key
   * @param key is the key that identifies the node to find
   * @param nodeList is the list of nodes to search through
   * 
   * @return Node is the found node if located, null otherwise
   */
  private static Node findNode(String path, String key, NodeList nodeList) throws XPathExpressionException {
    for (int i = 0; i < nodeList.getLength(); i++) {
      if (key.equals(xpath.evaluate(path, nodeList.item(i))))
        return nodeList.item(i);
    }
    return null;
  }
  
  /**
   * Asserts that two objects are equal.  Null and "" are considered equal.
   */
  protected static void assertEqualsOrNull(Object obj1, Object obj2) {
    if (obj1 == null) obj1 = "";
    if (obj2 == null) obj2 = "";
    assertEquals(obj1, obj2);
  }
}
