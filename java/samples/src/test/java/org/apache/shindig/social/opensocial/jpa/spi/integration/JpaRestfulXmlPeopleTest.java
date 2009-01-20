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

package org.apache.shindig.social.opensocial.jpa.spi.integration;

import org.apache.shindig.social.dataservice.integration.RestfulXmlPeopleTest;
import org.apache.shindig.social.opensocial.jpa.spi.SpiDatabaseBootstrap;
import org.apache.shindig.social.opensocial.util.XSDValidator;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.common.collect.Maps;
import com.google.inject.Injector;

/**
 * JPA restful Json people test, which wraps around shindig's <code>RestfulXmlPeopleTest</code>
 * but uses the JPA PersonService implementation <code>PersonServiceDb</code>
 */
public class JpaRestfulXmlPeopleTest extends RestfulXmlPeopleTest {
  
  // TODO Remove once jira issue 778 (see below) has been fixed.
  private XPathFactory xpathFactory = XPathFactory.newInstance();
  
  /** The bootstrap. */
  private SpiDatabaseBootstrap bootstrap;
  
  /**
   * Calls super.setup so to initialise servlet and easy mock objects.
   * Note that super.setup (i.e. AbstractLargeRestfulTests) also injects SocialApiTestsGuiceModule,
   * which will be overriden here to use the JPA guice bindings
   * 
   * @throws Exception the exception
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    // Init config
    Injector injector = JpaRestfulTestConfigHelper.init();
    this.setServlet(JpaRestfulTestConfigHelper.getDataServiceServlet(injector));
    
    // Bootstrap hibernate and associated test db, and setup db with test data
    this.bootstrap = injector.getInstance(SpiDatabaseBootstrap.class);
    this.bootstrap.init();
  }
  
  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    this.bootstrap.tearDown();
  }
  
  @Test
  public void testGetPersonJson() throws Exception {
    
  }
  
  @Test
  public void testGetPeople() throws Exception {
    
  }
	
  /**
   * TODO Overriding this test because of an existing jira issue
   * (https://issues.apache.org/jira/browse/SHINDIG-778)
   * where totalResults should be the same as specified in the count parameter.
   * Remove once issue 778 has been fixed.
   */
	@Test
  public void testGetPeoplePagination() throws Exception {
	  Map<String, String> extraParams = Maps.newHashMap();
    extraParams.put("sortBy", "name");
    extraParams.put("sortOrder", null);
    extraParams.put("filterBy", null);
    extraParams.put("startIndex", "0");
    extraParams.put("count", "1");
    extraParams.put("fields", null);

    String resp = getResponse("/people/john.doe/@friends", "GET", extraParams,
        "xml", "application/xml");

    XSDValidator.validate(resp, XMLSCHEMA, XSDRESOURCE,false);

    XPath xp = xpathFactory.newXPath();
    NodeList resultNodeList = (NodeList) xp.evaluate("/response",
        new InputSource(new StringReader(resp)), XPathConstants.NODESET);
    assertEquals(1, resultNodeList.getLength());

    Map<String, List<String>> result = childNodesToMap(resultNodeList.item(0));
    Map<String, List<Node>> resultNodes = childNodesToNodeMap(resultNodeList
        .item(0));

    assertEquals("1", result.get("totalResults").get(0));
    assertEquals("0", result.get("startIndex").get(0));

    Map<String, List<Node>> entryOne = childNodesToNodeMap(resultNodes.get("entry").get(0));
    
    assertPerson(childNodesToNodeMap(entryOne.get("person").get(0)),
        "george.doe", "George Doe");

    // Get the second page
    extraParams.put("startIndex", "1");
    resp = getResponse("/people/john.doe/@friends", "GET", extraParams, "xml",
        "application/xml");
    
    XSDValidator.validate(resp, XMLSCHEMA, XSDRESOURCE,false);

    xp = xpathFactory.newXPath();
    resultNodeList = (NodeList) xp.evaluate("/response", new InputSource(
        new StringReader(resp)), XPathConstants.NODESET);
    assertEquals(1, resultNodeList.getLength());

    result = childNodesToMap(resultNodeList.item(0));
    resultNodes = childNodesToNodeMap(resultNodeList.item(0));

    assertEquals("1", result.get("totalResults").get(0));
    assertEquals("1", result.get("startIndex").get(0));

    Map<String, List<Node>> entryTwo = childNodesToNodeMap(resultNodes.get("entry").get(0));
    assertPerson(childNodesToNodeMap(entryTwo.get("person").get(0)),
        "jane.doe", "Jane Doe");
  }
	
	/**
	 * TODO Remove once jira issue 778 (see above) has been fixed.
	 */
	private void assertPerson(Map<String, List<Node>> person, String expectedId,
      String expectedName) throws Exception {
    assertEquals(expectedId, person.get("id").get(0).getTextContent());
    assertEquals(expectedName, childNodesToMap(person.get("name").get(0)).get(
        "unstructured").get(0));
  }

}
