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

import com.google.common.collect.Maps;

import org.junit.Test;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

public class RestfulXmlDataDisabled extends AbstractLargeRestfulTests {

  private XPathFactory xpathFactory;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    xpathFactory = XPathFactory.newInstance();

  }

  /**
   * Expected response for app data in json:
   *
   * {
   * "entry" : {
   * "jane.doe" : {"count" : "7"},
   * "george.doe" : {"count" : "2"},
   * "maija.m" : {}, // TODO: Should this entry really be included if she
   * doesn't have any data? } }
   *
   * @throws Exception
   *           if test encounters an error
   */
  @Test
  public void testGetFriendsAppDataJson() throws Exception {
    // app id is mocked out
    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("fields", "count");
    String resp = getResponse("/appdata/john.doe/@friends/app", "GET",
        extraParams, "xml", "application/xml");
    
    System.err.println("Got Response \n"+resp);

    XPath xp = xpathFactory.newXPath();
    NodeList result = (NodeList) xp.evaluate("/appdata/entry", new InputSource(
        new StringReader(resp)), XPathConstants.NODESET);
    assertEquals(3, result.getLength());

    Map<String, Map<String, List<String>>> v = childNodesToMapofMap(result);

    assertEquals(3, v.size());
    assertTrue(v.containsKey("jane.doe"));
    assertTrue(v.containsKey("george.doe"));
    assertTrue(v.containsKey("maija.m"));

    assertEquals(1, v.get("jane.doe").size());
    assertEquals(1, v.get("george.doe").size());
    assertEquals(0, v.get("maija.m").size());

    assertEquals("7", v.get("jane.doe").get("count").get(0));
    assertEquals("2", v.get("george.doe").get("count").get(0));
  }

  /**
   * Expected response for app data in json:
   *
   * { "entry" : {
   * "john.doe" : {"count" : "0"}, } }
   *
   * @throws Exception
   *           if test encounters an error
   */
  @Test
  public void testGetSelfAppDataJson() throws Exception {
    // app id is mocked out
    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("fields", null);
    String resp = getResponse("/appdata/john.doe/@self/app", "GET",
        extraParams, "xml", "application/xml");

    System.err.println("Got Response \n"+resp);

    XPath xp = xpathFactory.newXPath();
    NodeList result = (NodeList) xp.evaluate("/appdata/entry", new InputSource(
        new StringReader(resp)), XPathConstants.NODESET);

    Map<String, Map<String, List<String>>> v = childNodesToMapofMap(result);

    assertEquals(1, v.size());
    assertTrue(v.containsKey("john.doe"));

    assertEquals(1, v.get("john.doe").size());

    assertEquals("0", v.get("john.doe").get("count").get(0));

  }

  /**
   * Expected response for app data in json:
   *
   * { "entry" : { "john.doe" : {"count" : "0"}, } }
   *
   * @throws Exception
   *           if test encounters an error
   */
  @Test
  public void testGetSelfAppDataJsonWithKey() throws Exception {
    // app id is mocked out
    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("fields", "count");
    String resp = getResponse("/appdata/john.doe/@self/app", "GET",
        extraParams, "xml", "application/xml");

    System.err.println("Got Response \n"+resp);

    XPath xp = xpathFactory.newXPath();
    NodeList result = (NodeList) xp.evaluate("/appdata/entry", new InputSource(
        new StringReader(resp)), XPathConstants.NODESET);

    Map<String, Map<String, List<String>>> v = childNodesToMapofMap(result);

    assertEquals(1, v.size());
    assertTrue(v.containsKey("john.doe"));

    assertEquals(1, v.get("john.doe").size());

    assertEquals("0", v.get("john.doe").get("count").get(0));
  }

  /**
   * Expected response for app data in json with non-existant key: TODO: Double
   * check this output with the spec
   *
   * { "entry" : { "john.doe" : {}, } }
   *
   * @throws Exception
   *           if test encounters an error
   */
  @Test
  public void testGetSelfAppDataJsonWithInvalidKeys() throws Exception {
    // app id is mocked out
    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("fields", "peabody");
    String resp = getResponse("/appdata/john.doe/@self/app", "GET",
        extraParams, "xml", "application/xml");

    System.err.println("Got Response \n"+resp);

    XPath xp = xpathFactory.newXPath();
    NodeList result = (NodeList) xp.evaluate("/appdata/entry", new InputSource(
        new StringReader(resp)), XPathConstants.NODESET);

    Map<String, Map<String, List<String>>> v = childNodesToMapofMap(result);

    assertEquals(1, v.size());
    assertTrue(v.containsKey("john.doe"));

    assertEquals(0, v.get("john.doe").size());
  }

  @Test
  public void testDeleteAppData() throws Exception {
    assertCount("0");

    // With the wrong field
    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("fields", "peabody");
    String resp = getResponse("/appdata/john.doe/@self/app", "DELETE", extraParams, "xml",
        "application/xml");

    System.err.println("Got Response \n"+resp);

    assertCount("0");

    // should be xml ?
    extraParams.put("fields", "count");
    getResponse("/appdata/john.doe/@self/app", "DELETE", extraParams, "xml",
        "application/xml");

    assertCount(null);
  }

  @Test
  public void testUpdateAppData() throws Exception {
    assertCount("0");

    Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("fields", "count");
    // should be xml ?
    String postData = "{count : 5}";
    String resp = getResponse("/appdata/john.doe/@self/app", "POST", extraParams, postData,
        "xml", "application/xml");
    
    System.err.println("Got Response \n"+resp);

    assertCount("5");
  }

  private void assertCount(String expectedCount) throws Exception {
    String resp = getResponse("/appdata/john.doe/@self/app", "GET", "xml",
        "application/xml");
    
    System.err.println("Got Response \n"+resp);


    XPath xp = xpathFactory.newXPath();
    NodeList result = (NodeList) xp.evaluate("/appdata/entry", new InputSource(
        new StringReader(resp)), XPathConstants.NODESET);

    Map<String, Map<String, List<String>>> v = childNodesToMapofMap(result);

    assertEquals(1, v.size());
    assertTrue(v.containsKey("john.doe"));

    if (expectedCount != null) {
      assertEquals(1, v.get("john.doe").size());

      assertEquals(String.valueOf(expectedCount), v.get("john.doe")
          .get("count").get(0));
    } else {
      assertEquals(0, v.get("john.doe").size());

    }
  }

  // TODO: support for indexBy??

}