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
package org.apache.shindig.social.opensocial.util;

import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.social.core.model.ActivityImpl;
import org.apache.shindig.social.core.model.AddressImpl;
import org.apache.shindig.social.core.model.ListFieldImpl;
import org.apache.shindig.social.core.model.MediaItemImpl;
import org.apache.shindig.social.core.model.NameImpl;
import org.apache.shindig.social.core.model.PersonImpl;
import org.apache.shindig.social.core.util.BeanXmlConverter;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.Person;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.TreeMap;

public class BeanXmlConverterTest extends TestCase {
  private Person johnDoe;
  private Activity activity;

  private BeanXmlConverter beanXmlConverter;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    johnDoe = new PersonImpl("johnDoeId", "Johnny", new NameImpl("John Doe"));
    johnDoe.setPhoneNumbers(Lists.<ListField>newArrayList(
        new ListFieldImpl("home", "+33H000000000"),
        new ListFieldImpl("mobile", "+33M000000000"),
        new ListFieldImpl("work", "+33W000000000")));

    johnDoe.setAddresses(Lists.<Address>newArrayList(new AddressImpl("My home address")));

    johnDoe.setEmails(Lists.<ListField>newArrayList(
        new ListFieldImpl("work", "john.doe@work.bar"),
        new ListFieldImpl("home", "john.doe@home.bar")));

    activity = new ActivityImpl("activityId", johnDoe.getId());

    activity.setMediaItems(Lists.<MediaItem>newArrayList(
        new MediaItemImpl("image/jpg", MediaItem.Type.IMAGE, "http://foo.bar")));

    beanXmlConverter = new BeanXmlConverter();
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

  public void testToXmlOnSimpleClass() throws Exception {
    SimplePerson cassie = new SimplePerson("5", "robot");
    String xml = beanXmlConverter.convertToXml(cassie);

    Element element = XmlUtil.parse(xml);
    Node id = element.getElementsByTagName("id").item(0);
    Node name = element.getElementsByTagName("name").item(0);

    assertEquals("5", id.getTextContent());
    assertEquals("robot", name.getTextContent());
  }

  public void testPersonToXml() throws Exception {
    String xml = beanXmlConverter.convertToXml(johnDoe);
    // TODO: Make the person xml stop returning empty elements!
    // TODO: Flush out the test to check all the sub fields
    Element element = XmlUtil.parse(xml);
    Node id = element.getElementsByTagName("id").item(0);
    assertEquals(johnDoe.getId(), id.getTextContent());
  }

  public void testActivityToXml() throws Exception {
    String xml = beanXmlConverter.convertToXml(activity);
    // TODO: Make the activity xml stop returning empty elements!
    // TODO: Flush out the test to check all the sub fields
    Element element = XmlUtil.parse(xml);
    Node id = element.getElementsByTagName("id").item(0);
    assertEquals(activity.getId(), id.getTextContent());
  }

  public void xxxtestMapsToXml() throws Exception {
    // This is the structure our app data currently takes
    Map<String, Map<String, String>> map = Maps.newTreeMap();
    Map<String, String> item1Map = ImmutableMap.of("value","1");
    map.put("item1", item1Map);

    Map<String, String> item2Map = ImmutableMap.of("value", "2");
    map.put("item2", item2Map);

    String xml = beanXmlConverter.convertToXml(map);

    // TODO: Change this test to use parsing once we have the right format
    XmlUtil.parse(xml);

    // TODO: I don't believe this is the output we are looking for for app
    // data... we will probably have to tweak this.
    String expectedXml =
        "<treemap>" +
        "<empty>false</empty>" +
        "<entry>" +
          "<key>item1</key>" +
          "<value>" +
            "<empty>false</empty>" +
            "<entry>" +
              "<key>value</key>" +
              "<value>1</value>" +
            "</entry>" +
          "</value>" +
        "</entry>" +
        "<entry>" +
          "<key>item2</key>" +
          "<value>" +
            "<empty>false</empty>" +
            "<entry>" +
              "<key>value</key>" +
              "<value>2</value>" +
            "</entry>" +
          "</value>" +
        "</entry>" +
        "</treemap>";
    assertEquals(expectedXml, StringUtils.deleteWhitespace(xml));
  }

}
