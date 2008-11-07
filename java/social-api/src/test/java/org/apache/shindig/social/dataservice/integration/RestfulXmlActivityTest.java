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
import org.apache.shindig.social.opensocial.util.XSDValidator;

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

public class RestfulXmlActivityTest extends AbstractLargeRestfulTests {
  private static final String XMLSCHEMA = " xmlns=\"http://ns.opensocial.org/2008/opensocial\" \n"
    + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
    + " xsi:schemaLocation=\"http://ns.opensocial.org/2008/opensocial classpath:opensocial.xsd\" ";
  private static final String XSDRESOURCE = "opensocial.xsd";
  private Activity johnsActivity;
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
   * Expected response for an activity in xml:
   * 
   * <pre>
   * &lt;response&gt;
   *    &lt;activity&gt;
   *       &lt;id&gt;1&lt;/id&gt;
   *       &lt;userId&gt;john.doe&lt;/userId&gt;
   *       &lt;title&gt;yellow&lt;/title&gt;
   *       &lt;body&gt;body&lt;/body&gt;
   *    &lt;/activity&gt;
   * &lt;/response&gt;
   * </pre>
   * 
   * @throws Exception
   *           if test encounters an error
   */
  @Test
  public void testGetActivityJson() throws Exception {
    String resp = getResponse("/activities/john.doe/@self/@app/1", "GET",
        "xml", "application/xml");

    XSDValidator.validate(resp, XMLSCHEMA, XSDRESOURCE,false);
    
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
   * <pre>
   * &lt;response xmlns=&quot;http://ns.opensocial.org/2008/opensocial&quot;
   *    xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot;
   *    xsi:schemaLocation=&quot;http://ns.opensocial.org/2008/opensocial file:/Users/ieb/Apache/shindig/trunk/java/social-api/src/test/resources/org/apache/shindig/social/opensocial/util/opensocial.xsd&quot;&gt;
   *   &lt;activity&gt;
   *     &lt;itemsPerPage&gt;10&lt;/itemsPerPage&gt;
   *     &lt;startIndex&gt;0&lt;/startIndex&gt;
   *     &lt;totalResults&gt;1&lt;/totalResults&gt;
   *     &lt;entry&gt;
   *       &lt;appId&gt;&lt;/appId&gt;
   *       &lt;body&gt;&lt;/body&gt;
   *       &lt;bodyId&gt;&lt;/bodyId&gt;
   *       &lt;externalId&gt;&lt;/externalId&gt;
   *       &lt;id&gt;&lt;/id&gt;
   *       &lt;mediaItems&gt;
   *         &lt;mimeType&gt;&lt;/mimeType&gt;
   *         &lt;type&gt;&lt;/type&gt;
   *         &lt;url&gt;&lt;/url&gt;
   *       &lt;/mediaItems&gt;
   *       &lt;postedTime&gt;&lt;/postedTime&gt;
   *       &lt;priority&gt;&lt;/priority&gt;
   *       &lt;streamFaviconUrl&gt;&lt;/streamFaviconUrl&gt;
   *       &lt;streamSourceUrl&gt;&lt;/streamSourceUrl&gt;
   *       &lt;streamTitle&gt;&lt;/streamTitle&gt;
   *       &lt;streamUrl&gt;&lt;/streamUrl&gt;
   *       &lt;title&gt;&lt;/title&gt;
   *       &lt;titleId&gt;&lt;/titleId&gt;
   *       &lt;url&gt;&lt;/url&gt;
   *       &lt;userId&gt;&lt;/userId&gt;
   *     &lt;/entry&gt;
   *   &lt;/activity&gt;
   * &lt;/response&gt;
   * </pre>
   * 
   * @throws Exception
   *           if test encounters an error
   */
  @Test
  public void testGetActivitiesJson() throws Exception {
    String resp = getResponse("/activities/john.doe/@self", "GET", "xml",
        "application/xml");
    XSDValidator.validate(resp, XMLSCHEMA, XSDRESOURCE,false);
    
    XPath xp = xpathFactory.newXPath();
    assertEquals("0", xp.evaluate("/response/startIndex", new InputSource(
        new StringReader(resp))));
    assertEquals("1", xp.evaluate("/response/totalResults", new InputSource(
        new StringReader(resp))));
    NodeList nl = (NodeList) xp.evaluate("/response/entry/activity",
        new InputSource(new StringReader(resp)), XPathConstants.NODESET);
    assertEquals(1, nl.getLength());

    assertActivitiesEqual(johnsActivity, childNodesToMap(nl.item(0)));
  }

  /**
   * Expected response for a list of activities in json:
   * 
   * 
   * <pre>
   * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
   * &lt;response xmlns=&quot;http://ns.opensocial.org/2008/opensocial&quot;
   *    xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot;
   *    xsi:schemaLocation=&quot;http://ns.opensocial.org/2008/opensocial file:/Users/ieb/Apache/shindig/trunk/java/social-api/src/test/resources/org/apache/shindig/social/opensocial/util/opensocial.xsd&quot;&gt;
   *   &lt;activity&gt;
   *     &lt;itemsPerPage&gt;3&lt;/itemsPerPage&gt;
   *     &lt;startIndex&gt;0&lt;/startIndex&gt;
   *     &lt;totalResults&gt;10&lt;/totalResults&gt;
   *     &lt;entry&gt;
   *       &lt;appId&gt;&lt;/appId&gt;
   *       &lt;body&gt;&lt;/body&gt;
   *       &lt;bodyId&gt;&lt;/bodyId&gt;
   *       &lt;externalId&gt;&lt;/externalId&gt;
   *       &lt;id&gt;&lt;/id&gt;
   *       &lt;mediaItems&gt;
   *         &lt;mimeType&gt;&lt;/mimeType&gt;
   *         &lt;type&gt;&lt;/type&gt;
   *         &lt;url&gt;&lt;/url&gt;
   *       &lt;/mediaItems&gt;
   *       &lt;postedTime&gt;&lt;/postedTime&gt;
   *       &lt;priority&gt;&lt;/priority&gt;
   *       &lt;streamFaviconUrl&gt;&lt;/streamFaviconUrl&gt;
   *       &lt;streamSourceUrl&gt;&lt;/streamSourceUrl&gt;
   *       &lt;streamTitle&gt;&lt;/streamTitle&gt;
   *       &lt;streamUrl&gt;&lt;/streamUrl&gt;
   *       &lt;title&gt;&lt;/title&gt;
   *       &lt;titleId&gt;&lt;/titleId&gt;
   *       &lt;url&gt;&lt;/url&gt;
   *       &lt;userId&gt;&lt;/userId&gt;
   *     &lt;/entry&gt;
   *   &lt;/activity&gt;
   * &lt;/response&gt;
   * </pre>
   * 
   * 
   * @throws Exception
   *           if test encounters an error
   */
  @Test
  public void testGetFriendsActivitiesJson() throws Exception {
    String resp = getResponse("/activities/john.doe/@friends", "GET", "xml",
        "application/xml");

    XSDValidator.validate(resp, XMLSCHEMA, XSDRESOURCE,false);
 
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
    String postData = XSDValidator.XMLDEC+"<activity><body>and dad.</body><title>hi mom!</title></activity>";
    String createResponse = getResponse("/activities/john.doe/@self", "POST",
        postData, "xml", "application/xml");

    XSDValidator.validate(createResponse, XMLSCHEMA, XSDRESOURCE,false);

    String resp = getResponse("/activities/john.doe/@self", "GET", "xml",
        "application/xml");
    
    XSDValidator.validate(resp, XMLSCHEMA, XSDRESOURCE,false);

    
    XPath xp = xpathFactory.newXPath();
    assertEquals("0", xp.evaluate("/response/startIndex", new InputSource(
        new StringReader(resp))));
    assertEquals("2", xp.evaluate("/response/totalResults", new InputSource(
        new StringReader(resp))));
    NodeList nl = (NodeList) xp.evaluate("/response/entry/activity", new InputSource(
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