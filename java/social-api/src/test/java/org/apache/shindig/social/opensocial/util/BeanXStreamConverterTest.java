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
package org.apache.shindig.social.opensocial.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.protocol.conversion.BeanXStreamConverter;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.core.model.ActivityImpl;
import org.apache.shindig.social.core.model.AddressImpl;
import org.apache.shindig.social.core.model.ListFieldImpl;
import org.apache.shindig.social.core.model.MediaItemImpl;
import org.apache.shindig.social.core.model.NameImpl;
import org.apache.shindig.social.core.model.PersonImpl;
import org.apache.shindig.social.core.util.xstream.XStream081Configuration;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.Person;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public class BeanXStreamConverterTest extends Assert {
  private static final String XMLSCHEMA =
      " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
      + " xsi:schemaLocation=\"http://ns.opensocial.org/2008/opensocial classpath:opensocial.xsd\" ";
  private Person johnDoe;
  private Activity activity;

  private BeanXStreamConverter beanXmlConverter;

  @Before
  public void setUp() throws Exception {
    Injector injector = Guice.createInjector(new SocialApiTestsGuiceModule());

    johnDoe = new PersonImpl("johnDoeId", "Johnny", new NameImpl("John Doe"));
    johnDoe.setPhoneNumbers(Lists.<ListField> newArrayList(new ListFieldImpl(
        "home", "+33H000000000"), new ListFieldImpl("mobile", "+33M000000000"),
        new ListFieldImpl("work", "+33W000000000")));

    johnDoe.setAddresses(Lists.<Address> newArrayList(new AddressImpl(
        "My home address")));

    johnDoe.setEmails(Lists.<ListField> newArrayList(new ListFieldImpl("work",
        "john.doe@work.bar"), new ListFieldImpl("home", "john.doe@home.bar")));

    activity = new ActivityImpl("activityId", johnDoe.getId());
    activity.setUrl("http://foo.com/");

    activity.setMediaItems(Lists.<MediaItem> newArrayList(new MediaItemImpl(
        "image/jpg", MediaItem.Type.IMAGE, "http://foo.bar")));


    beanXmlConverter = new BeanXStreamConverter(new XStream081Configuration(injector));
  }


  public static class SimplePerson {
    private String id;
    private String name;

    public SimplePerson(String id, String name) {
      this.id = id;
      this.name = name;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }
  }

  @Test
  public void testToXmlOnSimpleClass() throws Exception {
    // since this doent implement the model, it wont get mapped correctly, hence
    // we cant validate
    SimplePerson cassie = new SimplePerson("5", "robot");
    String xml = beanXmlConverter.convertToString(cassie);
    Element element = XmlUtil.parse(xml);
    Node id = element.getElementsByTagName("id").item(0);
    Node name = element.getElementsByTagName("name").item(0);

    assertEquals("5", id.getTextContent());
    assertEquals("robot", name.getTextContent());
  }

  @Test
  public void testPersonToXml() throws Exception {
    String xml = XSDValidator.validateOpenSocial(beanXmlConverter.convertToString(johnDoe));
    Element element = XmlUtil.parse(xml);
    Node id = element.getElementsByTagName("id").item(0);
    assertEquals(johnDoe.getId(), id.getTextContent());
  }

  @Test
  public void testActivityToXml() throws Exception {
    String xml = XSDValidator.validateOpenSocial(beanXmlConverter.convertToString(activity));

    Element element = XmlUtil.parse(xml);
    Node id = element.getElementsByTagName("id").item(0);
    assertEquals(activity.getId(), id.getTextContent());
  }

  @Test
  public void testMapsToXml() throws Exception {
    // This is the structure our app data currently takes
    Map<String, Map<String, String>> map = Maps.newTreeMap();

    Map<String, String> item1Map = Maps.newHashMap();
    item1Map.put("value", "1");
    map.put("item1", item1Map);

    Map<String, String> item2Map = Maps.newHashMap();
    item2Map.put("value", "2");
    map.put("item2", item2Map);

    String xml = beanXmlConverter.convertToString(map);

    XmlUtil.parse(xml);

    String expectedXml = XSDValidator.XMLDEC + "<response xmlns=\"http://ns.opensocial.org/2008/opensocial\"><map>"
        + "  <entry><key>item1</key><value><entry><key>value</key><value>1</value></entry></value></entry> "
        + "  <entry><key>item2</key><value><entry><key>value</key><value>2</value></entry></value></entry> "
        + "</map></response>";
    assertEquals(StringUtils.deleteWhitespace(expectedXml), StringUtils
        .deleteWhitespace(xml));
  }

  @Test
  public void testMapToXml() throws XmlException {
    Map<String, String> m = Maps.newLinkedHashMap();
    m.put("key1", "value1");
    m.put("key2", "value2");
    String xml = beanXmlConverter.convertToString(m);
    XmlUtil.parse(xml);
    String expectedXml = XSDValidator.XMLDEC + "<response xmlns=\"http://ns.opensocial.org/2008/opensocial\"><map>"
        + "  <entry><key>key1</key><value>value1</value></entry> "
        + "  <entry><key>key2</key><value>value2</value></entry> "
        + "</map></response>";
    assertEquals(StringUtils.deleteWhitespace(expectedXml), StringUtils
        .deleteWhitespace(xml));
  }

  @Test
  public void testEmptyList() throws XmlException {
    List<String> empty = Lists.newArrayList();
    String xml = beanXmlConverter.convertToString(empty);
    XmlUtil.parse(xml);
    String expectedXml = XSDValidator.XMLDEC + "<response xmlns=\"http://ns.opensocial.org/2008/opensocial\"><list/></response>";
    assertEquals(StringUtils.deleteWhitespace(expectedXml), StringUtils
        .deleteWhitespace(xml));

    List<List<String>> emptyLists = Lists.newArrayList();
    List<String> emptyList = Lists.newArrayList();
    emptyLists.add(emptyList);
    emptyLists.add(emptyList);
    emptyLists.add(emptyList);
    xml = beanXmlConverter.convertToString(emptyLists);
    XmlUtil.parse(xml);
    expectedXml = XSDValidator.XMLDEC + "<response xmlns=\"http://ns.opensocial.org/2008/opensocial\"><list.container>" + "  <list/>" + "  <list/>"
        + "  <list/>" + "</list.container></response>";
    assertEquals(StringUtils.deleteWhitespace(expectedXml), StringUtils
        .deleteWhitespace(xml));
  }

  @Test
  public void testElementNamesInList() throws Exception {

    List<Activity> activities = Lists.newArrayList();
    activities.add(activity);
    activities.add(activity);
    activities.add(activity);
    String xml = XSDValidator.validateOpenSocial(beanXmlConverter.convertToString(activities));

    // This test is a bit bogus and relies on some odd voodoo in the bundled opensocial.xsd
    XmlUtil.parse(xml);
    String expectedXml = "<response xmlns=\"http://ns.opensocial.org/2008/opensocial\">"
        + "<list.container>"
        + "  <activity xmlns=\"http://ns.opensocial.org/2008/opensocial\">"
        + "    <id>activityId</id>"
        + "    <mediaItems>"
        + "        <mimeType>image/jpg</mimeType>"
        + "        <type>IMAGE</type>"
        + "        <url>http://foo.bar</url>"
        + "    </mediaItems>"
        + "    <url>http://foo.com/</url>"
        + "    <userId>johnDoeId</userId>"
        + "  </activity>"
        + "  <activity xmlns=\"http://ns.opensocial.org/2008/opensocial\">"
        + "    <id>activityId</id>"
        + "    <mediaItems>"
        + "        <mimeType>image/jpg</mimeType>"
        + "        <type>IMAGE</type>"
        + "        <url>http://foo.bar</url>"
        + "    </mediaItems>"
        + "    <url>http://foo.com/</url>"
        + "    <userId>johnDoeId</userId>"
        + "  </activity>"
        + "  <activity xmlns=\"http://ns.opensocial.org/2008/opensocial\">"
        + "    <id>activityId</id>"
        + "    <mediaItems>"
        + "        <mimeType>image/jpg</mimeType>"
        + "        <type>IMAGE</type>"
        + "        <url>http://foo.bar</url>"
        + "    </mediaItems>"
        + "    <url>http://foo.com/</url>"
        + "    <userId>johnDoeId</userId>"
        + "  </activity>"
        + "</list.container>"
        + "</response>";
    expectedXml = XSDValidator.insertSchema(expectedXml, XMLSCHEMA, true);
    XMLUnit.setIgnoreWhitespace(true);
    XMLAssert.assertXMLEqual(expectedXml, xml);
  }

  @Test
  public void testPerson1() throws Exception {
    String xml = loadXML("testxml/person1.xml");
    beanXmlConverter.convertToObject(xml, Person.class);
  }

  @Test
  public void testActivity1() throws Exception {
    String xml = loadXML("testxml/activity1.xml");
    beanXmlConverter.convertToObject(xml, Activity.class);
  }

  @Test
  public void testAppdata1() throws Exception {
    String xml = loadXML("testxml/appdata1.xml");
    beanXmlConverter.convertToObject(xml, Map.class);
  }

  @Test
  @Ignore("TODO")
  public void testGroup1() throws XmlException {
    // TODO
  }

  /**
   * @param resource
   * @return a string
   * @throws IOException
   */
  private String loadXML(String resource) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(this
        .getClass().getResourceAsStream(resource)));
    StringBuilder sb = new StringBuilder();
    for (String line = in.readLine(); line != null; line = in.readLine()) {
      sb.append(line);
    }
    in.close();
    return sb.toString();
  }

}
