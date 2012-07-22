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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.core.model.ActivityImpl;
import org.apache.shindig.social.core.model.AddressImpl;
import org.apache.shindig.social.core.model.ListFieldImpl;
import org.apache.shindig.social.core.model.MediaItemImpl;
import org.apache.shindig.social.core.model.NameImpl;
import org.apache.shindig.social.core.model.PersonImpl;
import org.apache.shindig.social.core.util.BeanXStreamAtomConverter;
import org.apache.shindig.social.core.util.xstream.XStream081Configuration;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.Person;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;
import java.util.Map;

public class BeanXStreamAtomConverterTest extends Assert {
  private Person johnDoe;
  private Activity activity;

  private BeanXStreamAtomConverter beanXmlConverter;

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

    MediaItemImpl mediaItem = new MediaItemImpl();
    mediaItem.setMimeType("image/jpg");
    mediaItem.setType(MediaItem.Type.IMAGE);
    mediaItem.setUrl("http://foo.bar");
    mediaItem.setLocation(new AddressImpl("Foo bar address"));
    mediaItem.setNumViews("10000");

    activity.setMediaItems(Lists.<MediaItem> newArrayList(mediaItem));
    activity.setUrl("http://foo.com");

    beanXmlConverter = new BeanXStreamAtomConverter(
        new XStream081Configuration(injector));
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
    String xml = beanXmlConverter.convertToString(johnDoe);
    Element element = XmlUtil.parse(xml);
    Node id = element.getElementsByTagName("id").item(0);
    assertEquals("urn:guid:" + johnDoe.getId(), id.getTextContent());
  }

  @Test
  public void testActivityToXml() throws Exception {
    String xml = beanXmlConverter.convertToString(activity);

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

    String expectedXml = "<feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:osearch=\"http://a9.com/-/spec/opensearch/1.1\" > "
        + " <entry><id>item1</id>"
        + "    <content type=\"application/xml\" ><entry><key>value</key><value>1</value></entry></content>"
        + " </entry> "
        + " <entry><id>item2</id>"
        + "     <content type=\"application/xml\" ><entry><key>value</key><value>2</value></entry></content>"
        + " </entry> "
        + " <osearch:startIndex>0</osearch:startIndex> "
        + " <osearch:totalResults>2</osearch:totalResults> "
        + " <osearch:itemsPerPage>2</osearch:itemsPerPage></feed> ";
    XMLUnit.setIgnoreWhitespace(true);
    XMLAssert.assertXMLEqual(expectedXml, xml);
  }

  @Test
  public void testMapToXml() throws Exception {
    Map<String, String> m = Maps.newLinkedHashMap();
    m.put("key1", "value1");
    m.put("key2", "value2");
    String xml = beanXmlConverter.convertToString(m);
    XmlUtil.parse(xml);
    String expectedXml = "<feed xmlns=\"http://www.w3.org/2005/Atom\" "
        + " xmlns:osearch=\"http://a9.com/-/spec/opensearch/1.1\">"
        + "  <entry><id>key1</id><content type=\"application/xml\" >"
        + "    <value>value1</value></content>"
        + "  </entry>"
        + "  <entry><id>key2</id>"
        + "     <content type=\"application/xml\" ><value>value2</value></content>"
        + "  </entry>"
        + "  <osearch:startIndex>0</osearch:startIndex>"
        + "  <osearch:totalResults>2</osearch:totalResults>"
        + "  <osearch:itemsPerPage>2</osearch:itemsPerPage></feed>";
    XMLUnit.setIgnoreWhitespace(true);
    XMLAssert.assertXMLEqual(expectedXml, xml);
  }

  @Test
  public void testEmptyList() throws Exception {
    List<String> empty = Lists.newArrayList();
    String xml = beanXmlConverter.convertToString(empty);
    XmlUtil.parse(xml);
    String expectedXml = "<feed xmlns=\"http://www.w3.org/2005/Atom\" "
        + "xmlns:osearch=\"http://a9.com/-/spec/opensearch/1.1\" >"
        + "<entry><content/></entry>"
        + "<osearch:startIndex>0</osearch:startIndex>"
        + "<osearch:totalResults>1</osearch:totalResults>"
        + "<osearch:itemsPerPage>1</osearch:itemsPerPage></feed>";
    XMLUnit.setIgnoreWhitespace(true);
    XMLAssert.assertXMLEqual(expectedXml, xml);

    List<List<String>> emptyLists = Lists.newArrayList();
    List<String> emptyList = Lists.newArrayList();
    emptyLists.add(emptyList);
    emptyLists.add(emptyList);
    emptyLists.add(emptyList);
    xml = beanXmlConverter.convertToString(emptyLists);
    XmlUtil.parse(xml);
    expectedXml = "<feed xmlns=\"http://www.w3.org/2005/Atom\" "
        + "xmlns:osearch=\"http://a9.com/-/spec/opensearch/1.1\" >"
        + "<entry><content><list/><list/><list/></content></entry>"
        + "<osearch:startIndex>0</osearch:startIndex>"
        + "<osearch:totalResults>1</osearch:totalResults>"
        + "<osearch:itemsPerPage>1</osearch:itemsPerPage></feed>";
    XMLUnit.setIgnoreWhitespace(true);
    XMLAssert.assertXMLEqual(expectedXml, xml);
  }

  @Test
  public void testElementNamesInList() throws Exception {
    List<Activity> activities = Lists.newArrayList();
    activities.add(activity);
    activities.add(activity);
    activities.add(activity);
    String xml = beanXmlConverter.convertToString(activities);
    XmlUtil.parse(xml);
    String expectedXml = "<feed xmlns=\"http://www.w3.org/2005/Atom\" "
        + "   xmlns:osearch=\"http://a9.com/-/spec/opensearch/1.1\"><entry><content>"
        + "  <activity xmlns=\"http://ns.opensocial.org/2008/opensocial\">"
        + "    <id>activityId</id>"
        + "    <mediaItems>"
        + "        <mimeType>image/jpg</mimeType>"
        + "        <type>IMAGE</type>"
        + "        <url>http://foo.bar</url>"
        + "        <location>"
        + "           <formatted>Foo bar address</formatted>"
        + "        </location>"
        + "        <numViews>10000</numViews>"
        + "    </mediaItems>"
        + "    <url>http://foo.com</url>"
        + "    <userId>johnDoeId</userId>"
        + "  </activity>"
        + "  <activity xmlns=\"http://ns.opensocial.org/2008/opensocial\">"
        + "    <id>activityId</id>"
        + "    <mediaItems>"
        + "        <mimeType>image/jpg</mimeType>"
        + "        <type>IMAGE</type>"
        + "        <url>http://foo.bar</url>"
        + "        <location>"
        + "           <formatted>Foo bar address</formatted>"
        + "        </location>"
        + "        <numViews>10000</numViews>"
        + "    </mediaItems>"
        + "    <url>http://foo.com</url>"
        + "    <userId>johnDoeId</userId>"
        + "  </activity>"
        + "  <activity xmlns=\"http://ns.opensocial.org/2008/opensocial\">"
        + "    <id>activityId</id>"
        + "    <mediaItems>"
        + "        <mimeType>image/jpg</mimeType>"
        + "        <type>IMAGE</type>"
        + "        <url>http://foo.bar</url>"
        + "        <location>"
        + "           <formatted>Foo bar address</formatted>"
        + "        </location>"
        + "        <numViews>10000</numViews>"
        + "    </mediaItems>"
        + "    <url>http://foo.com</url>"
        + "    <userId>johnDoeId</userId>"
        + "  </activity>"
        + "</content></entry>"
        + "<osearch:startIndex>0</osearch:startIndex>"
        + "<osearch:totalResults>1</osearch:totalResults>"
        + "<osearch:itemsPerPage>1</osearch:itemsPerPage>" + "</feed>";
    XMLUnit.setIgnoreWhitespace(true);
    XMLAssert.assertXMLEqual(expectedXml, xml);
  }
}
