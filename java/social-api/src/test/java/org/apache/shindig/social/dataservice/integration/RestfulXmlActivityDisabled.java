/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.shindig.social.dataservice.integration;

import org.apache.shindig.social.core.model.ActivityImpl;
import org.apache.shindig.social.opensocial.model.Activity;

import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

public class RestfulXmlActivityDisabled extends AbstractLargeRestfulTests {
  Activity johnsActivity;
  private XPathFactory xpathFactory;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    johnsActivity = new ActivityImpl("1", "john.doe");
    johnsActivity.setTitle("yellow");
    johnsActivity.setBody("what a color!");

    xpathFactory = XPathFactory.newInstance();

  }

  /**
   * Expected response for an activity in xml: <map> <entry> <key>entry</key>
   * <value> <id>1</id> <userId>john.doe</userId> <title>yellow</title>
   * <body>what a color!</body> </value> </entry> </map>
   *
   * @throws Exception
   *           if test encounters an error
   */
  @Test
  public void testGetActivityJson() throws Exception {
    String resp = getResponse("/activities/john.doe/@self/@app/1", "GET",
        "xml", "application/xml");
    InputSource source = new InputSource(new StringReader(resp));
    XPath xp = xpathFactory.newXPath();
    NodeList result = (NodeList) xp.evaluate("/response/activity", source,
        XPathConstants.NODESET);
    assertEquals(1, result.getLength());
    Node n = result.item(0);

    Map<String, List<String>> v = childNodesToMap(n);

    assertEquals(4, v.size());
    assertActivitiesEqual(johnsActivity, v);
  }

  /**
   * Expected response for a list of activities in json:
   *
   * { "totalResults" : 1, "startIndex" : 0 "itemsPerPage" : 10 // Note: the js
   * doesn't support paging. Should rest? "entry" : [ {<activity>} // layed out
   * like above ] }
   *
   * @throws Exception
   *           if test encounters an error
   */
  @Test
  public void testGetActivitiesJson() throws Exception {
    String resp = getResponse("/activities/john.doe/@self", "GET", "xml",
        "application/xml");
    System.err.println("Got " + resp);
    XPath xp = xpathFactory.newXPath();
    assertEquals("0", xp.evaluate("/response/startIndex", new InputSource(
        new StringReader(resp))));
    assertEquals("1", xp.evaluate("/response/totalResults", new InputSource(
        new StringReader(resp))));
    NodeList nl = (NodeList) xp.evaluate("/response/entry", new InputSource(
        new StringReader(resp)), XPathConstants.NODESET);
    assertEquals(1, nl.getLength());

    assertActivitiesEqual(johnsActivity, childNodesToMap(nl.item(0)));
  }

  /**
   * Expected response for a list of activities in json:
   *
   * { "totalResults" : 3, "startIndex" : 0 "itemsPerPage" : 10 // Note: the js
   * doesn't support paging. Should rest? "entry" : [ {<activity>} // layed out
   * like above, except for jane.doe ] }
   *
   * @throws Exception
   *           if test encounters an error
   */
  @Test
  public void testGetFriendsActivitiesJson() throws Exception {
    String resp = getResponse("/activities/john.doe/@friends", "GET", "xml",
        "application/xml");
    System.err.println("Got " + resp);

    XPath xp = xpathFactory.newXPath();
    assertEquals("0", xp.evaluate("/response/startIndex", new InputSource(
        new StringReader(resp))));
    assertEquals("2", xp.evaluate("/response/totalResults", new InputSource(
        new StringReader(resp))));
    NodeList nl = (NodeList) xp.evaluate("/response/entry", new InputSource(
        new StringReader(resp)), XPathConstants.NODESET);
    assertEquals(2, nl.getLength());

  }

  private void assertActivitiesEqual(Activity activity,
      Map<String, List<String>> result) {
    assertEquals(activity.getId(), result.get("id").get(0));
    assertEquals(activity.getUserId(), result.get("userId").get(0));
    assertEquals(activity.getTitle(), result.get("title").get(0));
    assertEquals(activity.getBody(), result.get("body").get(0));
  }

  @Test
  public void testCreateActivity() throws Exception {
    String postData = "{title : 'hi mom!', body : 'and dad.'}";
    String createResponse = getResponse("/activities/john.doe/@self", "POST",
        postData, "xml", "application/xml");
    System.err.println("Got " + createResponse);

    String resp = getResponse("/activities/john.doe/@self", "GET", "xml",
        "application/xml");
    System.err.println("Got " + resp);

    XPath xp = xpathFactory.newXPath();
    assertEquals("0", xp.evaluate("/response/startIndex", new InputSource(
        new StringReader(resp))));
    assertEquals("2", xp.evaluate("/response/totalResults", new InputSource(
        new StringReader(resp))));
    NodeList nl = (NodeList) xp.evaluate("/response/entry", new InputSource(
        new StringReader(resp)), XPathConstants.NODESET);
    assertEquals(2, nl.getLength());

    Map<String, List<String>> v = childNodesToMap(nl.item(0));
    if (v.containsKey("id")) {
      v = childNodesToMap(nl.item(1));
    }

    assertEquals("hi mom!", v.get("title").get(0));
    assertEquals("and dad.", v.get("body").get(0));
  }

  // TODO: Add tests for the fields= parameter
}